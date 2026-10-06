package com.vita.wallet.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType

/**
 * Retrofit HTTP client configuration
 * Includes logging, error handling, and custom interceptors
 */
object NetworkFactory {

    fun createOkHttpClient(
        tokenStorage: TokenStorage? = null,
        loggingEnabled: Boolean = false
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)

        if (loggingEnabled) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
        }

        if (tokenStorage != null) {
            builder.addInterceptor(AuthInterceptor(tokenStorage))
        }

        builder.addInterceptor(ErrorInterceptor())

        return builder.build()
    }

    fun createRetrofit(
        baseUrl: String,
        okHttpClient: OkHttpClient
    ): Retrofit {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}

/**
 * Token-based authentication storage interface
 */
interface TokenStorage {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun saveTokens(accessToken: String, refreshToken: String)
    fun clearTokens()
}

/**
 * OkHttp interceptor for adding JWT authentication
 */
class AuthInterceptor(private val tokenStorage: TokenStorage) : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()

        // Skip auth for public endpoints
        if (originalRequest.url.encodedPath.contains("/auth/")) {
            return chain.proceed(originalRequest)
        }

        val accessToken = tokenStorage.getAccessToken()
        if (accessToken != null) {
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
            return chain.proceed(authenticatedRequest)
        }

        return chain.proceed(originalRequest)
    }
}

/**
 * OkHttp interceptor for consistent error handling
 */
class ErrorInterceptor : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            throw com.vita.wallet.core.model.DomainError.NetworkError(e.message ?: "Unknown error")
        }

        return when (response.code) {
            401 -> throw com.vita.wallet.core.model.DomainError.Unauthorized()
            403 -> throw com.vita.wallet.core.model.DomainError.Forbidden()
            404 -> throw com.vita.wallet.core.model.DomainError.NotFound()
            409 -> throw com.vita.wallet.core.model.DomainError.Conflict()
            429 -> throw com.vita.wallet.core.model.DomainError.RateLimited()
            in 500..599 -> throw com.vita.wallet.core.model.DomainError.ServerError()
            else -> response
        }
    }
}

/**
 * API DTOs for authentication
 */
data class LoginRequest(
    val email: String,
    val password: String
)

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
    val createdAt: Long
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class RefreshTokenResponse(
    val accessToken: String,
    val expiresIn: Long
)

/**
 * API DTOs for certificates
 */
data class CertificateDTO(
    val certificateId: String,
    val credentialType: String,
    val issuerId: String,
    val issuerName: String,
    val subjectId: String,
    val subjectName: String,
    val claims: Map<String, String>,
    val issuedAt: Long,
    val validFrom: Long,
    val expiresAt: Long,
    val status: String,
    val signature: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class VerificationRequest(
    val credentialId: String,
    val credentialPayload: String,
    val signature: String
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
