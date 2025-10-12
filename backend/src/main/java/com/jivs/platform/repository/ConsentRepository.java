package com.jivs.platform.repository;

import com.jivs.platform.domain.compliance.Consent;
import com.jivs.platform.domain.compliance.ConsentPurpose;
import com.jivs.platform.domain.compliance.ConsentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Consent entity
 */
@Repository
public interface ConsentRepository extends JpaRepository<Consent, Long> {

    /**
     * Find all consents by user
     */
    List<Consent> findByUserId(Long userId);

    /**
     * Find consents by purpose
     */
    List<Consent> findByPurpose(ConsentPurpose purpose);

    /**
     * Find consents by status
     */
    List<Consent> findByStatus(ConsentStatus status);

    /**
     * Find consents by user and purpose
     */
    List<Consent> findByUserIdAndPurpose(Long userId, ConsentPurpose purpose);

    /**
     * Find consents by user and status
     */
    List<Consent> findByUserIdAndStatus(Long userId, ConsentStatus status);

    /**
     * Find active consent for user and purpose
     */
    Optional<Consent> findFirstByUserIdAndPurposeAndStatusOrderByGrantedAtDesc(
        Long userId, ConsentPurpose purpose, ConsentStatus status);

    /**
     * Find expired consents
     */
    @Query("SELECT c FROM Consent c WHERE c.expiresAt < :now AND c.status = 'GRANTED'")
    List<Consent> findExpiredConsents(@Param("now") LocalDateTime now);

    /**
     * Find consents expiring soon
     */
    @Query("SELECT c FROM Consent c WHERE c.expiresAt BETWEEN :now AND :future " +
           "AND c.status = 'GRANTED'")
    List<Consent> findConsentsExpiringSoon(@Param("now") LocalDateTime now,
                                           @Param("future") LocalDateTime future);

    /**
     * Find consents granted within date range
     */
    List<Consent> findByGrantedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find consents withdrawn within date range
     */
    List<Consent> findByWithdrawnAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find valid consents for user
     */
    @Query("SELECT c FROM Consent c WHERE c.userId = :userId AND c.status = 'GRANTED' " +
           "AND (c.expiresAt IS NULL OR c.expiresAt > :now)")
    List<Consent> findValidConsentsByUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * Find consents by consent version
     */
    List<Consent> findByConsentVersion(String version);

    /**
     * Check if user has valid consent for purpose
     */
    @Query("SELECT COUNT(c) > 0 FROM Consent c WHERE c.userId = :userId " +
           "AND c.purpose = :purpose AND c.status = 'GRANTED' " +
           "AND (c.expiresAt IS NULL OR c.expiresAt > :now)")
    boolean hasValidConsent(@Param("userId") Long userId,
                           @Param("purpose") ConsentPurpose purpose,
                           @Param("now") LocalDateTime now);

    /**
     * Count consents by status
     */
    long countByStatus(ConsentStatus status);

    /**
     * Count consents by purpose
     */
    long countByPurpose(ConsentPurpose purpose);

    /**
     * Count consents by user and status
     */
    long countByUserIdAndStatus(Long userId, ConsentStatus status);

    /**
     * Find consents by subject email, purpose and active status
     */
    List<Consent> findBySubjectEmailAndPurposeAndActive(String subjectEmail, ConsentPurpose purpose, boolean active);

    /**
     * Find consents by consent date between
     */
    List<Consent> findByConsentDateBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find consents by active and expiry date before
     */
    List<Consent> findByActiveAndExpiresAtBefore(boolean active, LocalDateTime expiryDate);

    /**
     * Find consents by active and expiry date before (alias method)
     */
    default List<Consent> findByActiveAndExpiryDateBefore(boolean active, LocalDateTime expiryDate) {
        return findByActiveAndExpiresAtBefore(active, expiryDate);
    }
}
