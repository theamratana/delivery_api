package com.delivery.deliveryapi.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Base class for tenant-specific (multi-tenant) entities.
 * Extends AuditableEntity and adds companyId for multi-tenancy isolation.
 * 
 * Use this for entities that belong to a specific company:
 * - DeliveryFee, Order, Customer, etc.
 * 
 * For global entities (Province, District, SystemConfig), 
 * use AuditableEntity directly.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class TenantAuditableEntity extends AuditableEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;
}
