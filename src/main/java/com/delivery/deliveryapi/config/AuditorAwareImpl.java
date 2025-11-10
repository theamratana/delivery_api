package com.delivery.deliveryapi.config;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.delivery.deliveryapi.model.User;

@Component
public class AuditorAwareImpl implements AuditorAware<UUID> {

    @Override
    public Optional<UUID> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && 
            authentication.getPrincipal() instanceof User user) {
            return Optional.of(user.getId());
        }
        
        // For dev/testing purposes, return a default UUID if no user is authenticated
        return Optional.of(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }
}