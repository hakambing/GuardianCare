package com.example.guardiancare.ui.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.guardiancare.data.api.AuthApiService
import com.example.guardiancare.data.api.NotificationApiService
import com.example.guardiancare.data.api.RetrofitClient
import com.example.guardiancare.data.models.DeviceRegistrationRequest
import com.example.guardiancare.data.models.UpdateProfileRequest
import com.example.guardiancare.data.session.SessionManager
import com.example.guardiancare.utils.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileViewModel(private val context: Context) : ViewModel() {
    private val sessionManager = SessionManager.getInstance(context)
    
    // User profile state
    private val _userProfile = MutableStateFlow(UserProfileState())
    val userProfile: StateFlow<UserProfileState> = _userProfile.asStateFlow()
    
    // Loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Update success state
    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()
    
    // Test notification state
    private val _testNotificationSent = MutableStateFlow(false)
    val testNotificationSent: StateFlow<Boolean> = _testNotificationSent.asStateFlow()
    
    // Use the centralized API services
    private val authApiService: AuthApiService = RetrofitClient.createService(AuthApiService::class.java)
    private val notificationApiService: NotificationApiService = RetrofitClient.createService(NotificationApiService::class.java)
    
    init {
        loadUserProfile()
    }
    
    fun loadUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Get token from session manager
                val token = sessionManager.getToken()
                if (token == null) {
                    _error.value = "Authentication token not found. Please login again."
                    return@launch
                }
                
                // Make API call with token
                val authHeader = "Bearer $token"
                val response = authApiService.getProfile(authHeader)
                
                if (response.isSuccessful && response.body() != null) {
                    val profileData = response.body()!!
                    
                    // Format the date if available
                    val formattedDob = profileData.dob?.let { formatDate(it) } ?: ""
                    
                    // Update profile state
                    _userProfile.value = UserProfileState(
                        id = profileData._id,
                        name = profileData.name,
                        email = profileData.email,
                        userType = profileData.user_type,
                        dob = formattedDob,
                        medicalHistory = profileData.medical_history ?: emptyList(),
                        caretakerId = profileData.caretaker_id,
                        createdAt = formatDate(profileData.created_at),
                        updatedAt = formatDate(profileData.updated_at)
                    )
                } else {
                    _error.value = "Error fetching profile: ${response.errorBody()?.string()}"
                    Toast.makeText(context, "Error fetching profile", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: HttpException) {
                _error.value = "Error fetching profile: ${e.message()}"
                Toast.makeText(context, "Error fetching profile", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                _error.value = "Network error: Check your connection"
                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                _error.value = "Unexpected error: ${e.message}"
                Toast.makeText(context, "Unexpected error", Toast.LENGTH_SHORT).show()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateProfile(name: String? = null, dob: String? = null, medicalHistory: List<String>? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _updateSuccess.value = false
            
            try {
                // Get token from session manager
                val token = sessionManager.getToken()
                if (token == null) {
                    _error.value = "Authentication token not found. Please login again."
                    return@launch
                }
                
                // Create update request
                val updateRequest = UpdateProfileRequest(
                    name = name,
                    dob = dob,
                    medical_history = medicalHistory
                )
                
                // Make API call with token
                val authHeader = "Bearer $token"
                val response = authApiService.updateProfile(authHeader, updateRequest)
                
                if (response.isSuccessful && response.body() != null) {
                    val updateResponse = response.body()!!
                    
                    // Show success message
                    Toast.makeText(context, updateResponse.message, Toast.LENGTH_SHORT).show()
                    
                    // Set update success flag
                    _updateSuccess.value = true
                    
                    // Reload profile to get updated data
                    loadUserProfile()
                } else {
                    _error.value = "Error updating profile: ${response.errorBody()?.string()}"
                    Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: HttpException) {
                _error.value = "Error updating profile: ${e.message()}"
                Toast.makeText(context, "Error updating profile", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                _error.value = "Network error: Check your connection"
                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                _error.value = "Unexpected error: ${e.message}"
                Toast.makeText(context, "Unexpected error", Toast.LENGTH_SHORT).show()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Send a test notification to the current device
     */
    fun sendTestNotification() {
        viewModelScope.launch {
            _isLoading.value = true
            _testNotificationSent.value = false
            
            try {
                // Get FCM token from session manager
                val fcmToken = sessionManager.getFcmToken()
                if (fcmToken == null) {
                    Toast.makeText(context, "FCM token not found. Please restart the app.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Get user ID
                val userId = sessionManager.getUserId()
                if (userId == null) {
                    Toast.makeText(context, "User ID not found. Please login again.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Show a local notification first for immediate feedback
                val notificationHelper = NotificationHelper.getInstance(context)
                notificationHelper.showGenericNotification(
                    "Test Notification",
                    "This is a local test notification. A push notification should arrive shortly."
                )
                
                // Get token from session manager
                val authToken = sessionManager.getToken()
                if (authToken == null) {
                    Toast.makeText(context, "Authentication token not found. Please login again.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Create auth header
                val authHeader = "Bearer $authToken"
                
                // Send a test notification via the backend
                try {
                    // Try to send via backend API
                    val response = notificationApiService.sendTestNotification(
                        authHeader,
                        DeviceRegistrationRequest(
                            userId = userId,
                            deviceToken = fcmToken,
                            deviceType = "android"
                        )
                    )
                    
                    if (response.isSuccessful) {
                        _testNotificationSent.value = true
                        Toast.makeText(context, "Test notification sent successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Toast.makeText(context, "Failed to send test notification: $errorBody", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    // If backend API fails, fall back to local notification
                    Toast.makeText(
                        context, 
                        "Could not send push notification. Check server connection.", 
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Show a fallback notification
                    notificationHelper.showFallDetectionNotification(userId)
                }
                
            } catch (e: Exception) {
                Toast.makeText(context, "Error sending test notification: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val outputFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }
}

// State class to hold user profile data
data class UserProfileState(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val userType: String = "",
    val dob: String = "",
    val medicalHistory: List<String> = emptyList(),
    val caretakerId: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

// ViewModel Factory
class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
