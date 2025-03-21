package com.example.guardiancare.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.guardiancare.data.api.ElderlyApiService
import com.example.guardiancare.data.api.RetrofitClient
import com.example.guardiancare.data.api.UserProfileService
import com.example.guardiancare.data.models.CheckInResponse
import com.example.guardiancare.data.models.ElderlyResponse
import com.example.guardiancare.data.session.SessionManager
import kotlinx.coroutines.launch
import com.example.guardiancare.data.models.ProfileResponse

class ElderlyProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ElderlyProfileViewModel"
    
    private val sessionManager = SessionManager.getInstance(application)
    private val elderlyApiService = RetrofitClient.createService(ElderlyApiService::class.java)
    private val userProfileService = RetrofitClient.createService(UserProfileService::class.java)
    
    private val _elderlyData = MutableLiveData<ElderlyResponse>()
    val elderlyData: LiveData<ElderlyResponse> = _elderlyData
    
    private val _checkInsData = MutableLiveData<List<CheckInResponse>>()
    val checkInsData: LiveData<List<CheckInResponse>> = _checkInsData
    
    private val _latestCheckIn = MutableLiveData<CheckInResponse>()
    val latestCheckIn: LiveData<CheckInResponse> = _latestCheckIn
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error
    
    fun loadElderlyData(elderlyId: String) {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token == null) {
                    _error.value = "Authentication token not found. Please login again."
                    _isLoading.value = false
                    return@launch
                }
                
                val caretakerId = sessionManager.getUserId()
                if (caretakerId == null) {
                    _error.value = "User ID not found. Please login again."
                    _isLoading.value = false
                    return@launch
                }
                
                val authHeader = "Bearer $token"
                
                // Get all elderly assigned to the caretaker
                val elderlyResponse = elderlyApiService.getCaretakerElderly(authHeader, caretakerId)
                
                if (!elderlyResponse.isSuccessful) {
                    val errorMessage = elderlyResponse.errorBody()?.string() ?: "Unknown error"
                    _error.value = "Failed to fetch elderly profile: $errorMessage"
                    _isLoading.value = false
                    return@launch
                }
                
                val elderlyList = elderlyResponse.body()
                if (elderlyList != null && elderlyList.isNotEmpty()) {
                    // Find the specific elderly by ID
                    val elderly = elderlyList.find { it._id == elderlyId }
                    
                    if (elderly != null) {
                        _elderlyData.value = elderly
                    } else {
                        _error.value = "Elderly not found in your assigned list"
                    }
                } else {
                    _error.value = "No elderly data found"
                }
                
                _isLoading.value = false
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading elderly data", e)
                _error.value = "Error: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun loadCheckIns(elderlyId: String) {
        viewModelScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token == null) {
                    Log.e(TAG, "Authentication token not found")
                    return@launch
                }
                
                val authHeader = "Bearer $token"
                
                // Get all check-ins for the elderly
                val checkInsResponse = elderlyApiService.getElderlyCheckIns(authHeader, elderlyId)
                
                if (checkInsResponse.isSuccessful && checkInsResponse.body() != null) {
                    val checkIns = checkInsResponse.body()!!.sortedByDescending { it.created_at }
                    _checkInsData.value = checkIns
                    
                    // Also set the latest check-in
                    if (checkIns.isNotEmpty()) {
                        _latestCheckIn.value = checkIns.first()
                    }
                } else {
                    // Handle specific error codes gracefully
                    when (checkInsResponse.code()) {
                        404 -> {
                            // No check-ins found is a normal situation for new users
                            Log.i(TAG, "No check-ins found for elderly: $elderlyId")
                            _checkInsData.value = emptyList()
                        }
                        503 -> {
                            // Service unavailable - backend service is down
                            Log.w(TAG, "Check-in service is unavailable: $elderlyId")
                            _checkInsData.value = emptyList()
                            _error.value = "The check-in service is temporarily unavailable. Please try again later."
                        }
                        else -> {
                            // Log other errors
                            Log.e(TAG, "Failed to fetch check-ins: ${checkInsResponse.code()} - ${checkInsResponse.errorBody()?.string()}")
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading check-ins", e)
            }
        }
    }
}
