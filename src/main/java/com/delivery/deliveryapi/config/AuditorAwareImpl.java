package com.delivery.deliveryapi.config;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuditorAwareImpl implements AuditorAware<UUID> {

    private static final Logger log = LoggerFactory.getLogger(AuditorAwareImpl.class);

    @Override
    public Optional<UUID> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && 
            authentication.getPrincipal() instanceof String userIdStr) {
            try {
                return Optional.of(UUID.fromString(userIdStr));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid user ID format in principal: {}", userIdStr);
            }
        }
        
        // For dev/testing purposes, return a default UUID if no user is authenticated
        return Optional.of(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }
}