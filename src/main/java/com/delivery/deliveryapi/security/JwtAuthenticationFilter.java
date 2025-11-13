package com.delivery.deliveryapi.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.repo.UserRepository;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.validateAccessToken(token);
                String subject = claims.getSubject(); // userId
                log.info("JWT token valid for subject: {}", subject);
                
                // Get user and set authorities
                List<GrantedAuthority> authorities = Collections.emptyList();
                try {
                    UUID userId = UUID.fromString(subject);
                    Optional<User> optUser = userRepository.findById(userId);
                    if (optUser.isPresent()) {
                        User user = optUser.get();
                        if (user.getUserRole() != null) {
                            authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getUserRole().name()));
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to load user authorities: {}", e.getMessage());
                }
                
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        subject, null, authorities);
                ((UsernamePasswordAuthenticationToken) auth).setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                log.warn("Invalid JWT token: {}", e.getMessage());
                // Invalid token: clear context and continue; access rules will block
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
