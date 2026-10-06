package com.vita.wallet.network

import retrofit2.http.*

/**
 * Authentication API endpoints
 */
interface AuthService {
    @POST("/api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): LoginResponse

    @POST("/api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @POST("/api/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): RefreshTokenResponse

    @POST("/api/auth/logout")
    suspend fun logout()
}

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

/**
 * Certificate API endpoints for wallet
 */
interface CertificateService {
    @GET("/api/wallet/certificates")
    suspend fun getCertificates(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): CertificateListResponse

    @GET("/api/wallet/certificates/{id}")
    suspend fun getCertificateDetail(
        @Path("id") certificateId: String
    ): CertificateDTO

    @POST("/api/wallet/certificates/{id}/import")
    suspend fun importCertificate(
        @Path("id") certificateId: String
    ): CertificateDTO

    @DELETE("/api/wallet/certificates/{id}")
    suspend fun deleteCertificate(
        @Path("id") certificateId: String
    )
}

data class CertificateListResponse(
    val certificates: List<CertificateDTO>,
    val totalCount: Int,
    val page: Int,
    val size: Int
)

/**
 * Verification API endpoints
 */
interface VerificationService {
    @POST("/api/verification/verify")
    suspend fun verifyCertificate(
        @Body request: VerificationRequest
    ): VerificationResponse

    @GET("/api/verification/history")
    suspend fun getVerificationHistory(
        @Query("limit") limit: Int = 50
    ): VerificationHistoryResponse

    @GET("/api/verification/issuer/{id}")
    suspend fun getIssuerDetails(
        @Path("id") issuerId: String
    ): IssuerDetailsResponse
}

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
    val createdAt: Long
)

/**
 * Sync API endpoints for offline-first architecture
 */
interface SyncService {
    @GET("/api/sync/certificates")
    suspend fun syncCertificates(
        @Query("since") since: Long? = null
    ): SyncResponse

    @GET("/api/sync/status")
    suspend fun getSyncStatus(): SyncStatusResponse
}

data class SyncResponse(
    val certificates: List<CertificateDTO>,
    val deletedCertificateIds: List<String>,
    val lastSyncTime: Long
)

data class SyncStatusResponse(
    val lastSyncTime: Long,
    val isSyncing: Boolean,
    val certificateCount: Int
)
