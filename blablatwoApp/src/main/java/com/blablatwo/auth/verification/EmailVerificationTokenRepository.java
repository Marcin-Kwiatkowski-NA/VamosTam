package com.blablatwo.auth.verification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE EmailVerificationToken t SET t.usedAt = CURRENT_TIMESTAMP " +
           "WHERE t.user.id = :userId AND t.usedAt IS NULL")
    void invalidateAllForUser(@Param("userId") Long userId);

    @Query("SELECT MAX(t.createdAt) FROM EmailVerificationToken t WHERE t.user.id = :userId")
    Optional<Instant> findLatestCreatedAtForUser(@Param("userId") Long userId);

    void deleteByExpiresAtBefore(Instant cutoff);
}
