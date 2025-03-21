package com.example.guardiancare

/**
 * GuardianCare App
 * ==============
 * The main Compose entry point for the application, handling the top-level
 * navigation structure. This component manages the authentication flow and
 * determines the initial screen based on the user's login state.
 * 
 * Key responsibilities:
 * - Setting up the root navigation structure
 * - Checking login status to determine starting screen
 * - Providing the main app navigation controller
 * - Managing transitions between auth screens and main content
 */

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.guardiancare.data.session.SessionManager
import com.example.guardiancare.ui.screens.LoginScreen
import com.example.guardiancare.ui.screens.RegisterScreen
import com.example.guardiancare.ui.viewmodel.AuthViewModel

/**
 * Main Composable function for the GuardianCare app
 * 
 * This is the entry point for the app's UI, setting up the navigation
 * structure and handling the authentication flow.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianCareApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    // Check if user is already logged in
    val sessionManager = SessionManager.getInstance(context)
    val startDestination = if (sessionManager.getToken() != null) "drawerHome" else "login"

    if (sessionManager.getToken() != null) {
        Log.d("TOKEN", sessionManager.getToken()!!)
    }
    
    // Create AuthViewModel
    val authViewModel = remember { 
        AuthViewModel(context) 
    }

    // Root NavHost with routes
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Login Screen - Entry point for unauthenticated users
        composable("login") {
            LoginScreen(
                navController = navController,
                authViewModel = authViewModel,
                context = context
            )
        }

        // Registration Screen - For new user creation
        composable("register") {
            RegisterScreen(
                navController = navController,
                context = context
            )
        }

        // Main Drawer Screen - Contains the app's main functionality
        // This is shown after successful authentication
        composable("drawerHome") {
            // Pass the root navController to allow logout navigation
            MainDrawerScreen(rootNavController = navController)
        }
    }
}