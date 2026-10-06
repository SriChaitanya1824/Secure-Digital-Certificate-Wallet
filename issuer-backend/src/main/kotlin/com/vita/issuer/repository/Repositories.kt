package com.vita.issuer.repository

import com.vita.issuer.entity.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserRepository : JpaRepository<User, String> {
    fun findByEmail(email: String): Optional<User>
    fun existsByEmail(email: String): Boolean
}

@Repository
interface IssuerRepository : JpaRepository<Issuer, String> {
    fun findByStatus(status: IssuerStatus): List<Issuer>
}

@Repository
interface CertificateRepository : JpaRepository<Certificate, String> {
    fun findByCertificateId(certificateId: String): Optional<Certificate>
    fun findByStatus(status: CertificateStatus): List<Certificate>
    fun findByIssuer(issuer: Issuer): List<Certificate>

    @Query("SELECT c FROM Certificate c WHERE c.expiresAt < ?1 AND c.status = 'ACTIVE'")
    fun findExpiredCertificates(currentTime: Long): List<Certificate>
}

@Repository
interface CertificateClaimRepository : JpaRepository<CertificateClaim, String> {
    fun findByCertificate(certificate: Certificate): List<CertificateClaim>
}

@Repository
interface RevocationRepository : JpaRepository<Revocation, String> {
    fun findByCertificate(certificate: Certificate): Optional<Revocation>
}

@Repository
interface CertificateEventRepository : JpaRepository<CertificateEvent, String> {
    fun findByCertificate(certificate: Certificate): List<CertificateEvent>
    fun findByEventType(eventType: EventType): List<CertificateEvent>
}

@Repository
interface VerificationEventRepository : JpaRepository<VerificationEvent, String> {
    fun findByCertificate(certificate: Certificate): List<VerificationEvent>
}

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, String> {
    fun findByToken(token: String): Optional<RefreshToken>
    fun findByUser(user: User): List<RefreshToken>
    fun deleteByExpiresAtLessThan(expiresAt: Long)
}

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, String> {
    fun findByUser(user: User): List<AuditLog>
    fun findByAction(action: AuditAction): List<AuditLog>
}
