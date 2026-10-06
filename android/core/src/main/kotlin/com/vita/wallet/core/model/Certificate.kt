package com.vita.wallet.core.model

import kotlinx.serialization.Serializable

/**
 * Core Certificate Domain Model
 *
 * Represents a digitally signed credential issued by a trusted organization.
 * Contains cryptographic proof of authenticity and can be selectively shared.
 */
@Serializable
data class Certificate(
    val certificateId: String,
    val credentialType: CredentialType,
    val issuerId: String,
    val issuerName: String,
    val subjectId: String,
    val subjectName: String,
    val claims: Map<String, String>,
    val issuedAt: Long,
    val validFrom: Long,
    val expiresAt: Long,
    val status: CertificateStatus,
    val credentialVersion: String = "1.0",
    val signatureAlgorithm: String = "Ed25519",
    val signature: String,
    val proof: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val lastVerifiedAt: Long? = null,
    val isOfflineCached: Boolean = false
) {
    /**
     * Determines if certificate is currently valid for presentation
     */
    fun isValid(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        val currentSeconds = currentTimeMillis / 1000
        return status == CertificateStatus.ACTIVE &&
                currentSeconds >= validFrom &&
                currentSeconds < expiresAt
    }

    /**
     * Determines if certificate is expired
     */
    fun isExpired(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        val currentSeconds = currentTimeMillis / 1000
        return currentSeconds >= expiresAt
    }

    /**
     * Gets days until expiration
     */
    fun getDaysUntilExpiry(currentTimeMillis: Long = System.currentTimeMillis()): Long {
        val currentSeconds = currentTimeMillis / 1000
        val secondsUntilExpiry = expiresAt - currentSeconds
        return secondsUntilExpiry / 86400 // 86400 seconds in a day
    }
}

/**
 * Credential types (examples for portfolio)
 */
enum class CredentialType {
    UNIVERSITY_DEGREE,
    TRAINING_CERTIFICATE,
    EMPLOYMENT_CERTIFICATE,
    IDENTITY_CREDENTIAL,
    PROFESSIONAL_LICENSE,
    OTHER
}

/**
 * Certificate status in lifecycle
 */
enum class CertificateStatus {
    ACTIVE,
    EXPIRED,
    REVOKED,
    SUSPENDED
}

/**
 * Selective disclosure mode for sharing
 */
enum class DisclosureMode {
    FULL,           // Share all claims
    MINIMAL,        // Share only name and credential type
    PROOF_ONLY      // Share only verification status
}

/**
 * Selective disclosure payload for sharing
 */
@Serializable
data class CredentialPresentation(
    val certificateId: String,
    val issuer: String,
    val subject: String,
    val claims: Map<String, String>,
    val issuedAt: Long,
    val expiresAt: Long,
    val nonce: String,
    val timestamp: Long,
    val presentationSignature: String = "",
    val walletPublicKey: String = ""
) {
    /**
     * Determines if this presentation is still fresh (not replayed)
     */
    fun isFresh(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        val ageSeconds = (currentTimeMillis / 1000) - (timestamp / 1000)
        return ageSeconds < 300 // 5 minute window
    }
}

/**
 * QR Code payload containing signed credential
 */
@Serializable
data class QRCredentialPayload(
    val credentialId: String,
    val issuer: String,
    val subject: String,
    val claims: Map<String, String>,
    val issuedAt: Long,
    val expiresAt: Long,
    val nonce: String,
    val timestamp: Long,
    val presentationSignature: String,
    val version: String = "1.0"
)

/**
 * Verification result from scanning a QR
 */
data class VerificationResult(
    val certificateId: String,
    val status: VerificationStatus,
    val issuer: String,
    val subject: String,
    val claims: Map<String, String>,
    val issuedAt: Long,
    val expiresAt: Long,
    val verifiedAt: Long,
    val isOffline: Boolean,
    val reason: String = "",
    val message: String = ""
)

/**
 * Verification status outcomes
 */
enum class VerificationStatus {
    VERIFIED,
    EXPIRED,
    REVOKED,
    SUSPENDED,
    INVALID_SIGNATURE,
    UNKNOWN_ISSUER,
    MALFORMED_CREDENTIAL,
    NETWORK_ERROR,
    REPLAY_DETECTED
}

/**
 * Issuer trust registry entry
 */
data class TrustedIssuer(
    val issuerId: String,
    val issuerName: String,
    val publicKey: String,
    val status: IssuerStatus,
    val createdAt: Long,
    val updatedAt: Long
)

enum class IssuerStatus {
    TRUSTED,
    SUSPENDED,
    REVOKED
}

/**
 * Sync state for offline-first architecture
 */
enum class SyncState {
    SYNCED,
    PENDING_SYNC,
    STALE,
    SYNC_FAILED
}

/**
 * Certificate event for audit trail
 */
@Serializable
data class CertificateEvent(
    val eventId: String,
    val certificateId: String,
    val eventType: CertificateEventType,
    val timestamp: Long,
    val details: String = ""
)

enum class CertificateEventType {
    CREATED,
    IMPORTED,
    VIEWED,
    SHARED,
    VERIFIED,
    REVOKED,
    EXPIRED,
    SYNCED,
    DELETED
}

/**
 * Verification event for history tracking
 */
data class VerificationEvent(
    val eventId: String,
    val certificateId: String,
    val verificationResult: VerificationResult,
    val isOffline: Boolean
)

/**
 * Authentication token pair
 */
data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val tokenType: String = "Bearer"
)

/**
 * User session
 */
data class UserSession(
    val userId: String,
    val email: String,
    val name: String,
    val tokenPair: TokenPair,
    val createdAt: Long,
    val lastActivityAt: Long
) {
    fun isTokenExpired(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        return currentTimeMillis > (createdAt + expiresIn * 1000)
    }
}

/**
 * Audit log entry
 */
data class AuditLog(
    val logId: String,
    val userId: String,
    val action: AuditAction,
    val resourceType: String,
    val resourceId: String,
    val timestamp: Long,
    val details: String = ""
)

enum class AuditAction {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    CERTIFICATE_CREATED,
    CERTIFICATE_IMPORTED,
    CERTIFICATE_VIEWED,
    CERTIFICATE_SHARED,
    CERTIFICATE_VERIFIED,
    CERTIFICATE_REVOKED,
    CERTIFICATE_DELETED,
    BIOMETRIC_SUCCESS,
    BIOMETRIC_FAILURE,
    SYNC_SUCCESS,
    SYNC_FAILURE
}

/**
 * Error types for consistent error handling
 */
sealed class DomainError : Exception() {
    data class NetworkError(override val message: String = "Network error") : DomainError()
    data class Unauthorized(override val message: String = "Unauthorized") : DomainError()
    data class Forbidden(override val message: String = "Forbidden") : DomainError()
    data class NotFound(override val message: String = "Not found") : DomainError()
    data class ValidationError(override val message: String = "Validation error") : DomainError()
    data class Conflict(override val message: String = "Conflict") : DomainError()
    data class RateLimited(override val message: String = "Rate limited") : DomainError()
    data class ServerError(override val message: String = "Server error") : DomainError()
    data class CertificateExpired(override val message: String = "Certificate expired") : DomainError()
    data class CertificateRevoked(override val message: String = "Certificate revoked") : DomainError()
    data class InvalidSignature(override val message: String = "Invalid signature") : DomainError()
    data class UnknownIssuer(override val message: String = "Unknown issuer") : DomainError()
}
