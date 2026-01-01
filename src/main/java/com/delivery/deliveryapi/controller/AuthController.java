package com.delivery.deliveryapi.controller;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.delivery.deliveryapi.model.AuthIdentity;
import com.delivery.deliveryapi.model.Company;
import com.delivery.deliveryapi.model.Employee;
import com.delivery.deliveryapi.model.PendingEmployee;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.model.UserAudit;
import com.delivery.deliveryapi.model.UserRole;
import com.delivery.deliveryapi.model.UserType;
import com.delivery.deliveryapi.repo.AuthIdentityRepository;
import com.delivery.deliveryapi.repo.CompanyRepository;
import com.delivery.deliveryapi.repo.EmployeeRepository;
import com.delivery.deliveryapi.repo.PendingEmployeeRepository;
import com.delivery.deliveryapi.repo.UserAuditRepository;
import com.delivery.deliveryapi.repo.UserRepository;
import com.delivery.deliveryapi.security.JwtService;
import com.delivery.deliveryapi.service.AdminUserService;
import com.delivery.deliveryapi.service.CompanyAssignmentService;
import com.delivery.deliveryapi.service.TelegramAuthService;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.jsonwebtoken.Claims;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);
    private static final String AUDIT_TYPE_PROFILE_UPDATE = "PROFILE_UPDATE";
    private static final String AUDIT_SOURCE_TELEGRAM = "TELEGRAM";

    private final TelegramAuthService telegramAuthService;
    private final JwtService jwtService;
    private final boolean devTokenEnabled;
    private final UserRepository userRepository;
    private final AuthIdentityRepository identityRepository;
    private final UserAuditRepository userAuditRepository;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final PendingEmployeeRepository pendingEmployeeRepository;
    private final CompanyAssignmentService companyAssignmentService;
    private final AdminUserService adminUserService;

    public AuthController(TelegramAuthService telegramAuthService,
                          UserRepository userRepository,
                          AuthIdentityRepository identityRepository,
                          JwtService jwtService,
                          UserAuditRepository userAuditRepository,
                          CompanyRepository companyRepository,
                          EmployeeRepository employeeRepository,
                          PendingEmployeeRepository pendingEmployeeRepository,
                          CompanyAssignmentService companyAssignmentService,
                          AdminUserService adminUserService,
                          @Value("${jwt.dev-enabled:false}") boolean devTokenEnabled) {
        this.telegramAuthService = telegramAuthService;
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.jwtService = jwtService;
        this.userAuditRepository = userAuditRepository;
        this.companyRepository = companyRepository;
        this.employeeRepository = employeeRepository;
        this.pendingEmployeeRepository = pendingEmployeeRepository;
        this.companyAssignmentService = companyAssignmentService;
        this.adminUserService = adminUserService;
        this.devTokenEnabled = devTokenEnabled;
    }

    public static class TelegramVerifyRequest {
        @JsonProperty("id") public String id;
        @JsonProperty("first_name") public String firstName;
        @JsonProperty("last_name") public String lastName;
        @JsonProperty("username") public String username;
        @JsonProperty("photo_url") public String photoUrl;
        @JsonProperty("auth_date") public String authDate;
        @JsonProperty("hash") public String hash;

        public Map<String, String> toMap() {
            Map<String, String> m = new HashMap<>();
            if (id != null) m.put("id", id);
            if (firstName != null) m.put("first_name", firstName);
            if (lastName != null) m.put("last_name", lastName);
            if (username != null) m.put("username", username);
            if (photoUrl != null) m.put("photo_url", photoUrl);
            if (authDate != null) m.put("auth_date", authDate);
            if (hash != null) m.put("hash", hash);
            return m;
        }
    }

    public record AuthResponse(String accessToken, String refreshToken, UUID userId, String displayName, String username, String provider, long accessExpiresIn, long refreshExpiresIn) {}

    @PostMapping("/telegram/verify")
    @Transactional
    public ResponseEntity<Object> verifyTelegram(@RequestBody TelegramVerifyRequest req) {
        Map<String, String> payload = req.toMap();
        if (!telegramAuthService.verifyLoginPayload(payload)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_signature"));
        }

        String providerUserId = req.id;
        Optional<AuthIdentity> existing = identityRepository
                .findByProviderAndProviderUserId(com.delivery.deliveryapi.model.AuthProvider.TELEGRAM, providerUserId);

        User user;
        AuthIdentity identity;
        if (existing.isPresent()) {
            identity = existing.get();
            identity.setLastLoginAt(OffsetDateTime.now());
            identity.setDisplayName(displayNameFrom(req));
            identity.setUsername(req.username);
            user = identity.getUser();
            // Capture old values for audit
            String oldDisplayName = user.getDisplayName();
            String oldFirstName = user.getFirstName();
            String oldLastName = user.getLastName();
            String oldUsername = user.getUsername();
            String oldAvatarUrl = user.getAvatarUrl();
            // update user profile details from Telegram
            user.setFirstName(req.firstName);
            user.setLastName(req.lastName);
            user.setUsername(req.username);
            user.setAvatarUrl(req.photoUrl);
            user.setLastLoginAt(OffsetDateTime.now());
            // Audit changes
            auditUserChanges(user.getId(), "displayName", oldDisplayName, displayNameFrom(req), AUDIT_SOURCE_TELEGRAM);
            auditUserChanges(user.getId(), "firstName", oldFirstName, req.firstName, AUDIT_SOURCE_TELEGRAM);
            auditUserChanges(user.getId(), "lastName", oldLastName, req.lastName, AUDIT_SOURCE_TELEGRAM);
            auditUserChanges(user.getId(), "username", oldUsername, req.username, AUDIT_SOURCE_TELEGRAM);
            auditUserChanges(user.getId(), "avatarUrl", oldAvatarUrl, req.photoUrl, AUDIT_SOURCE_TELEGRAM);
        } else {
            user = new User();
            user.setDisplayName(displayNameFrom(req));
            user.setFirstName(req.firstName);
            user.setLastName(req.lastName);
            user.setUsername(req.username);
            user.setAvatarUrl(req.photoUrl);
            user.setLastLoginAt(OffsetDateTime.now());
            user = userRepository.save(user);

            identity = new AuthIdentity();
            identity.setUser(user);
            identity.setProvider(com.delivery.deliveryapi.model.AuthProvider.TELEGRAM);
            identity.setProviderUserId(providerUserId);
            identity.setUsername(req.username);
            identity.setDisplayName(displayNameFrom(req));
            identity.setLastLoginAt(OffsetDateTime.now());
        }
        identityRepository.save(identity);

        // Check for pending invitation by phone number (will be checked during profile update)
        // Auto-assignment happens when user updates their phone number in profile

    String display;
    if (user.getFullName() != null) {
        display = user.getFullName();
    } else if (user.getDisplayName() != null) {
        display = user.getDisplayName();
    } else {
        display = user.getUsername();
    }

    String accessToken = jwtService.generateAccessToken(
        user.getId(),
        user.getUsername(),
        Map.of("provider", AUDIT_SOURCE_TELEGRAM)
    );

    String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getUsername());

    // Store refresh token
    jwtService.storeRefreshToken(refreshToken, user.getId(), req.username, "telegram-login");

    return ResponseEntity.ok(new AuthResponse(
        accessToken,
        refreshToken,
        user.getId(),
        display,
        user.getUsername(),
        AUDIT_SOURCE_TELEGRAM,
        14400L * 60, // 4 hours in seconds
        10080L * 60  // 7 days in seconds
    ));
    }

    private void auditUserChanges(UUID userId, String fieldName, String oldValue, String newValue, String source) {
        if (!Objects.equals(oldValue, newValue)) {
            UserAudit audit = new UserAudit();
            audit.setUserId(userId);
            audit.setFieldName(fieldName);
            audit.setOldValue(oldValue);
            audit.setNewValue(newValue);
            audit.setSource(source);
            userAuditRepository.save(audit);
        }
    }

    private static String displayNameFrom(TelegramVerifyRequest req) {
        String first = Optional.ofNullable(req.firstName).orElse("");
        String last = Optional.ofNullable(req.lastName).orElse("");
        String full = (first + " " + last).trim();
        if (!full.isEmpty()) return full;
        if (req.username != null) return req.username;
        return "TG-" + req.id;
    }

    @GetMapping("/dev/token/{userId}")
    public ResponseEntity<Object> devToken(@PathVariable UUID userId) {
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        // Temporarily enable dev tokens for testing and create user if not exists
        Optional<User> existingUser = userRepository.findById(userId);
        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            // Create a test user
            user = new User();
            user.setUsername("testuser-" + userId.toString().substring(0, 8));
            user.setDisplayName("Test User");
            user.setLastLoginAt(OffsetDateTime.now());
            
            // Create or find a default test company
            Company testCompany = companyRepository.findByName("Test Company").orElse(null);
            if (testCompany == null) {
                testCompany = new Company();
                testCompany.setName("Test Company");
                testCompany = companyRepository.save(testCompany);
            }
            user.setCompany(testCompany);
            
            user = userRepository.save(user);
            // Return the actual generated ID
            return ResponseEntity.ok(Map.of(
                    "token", jwtService.generateToken(user.getId(), user.getUsername(), Map.of("provider", "DEV")),
                    "userId", user.getId(),
                    "created", true
            ));
        }
        return ResponseEntity.ok(Map.of(
                "token", jwtService.generateToken(user.getId(), user.getUsername(), Map.of("provider", "DEV")),
                "userId", user.getId()
        ));
    }

    @GetMapping("/dev/login/{userId}")
    public ResponseEntity<Object> devLogin(@PathVariable UUID userId) {
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        // Get or create user
        Optional<User> existingUser = userRepository.findById(userId);
        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            // Create a test user with unique username
            user = new User();
            user.setUsername("devuser-" + userId.toString().replace("-", ""));
            user.setDisplayName("Dev User");
            user.setLastLoginAt(OffsetDateTime.now());
            
            // Create or find a default test company for dev users
            Company testCompany = companyRepository.findByName("Test Company").orElse(null);
            if (testCompany == null) {
                testCompany = new Company();
                testCompany.setName("Test Company");
                testCompany = companyRepository.save(testCompany);
            }
            user.setCompany(testCompany);
            user.setUserRole(UserRole.OWNER); // Make dev users owners of the test company
            
            user = userRepository.save(user);
        }

        String display;
        if (user.getFullName() != null) {
            display = user.getFullName();
        } else if (user.getDisplayName() != null) {
            display = user.getDisplayName();
        } else {
            display = user.getUsername();
        }

        String accessToken = jwtService.generateAccessToken(
            user.getId(),
            user.getUsername(),
            Map.of("provider", "DEV")
        );

        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getUsername());

        // Store refresh token
        jwtService.storeRefreshToken(refreshToken, user.getId(), user.getUsername(), "dev-login");

        return ResponseEntity.ok(new AuthResponse(
            accessToken,
            refreshToken,
            user.getId(),
            display,
            user.getUsername(),
            "DEV",
            14400L * 60, // 4 hours in seconds
            10080L * 60  // 7 days in seconds
        ));
    }

    public record ProfileUpdateRequest(
        UserType userType, 
        String firstName, 
        String lastName, 
        String displayName, 
        String companyName, 
        String avatarUrl,
        String email,
        String address,
        UUID defaultProvinceId,
        UUID defaultDistrictId
    ) {}

    public record ProfileResponse(UUID id, String displayName, String username, String firstName, String lastName, UserType userType, UserRole userRole, UUID companyId, String companyName, boolean incomplete, String avatarUrl, String phoneNumber, String email, String address, UUID defaultProvinceId, UUID defaultDistrictId) {}

    @GetMapping("/profile")
    @SuppressWarnings("null")
    public ResponseEntity<ProfileResponse> getProfile() {
        // Get current user from security context
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String userIdStr)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = optUser.get();
        return ResponseEntity.ok(new ProfileResponse(
            user.getId(),
            user.getDisplayName(),
            user.getUsername(),
            user.getFirstName(),
            user.getLastName(),
            user.getUserType(),
            user.getUserRole(),
            user.getCompany() != null ? user.getCompany().getId() : null,
            user.getCompany() != null ? user.getCompany().getName() : null,
            user.isIncomplete(),
            user.getAvatarUrl(),
            user.getPhoneE164(),
            user.getEmail(),
            user.getDefaultAddress(),
            user.getDefaultProvinceId(),
            user.getDefaultDistrictId()
        ));
    }

    @PutMapping("/profile")
    @Transactional
    public ResponseEntity<Object> updateProfile(@RequestBody ProfileUpdateRequest req) {
        // Get current user from security context
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String userIdStr)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = optUser.get();

        // Capture old values
        UserType oldUserType = user.getUserType();
        String oldFirstName = user.getFirstName();
        String oldLastName = user.getLastName();
        String oldDisplayName = user.getDisplayName();
        String oldCompanyName = user.getCompany() != null ? user.getCompany().getName() : null;
        String oldAvatarUrl = user.getAvatarUrl();
        String oldEmail = user.getEmail();
        String oldAddress = user.getDefaultAddress();
        UUID oldProvinceId = user.getDefaultProvinceId();
        UUID oldDistrictId = user.getDefaultDistrictId();

        // Update fields (phone number updates are not allowed for security reasons)
        if (req.userType != null) user.setUserType(req.userType);
        if (req.firstName != null) user.setFirstName(req.firstName);
        if (req.lastName != null) user.setLastName(req.lastName);
        if (req.displayName != null) user.setDisplayName(req.displayName);
        if (req.avatarUrl != null) user.setAvatarUrl(req.avatarUrl);
        if (req.email != null) user.setEmail(req.email);
        if (req.address != null) user.setDefaultAddress(req.address);
        if (req.defaultProvinceId != null) user.setDefaultProvinceId(req.defaultProvinceId);
        if (req.defaultDistrictId != null) user.setDefaultDistrictId(req.defaultDistrictId);
        // Note: phoneNumber updates are disabled for security - changing phone requires re-verification

        // Handle company
        if (req.companyName != null && !req.companyName.trim().isEmpty()) {
            String companyNameTrimmed = req.companyName.trim();

            // If user already has a company
            if (user.getCompany() != null) {
                // If user is OWNER of their company, allow them to rename it
                if (user.getUserRole() == UserRole.OWNER) {
                    // Update existing company name
                    user.getCompany().setName(companyNameTrimmed);
                    companyRepository.save(user.getCompany());
                } else {
                    // Non-owners cannot change company
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Cannot change company. Please leave your current company first."));
                }
            } else {
                // User has no company, create new one or join existing
                Optional<Company> optCompany = companyRepository.findByName(companyNameTrimmed);
                if (optCompany.isPresent()) {
                    Company company = optCompany.get();
                    // If joining existing company, check if this company has any users - if not, make them OWNER
                    long userCount = userRepository.countByCompanyId(company.getId());
                    if (userCount == 0) {
                        user.setUserRole(UserRole.OWNER);
                        user.setUserType(UserType.COMPANY);
                    }
                    user.setCompany(company);
                } else {
                    // Create new company and make user OWNER
                    Company company = new Company(companyNameTrimmed);
                    company.setCreatedByUser(user);
                    // Don't set createdByCompany for user's own company (self-created)
                    
                    // Populate company address from user's profile
                    if (user.getDefaultAddress() != null) {
                        company.setAddress(user.getDefaultAddress());
                    }
                    if (user.getDefaultProvinceId() != null) {
                        company.setProvinceId(user.getDefaultProvinceId());
                    }
                    if (user.getDefaultDistrictId() != null) {
                        company.setDistrictId(user.getDefaultDistrictId());
                    }
                    
                    company = companyRepository.save(company);
                    user.setCompany(company);
                    user.setUserRole(UserRole.OWNER);
                    user.setUserType(UserType.COMPANY);

                    // Create employee record for the owner (only if one doesn't exist)
                    Optional<Employee> existingEmployee = employeeRepository.findByUserIdAndCompanyId(userId, company.getId());
                    if (existingEmployee.isEmpty()) {
                        Employee employee = new Employee(user, company, UserRole.OWNER);
                        employeeRepository.save(employee);
                    }
                }
            }
        }

        // If incomplete, set to false after update
        if (user.isIncomplete()) {
            user.setIncomplete(false);
        }

        userRepository.save(user);

        // Note: Phone number updates are disabled for security reasons
        // Automatic assignment happens during initial OTP verification via handlePhoneVerificationAssignment()

        // Audit changes (phone number changes are not allowed)
        auditUserChanges(userId, "userType", oldUserType != null ? oldUserType.name() : null, req.userType != null ? req.userType.name() : null, AUDIT_TYPE_PROFILE_UPDATE);
        auditUserChanges(userId, "firstName", oldFirstName, req.firstName, AUDIT_TYPE_PROFILE_UPDATE);
        auditUserChanges(userId, "lastName", oldLastName, req.lastName, AUDIT_TYPE_PROFILE_UPDATE);
        auditUserChanges(userId, "displayName", oldDisplayName, req.displayName, AUDIT_TYPE_PROFILE_UPDATE);
        auditUserChanges(userId, "companyName", oldCompanyName, req.companyName, AUDIT_TYPE_PROFILE_UPDATE);
        auditUserChanges(userId, "avatarUrl", oldAvatarUrl, req.avatarUrl, AUDIT_TYPE_PROFILE_UPDATE);
        auditUserChanges(userId, "email", oldEmail, req.email, AUDIT_TYPE_PROFILE_UPDATE);
        auditUserChanges(userId, "address", oldAddress, req.address, AUDIT_TYPE_PROFILE_UPDATE);
        auditUserChanges(userId, "defaultProvinceId", oldProvinceId != null ? oldProvinceId.toString() : null, req.defaultProvinceId != null ? req.defaultProvinceId.toString() : null, AUDIT_TYPE_PROFILE_UPDATE);
        auditUserChanges(userId, "defaultDistrictId", oldDistrictId != null ? oldDistrictId.toString() : null, req.defaultDistrictId != null ? req.defaultDistrictId.toString() : null, AUDIT_TYPE_PROFILE_UPDATE);
        // Note: phoneNumber audit removed since phone updates are disabled

        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }

    public record ChangePasswordRequest(String currentPassword, String newPassword) {}

    @PostMapping("/change-password")
    @Transactional
    public ResponseEntity<Object> changePassword(@RequestBody ChangePasswordRequest req) {
        // Get current user from security context
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String userIdStr)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Validate request
        if (req.currentPassword == null || req.currentPassword.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Current password is required"));
        }
        if (req.newPassword == null || req.newPassword.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "New password is required"));
        }
        if (req.newPassword.length() < 8) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "New password must be at least 8 characters long"));
        }

        // Change password
        boolean success = adminUserService.changePassword(userId, req.currentPassword, req.newPassword);
        if (!success) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Current password is incorrect"));
        }

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    public record SetPasswordRequest(String newPassword) {}

    @PostMapping("/set-password")
    @Transactional
    public ResponseEntity<Object> setPassword(@RequestBody SetPasswordRequest req) {
        // Get current user from security context
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String userIdStr)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Validate request
        if (req.newPassword == null || req.newPassword.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "New password is required"));
        }
        if (req.newPassword.length() < 8) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "New password must be at least 8 characters long"));
        }

        // Set password
        boolean success = adminUserService.setPassword(userId, req.newPassword);
        if (!success) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "System administrators cannot use this endpoint"));
        }

        return ResponseEntity.ok(Map.of("message", "Password set successfully"));
    }

    // This method should be called when phone number is first verified during OTP process
    // It handles automatic company assignment for pending employee invitations
    @Transactional
    public void handlePhoneVerificationAssignment(User user, String phoneNumber) {
        companyAssignmentService.handlePhoneVerificationAssignment(user, phoneNumber);
    }

    // QR Invitation endpoints

    @PostMapping("/companies/{companyId}/add-employee")
    public ResponseEntity<Object> addEmployee(@PathVariable UUID companyId, @RequestBody AddEmployeeRequest req) {
        // Check if current user has permission to add employees
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String userIdStr)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID userId = UUID.fromString(userIdStr);
        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = optUser.get();

        // Check company membership and role permissions
        if (user.getCompany() == null || !user.getCompany().getId().equals(companyId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You must be a member of this company"));
        }

        // Check if user has permission to add the requested role
        boolean canAssign = false;
        String errorMessage = "Insufficient permissions to add employee";

        if (user.getUserRole() == UserRole.OWNER) {
            // Owners can add any role
            canAssign = true;
        } else if (user.getUserRole() == UserRole.MANAGER) {
            // Managers can add STAFF and DRIVER roles
            if (req.userRole == UserRole.STAFF || req.userRole == UserRole.DRIVER) {
                canAssign = true;
            } else {
                errorMessage = "Managers can only add staff and driver employees";
            }
        } else {
            errorMessage = "Only owners and managers can add employees";
        }

        if (!canAssign) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", errorMessage));
        }

        // Check if a pending employee already exists for this phone number and company
        if (pendingEmployeeRepository.existsByPhoneE164AndCompanyId(req.phoneNumber, companyId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "An employee invitation already exists for this phone number"));
        }

        // Get the company
        Optional<Company> companyOpt = companyRepository.findById(companyId);
        if (companyOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Company not found"));
        }
        Company company = companyOpt.get();

        // Check if phone number already belongs to an existing user
        Optional<User> existingUser = userRepository.findByPhoneE164(req.phoneNumber);
        if (existingUser.isPresent()) {
            User existingUserObj = existingUser.get();
            
            // Check if user already has an employee record in THIS company
            Optional<Employee> existingEmployee = employeeRepository.findByUserIdAndCompanyId(existingUserObj.getId(), companyId);
            if (existingEmployee.isPresent()) {
                Employee emp = existingEmployee.get();
                if (emp.isActive()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "This user is already an active employee in your company"));
                } else {
                    // Reactivate the existing employee record
                    emp.setActive(true);
                    emp.setUserRole(req.userRole);
                    employeeRepository.save(emp);
                    
                    // Update user details
                    existingUserObj.setUserRole(req.userRole);
                    existingUserObj.setCompany(company);
                    if (req.firstName != null) existingUserObj.setFirstName(req.firstName);
                    if (req.lastName != null) existingUserObj.setLastName(req.lastName);
                    if (req.displayName != null) existingUserObj.setDisplayName(req.displayName);
                    if (req.email != null) existingUserObj.setEmail(req.email);
                    userRepository.save(existingUserObj);
                    
                    return ResponseEntity.ok(Map.of(
                        "message", "Employee reactivated successfully",
                        "userId", existingUserObj.getId(),
                        "phoneNumber", req.phoneNumber,
                        "userRole", req.userRole
                    ));
                }
            } else {
                // User exists but not in this company - check if they're in another company
                if (existingUserObj.getCompany() != null && !existingUserObj.getCompany().getId().equals(companyId)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "This user is already employed by another company"));
                }
                
                // User exists but no employee record - create employee record directly
                existingUserObj.setCompany(company);
                existingUserObj.setUserRole(req.userRole);
                existingUserObj.setUserType(UserType.COMPANY);
                if (req.firstName != null) existingUserObj.setFirstName(req.firstName);
                if (req.lastName != null) existingUserObj.setLastName(req.lastName);
                if (req.displayName != null) existingUserObj.setDisplayName(req.displayName);
                if (req.email != null) existingUserObj.setEmail(req.email);
                userRepository.save(existingUserObj);
                
                Employee newEmployee = new Employee(existingUserObj, company, req.userRole);
                employeeRepository.save(newEmployee);
                
                return ResponseEntity.ok(Map.of(
                    "message", "Employee added successfully (user already existed)",
                    "userId", existingUserObj.getId(),
                    "phoneNumber", req.phoneNumber,
                    "userRole", req.userRole
                ));
            }
        }

        // For DRIVER role, create user immediately without pending invitation
        if (req.userRole == UserRole.DRIVER) {
            try {
                // Create user directly
                User newUser = new User();
                newUser.setPhoneE164(req.phoneNumber);
                newUser.setUsername("u_" + req.phoneNumber);
                newUser.setCompany(company);
                newUser.setUserRole(req.userRole);
                newUser.setUserType(UserType.COMPANY);
                newUser.setActive(true);
                newUser.setIncomplete(true); // Mark as incomplete until they verify phone
                
                if (req.firstName != null) newUser.setFirstName(req.firstName);
                if (req.lastName != null) newUser.setLastName(req.lastName);
                if (req.displayName != null) newUser.setDisplayName(req.displayName);
                if (req.email != null) newUser.setEmail(req.email);
                if (req.address != null) newUser.setDefaultAddress(req.address);
                if (req.defaultProvinceId != null) newUser.setDefaultProvinceId(req.defaultProvinceId);
                if (req.defaultDistrictId != null) newUser.setDefaultDistrictId(req.defaultDistrictId);
                
                newUser = userRepository.save(newUser);
                
                // Create employee record
                Employee employee = new Employee(newUser, company, req.userRole);
                employeeRepository.save(employee);
                
                return ResponseEntity.ok(Map.of(
                    "message", "Driver created successfully (can be assigned to deliveries immediately)",
                    "userId", newUser.getId(),
                    "phoneNumber", req.phoneNumber,
                    "userRole", req.userRole,
                    "note", "Driver can verify phone later to access the app"
                ));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to create driver: " + e.getMessage()));
            }
        }

        // For other roles (STAFF, MANAGER, etc.), use pending invitation flow
        // Check if phone number has active pending invitations in any company
        Optional<PendingEmployee> activePending = pendingEmployeeRepository.findActiveByPhone(req.phoneNumber, java.time.Instant.now());
        if (activePending.isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "This phone number has a pending invitation in another company"));
        }

        try {
            // Create pending employee record for new user
            PendingEmployee pendingEmployee = new PendingEmployee();
            pendingEmployee.setPhoneE164(req.phoneNumber);
            pendingEmployee.setCompany(company);
            pendingEmployee.setRole(req.userRole);
            pendingEmployee.setFirstName(req.firstName);
            pendingEmployee.setLastName(req.lastName);
            pendingEmployee.setDisplayName(req.displayName);
            pendingEmployee.setEmail(req.email);
            pendingEmployee.setAddress(req.address);
            pendingEmployee.setDefaultProvinceId(req.defaultProvinceId);
            pendingEmployee.setDefaultDistrictId(req.defaultDistrictId);
            pendingEmployee.setExpiresAt(java.time.Instant.now().plusSeconds(7L * 24 * 60 * 60)); // 7 days

            pendingEmployeeRepository.save(pendingEmployee);

            return ResponseEntity.ok(Map.of(
                "message", "Employee invitation created successfully",
                "phoneNumber", pendingEmployee.getPhoneE164(),
                "userRole", pendingEmployee.getRole(),
                "expiresAt", pendingEmployee.getExpiresAt()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create employee invitation"));
        }
    }

    public record AddEmployeeRequest(
        UserRole userRole,
        String phoneNumber,
        String firstName,
        String lastName,
        String displayName,
        String email,
        String address,
        UUID defaultProvinceId,
        UUID defaultDistrictId
    ) {}

    @PostMapping("/leave-company")
    @Transactional
    public ResponseEntity<Object> leaveCompany() {
        // Get current user
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String userIdStr)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID userId = UUID.fromString(userIdStr);
        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = optUser.get();

        // Check if user is in a company
        if (user.getCompany() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "User is not part of any company"));
        }

        // Deactivate employee record
        Optional<Employee> employeeOpt = employeeRepository.findByUserIdAndCompanyId(userId, user.getCompany().getId());
        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            employee.setActive(false);
            employeeRepository.save(employee);
        }

        // Remove user from company
        user.setCompany(null);
        user.setUserRole(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Successfully left company"));
    }

    public record EmployeeInfo(UUID id, String displayName, String firstName, String lastName, String phoneNumber, UserRole userRole, String status, String invitedAt, String joinedAt) {}
    public record CompanyEmployeesResponse(List<EmployeeInfo> employees, int totalCount) {}

    @GetMapping("/companies/{companyId}/employees")
    public ResponseEntity<Object> getCompanyEmployees(@PathVariable UUID companyId) {
        // Check if current user has permission to view employees
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String userIdStr)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID userId = UUID.fromString(userIdStr);
        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = optUser.get();

        // System administrators can view employees from any company
        boolean isSystemAdmin = user.getUserRole() == UserRole.SYSTEM_ADMINISTRATOR;

        // Regular users must be members of the company
        if (!isSystemAdmin && (user.getCompany() == null || !user.getCompany().getId().equals(companyId))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You must be a member of this company"));
        }

        // Only owners, managers, and system administrators can view employees
        if (!isSystemAdmin && user.getUserRole() != UserRole.OWNER && user.getUserRole() != UserRole.MANAGER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only owners and managers can view employees"));
        }

        // Get all employees (active and inactive)
        List<Employee> allEmployees = employeeRepository.findByCompanyId(companyId);

        // Get pending employees
        var pendingEmployees = pendingEmployeeRepository.findByCompanyIdAndExpiresAtAfter(companyId, java.time.Instant.now());

        // Combine into response
        List<EmployeeInfo> employeeInfos = new java.util.ArrayList<>();

        // Add all employees with their status
        for (Employee emp : allEmployees) {
            employeeInfos.add(new EmployeeInfo(
                emp.getUser().getId(),
                emp.getUser().getDisplayName(),
                emp.getUser().getFirstName(),
                emp.getUser().getLastName(),
                emp.getUser().getPhoneE164(),
                emp.getUserRole(),
                emp.isActive() ? "ACTIVE" : "INACTIVE",
                null, // invitedAt not applicable for active employees
                emp.getCreatedAt() != null ? emp.getCreatedAt().toString() : null
            ));
        }

        // Add pending employees
        for (var pendingEmployee : pendingEmployees) {
            employeeInfos.add(new EmployeeInfo(
                null, // no user ID for pending employees
                null, // no display name
                null, // no first name
                null, // no last name
                pendingEmployee.getPhoneE164(),
                pendingEmployee.getRole(),
                "PENDING",
                pendingEmployee.getCreatedAt() != null ? pendingEmployee.getCreatedAt().toString() : null,
                null // joinedAt not applicable for pending employees
            ));
        }

        return ResponseEntity.ok(new CompanyEmployeesResponse(employeeInfos, employeeInfos.size()));
    }

    @PutMapping("/companies/{companyId}/employees/{userId}/deactivate")
    @Transactional
    public ResponseEntity<Object> deactivateEmployee(@PathVariable UUID companyId, @PathVariable UUID userId) {
        // Check if current user has permission
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String currentUserIdStr)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID currentUserId = UUID.fromString(currentUserIdStr);
        Optional<User> optCurrentUser = userRepository.findById(currentUserId);
        if (optCurrentUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User currentUser = optCurrentUser.get();

        log.info("Deactivate request - Current user: {}, role: {}, companyId: {}, target userId: {}", 
            currentUserId, currentUser.getUserRole(), 
            currentUser.getCompany() != null ? currentUser.getCompany().getId() : "null", userId);

        // Check company membership
        if (currentUser.getCompany() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "You are not a member of any company", "details", "Your company is null"));
        }
        if (!currentUser.getCompany().getId().equals(companyId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "You must be a member of this company", 
                    "yourCompanyId", currentUser.getCompany().getId().toString(),
                    "requestedCompanyId", companyId.toString()));
        }

        // Get the employee record
        Optional<Employee> optEmployee = employeeRepository.findByUserIdAndCompanyId(userId, companyId);
        if (optEmployee.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Employee not found", 
                    "details", "No employee record found for userId=" + userId + " in companyId=" + companyId));
        }
        Employee employee = optEmployee.get();

        // Check permissions
        boolean canDeactivate = false;
        String errorMessage = "Insufficient permissions to deactivate employee";

        if (currentUser.getUserRole() == UserRole.OWNER) {
            // Owners can deactivate any employee except themselves
            if (currentUserId.equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You cannot deactivate yourself", 
                        "yourUserId", currentUserId.toString()));
            }
            canDeactivate = true;
        } else if (currentUser.getUserRole() == UserRole.MANAGER) {
            // Managers can deactivate STAFF and DRIVER roles only
            if (employee.getUserRole() == UserRole.STAFF || employee.getUserRole() == UserRole.DRIVER) {
                canDeactivate = true;
            } else {
                errorMessage = "Managers can only deactivate staff and driver employees";
            }
        } else {
            errorMessage = "Only owners and managers can deactivate employees";
        }

        if (!canDeactivate) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", errorMessage, 
                    "yourRole", currentUser.getUserRole().toString(),
                    "targetRole", employee.getUserRole().toString()));
        }

        // Deactivate the employee
        employee.setActive(false);
        employeeRepository.save(employee);

        log.info("Employee deactivated successfully - userId: {}, companyId: {}", userId, companyId);
        return ResponseEntity.ok(Map.of("message", "Employee deactivated successfully"));
    }

    @PutMapping("/companies/{companyId}/employees/{userId}/activate")
    @Transactional
    public ResponseEntity<Object> activateEmployee(@PathVariable UUID companyId, @PathVariable UUID userId) {
        // Check if current user has permission
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String currentUserIdStr)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID currentUserId = UUID.fromString(currentUserIdStr);
        Optional<User> optCurrentUser = userRepository.findById(currentUserId);
        if (optCurrentUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User currentUser = optCurrentUser.get();

        // Check company membership
        if (currentUser.getCompany() == null || !currentUser.getCompany().getId().equals(companyId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You must be a member of this company"));
        }

        // Get the employee record
        Optional<Employee> optEmployee = employeeRepository.findByUserIdAndCompanyId(userId, companyId);
        if (optEmployee.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Employee not found"));
        }
        Employee employee = optEmployee.get();

        // Check permissions
        boolean canActivate = false;
        String errorMessage = "Insufficient permissions to activate employee";

        if (currentUser.getUserRole() == UserRole.OWNER) {
            canActivate = true;
        } else if (currentUser.getUserRole() == UserRole.MANAGER) {
            // Managers can activate STAFF and DRIVER roles only
            if (employee.getUserRole() == UserRole.STAFF || employee.getUserRole() == UserRole.DRIVER) {
                canActivate = true;
            } else {
                errorMessage = "Managers can only activate staff and driver employees";
            }
        } else {
            errorMessage = "Only owners and managers can activate employees";
        }

        if (!canActivate) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", errorMessage));
        }

        // Activate the employee
        employee.setActive(true);
        employeeRepository.save(employee);

        return ResponseEntity.ok(Map.of("message", "Employee activated successfully"));
    }

    @DeleteMapping("/companies/{companyId}/employees/{userId}")
    @Transactional
    public ResponseEntity<Object> deleteEmployee(@PathVariable UUID companyId, @PathVariable UUID userId) {
        // Check if current user has permission
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String currentUserIdStr)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID currentUserId = UUID.fromString(currentUserIdStr);
        Optional<User> optCurrentUser = userRepository.findById(currentUserId);
        if (optCurrentUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User currentUser = optCurrentUser.get();

        // Check company membership
        if (currentUser.getCompany() == null || !currentUser.getCompany().getId().equals(companyId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You must be a member of this company"));
        }

        // Only OWNER can delete employees
        if (currentUser.getUserRole() != UserRole.OWNER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Only company owners can permanently delete employees"));
        }

        // Prevent owner from deleting themselves
        if (currentUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "You cannot delete yourself"));
        }

        // Get the employee record
        Optional<Employee> optEmployee = employeeRepository.findByUserIdAndCompanyId(userId, companyId);
        if (optEmployee.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Employee not found"));
        }

        // Delete the employee record
        employeeRepository.delete(optEmployee.get());

        return ResponseEntity.ok(Map.of("message", "Employee deleted successfully"));
    }

    public record UpdateEmployeeRequest(
        UserRole userRole,
        String firstName,
        String lastName,
        String displayName,
        String email,
        String phoneNumber,
        String address,
        UUID defaultProvinceId,
        UUID defaultDistrictId
    ) {}

    @PutMapping("/companies/{companyId}/employees/{userId}")
    @Transactional
    public ResponseEntity<Object> updateEmployee(@PathVariable UUID companyId, @PathVariable UUID userId, @RequestBody UpdateEmployeeRequest req) {
        // Check if current user has permission
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String currentUserIdStr)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID currentUserId = UUID.fromString(currentUserIdStr);
        Optional<User> optCurrentUser = userRepository.findById(currentUserId);
        if (optCurrentUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User currentUser = optCurrentUser.get();

        // Check company membership
        if (currentUser.getCompany() == null || !currentUser.getCompany().getId().equals(companyId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You must be a member of this company"));
        }

        // Get the employee record
        Optional<Employee> optEmployee = employeeRepository.findByUserIdAndCompanyId(userId, companyId);
        if (optEmployee.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Employee not found"));
        }
        Employee employee = optEmployee.get();
        User employeeUser = employee.getUser();

        // Check permissions
        boolean canUpdate = false;
        String errorMessage = "Insufficient permissions to update employee";

        if (currentUser.getUserRole() == UserRole.OWNER) {
            canUpdate = true;
        } else if (currentUser.getUserRole() == UserRole.MANAGER) {
            // Managers can update STAFF and DRIVER roles only
            if (employee.getUserRole() == UserRole.STAFF || employee.getUserRole() == UserRole.DRIVER) {
                canUpdate = true;
            } else {
                errorMessage = "Managers can only update staff and driver employees";
            }
        } else {
            errorMessage = "Only owners and managers can update employees";
        }

        if (!canUpdate) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", errorMessage));
        }

        // Update user fields
        boolean updated = false;
        
        if (req.firstName != null && !req.firstName.equals(employeeUser.getFirstName())) {
            employeeUser.setFirstName(req.firstName);
            updated = true;
        }
        if (req.lastName != null && !req.lastName.equals(employeeUser.getLastName())) {
            employeeUser.setLastName(req.lastName);
            updated = true;
        }
        if (req.displayName != null && !req.displayName.equals(employeeUser.getDisplayName())) {
            employeeUser.setDisplayName(req.displayName);
            updated = true;
        }
        if (req.email != null && !req.email.equals(employeeUser.getEmail())) {
            // Check if email is already used by another user (only if not empty)
            if (!req.email.trim().isEmpty()) {
                // Email uniqueness will be validated by database constraint
                // If it fails, DataIntegrityViolationException will be thrown
            }
            employeeUser.setEmail(req.email.trim().isEmpty() ? null : req.email);
            updated = true;
        }
        if (req.phoneNumber != null && !req.phoneNumber.equals(employeeUser.getPhoneE164())) {
            // Check if phone is already used by another user
            if (!req.phoneNumber.trim().isEmpty()) {
                Optional<User> existingPhone = userRepository.findByPhoneE164(req.phoneNumber);
                if (existingPhone.isPresent() && !existingPhone.get().getId().equals(userId)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Phone number is already used by another user"));
                }
            }
            employeeUser.setPhoneE164(req.phoneNumber.trim().isEmpty() ? null : req.phoneNumber);
            if (!req.phoneNumber.trim().isEmpty()) {
                employeeUser.setUsername("u_" + req.phoneNumber); // Update username to match phone
            }
            updated = true;
        }
        if (req.address != null && !req.address.equals(employeeUser.getDefaultAddress())) {
            employeeUser.setDefaultAddress(req.address.trim().isEmpty() ? null : req.address);
            updated = true;
        }
        if (req.defaultProvinceId != null && !req.defaultProvinceId.equals(employeeUser.getDefaultProvinceId())) {
            employeeUser.setDefaultProvinceId(req.defaultProvinceId);
            updated = true;
        }
        if (req.defaultDistrictId != null && !req.defaultDistrictId.equals(employeeUser.getDefaultDistrictId())) {
            employeeUser.setDefaultDistrictId(req.defaultDistrictId);
            updated = true;
        }

        // Update role if changed
        if (req.userRole != null && req.userRole != employee.getUserRole()) {
            // Check if user has permission to assign the new role
            if (currentUser.getUserRole() == UserRole.MANAGER) {
                if (req.userRole != UserRole.STAFF && req.userRole != UserRole.DRIVER) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Managers can only assign STAFF and DRIVER roles"));
                }
            }
            employee.setUserRole(req.userRole);
            employeeUser.setUserRole(req.userRole);
            employeeRepository.save(employee);
            updated = true;
        }

        if (updated) {
            userRepository.save(employeeUser);
            return ResponseEntity.ok(Map.of("message", "Employee updated successfully"));
        } else {
            return ResponseEntity.ok(Map.of("message", "No changes made"));
        }
    }

    @DeleteMapping("/companies/{companyId}/pending-employees/{phoneNumber}")
    @Transactional
    public ResponseEntity<Object> cancelPendingInvitation(@PathVariable UUID companyId, @PathVariable String phoneNumber) {
        // Check if current user has permission
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String currentUserIdStr)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID currentUserId = UUID.fromString(currentUserIdStr);
        Optional<User> optCurrentUser = userRepository.findById(currentUserId);
        if (optCurrentUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User currentUser = optCurrentUser.get();

        // Check company membership
        if (currentUser.getCompany() == null || !currentUser.getCompany().getId().equals(companyId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You must be a member of this company"));
        }

        // Check permissions - only OWNER and MANAGER can cancel invitations
        if (currentUser.getUserRole() != UserRole.OWNER && currentUser.getUserRole() != UserRole.MANAGER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Only owners and managers can cancel pending invitations"));
        }

        // Find the pending employee
        Optional<com.delivery.deliveryapi.model.PendingEmployee> optPending = 
            pendingEmployeeRepository.findByPhoneE164AndCompanyId(phoneNumber, companyId);
        
        if (optPending.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Pending invitation not found"));
        }

        com.delivery.deliveryapi.model.PendingEmployee pendingEmployee = optPending.get();

        // Managers can only cancel STAFF and DRIVER invitations
        if (currentUser.getUserRole() == UserRole.MANAGER) {
            if (pendingEmployee.getRole() != UserRole.STAFF && pendingEmployee.getRole() != UserRole.DRIVER) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Managers can only cancel staff and driver invitations"));
            }
        }

        // Delete the pending invitation
        pendingEmployeeRepository.delete(pendingEmployee);

        return ResponseEntity.ok(Map.of("message", "Pending invitation canceled successfully"));
    }

    public record LoginRequest(String username, String password) {}

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody LoginRequest req) {
        try {
            // Authenticate admin user
            boolean authenticated = adminUserService.authenticateAdmin(req.username, req.password);
            
            if (!authenticated) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid username or password"));
            }

            // Get user details
            Optional<User> optUser = adminUserService.findAdminByUsername(req.username);
            if (optUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }
            User user = optUser.get();

            String display;
            if (user.getFullName() != null) {
                display = user.getFullName();
            } else if (user.getDisplayName() != null) {
                display = user.getDisplayName();
            } else {
                display = user.getUsername();
            }

            String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getUsername(),
                Map.of("provider", "LOCAL")
            );

            String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getUsername());

            // Store refresh token
            jwtService.storeRefreshToken(refreshToken, user.getId(), user.getUsername(), "local-login");

            return ResponseEntity.ok(new AuthResponse(
                accessToken,
                refreshToken,
                user.getId(),
                display,
                user.getUsername(),
                "LOCAL",
                14400L * 60, // 4 hours in seconds
                10080L * 60  // 7 days in seconds
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Login failed"));
        }
    }

    public record RefreshRequest(String refreshToken) {}

    public record RefreshResponse(String accessToken, long accessExpiresIn) {}

    @PostMapping("/refresh")
    public ResponseEntity<Object> refreshToken(@RequestBody RefreshRequest req) {
        try {
            // Validate refresh token
            Claims claims = jwtService.validateRefreshToken(req.refreshToken);
            String userIdStr = claims.getSubject();
            UUID userId = UUID.fromString(userIdStr);

            // Get user
            Optional<User> optUser = userRepository.findById(userId);
            if (optUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }
            User user = optUser.get();

            // Generate new access token
            String newAccessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getUsername(),
                Map.of("provider", "refresh")
            );

            return ResponseEntity.ok(new RefreshResponse(
                newAccessToken,
                14400L * 60 // 4 hours in seconds
            ));

        } catch (io.jsonwebtoken.security.SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid refresh token"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Token refresh failed"));
        }
    }
}
