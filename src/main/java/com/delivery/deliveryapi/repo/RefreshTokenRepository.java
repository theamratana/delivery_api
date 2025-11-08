package com.delivery.deliveryapi.repo;

import com.delivery.deliveryapi.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserId(UUID userId);

    boolean existsByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") OffsetDateTime now);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.userId = :userId")
    int deleteByUserId(@Param("userId") UUID userId);
}