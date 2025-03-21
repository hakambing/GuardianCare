package com.example.guardiancare.ui.viewmodel

/**
 * Notification ViewModel
 * ====================
 * Manages notification data for the application, specifically focused on
 * fall detection notifications. This ViewModel handles loading notifications 
 * from the API, transforming them to UI models, and providing formatted timestamps.
 * 
 * Key responsibilities:
 * - Loading notification data from the backend API
 * - Filtering notifications by type
 * - Formatting notification timestamps
 * - Managing loading and error states
 */

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guardiancare.data.api.NotificationApiService
import com.example.guardiancare.data.api.RetrofitClient
import com.example.guardiancare.data.models.NotificationData
import com.example.guardiancare.data.models.NotificationType
import com.example.guardiancare.data.session.SessionManager
import com.example.guardiancare.ui.screens.FallNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class NotificationViewModel(
    private val sessionManager: SessionManager
) : ViewModel() {
    
    // UI state for notifications
    private val _notifications = MutableStateFlow<List<FallNotification>>(emptyList())
    val notifications: StateFlow<List<FallNotification>> = _notifications.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // API service for notifications
    private val notificationApiService = RetrofitClient.createService(NotificationApiService::class.java)
    
    /**
     * Initialize the ViewModel and load notifications if on Android O or higher
     */
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            loadNotifications()
        }
    }
    
    /**
     * Load notifications from the API
     * 
     * Fetches notifications for the current user, filters for fall detection
     * notifications, and updates the UI state.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadNotifications() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                // Get the user ID and auth token
                val userId = sessionManager.getUserId()
                val token = sessionManager.getToken()
                
                if (userId.isNullOrEmpty() || token.isNullOrEmpty()) {
                    _error.value = "User not logged in"
                    return@launch
                }
                
                // Create auth header
                val authHeader = "Bearer $token"
                
                // Fetch notifications from the API
                val response = notificationApiService.getNotifications(
                    authHeader = authHeader,
                    userId = userId
                )
                
                if (response.isSuccessful && response.body() != null) {
                    // Filter for fall detection notifications only
                    val fallNotifications = response.body()!!.data
                        .filter { it.type == NotificationType.FALL_DETECTION.value }
                        .map { mapToFallNotification(it) }
                    
                    _notifications.value = fallNotifications
                } else {
                    _error.value = "Failed to load notifications: ${response.message()}"
                }
                
            } catch (e: Exception) {
                _error.value = "Failed to load notifications: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Map API notification data to UI model
     * 
     * Converts the raw API notification data into a UI-friendly model
     * for display in the notification list.
     * 
     * @param notification The notification data from the API
     * @return A FallNotification object for use in the UI
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun mapToFallNotification(notification: NotificationData): FallNotification {
        // Parse ISO 8601 timestamp to milliseconds
        val timestamp = try {
            if (notification.timestamp != null) {
                val instant = java.time.Instant.parse(notification.timestamp)
                instant.toEpochMilli()
            } else {
                System.currentTimeMillis()
            }
        } catch (e: Exception) {
            // If parsing fails, use current time
            System.currentTimeMillis()
        }
        
        return FallNotification(
            id = notification.id ?: "",
            elderlyId = notification.elderlyId, // Include elderlyId from notification
            elderlyName = notification.content?.elderlyName ?: "Unknown",
            timestamp = timestamp
        )
    }
    
    /**
     * Format relative time for notifications
     * 
     * Converts a timestamp to a human-readable relative time string
     * (e.g., "5 minutes ago", "Yesterday", etc.)
     * 
     * @param timestamp The timestamp in milliseconds
     * @return A formatted relative time string
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun formatRelativeTime(timestamp: Long): String {
        val instant = Instant.ofEpochMilli(timestamp)
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val now = LocalDateTime.now()
        
        val minutesDiff = ChronoUnit.MINUTES.between(dateTime, now)
        val hoursDiff = ChronoUnit.HOURS.between(dateTime, now)
        val daysDiff = ChronoUnit.DAYS.between(dateTime, now)
        
        return when {
            minutesDiff < 1 -> "Just now"
            minutesDiff < 60 -> "$minutesDiff minutes ago"
            hoursDiff < 24 -> "$hoursDiff hours ago"
            daysDiff < 2 -> "Yesterday"
            daysDiff < 7 -> "$daysDiff days ago"
            else -> dateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
    }
    
    /**
     * Format precise date and time for notifications
     * 
     * Converts a timestamp to a complete date and time string
     * (e.g., "Jan 12, 2023 3:45 PM")
     * 
     * @param timestamp The timestamp in milliseconds
     * @return A formatted date and time string
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun formatDateTime(timestamp: Long): String {
        val instant = Instant.ofEpochMilli(timestamp)
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        return dateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))
    }
}
