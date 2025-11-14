package com.delivery.deliveryapi.service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.deliveryapi.model.AuthIdentity;
import com.delivery.deliveryapi.model.AuthProvider;
import com.delivery.deliveryapi.model.Company;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.model.UserRole;
import com.delivery.deliveryapi.model.UserType;
import com.delivery.deliveryapi.repo.AuthIdentityRepository;
import com.delivery.deliveryapi.repo.CompanyRepository;
import com.delivery.deliveryapi.repo.UserRepository;

@Service
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    private final UserRepository userRepository;
    private final AuthIdentityRepository identityRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository,
                           AuthIdentityRepository identityRepository,
                           CompanyRepository companyRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createSystemAdministrator(String username, String password, String displayName) {
        log.info("Creating system administrator user: {}", username);

        // Check if admin user already exists
        Optional<User> existingUser = userRepository.findByUsernameIgnoreCase(username);
        if (existingUser.isPresent()) {
            log.info("System administrator user already exists: {}", username);
            return existingUser.get();
        }

        // Create or get system admin company
        Company adminCompany = companyRepository.findByName("System Administration")
                .orElseGet(() -> {
                    Company company = new Company();
                    company.setName("System Administration");
                    company.setActive(true);
                    return companyRepository.save(company);
                });

        // Create admin user
        User adminUser = new User();
        adminUser.setUsername(username);
        adminUser.setDisplayName(displayName);
        adminUser.setFirstName("System");
        adminUser.setLastName("Administrator");
        adminUser.setUserType(UserType.ADMIN);
        adminUser.setUserRole(UserRole.SYSTEM_ADMINISTRATOR);
        adminUser.setCompany(adminCompany);
        adminUser.setActive(true);
        adminUser.setIncomplete(false);
        adminUser.setLastLoginAt(OffsetDateTime.now());

        adminUser = userRepository.save(adminUser);

        // Create auth identity for password-based login
        AuthIdentity identity = new AuthIdentity();
        identity.setUser(adminUser);
        identity.setProvider(AuthProvider.LOCAL);
        identity.setProviderUserId(username);
        identity.setUsername(username);
        identity.setDisplayName(displayName);
        identity.setPasswordHash(passwordEncoder.encode(password));
        identity.setLastLoginAt(OffsetDateTime.now());

        identityRepository.save(identity);

        log.info("Created system administrator user: {} with ID: {}", username, adminUser.getId());
        return adminUser;
    }

    public boolean authenticateAdmin(String username, String password) {
        Optional<AuthIdentity> identity = identityRepository
                .findByProviderAndProviderUserId(AuthProvider.LOCAL, username);

        if (identity.isEmpty()) {
            return false;
        }

        // Verify password
        boolean passwordMatches = passwordEncoder.matches(password, identity.get().getPasswordHash());

        if (passwordMatches) {
            // Update last login
            User user = identity.get().getUser();
            user.setLastLoginAt(OffsetDateTime.now());
            userRepository.save(user);

            identity.get().setLastLoginAt(OffsetDateTime.now());
            identityRepository.save(identity.get());
        }

        return passwordMatches;
    }

    public Optional<User> findAdminByUsername(String username) {
        Optional<AuthIdentity> identity = identityRepository
                .findByProviderAndProviderUserId(AuthProvider.LOCAL, username);

        return identity.map(AuthIdentity::getUser);
    }

    @Transactional
    public boolean changePassword(UUID userId, String currentPassword, String newPassword) {
        // Find the user's auth identity for LOCAL provider
        Optional<AuthIdentity> identityOpt = identityRepository.findAll()
                .stream()
                .filter(identity -> identity.getUser() != null &&
                                   identity.getUser().getId().equals(userId) &&
                                   identity.getProvider() == AuthProvider.LOCAL)
                .findFirst();

        if (identityOpt.isEmpty()) {
            return false;
        }

        AuthIdentity identity = identityOpt.get();

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, identity.getPasswordHash())) {
            return false;
        }

        // Update password
        identity.setPasswordHash(passwordEncoder.encode(newPassword));
        identityRepository.save(identity);

        log.info("Password changed for user: {}", userId);
        return true;
    }

    @Transactional
    public boolean setPassword(UUID userId, String newPassword) {
        // Find the user
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        // Check if user is system administrator (not allowed to use this endpoint)
        if (user.getUserRole() == UserRole.SYSTEM_ADMINISTRATOR) {
            return false;
        }

        // Find or create LOCAL auth identity for this user
        Optional<AuthIdentity> identityOpt = identityRepository.findAll()
                .stream()
                .filter(identity -> identity.getUser() != null &&
                                   identity.getUser().getId().equals(userId) &&
                                   identity.getProvider() == AuthProvider.LOCAL)
                .findFirst();

        AuthIdentity identity;
        if (identityOpt.isPresent()) {
            // Update existing LOCAL identity
            identity = identityOpt.get();
        } else {
            // Create new LOCAL identity
            identity = new AuthIdentity();
            identity.setUser(user);
            identity.setProvider(AuthProvider.LOCAL);
            identity.setProviderUserId(user.getUsername() != null ? user.getUsername() : userId.toString());
            identity.setUsername(user.getUsername());
            identity.setDisplayName(user.getDisplayName());
        }

        // Set/update password
        identity.setPasswordHash(passwordEncoder.encode(newPassword));
        identity.setLastLoginAt(OffsetDateTime.now());
        identityRepository.save(identity);

        log.info("Password set for user: {}", userId);
        return true;
    }
}