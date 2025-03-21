package com.example.guardiancare

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.guardiancare.ui.theme.GuardianCareTheme
import com.example.guardiancare.utils.BleManager
import com.example.guardiancare.utils.LocaleHelper
import com.example.guardiancare.utils.NotificationHelper
import com.example.guardiancare.utils.ThemeManager
import java.util.Locale

/**
 * Main entry point for the GuardianCare application
 * Handles initial setup and configuration
 */
@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : ComponentActivity() {
    // ==================== COMPONENT MANAGEMENT ====================
    
    private lateinit var bleManager: BleManager
    private lateinit var notificationHelper: NotificationHelper
    
    // Track permission state
    private val showPermissionDialog = mutableStateOf(false)
    
    // All required permissions for BLE
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        // ==================== CHECKING FOR PRESERVED THEME STATE ====================
        // Check if we're coming from a language switch with preserved dark mode state
        val preservedDarkMode = intent.getBooleanExtra("PRESERVE_DARK_MODE", false)
        val isDarkModeFromIntent = intent.hasExtra("PRESERVE_DARK_MODE")
        
        // Apply appropriate theme BEFORE super.onCreate()
        if (isDarkModeFromIntent) {
            // Coming from language switch - use the preserved dark mode value
            ThemeManager.setDarkMode(this, preservedDarkMode)
        } else {
            // Normal startup - use the stored preferences
            ThemeManager.applyStoredDarkMode(this)
        }
        
        // ==================== LOCALE SETUP ====================
        
        // 1. Get stored language preference
        val storedLanguage = LocaleHelper.getStoredLanguage(this)
        val locale = LocaleHelper.SUPPORTED_LOCALES[storedLanguage] ?: Locale.US
        
        // 2. Set default locale for legacy support
        Locale.setDefault(locale)
        
        // 3. Configure resources with the selected locale
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val config = Configuration(resources.configuration)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocales(android.os.LocaleList(locale))
            } else {
                @Suppress("DEPRECATION")
                config.locale = locale
            }
            
            // This is deprecated but necessary for backward compatibility
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        }
        
        // 4. Apply application-wide locale through AppCompat (for Android 13+)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale))
        
        super.onCreate(savedInstanceState)
        
        // ==================== SYSTEM BARS & THEME SETUP ====================
        // Use WindowCompat to handle the system bars - this is the modern approach
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Force update system bars immediately
        if (isDarkModeFromIntent) {
            updateSystemBarsAppearance(preservedDarkMode)
        } else {
            updateSystemBarsAppearance(ThemeManager.isDarkModeEnabled(this))
        }
        
        // ==================== COMPONENTS INITIALIZATION ====================
        bleManager = BleManager.getInstance(this)
        notificationHelper = NotificationHelper.getInstance(this)

        // ==================== PERMISSIONS SETUP ====================
        
        // Request permissions when the app starts
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.all { it.value }
            if (!allGranted) {
                // Show a toast with more helpful information
                Toast.makeText(
                    this, 
                    "Bluetooth and location permissions are required for BLE features. " +
                    "You can grant them in app settings.", 
                    Toast.LENGTH_LONG
                ).show()
                
                // Set flag to show permission dialog
                showPermissionDialog.value = true
            }
        }

        // Launch permission request
        permissionLauncher.launch(requiredPermissions)

        // ==================== BLE MONITORING SETUP ====================
        
        // Always listen for fall detection messages
        bleManager.setDataCallback { message ->
            if (message.contains("Fall detected", ignoreCase = true)) {
                notificationHelper.showFallDetectionNotification()
            }
        }

        // ==================== UI SETUP ====================
        
        setContent {
            // Use collectAsState to observe theme changes from ThemeManager
            val isDarkMode = ThemeManager.themeState.collectAsState().value
            
            GuardianCareTheme(darkTheme = isDarkMode) {
                // Show permission dialog if needed
                if (showPermissionDialog.value) {
                    AlertDialog(
                        onDismissRequest = { showPermissionDialog.value = false },
                        title = { Text("Permissions Required") },
                        text = { 
                            Text(
                                "Bluetooth and location permissions are required for BLE features. " +
                                "Would you like to open app settings to grant these permissions?"
                            ) 
                        },
                        confirmButton = {
                            Button(onClick = {
                                // Open app settings
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                val uri = Uri.fromParts("package", packageName, null)
                                intent.data = uri
                                startActivity(intent)
                                showPermissionDialog.value = false
                            }) {
                                Text("Open Settings")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showPermissionDialog.value = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                
                GuardianCareApp()
            }
            
            // Apply system bar colors whenever theme changes
            updateSystemBarsAppearance(isDarkMode)
        }
    }
    
    /**
     * Updates system UI (status bar and navigation bar) appearance based on theme
     * Using modern WindowInsetsControllerCompat which replaces deprecated systemUiVisibility
     * 
     * @param darkTheme Whether dark mode is enabled
     */
    fun updateSystemBarsAppearance(darkTheme: Boolean) {
        // Get the window insets controller
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        
        if (darkTheme) {
            // Dark theme settings
            window.statusBarColor = Color.BLACK
            window.navigationBarColor = Color.BLACK
            
            // Light icons for dark background
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        } else {
            // Light theme settings
            window.statusBarColor = Color.WHITE
            window.navigationBarColor = Color.WHITE
            
            // Dark icons for light background
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
        }
    }
    
    /**
     * Override attachBaseContext to apply locale configuration
     * This ensures that the Activity uses the correct locale from the start
     */
    override fun attachBaseContext(newBase: Context) {
        // Use the improved LocaleHelper method to create a properly configured context
        val localeContext = LocaleHelper.createLocaleContext(newBase)
        super.attachBaseContext(localeContext)
    }
    
    /**
     * Called when activity is resumed
     * Good place to ensure system bars are correctly themed
     */
    override fun onResume() {
        super.onResume()
        // Always update system bars on resume
        updateSystemBarsAppearance(ThemeManager.isDarkModeEnabled(this))
    }
}