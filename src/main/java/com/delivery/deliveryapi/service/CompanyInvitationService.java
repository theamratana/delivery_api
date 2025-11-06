package com.delivery.deliveryapi.service;

import com.delivery.deliveryapi.model.Company;
import com.delivery.deliveryapi.model.CompanyInvitation;
import com.delivery.deliveryapi.model.UserRole;
import com.delivery.deliveryapi.repo.CompanyInvitationRepository;
import com.delivery.deliveryapi.repo.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CompanyInvitationService {

    private final CompanyInvitationRepository invitationRepository;
    private final CompanyRepository companyRepository;

    public CompanyInvitationService(CompanyInvitationRepository invitationRepository, CompanyRepository companyRepository) {
        this.invitationRepository = invitationRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional
    public CompanyInvitation generateInvitation(UUID companyId, UserRole role, int hoursValid) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(hoursValid);
        String invitationCode = generateInvitationCode();

        CompanyInvitation invitation = new CompanyInvitation(company, role, expiresAt, invitationCode);
        return invitationRepository.save(invitation);
    }

    @Transactional
    public Optional<CompanyInvitation> getInvitationDetails(String invitationCode) {
        LocalDateTime now = LocalDateTime.now();
        return invitationRepository.findByInvitationCodeAndExpiresAtAfter(invitationCode, now);
    }

    public List<CompanyInvitation> getActiveInvitations(UUID companyId) {
        LocalDateTime now = LocalDateTime.now();
        return invitationRepository.findByCompanyIdAndExpiresAtAfter(companyId, now);
    }

    @Transactional
    public CompanyInvitation assignDriverByPhone(UUID companyId, UserRole role, String phoneNumber, int hoursValid) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(hoursValid);
        String invitationCode = generateInvitationCode();

        CompanyInvitation invitation = new CompanyInvitation(company, role, expiresAt, invitationCode, phoneNumber);
        return invitationRepository.save(invitation);
    }

    @Transactional
    public Optional<CompanyInvitation> getInvitationByPhone(String phoneNumber) {
        LocalDateTime now = LocalDateTime.now();
        return invitationRepository.findByPhoneNumberAndExpiresAtAfter(phoneNumber, now);
    }

    private String generateInvitationCode() {
        // Generate a unique invitation code (you might want to use a more secure method)
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}