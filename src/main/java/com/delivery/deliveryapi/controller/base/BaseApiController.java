package com.delivery.deliveryapi.controller.base;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.repo.UserRepository;
import com.delivery.deliveryapi.service.base.BaseService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Base API controller providing standard CRUD operations.
 * All endpoints require JWT authentication.
 * 
 * @param <T> Entity type
 */
public abstract class BaseApiController<T> {

    protected final BaseService<T> service;
    protected final UserRepository userRepository;

    public BaseApiController(BaseService<T> service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    /**
     * List all entities with optional filters (paginated)
     * GET /{resource}?page=0&size=20&sort=field,desc&provinceId=xxx&active=true
     * 
     * Pagination params (handled by Pageable):
     * - page: Page number (default 0)
     * - size: Page size (default 20)
     * - sort: Sort field and direction (e.g., fee,desc)
     * 
     * Filter operators (use underscore separator):
     * - fieldName=value     : Exact match (e.g., ?active=true)
     * - min_fieldName=value : Greater than or equal (e.g., ?min_fee=2.0)
     * - max_fieldName=value : Less than or equal (e.g., ?max_fee=10.0)
     * - from_fieldName=value: Greater than or equal, for dates (e.g., ?from_createdAt=2026-01-01T00:00:00Z)
     * - to_fieldName=value  : Less than or equal, for dates (e.g., ?to_createdAt=2026-12-31T23:59:59Z)
     */
    @GetMapping
    public ResponseEntity<?> list(
            HttpServletRequest request,
            Pageable pageable) {
        try {
            UUID companyId = getCompanyId(getCurrentUser());
            
            // Extract all query parameters except pagination params
            Map<String, Object> filters = new java.util.HashMap<>();
            request.getParameterMap().forEach((key, values) -> {
                // Skip pagination parameters
                if (!key.equals("page") && !key.equals("size") && !key.equals("sort")) {
                    // Use first value if multiple values exist
                    if (values != null && values.length > 0) {
                        filters.put(key, values[0]);
                    }
                }
            });
            
            Page<T> page;
            if (filters.isEmpty()) {
                page = service.list(companyId, pageable);
            } else {
                page = service.query(companyId, filters, pageable);
            }
            
            return ResponseEntity.ok(new PageResponse<>(page));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get entity by ID
     * GET /{resource}/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable UUID id) {
        try {
            UUID companyId = getCompanyId(getCurrentUser());
            T entity = service.getById(companyId, id);
            if (entity == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Resource not found"));
            }
            return ResponseEntity.ok(entity);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create new entity
     * POST /{resource}
     */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody T entity) {
        try {
            UUID companyId = getCompanyId(getCurrentUser());
            T saved = service.save(companyId, entity);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update existing entity
     * PUT /{resource}/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody T entity) {
        try {
            UUID companyId = getCompanyId(getCurrentUser());
            
            // Verify entity exists and belongs to company
            T existing = service.getById(companyId, id);
            if (existing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Resource not found"));
            }
            
            // Ensure ID in path matches entity (if entity has ID)
            setEntityId(entity, id);
            
            T updated = service.save(companyId, entity);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Remove entity by ID
     * DELETE /{resource}/{id}?hardDelete=true
     * 
     * @param hardDelete false (default) = soft delete (sets deleted flag), true = hard delete (removes from DB)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "false") boolean hardDelete) {
        try {
            UUID companyId = getCompanyId(getCurrentUser());
            UUID userId = getCurrentUser().getId();
            service.remove(companyId, id, userId, hardDelete);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Batch remove entities
     * DELETE /{resource}?ids=uuid1,uuid2&hardDelete=true
     * 
     * @param hardDelete false (default) = soft delete, true = hard delete
     */
    @DeleteMapping
    public ResponseEntity<?> deleteBatch(
            @RequestParam List<UUID> ids,
            @RequestParam(required = false, defaultValue = "false") boolean hardDelete) {
        try {
            UUID companyId = getCompanyId(getCurrentUser());
            UUID userId = getCurrentUser().getId();
            service.removeBatch(companyId, ids, userId, hardDelete);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Recover a soft-deleted entity
     * PATCH /{resource}/{id}/recover
     */
    @PatchMapping("/{id}/recover")
    public ResponseEntity<?> recover(@PathVariable UUID id) {
        try {
            User user = getCurrentUser();
            UUID companyId = getCompanyId(user);
            T recovered = service.recover(companyId, id, user.getId());
            return ResponseEntity.ok(recovered);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get current authenticated user
     */
    protected User getCurrentUser() {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        // JWT authentication sets user ID as principal
        try {
            UUID userId = UUID.fromString(principal);
            return userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        } catch (IllegalArgumentException e) {
            // Fallback: try username (for other auth methods)
            return userRepository.findByUsernameIgnoreCase(principal)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }
    }

    /**
     * Verify user belongs to a company
     */
    protected void verifyUserHasCompany(User user) {
        if (user.getCompany() == null) {
            throw new RuntimeException("User must belong to a company");
        }
    }

    /**
     * Get company ID from user (automatically validates user has company)
     */
    protected UUID getCompanyId(User user) {
        verifyUserHasCompany(user);
        return user.getCompany().getId();
    }

    /**
     * Set entity ID via reflection (used for PUT operations)
     */
    protected void setEntityId(T entity, UUID id) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            // If no 'id' field or reflection fails, ignore
            // The service layer will handle validation
        }
    }

    /**
     * Custom page response wrapper to use "records" instead of "content"
     */
    public static class PageResponse<T> {
        private final java.util.List<T> records;
        private final int page;
        private final int size;
        private final long totalElements;
        private final int totalPages;
        private final boolean first;
        private final boolean last;
        private final boolean empty;

        public PageResponse(Page<T> page) {
            this.records = page.getContent();
            this.page = page.getNumber();
            this.size = page.getSize();
            this.totalElements = page.getTotalElements();
            this.totalPages = page.getTotalPages();
            this.first = page.isFirst();
            this.last = page.isLast();
            this.empty = page.isEmpty();
        }

        public java.util.List<T> getRecords() { return records; }
        public int getPage() { return page; }
        public int getSize() { return size; }
        public long getTotalElements() { return totalElements; }
        public int getTotalPages() { return totalPages; }
        public boolean isFirst() { return first; }
        public boolean isLast() { return last; }
        public boolean isEmpty() { return empty; }
    }
}
