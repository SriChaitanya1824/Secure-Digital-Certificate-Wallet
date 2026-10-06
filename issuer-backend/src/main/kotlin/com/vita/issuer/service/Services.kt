package com.vita.issuer.service

import com.vita.issuer.dto.*
import com.vita.issuer.entity.*
import com.vita.issuer.repository.*
import com.vita.issuer.security.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

/**
 * User authentication service
 */
@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val auditLogRepository: AuditLogRepository
) {
    fun register(request: RegisterRequest): LoginResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already registered")
        }

        val user = User(
            email = request.email,
            name = request.name,
            passwordHash = passwordEncoder.encode(request.password)
        )
        userRepository.save(user)

        return issueTokens(user, request.email)
    }

    fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { throw IllegalArgumentException("Invalid credentials") }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid credentials")
        }

        auditLogRepository.save(AuditLog(
            user = user,
            action = AuditAction.LOGIN_SUCCESS,
            resourceType = "USER",
            resourceId = user.id,
            timestamp = System.currentTimeMillis()
        ))

        return issueTokens(user, request.email)
    }

    fun refreshToken(token: String): RefreshTokenResponse {
        val refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow { throw IllegalArgumentException("Invalid refresh token") }

        if (refreshToken.expiresAt < System.currentTimeMillis()) {
            throw IllegalArgumentException("Refresh token expired")
        }

        val newAccessToken = jwtTokenProvider.generateToken(
            refreshToken.user.email,
            refreshToken.user.id
        )

        return RefreshTokenResponse(
            accessToken = newAccessToken,
            expiresIn = 900 // 15 minutes
        )
    }

    private fun issueTokens(user: User, email: String): LoginResponse {
        val accessToken = jwtTokenProvider.generateToken(email, user.id)
        val refreshTokenString = UUID.randomUUID().toString()
        val refreshToken = RefreshToken(
            user = user,
            token = refreshTokenString,
            expiresAt = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000) // 7 days
        )
        refreshTokenRepository.save(refreshToken)

        return LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshTokenString,
            expiresIn = 900,
            user = user.toDTO()
        )
    }
}

/**
 * Certificate issuance service
 */
@Service
@Transactional
class CertificateService(
    private val certificateRepository: CertificateRepository,
    private val certificateClaimRepository: CertificateClaimRepository,
    private val certificateEventRepository: CertificateEventRepository,
    private val revocationRepository: RevocationRepository,
    private val issuerRepository: IssuerRepository,
    private val signingService: CertificateSigningService
) {
    fun issueCertificate(request: IssueCertificateRequest, issuerId: String): CertificateResponse {
        val issuer = issuerRepository.findById(issuerId)
            .orElseThrow { throw IllegalArgumentException("Issuer not found") }

        val certificateId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis() / 1000

        val certificate = Certificate(
            certificateId = certificateId,
            credentialType = request.credentialType,
            issuer = issuer,
            subjectId = request.subjectId,
            subjectName = request.subjectName,
            issuedAt = now,
            validFrom = request.validFrom ?: now,
            expiresAt = request.expiresAt,
            status = CertificateStatus.ACTIVE,
            signature = "" // Will be filled by signing service
        )

        val signedCertificate = signingService.signCertificate(certificate, request.claims)
        val saved = certificateRepository.save(signedCertificate)

        // Save claims
        request.claims.forEach { (key, value) ->
            certificateClaimRepository.save(CertificateClaim(
                certificate = saved,
                claimKey = key,
                claimValue = value
            ))
        }

        // Record event
        certificateEventRepository.save(CertificateEvent(
            certificate = saved,
            eventType = EventType.CREATED,
            timestamp = System.currentTimeMillis()
        ))

        return saved.toResponse(request.claims)
    }

    fun getCertificate(certificateId: String): CertificateResponse {
        val certificate = certificateRepository.findByCertificateId(certificateId)
            .orElseThrow { throw IllegalArgumentException("Certificate not found") }

        val claims = certificateClaimRepository.findByCertificate(certificate)
            .associate { it.claimKey to it.claimValue }

        certificateEventRepository.save(CertificateEvent(
            certificate = certificate,
            eventType = EventType.VIEWED,
            timestamp = System.currentTimeMillis()
        ))

        return certificate.toResponse(claims)
    }

    fun revokeCertificate(certificateId: String, reason: String): CertificateResponse {
        val certificate = certificateRepository.findByCertificateId(certificateId)
            .orElseThrow { throw IllegalArgumentException("Certificate not found") }

        if (certificate.status == CertificateStatus.REVOKED) {
            // Idempotent: already revoked
            return certificate.toResponse(getClaims(certificate))
        }

        val updated = certificate.copy(status = CertificateStatus.REVOKED)
        certificateRepository.save(updated)

        revocationRepository.save(Revocation(
            certificate = updated,
            reason = reason,
            revokedAt = System.currentTimeMillis(),
            revokedBy = "system"
        ))

        certificateEventRepository.save(CertificateEvent(
            certificate = updated,
            eventType = EventType.REVOKED,
            timestamp = System.currentTimeMillis(),
            details = reason
        ))

        return updated.toResponse(getClaims(updated))
    }

    fun getCertificates(
        status: String? = null,
        page: Int = 0,
        size: Int = 20
    ): CertificateListResponse {
        val certificates = if (status != null) {
            certificateRepository.findByStatus(CertificateStatus.valueOf(status))
        } else {
            certificateRepository.findAll()
        }

        val paginated = certificates.drop(page * size).take(size)
        return CertificateListResponse(
            certificates = paginated.map { cert ->
                cert.toResponse(getClaims(cert))
            },
            totalCount = certificates.size,
            page = page,
            size = size
        )
    }

    private fun getClaims(certificate: Certificate): Map<String, String> {
        return certificateClaimRepository.findByCertificate(certificate)
            .associate { it.claimKey to it.claimValue }
    }
}

