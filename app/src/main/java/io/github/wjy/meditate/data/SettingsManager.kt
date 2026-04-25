package io.github.wjy.meditate.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class WebDavConfig(
    val url: String = "",
    val user: String = "",
    val pass: String = "",
    val ignoreCert: Boolean = false
)

class SettingsManager(private val context: Context) {
    companion object {
        private val BLUR_ENABLED = booleanPreferencesKey("blur_enabled")
        private val BLUR_IMPLEMENTATION = stringPreferencesKey("blur_implementation")
        private val BLUR_INTENSITY = floatPreferencesKey("blur_intensity")
        
        private val WEBDAV_URL = stringPreferencesKey("webdav_url")
        private val WEBDAV_USER = stringPreferencesKey("webdav_user")
        private val WEBDAV_PASS = stringPreferencesKey("webdav_pass")
        private val WEBDAV_IGNORE_CERT = booleanPreferencesKey("webdav_ignore_cert")
        private val ACTIVE_SLOT = stringPreferencesKey("active_slot")
        
        const val IMPL_HARDWARE = "A13+"
        const val IMPL_RENDER_SCRIPT = "A12-"
    }

    val blurEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BLUR_ENABLED] ?: false
    }

    val blurImplementation: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[BLUR_IMPLEMENTATION] ?: IMPL_HARDWARE
    }

    val blurIntensity: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[BLUR_INTENSITY] ?: 16f
    }

    val webDavConfig: Flow<WebDavConfig> = context.dataStore.data.map { preferences ->
        WebDavConfig(
            url = preferences[WEBDAV_URL] ?: "",
            user = preferences[WEBDAV_USER] ?: "",
            pass = preferences[WEBDAV_PASS] ?: "",
            ignoreCert = preferences[WEBDAV_IGNORE_CERT] ?: false
        )
    }

    val activeSlot: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ACTIVE_SLOT] ?: "a"
    }

    suspend fun setBlurEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BLUR_ENABLED] = enabled
        }
    }

    suspend fun setBlurImplementation(implementation: String) {
        context.dataStore.edit { preferences ->
            preferences[BLUR_IMPLEMENTATION] = implementation
        }
    }

    suspend fun setBlurIntensity(intensity: Float) {
        context.dataStore.edit { preferences ->
            preferences[BLUR_INTENSITY] = intensity
        }
    }

    suspend fun updateWebDavConfig(config: WebDavConfig) {
        context.dataStore.edit { preferences ->
            preferences[WEBDAV_URL] = config.url
            preferences[WEBDAV_USER] = config.user
            preferences[WEBDAV_PASS] = config.pass
            preferences[WEBDAV_IGNORE_CERT] = config.ignoreCert
        }
    }

    suspend fun switchSlot() {
        context.dataStore.edit { preferences ->
            val current = preferences[ACTIVE_SLOT] ?: "a"
            preferences[ACTIVE_SLOT] = if (current == "a") "b" else "a"
        }
    }
}
