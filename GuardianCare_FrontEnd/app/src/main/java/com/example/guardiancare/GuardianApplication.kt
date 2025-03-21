package com.example.guardiancare

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.guardiancare.data.session.SessionManager
import com.example.guardiancare.utils.LocaleHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Locale

/**
 * Main application class for GuardianCare.
 * Initializes app-wide settings like locale preferences and Firebase.
 */
class GuardianApplication : Application() {
    
    companion object {
        private const val TAG = "GuardianApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        initializeFirebase()
        
        // Initialize default language from stored preference
        val storedLanguage = LocaleHelper.getStoredLanguage(this)
        
        // Set the initial locale based on stored preferences
        applyLanguage(storedLanguage)
    }
    
    private fun initializeFirebase() {
        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "Firebase initialized successfully")
            
            // Get FCM token
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }
                
                // Get new FCM registration token
                val token = task.result
                
                // Save token to SessionManager
                val sessionManager = SessionManager.getInstance(this)
                sessionManager.saveFcmToken(token)
                
                // Log token for debugging (only show first 10 chars for security)
                Log.d(TAG, "FCM Token: ${token.take(10)}...")
                
                // Check if user is already logged in
                val userId = sessionManager.getUserId()
                if (userId != null) {
                    Log.d(TAG, "User already logged in (ID: $userId), token will be registered with backend")
                } else {
                    Log.d(TAG, "No user logged in yet, token saved locally for later registration")
                }
                
                // Set up token refresh listener
                FirebaseMessaging.getInstance().isAutoInitEnabled = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase", e)
        }
    }
    
    /**
     * Attaches base context with proper locale configuration
     * Called before onCreate - critical for resource initialization
     */
    override fun attachBaseContext(base: Context) {
        // Create properly configured context using LocaleHelper
        val localeContext = LocaleHelper.createLocaleContext(base)
        super.attachBaseContext(localeContext)
    }
    
    /**
     * Apply language to the application globally
     * Sets application-wide locale preferences
     * 
     * @param language The language display name to apply
     */
    fun applyLanguage(language: String) {
        // Get the corresponding locale
        val locale = LocaleHelper.SUPPORTED_LOCALES[language] ?: Locale.US
        
        // 1. Update system default locale (affects formatting)
        Locale.setDefault(locale)
        
        // 2. Set application locales using modern API (Android 13+)
        val localeList = LocaleListCompat.create(locale)
        AppCompatDelegate.setApplicationLocales(localeList)
        
        // 3. For older Android versions, use legacy approach in applyCurrentLanguage()
    }
    
    /**
     * Apply current language to a specific context
     * Useful for updating individual activities or views
     * 
     * @param context The context to update with current language
     */
    fun applyCurrentLanguage(context: Context) {
        val storedLanguage = LocaleHelper.getStoredLanguage(context)
        
        // Apply global settings
        applyLanguage(storedLanguage)
        
        // For older Android versions that need explicit configuration updates
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val locale = LocaleHelper.SUPPORTED_LOCALES[storedLanguage] ?: Locale.US
            
            // Update the resources configuration
            val resources = context.resources
            val configuration = Configuration(resources.configuration)
            
            // Set the locale in the configuration based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                configuration.setLocales(android.os.LocaleList(locale))
            } else {
                @Suppress("DEPRECATION")
                configuration.locale = locale
            }
            
            // Apply the updated configuration to resources
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
        }
    }
}