/**
 * Certificate signing service (cryptographic operations)
 */
@Service
class CertificateSigningService {
    fun signCertificate(certificate: Certificate, claims: Map<String, String>): Certificate {
        // In production, use HSM or AWS KMS for key management
        // For now, we'll sign with a development key stored in configuration
        val payload = buildPayload(certificate, claims)
        val signature = sign(payload)

        return certificate.copy(
            signature = signature,
            proof = payload
        )
    }

    fun verifySignature(certificate: Certificate): Boolean {
        return try {
            val payload = certificate.proof
            val signature = certificate.signature
            verify(payload, signature)
        } catch (e: Exception) {
            false
        }
    }

    private fun buildPayload(certificate: Certificate, claims: Map<String, String>): String {
        return """
            {
              "certificateId": "${certificate.certificateId}",
              "issuer": "${certificate.issuer.id}",
              "subjectId": "${certificate.subjectId}",
              "issuedAt": ${certificate.issuedAt},
              "expiresAt": ${certificate.expiresAt},
              "claims": ${claims.toJsonString()}
            }
        """.trimIndent()
    }

    private fun sign(payload: String): String {
        // Placeholder: in production, use Ed25519 or similar
        return payload.hashCode().toString()
    }

    private fun verify(payload: String, signature: String): Boolean {
        // Placeholder: in production, verify against issuer public key
        return payload.hashCode().toString() == signature
    }

    private fun Map<String, String>.toJsonString(): String {
        return "{" + this.entries.joinToString(",") { (k, v) ->
            """"$k":"$v""""
        } + "}"
    }
}

/**
 * Verification service
 */
@Service
@Transactional
class VerificationService(
    private val certificateRepository: CertificateRepository,
    private val signingService: CertificateSigningService,
    private val verificationEventRepository: VerificationEventRepository,
    private val revocationRepository: RevocationRepository
) {
    fun verifyCertificate(request: VerificationRequest): VerificationResponse {
        val certificate = certificateRepository.findByCertificateId(request.credentialId)
            .orElseThrow { throw IllegalArgumentException("Certificate not found") }

        val claims = certificate.getClaims()

        // Check signature
        if (!signingService.verifySignature(certificate)) {
            recordEvent(certificate, VerificationResult.INVALID_SIGNATURE)
            return VerificationResponse(
                status = "INVALID_SIGNATURE",
                verified = false,
                issuer = certificate.issuer.name,
                subject = certificate.subjectName,
                claims = claims,
                issuedAt = certificate.issuedAt,
                expiresAt = certificate.expiresAt,
                message = "Certificate signature verification failed"
            )
        }

        // Check revocation
        if (revocationRepository.findByCertificate(certificate).isPresent) {
            recordEvent(certificate, VerificationResult.REVOKED)
            return VerificationResponse(
                status = "REVOKED",
                verified = false,
                issuer = certificate.issuer.name,
                subject = certificate.subjectName,
                claims = claims,
                issuedAt = certificate.issuedAt,
                expiresAt = certificate.expiresAt,
                message = "Certificate has been revoked"
            )
        }

        // Check expiration
        val now = System.currentTimeMillis() / 1000
        if (now >= certificate.expiresAt) {
            recordEvent(certificate, VerificationResult.EXPIRED)
            return VerificationResponse(
                status = "EXPIRED",
                verified = false,
                issuer = certificate.issuer.name,
                subject = certificate.subjectName,
                claims = claims,
                issuedAt = certificate.issuedAt,
                expiresAt = certificate.expiresAt,
                message = "Certificate has expired"
            )
        }

        recordEvent(certificate, VerificationResult.VERIFIED)
        return VerificationResponse(
            status = "VERIFIED",
            verified = true,
            issuer = certificate.issuer.name,
            subject = certificate.subjectName,
            claims = claims,
            issuedAt = certificate.issuedAt,
            expiresAt = certificate.expiresAt,
            message = "Certificate verified successfully"
        )
    }

    private fun recordEvent(certificate: Certificate, result: VerificationResult) {
        verificationEventRepository.save(VerificationEvent(
            certificate = certificate,
            result = result,
            isOffline = false,
            timestamp = System.currentTimeMillis()
        ))
    }

    private fun Certificate.getClaims(): Map<String, String> {
        return emptyMap() // In real implementation, fetch from claims table
    }
}
