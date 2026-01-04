package com.delivery.deliveryapi.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
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
}