package com.example.guardiancare.data.session

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.guardiancare.data.api.NotificationApiService
import com.example.guardiancare.data.api.RetrofitClient
import com.example.guardiancare.data.models.DeviceRegistrationRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class SessionManager private constructor(private val context: Context) {

    private val tokenKey = stringPreferencesKey("jwt_token")
    private val userTypeKey = stringPreferencesKey("user_type")
    private val userIdKey = stringPreferencesKey("user_id")
    private val fcmTokenKey = stringPreferencesKey("fcm_token")
    private val elderlyIdKey = stringPreferencesKey("elderly_id")

    fun saveUserSession(token: String, userType: String, userId: String, elderlyId: String? = null) {
        Log.d("SessionManager", "Saving user session for user $userId")

        runBlocking {
            context.dataStore.edit { prefs ->
                prefs[tokenKey] = token
                prefs[userTypeKey] = userType
                prefs[userIdKey] = userId
                elderlyId?.let { prefs[elderlyIdKey] = it }
            }
        }

        // Retrieve FCM token before using runBlocking
        val fcmToken = getFcmToken()

        if (fcmToken != null) {
            Log.d("SessionManager", "FCM token available, registering with backend")
            registerTokenWithBackend(userId, fcmToken)
        } else {
            Log.w("SessionManager", "No FCM token available to register")
        }
    }


    fun getToken(): String? {
        return runBlocking {
            val prefs = context.dataStore.data.first()
            prefs[tokenKey]
        }
    }

    fun getUserType(): String? {
        return runBlocking {
            val prefs = context.dataStore.data.first()
            prefs[userTypeKey]
        }
    }
    
    fun getUserId(): String? {
        return runBlocking {
            val prefs = context.dataStore.data.first()
            prefs[userIdKey]
        }
    }
    
    fun saveFcmToken(token: String) {
        Log.d("SessionManager", "Saving FCM token: ${token.take(10)}...")
        
        runBlocking {
            context.dataStore.edit { prefs ->
                prefs[fcmTokenKey] = token
            }
        }
        
        // If user is already logged in, register token with backend
        getUserId()?.let { userId ->
            Log.d("SessionManager", "User logged in, registering FCM token with backend")
            registerTokenWithBackend(userId, token)
        }
    }
    
    private fun registerTokenWithBackend(userId: String, token: String) {
        // Use Kotlin coroutines to perform network operation in background
        CoroutineScope(Dispatchers.IO).launch {
            registerTokenWithBackendSuspend(userId, token)
        }
    }
    
    private suspend fun registerTokenWithBackendSuspend(userId: String, token: String, retryCount: Int = 0) {
        try {
            Log.d("SessionManager", "Registering FCM token for user $userId")
            
            val notificationApiService = RetrofitClient
                .createService(NotificationApiService::class.java)
            
            val request = DeviceRegistrationRequest(
                userId = userId,
                deviceToken = token,
                deviceType = "android"
            )
            
            // Get auth token
            val authToken = getToken()
            if (authToken == null) {
                Log.e("SessionManager", "Authentication token not found. Cannot register FCM token.")
                return
            }
            
            // Create auth header
            val authHeader = "Bearer $authToken"
            
            val response = notificationApiService.registerDevice(authHeader, request)
            
            if (response.isSuccessful) {
                Log.d("SessionManager", "FCM token registered successfully with backend")
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("SessionManager", "Failed to register FCM token: $errorBody")
                
                // Retry up to 3 times with exponential backoff
                if (retryCount < 3) {
                    val delayTime = 5000L * (retryCount + 1) // 5s, 10s, 15s
                    Log.d("SessionManager", "Retrying FCM token registration in ${delayTime/1000} seconds")
                    delay(delayTime)
                    registerTokenWithBackendSuspend(userId, token, retryCount + 1)
                }
            }
        } catch (e: Exception) {
            Log.e("SessionManager", "Error registering FCM token", e)
            
            // Retry up to 3 times with exponential backoff
            if (retryCount < 3) {
                val delayTime = 10000L * (retryCount + 1) // 10s, 20s, 30s
                Log.d("SessionManager", "Retrying FCM token registration after error in ${delayTime/1000} seconds")
                delay(delayTime)
                registerTokenWithBackendSuspend(userId, token, retryCount + 1)
            }
        }
    }
    
    fun getFcmToken(): String? {
        return runBlocking {
            val prefs = context.dataStore.data.first()
            prefs[fcmTokenKey]
        }
    }
    
    fun saveElderlyId(elderlyId: String) {
        runBlocking {
            context.dataStore.edit { prefs ->
                prefs[elderlyIdKey] = elderlyId
            }
        }
    }
    
    fun fetchElderlyId(): String? {
        return runBlocking {
            val prefs = context.dataStore.data.first()
            prefs[elderlyIdKey] ?: getUserId() // Fallback to user ID if elderly ID is not set
        }
    }

    fun logout() {
        Log.d("SessionManager", "Clearing user session") // ✅ Debugging Log
        runBlocking {
            context.dataStore.edit { it.clear() }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context).also { INSTANCE = it }
            }
        }
    }
}
