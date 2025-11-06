package com.delivery.deliveryapi.repo;

import com.delivery.deliveryapi.model.UserAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserAuditRepository extends JpaRepository<UserAudit, UUID> {
}