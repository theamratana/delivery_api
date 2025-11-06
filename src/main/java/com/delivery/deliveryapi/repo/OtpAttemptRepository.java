package com.delivery.deliveryapi.repo;

import com.delivery.deliveryapi.model.OtpAttempt;
import com.delivery.deliveryapi.model.OtpStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OtpAttemptRepository extends JpaRepository<OtpAttempt, UUID> {
    Optional<OtpAttempt> findByLinkCode(String linkCode);
    Optional<OtpAttempt> findTopByChatIdAndStatusOrderByCreatedAtDesc(Long chatId, OtpStatus status);
}
