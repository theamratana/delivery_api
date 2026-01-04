package com.delivery.deliveryapi.service.base;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abstract base service providing default CRUD implementations.
 * Child services just need to extend this and inject their repository.
 * 
 * The entity must have a 'companyId' field for multi-tenant isolation.
 * For soft delete, the entity should have an 'active' boolean field.
 * 
 * @param <T> Entity type
 * @param <R> Repository type extending JpaRepository
 */
public abstract class BaseServiceImpl<T, R extends JpaRepository<T, UUID>> implements BaseService<T> {

    protected final R repository;

    public BaseServiceImpl(R repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<T> list(UUID companyId, Pageable pageable) {
        // Use Specification to filter by company and exclude deleted records
        if (repository instanceof org.springframework.data.jpa.repository.JpaSpecificationExecutor) {
            org.springframework.data.jpa.domain.Specification<T> spec = (root, query, cb) -> {
                var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                
                // Company filter
                predicates.add(cb.equal(root.get("companyId"), companyId));
                
                // Exclude soft-deleted records if entity has 'deleted' field
                try {
                    root.get("deleted");
                    predicates.add(cb.equal(root.get("deleted"), false));
                } catch (Exception e) {
                    // Entity doesn't have deleted field, skip this filter
                }
                
                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            };
            
            return ((org.springframework.data.jpa.repository.JpaSpecificationExecutor<T>) repository).findAll(spec, pageable);
        }
        
        // Fallback for repositories without Specification support
        return repository.findAll(pageable).map(entity -> {
            if (belongsToCompany(entity, companyId) && !isDeleted(entity)) {
                return entity;
            }
            return null;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<T> query(UUID companyId, Map<String, Object> filters, Pageable pageable) {
        // Validate all filter field names first
        validateFilterFields(filters);
        
        // Build a map of lowercase field names to actual field names for case-insensitive lookup
        java.util.Map<String, String> fieldNameMap = buildFieldNameMap();
        
        // Check if user explicitly specified deleted filter
        boolean hasDeletedFilter = filters.keySet().stream()
            .anyMatch(key -> key.equalsIgnoreCase("deleted"));
        
        // Generic filtering using JPA Specification
        // Filters are applied BEFORE pagination
        org.springframework.data.jpa.domain.Specification<T> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            
            // Company filter (required)
            predicates.add(cb.equal(root.get("companyId"), companyId));
            
            // Exclude deleted records by default (unless explicitly filtered)
            if (!hasDeletedFilter) {
                try {
                    root.get("deleted");
                    predicates.add(cb.equal(root.get("deleted"), false));
                } catch (Exception e) {
                    // Entity doesn't have deleted field, skip this filter
                }
            }
            
            // Apply each filter
            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();
                
                if (value != null && !value.toString().isEmpty()) {
                    try {
                        // Parse field name and operator
                        String actualField = fieldName;
                        String operator = "eq"; // default: equals
                        
                        // Check for range operators with underscore separator
                        if (fieldName.startsWith("min_")) {
                            actualField = fieldName.substring(4); // Remove "min_"
                            operator = "gte"; // greater than or equal
                        } else if (fieldName.startsWith("max_")) {
                            actualField = fieldName.substring(4); // Remove "max_"
                            operator = "lte"; // less than or equal
                        } else if (fieldName.startsWith("from_")) {
                            actualField = fieldName.substring(5); // Remove "from_"
                            operator = "gte"; // greater than or equal
                        } else if (fieldName.startsWith("to_")) {
                            actualField = fieldName.substring(3); // Remove "to_"
                            operator = "lte"; // less than or equal
                        }
                        
                        // Get the correct case-sensitive field name
                        String correctFieldName = fieldNameMap.getOrDefault(actualField.toLowerCase(), actualField);
                        
                        // Convert string values to appropriate types
                        Object convertedValue = convertValue(value, root.get(correctFieldName).getJavaType(), operator);
                        
                        // Apply operator
                        switch (operator) {
                            case "gte":
                                predicates.add(cb.greaterThanOrEqualTo(root.get(correctFieldName), (Comparable) convertedValue));
                                break;
                            case "lte":
                                predicates.add(cb.lessThanOrEqualTo(root.get(correctFieldName), (Comparable) convertedValue));
                                break;
                            default:
                                predicates.add(cb.equal(root.get(correctFieldName), convertedValue));
                        }
                    } catch (Exception e) {
                        // This should not happen as we validated fields earlier
                        throw new RuntimeException("Error applying filter '" + fieldName + "': " + e.getMessage());
                    }
                }
            }
            
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        
        if (repository instanceof org.springframework.data.jpa.repository.JpaSpecificationExecutor) {
            return ((org.springframework.data.jpa.repository.JpaSpecificationExecutor<T>) repository).findAll(spec, pageable);
        }
        
        // Fallback if repository doesn't support Specification
        return list(companyId, pageable);
    }
    
    /**
     * Build a map of lowercase field names to actual field names for case-insensitive lookup
     */
    private java.util.Map<String, String> buildFieldNameMap() {
        java.util.Map<String, String> fieldNameMap = new java.util.HashMap<>();
        Class<?> entityClass = getEntityClass();
        
        // Add all fields from entity hierarchy
        addFieldsToMap(fieldNameMap, entityClass);
        
        return fieldNameMap;
    }
    
    /**
     * Recursively add fields from class hierarchy
     */
    private void addFieldsToMap(java.util.Map<String, String> fieldNameMap, Class<?> clazz) {
        if (clazz == null || clazz == Object.class) {
            return;
        }
        
        // Add fields from current class
        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            fieldNameMap.put(field.getName().toLowerCase(), field.getName());
        }
        
        // Recursively add fields from superclass
        addFieldsToMap(fieldNameMap, clazz.getSuperclass());
    }
    
    /**
     * Validate that all filter field names are valid entity fields
     */
    private void validateFilterFields(Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return;
        }
        
        // Get all valid field names from the entity class hierarchy
        java.util.Set<String> validFields = new java.util.HashSet<>();
        Class<?> entityClass = getEntityClass();
        
        // Add fields from entire class hierarchy
        addFieldsToSet(validFields, entityClass);
        
        // Validate each filter (case-insensitive)
        java.util.List<String> invalidFields = new java.util.ArrayList<>();
        for (String filterName : filters.keySet()) {
            String actualField = filterName;
            
            // Check for range operators with underscore separator
            if (filterName.startsWith("min_")) {
                actualField = filterName.substring(4);
            } else if (filterName.startsWith("max_")) {
                actualField = filterName.substring(4);
            } else if (filterName.startsWith("from_")) {
                actualField = filterName.substring(5);
            } else if (filterName.startsWith("to_")) {
                actualField = filterName.substring(3);
            }
            
            // Check if the field exists (case-insensitive)
            final String fieldToCheck = actualField; // Make it final for lambda
            boolean fieldExists = validFields.stream()
                .anyMatch(f -> f.equalsIgnoreCase(fieldToCheck));
            
            if (!fieldExists) {
                invalidFields.add(filterName);
            }
        }
        
        // Throw exception if any invalid fields found
        if (!invalidFields.isEmpty()) {
            String fieldList = String.join(", ", invalidFields);
            String validFieldList = validFields.stream()
                .filter(f -> !f.equals("serialVersionUID")) // Exclude technical fields
                .sorted()
                .collect(java.util.stream.Collectors.joining(", "));
            
            throw new IllegalArgumentException(
                "Invalid filter field(s): " + fieldList + ". " +
                "Valid fields are: " + validFieldList
            );
        }
    }
    
    /**
     * Recursively add fields from class hierarchy to set
     */
    private void addFieldsToSet(java.util.Set<String> validFields, Class<?> clazz) {
        if (clazz == null || clazz == Object.class) {
            return;
        }
        
        // Add fields from current class
        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            validFields.add(field.getName());
        }
        
        // Recursively add fields from superclass
        addFieldsToSet(validFields, clazz.getSuperclass());
    }
    
