package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.UserPreferencesStore
import com.example.model.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PreferencesViewModel(
    private val store: UserPreferencesStore,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = store.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UserPreferences(),
        )

    fun setLanguage(language: String) {
        viewModelScope.launch { store.setLanguage(language) }
    }

    fun setDarkMode(mode: String) {
        viewModelScope.launch { store.setDarkMode(mode) }
    }

    fun setLargeText(enabled: Boolean) {
        viewModelScope.launch { store.setLargeText(enabled) }
    }

    fun setHighContrast(enabled: Boolean) {
        viewModelScope.launch { store.setHighContrast(enabled) }
    }

    fun setShowDemoContent(enabled: Boolean) {
        viewModelScope.launch { store.setShowDemoContent(enabled) }
    }
}
