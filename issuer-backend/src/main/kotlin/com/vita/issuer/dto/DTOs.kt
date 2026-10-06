package com.vita.issuer.dto

import com.vita.issuer.entity.Certificate
import com.vita.issuer.entity.User
import java.time.Instant

// Request DTOs

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class IssueCertificateRequest(
    val credentialType: String,
    val subjectId: String,
    val subjectName: String,
    val validFrom: Long? = null,
    val expiresAt: Long,
    val claims: Map<String, String>
)

data class RevokeCertificateRequest(
    val reason: String
)

data class VerificationRequest(
    val credentialId: String,
    val credentialPayload: String,
    val signature: String
)

// Response DTOs

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserDTO
)

data class UserDTO(
    val id: String,
    val email: String,
    val name: String,
    val createdAt: Instant
) {
    companion object {
        fun from(user: User) = UserDTO(
            id = user.id,
            email = user.email,
            name = user.name,
            createdAt = user.createdAt
        )
    }
}

data class RefreshTokenResponse(
    val accessToken: String,
    val expiresIn: Long
)

data class CertificateResponse(
    val certificateId: String,
    val credentialType: String,
    val issuerName: String,
    val subjectName: String,
    val claims: Map<String, String>,
    val issuedAt: Long,
    val validFrom: Long,
    val expiresAt: Long,
    val status: String,
    val signature: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CertificateListResponse(
    val certificates: List<CertificateResponse>,
    val totalCount: Int,
    val page: Int,
    val size: Int
)

data class VerificationResponse(
    val status: String,
    val verified: Boolean,
    val issuer: String,
    val subject: String,
    val claims: Map<String, String>,
    val issuedAt: Long,
    val expiresAt: Long,
    val message: String
)

data class VerificationHistoryResponse(
    val events: List<VerificationEventDTO>,
    val totalCount: Int
)

data class VerificationEventDTO(
    val eventId: String,
    val certificateId: String,
    val status: String,
    val timestamp: Long,
    val isOffline: Boolean
)

data class IssuerDetailsResponse(
    val issuerId: String,
    val issuerName: String,
    val status: String,
    val createdAt: Instant
)

data class SyncResponse(
    val certificates: List<CertificateResponse>,
    val deletedCertificateIds: List<String>,
    val lastSyncTime: Long
)

data class SyncStatusResponse(
    val lastSyncTime: Long,
    val isSyncing: Boolean,
    val certificateCount: Int
)

// Extension functions

fun User.toDTO() = UserDTO.from(this)

fun Certificate.toResponse(claims: Map<String, String>) = CertificateResponse(
    certificateId = this.certificateId,
    credentialType = this.credentialType,
    issuerName = this.issuer.name,
    subjectName = this.subjectName,
    claims = claims,
    issuedAt = this.issuedAt,
    validFrom = this.validFrom,
    expiresAt = this.expiresAt,
    status = this.status.name,
    signature = this.signature,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)
