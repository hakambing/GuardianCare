package com.example.guardiancare.services

import android.util.Log
import com.example.guardiancare.data.api.NotificationApiService
import com.example.guardiancare.data.api.RetrofitClient
import com.example.guardiancare.data.models.DeviceRegistrationRequest
import com.example.guardiancare.data.session.SessionManager
import com.example.guardiancare.utils.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GuardianFirebaseMessagingService : FirebaseMessagingService() {
    private val TAG = "FCMService"
    
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed FCM token: ${token.take(10)}...")
        
        // Get current user ID from SessionManager
        val sessionManager = SessionManager.getInstance(applicationContext)
        val userId = sessionManager.getUserId()
        
        // If user is logged in, register the token with the server
        if (!userId.isNullOrEmpty()) {
            Log.d(TAG, "User logged in (ID: $userId), registering token with server")
            registerTokenWithServer(userId, token)
        } else {
            // Store token locally to register later when user logs in
            Log.d(TAG, "No user logged in, saving token locally for later registration")
            sessionManager.saveFcmToken(token)
        }
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")
        
        // Log message data
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data: ${remoteMessage.data}")
        }
        
        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message notification - Title: ${it.title}, Body: ${it.body}")
            
            // Display the notification
            val notificationHelper = NotificationHelper.getInstance(applicationContext)
            
            // Handle different notification types
            when (remoteMessage.data["type"]) {
                "FALL_DETECTION" -> {
                    val userId = remoteMessage.data["elderlyId"] ?: ""
                    Log.d(TAG, "Showing fall detection notification for user: $userId")
                    notificationHelper.showFallDetectionNotification(userId)
                }
                else -> {
                    // Default notification
                    Log.d(TAG, "Showing generic notification")
                    notificationHelper.showGenericNotification(
                        it.title ?: "GuardianCare",
                        it.body ?: "You have a new notification"
                    )
                }
            }
        } ?: run {
            // If there's no notification payload but there is data, handle data message
            if (remoteMessage.data.isNotEmpty()) {
                Log.d(TAG, "Received data-only message, handling based on type")
                
                val notificationHelper = NotificationHelper.getInstance(applicationContext)
                
                when (remoteMessage.data["type"]) {
                    "FALL_DETECTION" -> {
                        val userId = remoteMessage.data["elderlyId"] ?: ""
                        val title = remoteMessage.data["title"] ?: "Fall Detection Alert"
                        val message = remoteMessage.data["message"] 
                            ?: "A fall has been detected. Immediate assistance may be required."
                        
                        Log.d(TAG, "Showing fall detection notification from data message")
                        notificationHelper.showFallDetectionNotification(userId)
                    }
                    else -> {
                        val title = remoteMessage.data["title"] ?: "GuardianCare"
                        val message = remoteMessage.data["message"] ?: "You have a new notification"
                        
                        Log.d(TAG, "Showing generic notification from data message")
                        notificationHelper.showGenericNotification(title, message)
                    }
                }
            } else {
                Log.w(TAG, "Received empty message with no notification or data")
            }
        }
    }
    
    private fun registerTokenWithServer(userId: String, token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Registering FCM token with server for user $userId")
                
                // Create notification API service
                val notificationApiService = RetrofitClient.createService(NotificationApiService::class.java)
                
                // Get auth token from session manager
                val sessionManager = SessionManager.getInstance(applicationContext)
                val authToken = sessionManager.getToken()
                
                if (authToken == null) {
                    Log.e(TAG, "Authentication token not found. Saving FCM token locally for later registration.")
                    sessionManager.saveFcmToken(token)
                    return@launch
                }
                
                // Create auth header
                val authHeader = "Bearer $authToken"
                
                // Create request
                val request = DeviceRegistrationRequest(userId, token, "android")
                
                // Register device
                val response = notificationApiService.registerDevice(authHeader, request)
                
                if (response.isSuccessful) {
                    Log.d(TAG, "FCM token registered successfully with server")
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Failed to register FCM token: $errorBody")
                    
                    // Schedule a retry after a delay
                    kotlinx.coroutines.delay(5000) // 5 seconds
                    Log.d(TAG, "Retrying FCM token registration")
                    registerTokenWithServer(userId, token)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error registering FCM token", e)
                
                // Schedule a retry after a delay
                kotlinx.coroutines.delay(10000) // 10 seconds
                Log.d(TAG, "Retrying FCM token registration after error")
                registerTokenWithServer(userId, token)
            }
        }
    }
    
    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.d(TAG, "FCM messages deleted on server")
    }
    
    override fun onMessageSent(msgId: String) {
        super.onMessageSent(msgId)
        Log.d(TAG, "FCM message sent: $msgId")
    }
    
    override fun onSendError(msgId: String, exception: Exception) {
        super.onSendError(msgId, exception)
        Log.e(TAG, "FCM message send error: $msgId", exception)
    }
}
