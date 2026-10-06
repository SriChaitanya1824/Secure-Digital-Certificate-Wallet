package com.vita.issuer.controller

import com.vita.issuer.dto.*
import com.vita.issuer.service.AuthService
import com.vita.issuer.service.CertificateService
import com.vita.issuer.service.VerificationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.security.Principal

/**
 * Authentication endpoints
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication endpoints")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    @Operation(summary = "Register new user")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<LoginResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(authService.register(request))
    }

    @PostMapping("/login")
    @Operation(summary = "Login user")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        return ResponseEntity.ok(authService.login(request))
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh authentication token")
    fun refreshToken(@RequestBody request: RefreshTokenRequest): ResponseEntity<RefreshTokenResponse> {
        return ResponseEntity.ok(authService.refreshToken(request.refreshToken))
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Logout user")
    fun logout(): ResponseEntity<Unit> {
        return ResponseEntity.ok().build()
    }
}

/**
 * Certificate endpoints
 */
@RestController
@RequestMapping("/api/issuer/certificates")
@Tag(name = "Certificates", description = "Certificate management endpoints")
class CertificateController(private val certificateService: CertificateService) {

    @PostMapping
    @PreAuthorize("hasRole('ISSUER')")
    @Operation(summary = "Issue new certificate")
    fun issueCertificate(
        @RequestBody request: IssueCertificateRequest,
        principal: Principal
    ): ResponseEntity<CertificateResponse> {
        // In real implementation, get issuer ID from principal/JWT
        val issuerId = principal.name // simplified
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(certificateService.issueCertificate(request, issuerId))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get certificate details")
    fun getCertificate(@PathVariable id: String): ResponseEntity<CertificateResponse> {
        return ResponseEntity.ok(certificateService.getCertificate(id))
    }

    @GetMapping
    @Operation(summary = "List certificates")
    fun listCertificates(
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<CertificateListResponse> {
        return ResponseEntity.ok(certificateService.getCertificates(status, page, size))
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasRole('ISSUER')")
    @Operation(summary = "Revoke certificate")
    fun revokeCertificate(
        @PathVariable id: String,
        @RequestBody request: RevokeCertificateRequest
    ): ResponseEntity<CertificateResponse> {
        return ResponseEntity.ok(certificateService.revokeCertificate(id, request.reason))
    }
}

/**
 * Verification endpoints
 */
@RestController
@RequestMapping("/api/verification")
@Tag(name = "Verification", description = "Certificate verification endpoints")
class VerificationController(private val verificationService: VerificationService) {

    @PostMapping("/verify")
    @Operation(summary = "Verify credential")
    fun verifyCertificate(@RequestBody request: VerificationRequest): ResponseEntity<VerificationResponse> {
        return ResponseEntity.ok(verificationService.verifyCertificate(request))
    }

    @GetMapping("/history")
    @Operation(summary = "Get verification history")
    fun getVerificationHistory(@RequestParam(defaultValue = "50") limit: Int): ResponseEntity<VerificationHistoryResponse> {
        return ResponseEntity.ok(VerificationHistoryResponse(emptyList(), 0))
    }
}

/**
 * Sync endpoints for offline-first architecture
 */
@RestController
@RequestMapping("/api/sync")
@Tag(name = "Sync", description = "Synchronization endpoints")
class SyncController {

    @GetMapping("/certificates")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Sync certificates")
    fun syncCertificates(@RequestParam(required = false) since: Long?): ResponseEntity<SyncResponse> {
        return ResponseEntity.ok(SyncResponse(emptyList(), emptyList(), System.currentTimeMillis()))
    }

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get sync status")
    fun getSyncStatus(): ResponseEntity<SyncStatusResponse> {
        return ResponseEntity.ok(SyncStatusResponse(
            lastSyncTime = System.currentTimeMillis(),
            isSyncing = false,
            certificateCount = 0
        ))
    }
}

/**
 * Error response
 */
data class ErrorResponse(
    val code: String,
    val message: String,
    val requestId: String = ""
)

/**
 * Global exception handler
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                code = "VALIDATION_ERROR",
                message = ex.message ?: "Invalid request"
            ))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(
                code = "SERVER_ERROR",
                message = "An unexpected error occurred"
            ))
    }
}
