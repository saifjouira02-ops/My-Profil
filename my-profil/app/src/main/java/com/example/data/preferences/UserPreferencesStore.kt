package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.UserPreferences
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPrefsDataStore by preferencesDataStore(name = "my_profil_prefs")

class UserPreferencesStore(private val context: Context) {
    private val keyLanguage = stringPreferencesKey("language")
    private val keyDarkMode = stringPreferencesKey("dark_mode")
    private val keyLargeText = booleanPreferencesKey("large_text")
    private val keyHighContrast = booleanPreferencesKey("high_contrast")
    private val keyShowDemo = booleanPreferencesKey("show_demo_content")

    val preferencesFlow: Flow<UserPreferences> = context.userPrefsDataStore.data.map { prefs ->
        val defaultLang = if (Locale.getDefault().language == "ar") "ar" else "ar"
        UserPreferences(
            language = prefs[keyLanguage] ?: defaultLang,
            darkMode = prefs[keyDarkMode] ?: "system",
            largeText = prefs[keyLargeText] ?: false,
            highContrast = prefs[keyHighContrast] ?: false,
            showDemoContent = prefs[keyShowDemo] ?: true,
        )
    }

    suspend fun setLanguage(language: String) {
        val safeLang = if (language in setOf("ar", "en")) language else "ar"
        context.userPrefsDataStore.edit { it[keyLanguage] = safeLang }
    }

    suspend fun setDarkMode(mode: String) {
        val safeMode = if (mode in setOf("system", "light", "dark")) mode else "system"
        context.userPrefsDataStore.edit { it[keyDarkMode] = safeMode }
    }

    suspend fun setLargeText(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[keyLargeText] = enabled }
    }

    suspend fun setHighContrast(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[keyHighContrast] = enabled }
    }

    suspend fun setShowDemoContent(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[keyShowDemo] = enabled }
    }

    suspend fun applyRemotePreferences(prefs: UserPreferences) {
        context.userPrefsDataStore.edit {
            it[keyLanguage] = prefs.language
            it[keyDarkMode] = prefs.darkMode
            it[keyLargeText] = prefs.largeText
            it[keyHighContrast] = prefs.highContrast
            it[keyShowDemo] = prefs.showDemoContent
        }
    }
}
