package com.delivery.deliveryapi.service;

import com.delivery.deliveryapi.repo.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class TokenCleanupService {
    private static final Logger log = LoggerFactory.getLogger(TokenCleanupService.class);

    private final RefreshTokenRepository refreshTokenRepository;

    public TokenCleanupService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            OffsetDateTime now = OffsetDateTime.now();
            int deletedCount = refreshTokenRepository.deleteExpiredTokens(now);
            if (deletedCount > 0) {
                log.info("Cleaned up {} expired refresh tokens", deletedCount);
            }
        } catch (Exception e) {
            log.error("Error during token cleanup", e);
        }
    }
}