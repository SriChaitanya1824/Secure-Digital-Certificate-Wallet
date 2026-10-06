package com.vita.issuer.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

/**
 * User entity for authentication
 */
@Entity
@Table(name = "users", indexes = [
    Index(name = "idx_email", columnList = "email", unique = true)
])
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",

    @Column(nullable = false, unique = true)
    val email: String = "",

    @Column(nullable = false)
    val name: String = "",

    @Column(nullable = false)
    val passwordHash: String = "",

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(nullable = false)
    val updatedAt: Instant = Instant.now()
)

/**
 * Issuer entity representing an organization issuing certificates
 */
@Entity
@Table(name = "issuers", indexes = [
    Index(name = "idx_issuer_status", columnList = "status")
])
data class Issuer(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",

    @Column(nullable = false)
    val name: String = "",

    @Column(nullable = false, columnDefinition = "TEXT")
    val publicKey: String = "",

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val status: IssuerStatus = IssuerStatus.TRUSTED,

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(nullable = false)
    val updatedAt: Instant = Instant.now()
)

enum class IssuerStatus {
    TRUSTED, SUSPENDED, REVOKED
}

/**
 * Certificate entity - the core domain object
 */
@Entity
@Table(name = "certificates", indexes = [
    Index(name = "idx_cert_status", columnList = "status"),
    Index(name = "idx_cert_issuer", columnList = "issuer_id"),
    Index(name = "idx_cert_expires", columnList = "expires_at")
])
data class Certificate(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",

    @Column(nullable = false, unique = true)
    val certificateId: String = "",

    @Column(nullable = false)
    val credentialType: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuer_id", nullable = false)
    val issuer: Issuer = Issuer(),

    @Column(nullable = false)
    val subjectId: String = "",

    @Column(nullable = false)
    val subjectName: String = "",

    @Column(nullable = false)
    val issuedAt: Long = 0,

    @Column(nullable = false)
    val validFrom: Long = 0,

    @Column(nullable = false)
    val expiresAt: Long = 0,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val status: CertificateStatus = CertificateStatus.ACTIVE,

    @Column(nullable = false, columnDefinition = "TEXT")
    val signature: String = "",

    @Column(columnDefinition = "TEXT")
    val proof: String = "",

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(nullable = false)
    val updatedAt: Instant = Instant.now()
)

enum class CertificateStatus {
    ACTIVE, EXPIRED, REVOKED, SUSPENDED
}

/**
 * Certificate claims (key-value pairs)
 */
@Entity
@Table(name = "certificate_claims", indexes = [
    Index(name = "idx_claim_cert", columnList = "certificate_id")
])
data class CertificateClaim(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_id", nullable = false)
    val certificate: Certificate = Certificate(),

    @Column(nullable = false)
    val claimKey: String = "",

    @Column(nullable = false, columnDefinition = "TEXT")
    val claimValue: String = ""
)

/**
 * Revocation entry
 */
@Entity
@Table(name = "revocations", indexes = [
    Index(name = "idx_revoke_cert", columnList = "certificate_id", unique = true)
])
data class Revocation(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_id", nullable = false, unique = true)
    val certificate: Certificate = Certificate(),

    @Column(nullable = false)
    val reason: String = "",

    @Column(nullable = false)
    val revokedAt: Long = 0,

    @Column(nullable = false)
    val revokedBy: String = "",

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)

/**
 * Certificate event for audit trail
 */
@Entity
@Table(name = "certificate_events", indexes = [
    Index(name = "idx_event_cert", columnList = "certificate_id"),
    Index(name = "idx_event_type", columnList = "event_type")
])
data class CertificateEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_id", nullable = false)
    val certificate: Certificate = Certificate(),

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val eventType: EventType = EventType.CREATED,

    @Column(nullable = false)
    val timestamp: Long = 0,

    @Column(columnDefinition = "TEXT")
    val details: String = ""
)

enum class EventType {
    CREATED, IMPORTED, VIEWED, SHARED, VERIFIED, REVOKED, EXPIRED, SYNCED, DELETED
}

/**
 * Verification event
 */
@Entity
@Table(name = "verification_events", indexes = [
    Index(name = "idx_verify_cert", columnList = "certificate_id"),
    Index(name = "idx_verify_time", columnList = "timestamp")
])
data class VerificationEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_id", nullable = false)
    val certificate: Certificate = Certificate(),

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val result: VerificationResult = VerificationResult.VERIFIED,

    @Column(nullable = false)
    val isOffline: Boolean = false,

    @Column(nullable = false)
    val timestamp: Long = 0
)

enum class VerificationResult {
    VERIFIED, EXPIRED, REVOKED, SUSPENDED, INVALID_SIGNATURE, UNKNOWN_ISSUER, MALFORMED_CREDENTIAL, NETWORK_ERROR
}

/**
 * Refresh token for JWT authentication
 */
@Entity
@Table(name = "refresh_tokens", indexes = [
    Index(name = "idx_user_token", columnList = "user_id"),
    Index(name = "idx_token_expiry", columnList = "expires_at")
])
data class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User = User(),

    @Column(nullable = false, unique = true)
    val token: String = "",

    @Column(nullable = false)
    val expiresAt: Long = 0,

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)

/**
 * Audit log
 */
@Entity
@Table(name = "audit_logs", indexes = [
    Index(name = "idx_audit_user", columnList = "user_id"),
    Index(name = "idx_audit_action", columnList = "action"),
    Index(name = "idx_audit_time", columnList = "timestamp")
])
data class AuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User = User(),

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val action: AuditAction = AuditAction.LOGIN_SUCCESS,

    @Column(nullable = false)
    val resourceType: String = "",

    @Column(nullable = false)
    val resourceId: String = "",

    @Column(nullable = false)
    val timestamp: Long = 0,

    @Column(columnDefinition = "TEXT")
    val details: String = ""
)

enum class AuditAction {
    LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT, CERTIFICATE_CREATED, CERTIFICATE_REVOKED, SYNC_SUCCESS, SYNC_FAILURE
}
