package com.delivery.deliveryapi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "company_invitations")
public class CompanyInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, unique = true)
    private String invitationCode;

    @Column
    private String phoneNumber; // For phone-based assignments

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Constructors
    public CompanyInvitation() {}

    public CompanyInvitation(Company company, UserRole role, LocalDateTime expiresAt, String invitationCode) {
        this.company = company;
        this.role = role;
        this.expiresAt = expiresAt;
        this.invitationCode = invitationCode;
        this.createdAt = LocalDateTime.now();
    }

    public CompanyInvitation(Company company, UserRole role, LocalDateTime expiresAt, String invitationCode, String phoneNumber) {
        this.company = company;
        this.role = role;
        this.expiresAt = expiresAt;
        this.invitationCode = invitationCode;
        this.phoneNumber = phoneNumber;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getInvitationCode() { return invitationCode; }
    public void setInvitationCode(String invitationCode) { this.invitationCode = invitationCode; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}