package com.example.guardiancare.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.guardiancare.MainActivity
import java.util.Locale

/**
 * Helper class to manage application locales and language switching
 */
object LocaleHelper {
    
    // ==================== CONSTANTS ====================
    
    private const val PREF_NAME = "language_pref"
    private const val SELECTED_LANGUAGE = "selected_language"
    
    /**
     * Supported languages with their corresponding locales
     * Key: Display name for UI
     * Value: Locale object for system
     */
    val SUPPORTED_LOCALES = mapOf(
        "English (US)" to Locale.US,
        "Bahasa Melayu" to Locale("ms", "MY"),
        "简体中文 (Simplified Chinese)" to Locale.SIMPLIFIED_CHINESE,
        "हिन्दी (Hindi)" to Locale("hi", "IN")
    )
    
    // ==================== PREFERENCE MANAGEMENT ====================
    
    /**
     * Save the selected language preference
     * 
     * @param context Application or activity context
     * @param language The language display name to save
     */
    private fun savePreference(context: Context, language: String) {
        val pref: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putString(SELECTED_LANGUAGE, language)
        
        // Use commit() instead of apply() to ensure immediate write
        editor.commit()
    }
    
    /**
     * Get the saved language preference
     * 
     * @param context Application or activity context
     * @return The stored language display name, defaults to English
     */
    fun getStoredLanguage(context: Context): String {
        val pref: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pref.getString(SELECTED_LANGUAGE, "English (US)") ?: "English (US)"
    }
    
    // ==================== LOCALE CONFIGURATION ====================
    
    /**
     * Update the app's locale based on the selected language
     * 
     * @param context Application or activity context
     * @param language The language display name to apply
     * @return The context with updated configuration
     */
    fun setLocale(context: Context, language: String): Context {
        // Get the corresponding locale
        val locale = SUPPORTED_LOCALES[language] ?: Locale.US
        
        // Save the selected language to SharedPreferences
        savePreference(context, language)
        
        // Set default locale (affects new instances of formatters, etc.)
        Locale.setDefault(locale)
        
        try {
            // Set the locale using AppCompatDelegate (modern API - Android 13+)
            val localeList = LocaleListCompat.create(locale)
            AppCompatDelegate.setApplicationLocales(localeList)
            
            // For older Android versions, also update the configuration directly
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                updateResourcesLegacy(context, locale)
            }
        } catch (e: Exception) {
            android.util.Log.e("LocaleHelper", "Error setting locale", e)
        }
        
        return context
    }
    
    /**
     * Get the current language display name
     * 
     * @param context Application or activity context
     * @return The current language display name
     */
    fun getCurrentLanguage(context: Context): String {
        return getStoredLanguage(context)
    }
    
    /**
     * Legacy method to update resources for older Android versions
     * Uses deprecated APIs but necessary for backwards compatibility
     * 
     * @param context Application or activity context
     * @param locale The locale to apply
     * @return The context with updated configuration
     */
    private fun updateResourcesLegacy(context: Context, locale: Locale): Context {
        try {
            // Get resources and create new configuration based on current
            val resources = context.resources
            val configuration = Configuration(resources.configuration)
            
            // Set the locale in the configuration (different API for different Android versions)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                configuration.setLocales(android.os.LocaleList(locale))
            } else {
                @Suppress("DEPRECATION")
                configuration.locale = locale
            }
            
            // Update the configuration (deprecated but needed for older versions)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
            
            return context
        } catch (e: Exception) {
            android.util.Log.e("LocaleHelper", "Error updating resources legacy", e)
            return context
        }
    }
    
    // ==================== ACTIVITY MANAGEMENT ====================
    
    /**
     * Creates a context with the specified locale configuration
     * Used in attachBaseContext() of activities
     * 
     * @param context Base context
     * @return Context with updated locale configuration
     */
    fun createLocaleContext(context: Context): Context {
        val language = getStoredLanguage(context)
        val locale = SUPPORTED_LOCALES[language] ?: Locale.US
        
        // Create a context with the desired locale using proper APIs for different versions
        val config = Configuration(context.resources.configuration)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(android.os.LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        
        return context.createConfigurationContext(config)
    }
    
    /**
     * Apply locale configuration to an existing context
     * Useful for updating activity contexts after language change
     * 
     * @param context The context to update
     * @param locale The locale to apply
     * @return Updated context
     */
    fun updateLocale(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(android.os.LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        
        return context.createConfigurationContext(config)
    }
    
    /**
     * Restart activities to apply language changes
     * Uses a more graceful approach than killing the process
     * 
     * @param context Application or activity context
     */
    fun restartApp(context: Context) {
        try {
            // IMPORTANT: Store current dark mode state before restart
            val isDarkMode = com.example.guardiancare.utils.ThemeManager.isDarkModeEnabled(context)
            
            // Create intent to restart main activity
            val intent = Intent(context, MainActivity::class.java).apply {
                // Clear activity stack
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or 
                       Intent.FLAG_ACTIVITY_NEW_TASK or
                       Intent.FLAG_ACTIVITY_CLEAR_TOP)
                // Add timestamp to prevent intent reuse
                putExtra("LANGUAGE_SWITCH_TIMESTAMP", System.currentTimeMillis())
                // CRITICAL: Pass dark mode state to the new activity
                putExtra("PRESERVE_DARK_MODE", isDarkMode)
            }
            
            // Start activity with the new intent
            context.startActivity(intent)
            
            // Optional: If context is an Activity, finish it
            if (context is android.app.Activity) {
                context.finish()
            }
            
            // Graceful exit if needed (finishes current activity but doesn't kill process)
            // CRITICAL: Always finish activities to reset the system UI properly
            Runtime.getRuntime().exit(0) // This is needed to fully reset system UI on language change
        } catch (e: Exception) {
            android.util.Log.e("LocaleHelper", "Error restarting app", e)
            
            // Fallback to Process.killProcess only as last resort
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}