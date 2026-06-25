package com.example.guerrilla450.dash

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Per-rider dash WiFi configuration, persisted on-device.
 *
 * Guerrilla 450 is meant to work on ANY Royal Enfield Tripper dash, not just the author's.
 * Every dash advertises a different SSID (e.g. `RE_P0RP_260525`, `RE_XXXX_yymmdd`) but
 * they all share the `RE_` prefix and the factory passphrase `12345678`. So out of the
 * box we connect by PREFIX (see [DashWifiManager]) — the rider just picks their dash from
 * the system dialog once — and then we remember that exact SSID here for direct reconnects.
 *
 * Everything is overridable in Settings for dashes that don't fit the defaults.
 *
 * The WiFi password is stored in [EncryptedSharedPreferences] (AES-256-GCM, key in the
 * Android Keystore). SSID and prefix are non-sensitive and stay in plain SharedPreferences.
 * On first launch after this change, any plaintext password in the old prefs is migrated
 * and erased from the unencrypted store.
 */
class DashConfig private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("dash_config", Context.MODE_PRIVATE)

    private val securePrefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            "dash_config_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    init {
        migratePlaintextPassword()
    }

    /** Broadest match across Tripper variants; rider-overridable. */
    var ssidPrefix: String
        get() = prefs.getString(KEY_PREFIX, DEFAULT_PREFIX) ?: DEFAULT_PREFIX
        set(v) = prefs.edit().putString(KEY_PREFIX, v).apply()

    /** The exact SSID once learned/entered. Empty = not yet known → discover by prefix. */
    var ssid: String
        get() = prefs.getString(KEY_SSID, "") ?: ""
        set(v) = prefs.edit().putString(KEY_SSID, v).apply()

    var password: String
        get() = securePrefs.getString(KEY_PASSWORD, DEFAULT_PASSWORD) ?: DEFAULT_PASSWORD
        set(v) = securePrefs.edit().putString(KEY_PASSWORD, v).apply()

    /** True until a specific dash has been identified — connect by prefix discovery. */
    val needsDiscovery: Boolean get() = ssid.isBlank()

    /** Forget the learned dash so the next connect re-runs prefix discovery. */
    fun forgetDash() { ssid = "" }

    /** One-time migration: move a password written by an older build into encrypted storage. */
    private fun migratePlaintextPassword() {
        if (!prefs.contains(KEY_PASSWORD)) return
        val legacy = prefs.getString(KEY_PASSWORD, null) ?: return
        securePrefs.edit().putString(KEY_PASSWORD, legacy).apply()
        prefs.edit().remove(KEY_PASSWORD).apply()
    }

    companion object {
        private const val KEY_PREFIX   = "ssid_prefix"
        private const val KEY_SSID     = "ssid"
        private const val KEY_PASSWORD = "password"
        const val DEFAULT_PREFIX   = "RE_"
        const val DEFAULT_PASSWORD = "12345678"

        @Volatile private var instance: DashConfig? = null
        fun get(context: Context): DashConfig =
            instance ?: synchronized(this) {
                instance ?: DashConfig(context).also { instance = it }
            }
    }
}
