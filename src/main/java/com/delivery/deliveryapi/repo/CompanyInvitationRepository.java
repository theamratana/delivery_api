package com.delivery.deliveryapi.repo;

import com.delivery.deliveryapi.model.CompanyInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyInvitationRepository extends JpaRepository<CompanyInvitation, Long> {

    Optional<CompanyInvitation> findByInvitationCodeAndExpiresAtAfter(String invitationCode, LocalDateTime now);

    List<CompanyInvitation> findByCompanyIdAndExpiresAtAfter(UUID companyId, LocalDateTime now);

    Optional<CompanyInvitation> findByPhoneNumberAndExpiresAtAfter(String phoneNumber, LocalDateTime now);

    List<CompanyInvitation> findByExpiresAtBefore(LocalDateTime now);
}