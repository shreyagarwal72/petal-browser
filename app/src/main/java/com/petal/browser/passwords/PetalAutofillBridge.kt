package com.petal.browser.passwords

import android.content.Context
import android.util.Log
import java.util.UUID

/**
 * PetalAutofillBridge
 * ─────────────────────────────────────────────────────────────────────────────
 * Chromium & Petal native Autofill / Password Manager Bridge.
 *
 * Interfaces with Chromium's PasswordStoreBridge / CredentialManager and connects
 * directly to Petal's AES-256 encrypted local vault [PetalCredentialVault].
 * Replaces old GeckoView Autocomplete.StorageDelegate with zero Mozilla dependencies.
 */
object PetalAutofillBridge {

    private const val TAG = "PetalAutofillBridge"

    @JvmStatic
    fun init(context: Context) {
        PetalCredentialVault.init(context)
        Log.i(TAG, "PetalAutofillBridge initialized with PetalCredentialVault")
    }

    /**
     * Retrieve matching credentials for a given URL / domain.
     */
    @JvmStatic
    fun getLoginsForDomain(context: Context, domainOrUrl: String): List<PetalCredential> {
        PetalCredentialVault.init(context)
        val target = normalizeDomain(domainOrUrl)
        if (target.isBlank()) return emptyList()

        return PetalCredentialVault.getAll().filter { cred ->
            domainMatches(cred.domain, target)
        }
    }

    /**
     * Save or update credential from web form submission or Chromium prompt.
     */
    @JvmStatic
    fun saveLogin(
        context: Context,
        domainOrUrl: String,
        username: String,
        password: String
    ): Boolean {
        PetalCredentialVault.init(context)
        val saveDomain = normalizeDomain(domainOrUrl)
        if (password.isBlank() || saveDomain.isBlank()) {
            Log.d(TAG, "saveLogin: blank password or domain – skip")
            return false
        }

        if (PetalCredentialVault.isNeverSave(context, saveDomain)) {
            Log.d(TAG, "saveLogin: $saveDomain is in Never-Save list – skip")
            return false
        }

        val existing = PetalCredentialVault.getAll().firstOrNull {
            normalizeDomain(it.domain) == saveDomain &&
                    it.username.equals(username, ignoreCase = true)
        }

        return when {
            existing == null -> {
                PetalCredentialVault.save(
                    PetalCredential(
                        id = UUID.randomUUID().toString(),
                        domain = saveDomain,
                        originUrl = if (domainOrUrl.startsWith("http")) domainOrUrl else "https://$saveDomain",
                        username = username,
                        password = password
                    )
                )
                Log.i(TAG, "saveLogin: saved new credentials for $saveDomain")
                true
            }
            existing.password != password -> {
                PetalCredentialVault.save(
                    existing.copy(password = password, updatedAt = System.currentTimeMillis())
                )
                Log.i(TAG, "saveLogin: updated credentials for $saveDomain")
                true
            }
            else -> {
                Log.d(TAG, "saveLogin: credential already up to date for $saveDomain")
                false
            }
        }
    }

    /**
     * Mark a credential as recently used.
     */
    @JvmStatic
    fun onLoginUsed(context: Context, credentialId: String) {
        PetalCredentialVault.init(context)
        val existing = PetalCredentialVault.findById(credentialId) ?: return
        PetalCredentialVault.save(existing.copy(updatedAt = System.currentTimeMillis()))
        Log.d(TAG, "onLoginUsed: updated last-used timestamp for ${existing.domain}")
    }

    private fun normalizeDomain(raw: String): String =
        raw.trim().lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore('/')
            .substringBefore(':')

    private fun domainMatches(saved: String, page: String): Boolean {
        val s = normalizeDomain(saved)
        val p = normalizeDomain(page)
        if (s.isEmpty() || p.isEmpty()) return false
        return p == s || p.endsWith(".$s")
    }
}
