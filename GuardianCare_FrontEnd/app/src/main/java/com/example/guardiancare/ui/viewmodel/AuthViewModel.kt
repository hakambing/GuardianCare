package com.example.guardiancare.ui.viewmodel

/**
 * Authentication ViewModel
 * ======================
 * Handles all authentication-related operations including user login,
 * registration, and device token registration for push notifications.
 *
 * This ViewModel manages the communication between the UI and the backend 
 * authentication API, handling credential validation and session management.
 */

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.guardiancare.data.api.AuthApiService
import com.example.guardiancare.data.api.NotificationApiService
import com.example.guardiancare.data.api.RetrofitClient
import com.example.guardiancare.data.models.DeviceRegistrationRequest
import com.example.guardiancare.data.models.LoginRequest
import com.example.guardiancare.data.models.RegisterRequest
import com.example.guardiancare.data.session.SessionManager
import kotlinx.coroutines.launch

class AuthViewModel(private val context: Context) : ViewModel() {

    private val TAG = "AuthViewModel"
    private val sessionManager = SessionManager.getInstance(context)

    /**
     * Attempts to log in the user with email and password
     * 
     * @param email User's email address
     * @param password User's password
     * @param navController Navigation controller to redirect after login
     */
    fun loginUser(email: String, password: String, navController: NavController) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            val apiService = RetrofitClient.createService(AuthApiService::class.java)
            val response = apiService.login(LoginRequest(email, password))

            if (response.isSuccessful) {
                response.body()?.let {
                    // Save user session
                    sessionManager.saveUserSession(it.token, it.user.user_type, it.user.id)
                    
                    // Register FCM token if available
                    registerFcmToken(it.user.id, it.token)
                    
                    // Navigate to drawer home screen
                    navController.navigate("drawerHome") {
                        // Clear back stack
                        popUpTo("login") { inclusive = true }
                    }
                }
            } else {
                Toast.makeText(context, "Login failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Registers the device's FCM token with the backend for push notifications
     * 
     * @param userId User's unique identifier
     * @param authToken Authentication token for API authorization
     */
    private fun registerFcmToken(userId: String, authToken: String) {
        // Get FCM token from SessionManager
        val fcmToken = sessionManager.getFcmToken()
        
        if (fcmToken != null) {
            viewModelScope.launch {
                try {
                    val notificationApiService = RetrofitClient.createService(NotificationApiService::class.java)
                    
                    // Create auth header
                    val authHeader = "Bearer $authToken"
                    
                    // Create request
                    val request = DeviceRegistrationRequest(userId, fcmToken, "android")
                    
                    val response = notificationApiService.registerDevice(authHeader, request)
                    
                    if (response.isSuccessful) {
                        Log.d(TAG, "FCM token registered successfully")
                    } else {
                        Log.e(TAG, "Failed to register FCM token: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error registering FCM token", e)
                }
            }
        } else {
            Log.w(TAG, "No FCM token available to register")
        }
    }

    /**
     * Registers a new user with the provided information
     * 
     * @param name User's full name
     * @param email User's email address
     * @param password User's password
     * @param userType Type of user (elderly or caretaker)
     * @param dob Date of birth (optional)
     * @param address User's address (optional)
     * @param navController Navigation controller to redirect after registration
     */
    fun registerUser(
        name: String, 
        email: String, 
        password: String, 
        userType: String,
        dob: String? = null,
        address: String? = null,
        navController: NavController
    ) {
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(context, "Name, email and password are required", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            try {
                val apiService = RetrofitClient.createService(AuthApiService::class.java)
                val registerRequest = RegisterRequest(
                    user_type = userType,
                    name = name,
                    email = email,
                    password = password,
                    dob = dob,
                    address = address
                )
                
                val response = apiService.register(registerRequest)

                if (response.isSuccessful) {
                    Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                    // Navigate to login screen
                    navController.navigate("login") {
                        // Clear back stack
                        popUpTo("register") { inclusive = true }
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                    Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/**
 * ViewModel Factory for AuthViewModel
 * 
 * Provides the context required for the AuthViewModel
 */
class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}