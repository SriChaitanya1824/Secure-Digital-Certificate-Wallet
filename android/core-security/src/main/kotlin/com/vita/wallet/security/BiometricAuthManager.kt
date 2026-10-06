package com.vita.wallet.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

/**
 * Manages BiometricPrompt for secure authentication
 *
 * Handles:
 * - Fingerprint authentication
 * - Face recognition
 * - Iris scanning
 * - Fallback to device credential (PIN, pattern, password)
 */
class BiometricAuthManager(private val context: Context) {

    private val biometricManager = BiometricManager.from(context)

    /**
     * Check if device supports biometric authentication
     */
    fun isBiometricAvailable(): Boolean {
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Check if device credential (PIN/pattern/password) is available
     */
    fun isDeviceCredentialAvailable(): Boolean {
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Check if any authentication method is available
     */
    fun isAuthenticationAvailable(): Boolean {
        return isBiometricAvailable() || isDeviceCredentialAvailable()
    }

    /**
     * Show biometric prompt for authentication
     */
    fun authenticate(
        activity: FragmentActivity,
        executor: Executor,
        callback: BiometricPromptCallback
    ) {
        if (!isAuthenticationAvailable()) {
            callback.onError("No authentication method available")
            return
        }

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    callback.onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    callback.onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    callback.onError("Authentication failed")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authenticate to Access Wallet")
            .setSubtitle("Verify your identity to continue")
            .setDescription("Use your biometric credentials to unlock sensitive wallet operations")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Callback for biometric authentication results
     */
    interface BiometricPromptCallback {
        fun onSuccess()
        fun onError(message: String)
    }
}

/**
 * Token storage with encryption
 * Uses EncryptedSharedPreferences for secure token persistence
 */
class SecureTokenStorage(private val context: Context, private val keystore: KeystoreManager) {

    private val encryptedPrefs = androidx.security.crypto.EncryptedSharedPreferences.create(
        context,
        "wallet_tokens",
        androidx.security.crypto.MasterKey.Builder(context).setKeyScheme(
            androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM
        ).build(),
        androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Save JWT tokens securely
     */
    fun saveTokens(accessToken: String, refreshToken: String) {
        encryptedPrefs.edit().apply {
            putString("access_token", accessToken)
            putString("refresh_token", refreshToken)
            putLong("saved_at", System.currentTimeMillis())
            apply()
        }
    }

    /**
     * Retrieve access token
     */
    fun getAccessToken(): String? {
        return encryptedPrefs.getString("access_token", null)
    }

    /**
     * Retrieve refresh token
     */
    fun getRefreshToken(): String? {
        return encryptedPrefs.getString("refresh_token", null)
    }

    /**
     * Clear all tokens
     */
    fun clearTokens() {
        encryptedPrefs.edit().apply {
            remove("access_token")
            remove("refresh_token")
            remove("saved_at")
            apply()
        }
    }

    /**
     * Check if tokens exist
     */
    fun hasTokens(): Boolean {
        return getAccessToken() != null && getRefreshToken() != null
    }
}
