package com.example.guardiancare.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.guardiancare.GuardianApplication
import com.example.guardiancare.R
import com.example.guardiancare.data.api.RetrofitClient
import com.example.guardiancare.data.session.SessionManager
import com.example.guardiancare.utils.LocaleHelper
import com.example.guardiancare.utils.ThemeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val context: Context) : ViewModel() {
    private val sessionManager = SessionManager.getInstance(context)
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    // Settings state
    private val _settingsState = MutableStateFlow(SettingsState())
    val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()
    
    init {
        // Load user type and current language
        val userType = sessionManager.getUserType() ?: "elderly"
        val currentLanguage = LocaleHelper.getStoredLanguage(context)
        val isDarkModeEnabled = ThemeManager.isDarkModeEnabled(context)
        
        // Load server URL from preferences or use default
        val serverUrl = sharedPreferences.getString("server_url", RetrofitClient.baseUrl) ?: RetrofitClient.baseUrl
        
        _settingsState.value = _settingsState.value.copy(
            userType = userType,
            language = currentLanguage,
            darkModeEnabled = isDarkModeEnabled,
            serverUrl = serverUrl
        )
    }
    
    fun toggleNotifications() {
        _settingsState.value = _settingsState.value.copy(
            notificationsEnabled = !_settingsState.value.notificationsEnabled
        )
    }
    
    fun toggleDarkMode() {
        val newDarkModeState = !_settingsState.value.darkModeEnabled
        
        // Update the UI state
        _settingsState.value = _settingsState.value.copy(
            darkModeEnabled = newDarkModeState
        )
        
        // Show a toast message indicating theme change
        val message = if (newDarkModeState) {
            context.getString(R.string.applying_dark_mode)
        } else {
            context.getString(R.string.applying_light_mode)
        }
        android.widget.Toast.makeText(
            context,
            message,
            android.widget.Toast.LENGTH_SHORT
        ).show()
        
        // Apply the dark mode setting
        ThemeManager.setDarkMode(context, newDarkModeState)
        
        // Force system bars update immediately if context is Activity
        if (context is android.app.Activity) {
            try {
                // Try to call the updateSystemBarsAppearance method if it exists
                val updateMethod = context.javaClass.getDeclaredMethod("updateSystemBarsAppearance", Boolean::class.java)
                updateMethod.isAccessible = true
                updateMethod.invoke(context, newDarkModeState)
            } catch (e: Exception) {
                // Fallback to direct manipulation using modern APIs if reflection fails
                val window = context.window
                val controller = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
                
                // Use the ThemeManager instead of direct manipulation
                // This centralizes our theme logic and avoids deprecated API issues
                try {
                    // Let ThemeManager handle system bar appearance
                    ThemeManager.updateSystemBars(context as android.app.Activity, newDarkModeState)
                } catch (e: Exception) {
                    android.util.Log.e("SettingsViewModel", "Error updating system bars", e)
                    
                    // Fallback to basic appearance control if ThemeManager fails
                    androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !newDarkModeState
                        isAppearanceLightNavigationBars = !newDarkModeState
                    }
                }
            }
        }
    }
    
    fun toggleLocationSharing() {
        _settingsState.value = _settingsState.value.copy(
            locationSharingEnabled = !_settingsState.value.locationSharingEnabled
        )
    }
    
    fun toggleFallDetection() {
        _settingsState.value = _settingsState.value.copy(
            fallDetectionEnabled = !_settingsState.value.fallDetectionEnabled
        )
    }
    
    /**
     * Update the app's language with improved user experience
     * 
     * @param language The language display name to set
     */
    fun updateLanguage(language: String) {
        // ==================== VALIDATION ====================
        
        // Check if the language has actually changed
        if (language == _settingsState.value.language) {
            return // Skip if language unchanged
        }
        
        try {
            // ==================== PREFERENCE UPDATES ====================
            
            // Save preference and update locale configuration
            LocaleHelper.setLocale(context, language)
            
            // Apply global configuration via application object if possible
            try {
                val application = context.applicationContext
                if (application is GuardianApplication) {
                    application.applyLanguage(language)
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error applying language to application", e)
            }
            
            // Update UI state to reflect the new language
            _settingsState.value = _settingsState.value.copy(
                language = language
            )
            
            // ==================== USER FEEDBACK ====================
            
            // Show progress dialog if on Android < 13
            var progressDialog: android.app.AlertDialog? = null
            
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                // For older Android versions that require restart,
                // show a progress dialog for better user experience
                progressDialog = android.app.AlertDialog.Builder(context)
                    .setMessage(context.getString(R.string.applying_language, language))
                    .setCancelable(false)
                    .create()
                progressDialog.show()
            } else {
                // No toast for newer Android versions
                // Removed toast message here
            }
            
            // ==================== APPLY CHANGES ====================
            
            // Apply changes with slight delay to allow UI to update
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    // On older Android versions that require restart
                    progressDialog?.dismiss()
                    LocaleHelper.restartApp(context)
                } else {
                    // On Android 13+, we can apply changes on-the-fly without restart
                    // Just force recreate the current activity if possible
                    if (context is android.app.Activity) {
                        context.recreate()
                    } else {
                        // Fallback to restart if not an activity context
                        LocaleHelper.restartApp(context)
                    }
                }
            }, 800) // Short delay for UI feedback
            
        } catch (e: Exception) {
            android.util.Log.e("SettingsViewModel", "Error updating language", e)
            
            // Show error toast if something fails
            android.widget.Toast.makeText(
                context,
                "Error changing language: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    fun updateServerUrl(url: String) {
        // Update the state
        _settingsState.value = _settingsState.value.copy(
            serverUrl = url
        )
        
        // Update the RetrofitClient
        RetrofitClient.updateBaseUrl(url)
        
        // Save to shared preferences
        sharedPreferences.edit().putString("server_url", url).apply()
    }
    
    fun logout() {
        viewModelScope.launch {
            sessionManager.logout()
            // Navigation would be handled in the UI layer
        }
    }
}

// State class to hold settings data
data class SettingsState(
    val userType: String = "",
    val notificationsEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val locationSharingEnabled: Boolean = true,
    val fallDetectionEnabled: Boolean = true,
    val language: String = "English (US)",
    val dataUsage: String = "22.5 MB",
    val appVersion: String = "1.0.0",
//    val serverUrl: String = "http://10.0.2.2:8000/"
    val serverUrl: String = "http://172.20.10.14:8000/"
)

// ViewModel Factory
class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}