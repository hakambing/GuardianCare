package com.example.guardiancare.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages theme preferences and application including dark mode
 * Provides reactive state updates via StateFlow for UI components
 */
object ThemeManager {
    
    // ==================== CONSTANTS & STATE ====================
    
    private const val PREF_NAME = "theme_pref"
    private const val DARK_MODE_ENABLED = "dark_mode_enabled"
    
    /**
     * StateFlow to notify observers of theme changes
     * UI components can collect this flow to react to theme changes
     */
    private val _themeState = MutableStateFlow(false)
    val themeState: StateFlow<Boolean> = _themeState.asStateFlow()
    
    // ==================== PUBLIC API ====================
    
    /**
     * Set the dark mode preference and apply it immediately
     * Updates both the shared preference and the runtime state
     * 
     * @param context Application or activity context
     * @param enabled True for dark mode, false for light mode
     */
    fun setDarkMode(context: Context, enabled: Boolean) {
        // 1. Save preference persistently
        savePreference(context, enabled)
        
        // 2. Apply the mode to the current app instance
        applyDarkMode(enabled)
        
        // 3. Apply to system UI if context is an activity
        if (context is android.app.Activity) {
            try {
                updateSystemBars(context, enabled)
            } catch (e: Exception) {
                android.util.Log.e("ThemeManager", "Error applying theme to system bars", e)
            }
        }
        
        // 4. Update the state flow to notify observers (triggers UI updates)
        _themeState.value = enabled
    }
    
    /**
     * Get the current dark mode preference from storage
     * 
     * @param context Application or activity context
     * @return True if dark mode is enabled, false otherwise
     */
    fun isDarkModeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(DARK_MODE_ENABLED, false)
    }
    
    /**
     * Apply the stored dark mode preference and initialize state flow
     * Used during app startup to restore user preference
     * 
     * @param context Application or activity context
     */
    fun applyStoredDarkMode(context: Context) {
        val isDarkMode = isDarkModeEnabled(context)
        _themeState.value = isDarkMode
        applyDarkMode(isDarkMode)
        
        // Apply to system UI if context is an activity
        if (context is android.app.Activity) {
            try {
                updateSystemBars(context, isDarkMode)
            } catch (e: Exception) {
                android.util.Log.e("ThemeManager", "Error applying theme to system bars", e)
            }
        }
    }
    
    /**
     * Updates system bars to match the current theme
     * This is a helper method used when the activity may not be fully initialized
     * 
     * @param activity The activity whose system bars should be themed
     * @param isDarkMode Whether dark mode is enabled
     */
    fun updateSystemBars(activity: android.app.Activity, isDarkMode: Boolean) {
        try {
            // Get the window and controller
            val window = activity.window
            
            // Use modern WindowInsetsControllerCompat to handle system bars appearance
            val controller = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
            
            // Set appearance (light vs dark icons)
            controller.isAppearanceLightStatusBars = !isDarkMode
            controller.isAppearanceLightNavigationBars = !isDarkMode
            
            // Use reflection to get the method that sets the color
            // This avoids using the deprecated property directly
            val statusBarColorMethod = window.javaClass.getMethod("setStatusBarColor", Int::class.java)
            val navBarColorMethod = window.javaClass.getMethod("setNavigationBarColor", Int::class.java)
            
            // Call the methods via reflection
            if (isDarkMode) {
                // Dark theme: black system bars
                statusBarColorMethod.invoke(window, android.graphics.Color.BLACK)
                navBarColorMethod.invoke(window, android.graphics.Color.BLACK)
            } else {
                // Light theme: white system bars
                statusBarColorMethod.invoke(window, android.graphics.Color.WHITE)
                navBarColorMethod.invoke(window, android.graphics.Color.WHITE)
            }
        } catch (e: Exception) {
            // Fallback if reflection fails
            android.util.Log.e("ThemeManager", "Error updating system bars", e)
            
            // Use the direct properties as a last resort
            try {
                val window = activity.window
                if (isDarkMode) {
                    @Suppress("DEPRECATION")
                    window.statusBarColor = android.graphics.Color.BLACK
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = android.graphics.Color.BLACK
                } else {
                    @Suppress("DEPRECATION")
                    window.statusBarColor = android.graphics.Color.WHITE
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = android.graphics.Color.WHITE
                }
                
                // Set the appearance
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = !isDarkMode
                controller.isAppearanceLightNavigationBars = !isDarkMode
            } catch (e2: Exception) {
                android.util.Log.e("ThemeManager", "Complete failure updating system bars", e2)
            }
        }
    }
    
    // ==================== IMPLEMENTATION DETAILS ====================
    
    /**
     * Save the dark mode preference to persistent storage
     * 
     * @param context Application or activity context
     * @param enabled True for dark mode, false for light mode
     */
    private fun savePreference(context: Context, enabled: Boolean) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean(DARK_MODE_ENABLED, enabled)
        editor.apply()
    }
    
    /**
     * Apply dark mode setting to the current app instance
     * Uses AppCompatDelegate to apply the theme system-wide
     * 
     * @param enabled True for dark mode, false for light mode
     */
    private fun applyDarkMode(enabled: Boolean) {
        val mode = if (enabled) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}