package com.vita.issuer.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

/**
 * JWT token provider for authentication
 */
@Component
class JwtTokenProvider(
    @Value("\${jwt.secret:your-super-secret-key-that-is-at-least-32-characters-long}")
    private val secretKey: String,
    @Value("\${jwt.expiration:900000}")
    private val expirationMillis: Long
) {
    private val key = Keys.hmacShaKeyFor(secretKey.toByteArray())

    /**
     * Generate JWT token
     */
    fun generateToken(email: String, userId: String): String {
        val now = Date()
        val expiryDate = Date(now.time + expirationMillis)

        return Jwts.builder()
            .setSubject(email)
            .claim("userId", userId)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS512)
            .compact()
    }

    /**
     * Extract email from token
     */
    fun getEmailFromToken(token: String): String {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
            .subject
    }

    /**
     * Extract user ID from token
     */
    fun getUserIdFromToken(token: String): String {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
            .get("userId", String::class.java)
    }

    /**
     * Validate token
     */
    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
            true
        } catch (e: Exception) {
            false
        }
    }
}