    /**
     * Get the entity class type
     */
    private Class<?> getEntityClass() {
        // Get generic type from class signature
        java.lang.reflect.Type genericSuperclass = getClass().getGenericSuperclass();
        if (genericSuperclass instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.Type[] typeArgs = ((java.lang.reflect.ParameterizedType) genericSuperclass).getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                return (Class<?>) typeArgs[0];
            }
        }
        throw new RuntimeException("Unable to determine entity class");
    }
    
    /**
     * Convert string values from query params to appropriate types
     */
    private Object convertValue(Object value, Class<?> targetType, String operator) {
        if (value == null) {
            return null;
        }
        
        String stringValue = value.toString();
        
        // Already correct type
        if (targetType.isInstance(value)) {
            return value;
        }
        
        // Convert based on target type
        try {
            if (targetType == UUID.class) {
                return UUID.fromString(stringValue);
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                return Boolean.parseBoolean(stringValue);
            } else if (targetType == Integer.class || targetType == int.class) {
                return Integer.parseInt(stringValue);
            } else if (targetType == Long.class || targetType == long.class) {
                return Long.parseLong(stringValue);
            } else if (targetType == Double.class || targetType == double.class) {
                return Double.parseDouble(stringValue);
            } else if (targetType == Float.class || targetType == float.class) {
                return Float.parseFloat(stringValue);
            } else if (targetType == java.time.OffsetDateTime.class) {
                // Try parsing as date-only first (YYYY-MM-DD)
                if (stringValue.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    // Date only - for "to_" operator use end of day, otherwise start of day
                    if ("lte".equals(operator)) {
                        // End of day: 23:59:59.999999999
                        return java.time.LocalDate.parse(stringValue)
                            .atTime(23, 59, 59, 999999999)
                            .atOffset(java.time.ZoneOffset.UTC);
                    } else {
                        // Start of day: 00:00:00
                        return java.time.LocalDate.parse(stringValue)
                            .atStartOfDay()
                            .atOffset(java.time.ZoneOffset.UTC);
                    }
                }
                // Full ISO format with time
                return java.time.OffsetDateTime.parse(stringValue);
            } else if (targetType == java.time.LocalDateTime.class) {
                if (stringValue.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    if ("lte".equals(operator)) {
                        return java.time.LocalDate.parse(stringValue).atTime(23, 59, 59, 999999999);
                    } else {
                        return java.time.LocalDate.parse(stringValue).atStartOfDay();
                    }
                }
                return java.time.LocalDateTime.parse(stringValue);
            } else if (targetType == java.time.LocalDate.class) {
                return java.time.LocalDate.parse(stringValue);
            } else if (targetType == java.time.Instant.class) {
                if (stringValue.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    if ("lte".equals(operator)) {
                        return java.time.LocalDate.parse(stringValue)
                            .atTime(23, 59, 59, 999999999)
                            .toInstant(java.time.ZoneOffset.UTC);
                    } else {
                        return java.time.LocalDate.parse(stringValue)
                            .atStartOfDay()
                            .toInstant(java.time.ZoneOffset.UTC);
                    }
                }
                return java.time.Instant.parse(stringValue);
            } else if (targetType == java.util.Date.class) {
                if (stringValue.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    if ("lte".equals(operator)) {
                        return java.util.Date.from(
                            java.time.LocalDate.parse(stringValue)
                                .atTime(23, 59, 59, 999999999)
                                .toInstant(java.time.ZoneOffset.UTC)
                        );
                    } else {
                        return java.util.Date.from(
                            java.time.LocalDate.parse(stringValue)
                                .atStartOfDay()
                                .toInstant(java.time.ZoneOffset.UTC)
                        );
                    }
                }
                return java.util.Date.from(java.time.Instant.parse(stringValue));
            } else if (targetType == java.sql.Timestamp.class) {
                if (stringValue.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    if ("lte".equals(operator)) {
                        return java.sql.Timestamp.from(
                            java.time.LocalDate.parse(stringValue)
                                .atTime(23, 59, 59, 999999999)
                                .toInstant(java.time.ZoneOffset.UTC)
                        );
                    } else {
                        return java.sql.Timestamp.from(
                            java.time.LocalDate.parse(stringValue)
                                .atStartOfDay()
                                .toInstant(java.time.ZoneOffset.UTC)
                        );
                    }
                }
                return java.sql.Timestamp.from(java.time.Instant.parse(stringValue));
            }
        } catch (Exception e) {
            // Return original value if conversion fails
        }
        
        return value;
    }

    @Override
    @Transactional(readOnly = true)
    public T getById(UUID companyId, UUID id) {
        T entity = repository.findById(id).orElse(null);
        
        if (entity != null && !belongsToCompany(entity, companyId)) {
            throw new RuntimeException("Access denied");
        }
        
        return entity;
    }

    @Override
    @Transactional
    public T save(UUID companyId, T entity) {
        UUID entityId = getEntityId(entity);
        
        // New entity - set company ID
        if (entityId == null) {
            setCompanyId(entity, companyId);
        } else {
            // Update - verify ownership
            T existing = repository.findById(entityId).orElse(null);
            if (existing != null && !belongsToCompany(existing, companyId)) {
                throw new RuntimeException("Access denied");
            }
            // Ensure company ID cannot be changed
            setCompanyId(entity, companyId);
        }
        
        return repository.save(entity);
    }

    @Override
    @Transactional
    public void remove(UUID companyId, UUID id, UUID userId, boolean hardDelete) {
        T entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resource not found"));
        
        if (!belongsToCompany(entity, companyId)) {
            throw new RuntimeException("Access denied");
        }
        
        if (hardDelete) {
            // Hard delete - actually remove from database
            repository.delete(entity);
        } else {
            // Soft delete - set deleted flag
            performSoftDelete(entity, userId);
            repository.save(entity);
        }
    }

    @Override
    @Transactional
    public void removeBatch(UUID companyId, List<UUID> ids, UUID userId, boolean hardDelete) {
        for (UUID id : ids) {
            try {
                remove(companyId, id, userId, hardDelete);
            } catch (Exception e) {
                // Skip if not found or access denied
            }
        }
    }
    
    /**
     * Perform soft delete on entity (if it extends AuditableEntity)
     */
    private void performSoftDelete(T entity, UUID userId) {
        try {
            // Set deleted flag
            java.lang.reflect.Method setDeleted = entity.getClass().getMethod("setDeleted", boolean.class);
            setDeleted.invoke(entity, true);
            
            // Set deletedAt timestamp
            java.lang.reflect.Method setDeletedAt = entity.getClass().getMethod("setDeletedAt", java.time.OffsetDateTime.class);
            setDeletedAt.invoke(entity, java.time.OffsetDateTime.now());
            
            // Set deletedBy user
            java.lang.reflect.Method setDeletedBy = entity.getClass().getMethod("setDeletedBy", UUID.class);
            setDeletedBy.invoke(entity, userId);
        } catch (Exception e) {
            throw new RuntimeException("Entity does not support soft delete. Use hardDelete=true parameter.", e);
        }
    }
    
    /**
     * Recover a soft-deleted entity
     */
    @Override
    public T recover(UUID companyId, UUID id, UUID userId) {
        T entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entity not found"));
        
        // Verify company ownership
        if (!belongsToCompany(entity, companyId)) {
            throw new RuntimeException("Access denied: Entity does not belong to your company");
        }
        
        // Verify entity is currently deleted
        try {
            java.lang.reflect.Method isDeleted = entity.getClass().getMethod("isDeleted");
            Boolean deleted = (Boolean) isDeleted.invoke(entity);
            if (!deleted) {
                throw new RuntimeException("Entity is not deleted");
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Entity does not support soft delete recovery", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to check deleted status", e);
        }
        
        // Perform recovery
        performRecovery(entity, userId);
        return repository.save(entity);
    }
    
    /**
     * Perform recovery on soft-deleted entity
     */
    private void performRecovery(T entity, UUID userId) {
        try {
            // Clear deleted flag
            java.lang.reflect.Method setDeleted = entity.getClass().getMethod("setDeleted", boolean.class);
            setDeleted.invoke(entity, false);
            
            // Clear deletedAt timestamp
            java.lang.reflect.Method setDeletedAt = entity.getClass().getMethod("setDeletedAt", java.time.OffsetDateTime.class);
            setDeletedAt.invoke(entity, (Object) null);
            
            // Clear deletedBy user
            java.lang.reflect.Method setDeletedBy = entity.getClass().getMethod("setDeletedBy", UUID.class);
            setDeletedBy.invoke(entity, (Object) null);
        } catch (Exception e) {
            throw new RuntimeException("Entity does not support recovery", e);
        }
    }

    // --- Helper methods using reflection ---

    /**
     * Check if entity belongs to the given company
     */
    protected boolean belongsToCompany(T entity, UUID companyId) {
        try {
            Field field = findField(entity.getClass(), "companyId");
            if (field != null) {
                field.setAccessible(true);
                UUID entityCompanyId = (UUID) field.get(entity);
                return companyId.equals(entityCompanyId);
            }
        } catch (Exception e) {
            // If no companyId field, assume it belongs (for non-tenant entities)
        }
        return true;
    }

    /**
     * Check if entity is soft-deleted
     */
    protected boolean isDeleted(T entity) {
        try {
            Field field = findField(entity.getClass(), "deleted");
            if (field != null) {
                field.setAccessible(true);
                Object value = field.get(entity);
                return value instanceof Boolean && (Boolean) value;
            }
        } catch (Exception e) {
            // If no deleted field, assume not deleted
        }
        return false;
    }

    /**
     * Get entity ID using reflection
     */
    protected UUID getEntityId(T entity) {
        try {
            Field field = findField(entity.getClass(), "id");
            if (field != null) {
                field.setAccessible(true);
                return (UUID) field.get(entity);
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    /**
     * Set company ID on entity
     */
    protected void setCompanyId(T entity, UUID companyId) {
        try {
            Field field = findField(entity.getClass(), "companyId");
            if (field != null) {
                field.setAccessible(true);
                field.set(entity, companyId);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set companyId", e);
        }
    }

    /**
     * Try to soft delete entity (set active = false)
     * @return true if soft deleted, false if no 'active' field exists
     */
    protected boolean softDelete(T entity) {
        try {
            Field field = findField(entity.getClass(), "active");
            if (field != null) {
                field.setAccessible(true);
                if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                    field.set(entity, false);
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    /**
     * Find field in class hierarchy
     */
    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}
