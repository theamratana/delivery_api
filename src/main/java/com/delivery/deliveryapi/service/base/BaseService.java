package com.delivery.deliveryapi.service.base;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Base service interface for standard CRUD operations
 * All operations are automatically scoped to the company
 * 
 * @param <T> Entity type
 */
public interface BaseService<T> {
    
    /**
     * List all entities with pagination (filtered by company)
     */
    Page<T> list(UUID companyId, Pageable pageable);
    
    /**
     * Query entities with filters (filtered by company)
     */
    Page<T> query(UUID companyId, Map<String, Object> filters, Pageable pageable);
    
    /**
     * Get entity by ID (verified against company)
     */
    T getById(UUID companyId, UUID id);
    
    /**
     * Save (create or update) entity (scoped to company)
     */
    T save(UUID companyId, T entity);
    
    /**
     * Remove entity by ID (verified against company)
     * @param hardDelete false = soft delete (sets deleted flag), true = hard delete (removes from DB)
     */
    void remove(UUID companyId, UUID id, UUID userId, boolean hardDelete);
    
    /**
     * Remove multiple entities (verified against company)
     * @param hardDelete false = soft delete, true = hard delete
     */
    void removeBatch(UUID companyId, List<UUID> ids, UUID userId, boolean hardDelete);
    
    /**
     * Recover a soft-deleted entity (verified against company)
     */
    T recover(UUID companyId, UUID id, UUID userId);
}
