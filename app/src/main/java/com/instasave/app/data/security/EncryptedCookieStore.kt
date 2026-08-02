package com.instasave.app.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedCookieStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "instasave_session_encrypted",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun put(cookies: Map<String, String>, handle: String?, userId: String?) {
        val editor = prefs.edit()
        cookies.forEach { (key, value) ->
            editor.putString("cookie_$key", value)
        }
        if (handle != null) editor.putString("user_handle", handle)
        if (userId != null) editor.putString("user_id", userId)
        editor.apply()
    }

    fun cookieHeader(): String? {
        val sessionid = prefs.getString("cookie_sessionid", null) ?: return null
        val dsUserId = prefs.getString("cookie_ds_user_id", "")
        val csrfToken = prefs.getString("cookie_csrftoken", "")

        return "sessionid=$sessionid; ds_user_id=$dsUserId; csrftoken=$csrfToken;"
    }

    fun handle(): String? = prefs.getString("user_handle", null)

    fun userId(): String? = prefs.getString("user_id", null)

    fun clear() {
        prefs.edit().clear().apply()
    }

    override fun toString(): String {
        val isPresent = cookieHeader() != null
        return "EncryptedCookieStore(sessionPresent=$isPresent)"
    }
}
