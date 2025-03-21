package com.example.guardiancare.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.guardiancare.data.api.ElderlyApiService
import com.example.guardiancare.data.api.RetrofitClient
import com.example.guardiancare.data.models.AssignElderlyRequest
import com.example.guardiancare.data.models.CheckInResponse
import com.example.guardiancare.data.models.ElderlyResponse
import com.example.guardiancare.data.models.ErrorResponse
import com.example.guardiancare.data.models.UnassignElderlyRequest
import com.example.guardiancare.data.session.SessionManager
import com.example.guardiancare.ui.screens.Patient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ElderlyViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ElderlyViewModel"
    
    private val sessionManager = SessionManager.getInstance(application)
    private val elderlyApiService = RetrofitClient.createService(ElderlyApiService::class.java)
    
    private val _patientsData = MutableLiveData<List<Patient>>()
    val patientsData: LiveData<List<Patient>> = _patientsData
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

    fun loadPatientsData() {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val caretakerId = sessionManager.getUserId()
                if (caretakerId == null) {
                    _error.value = "User ID not found. Please login again."
                    _isLoading.value = false
                    return@launch
                }
                
                val token = sessionManager.getToken()
                if (token == null) {
                    _error.value = "Authentication token not found. Please login again."
                    _isLoading.value = false
                    return@launch
                }
                
                val authHeader = "Bearer $token"
                
                // 1. Get all elderly assigned to the caretaker
                val elderlyResponse = elderlyApiService.getCaretakerElderly(authHeader, caretakerId)
                
                if (!elderlyResponse.isSuccessful) {
                    val errorMessage = elderlyResponse.errorBody()?.string() ?: "Unknown error"
                    _error.value = "Failed to fetch elderly: $errorMessage"
                    _isLoading.value = false
                    return@launch
                }
                
                val elderlyList = elderlyResponse.body() ?: emptyList()
                
                // 2. Get the latest check-in for each elderly and convert to Patient data model
                val patients = mutableListOf<Patient>()
                
                elderlyList.forEach { elderly ->
                    try {
                        val checkInResponse = elderlyApiService.getLatestCheckIn(authHeader, elderly._id)
                        
                        try {
                            if (checkInResponse.isSuccessful && checkInResponse.body() != null) {
                                val checkIn = checkInResponse.body()!!
                                val lastCheckInTime = formatLastCheckInTime(checkIn.created_at)
                                
                                patients.add(
                                    Patient(
                                        id = elderly._id,
                                        name = elderly.name,
                                        status = checkIn.status,
                                        lastCheckIn = lastCheckInTime,
                                        hasAlert = checkIn.priority >= 3,
                                        priority = checkIn.priority,
                                        mood = checkIn.mood,
                                        summary = checkIn.summary,
                                        transcript = checkIn.transcript,
                                        hasCheckedInToday = checkIfLastCheckInWasToday(checkIn.created_at)
                                    )
                                )
                            } else {
                                // Handle specific error codes
                                val errorMsg = when (checkInResponse.code()) {
                                    404 -> "No check-in recorded"
                                    503 -> "Service temporarily unavailable"
                                    else -> "No data available"
                                }
                                
                                val status = when (checkInResponse.code()) {
                                    503 -> "Service unavailable"
                                    else -> "No data"
                                }
                                
                                // Log the error response body for debugging
                                val errorBody = checkInResponse.errorBody()?.string()
                                Log.e(TAG, "Error response body: $errorBody")
                                
                                // Add elderly with default values
                                patients.add(
                                    Patient(
                                        id = elderly._id,
                                        name = elderly.name,
                                        status = status,
                                        lastCheckIn = errorMsg,
                                        hasAlert = false,
                                        priority = 0,
                                        mood = 2, // Default neutral mood
                                        summary = "No check-in data available for this elderly person.",
                                        transcript = "",
                                        hasCheckedInToday = false
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing check-in response for elderly ${elderly._id}", e)
                            
                            // Add elderly with default values
                            patients.add(
                                Patient(
                                    id = elderly._id,
                                    name = elderly.name,
                                    status = "Error",
                                    lastCheckIn = "Failed to process check-in data",
                                    hasAlert = false,
                                    priority = 0,
                                    mood = 2, // Default neutral mood
                                    summary = "Unable to retrieve check-in data. Please try again later.",
                                    transcript = "",
                                    hasCheckedInToday = false
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching check-in for elderly ${elderly._id}", e)
                        
                        // Add elderly with error status
                        patients.add(
                            Patient(
                                id = elderly._id,
                                name = elderly.name,
                                status = "Error",
                                lastCheckIn = "Failed to fetch check-in data",
                                hasAlert = false,
                                priority = 0
                            )
                        )
                    }
                }
                
                // Sort patients by priority (highest first)
                val sortedPatients = patients.sortedByDescending { it.priority }
                _patientsData.value = sortedPatients
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading patients data", e)
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun assignElderlyByEmail(email: String, callback: (success: Boolean, message: String) -> Unit) {
        viewModelScope.launch {
            try {
                val caretakerId = sessionManager.getUserId()
                if (caretakerId == null) {
                    callback(false, "User ID not found. Please login again.")
                    return@launch
                }
                
                val token = sessionManager.getToken()
                if (token == null) {
                    callback(false, "Authentication token not found. Please login again.")
                    return@launch
                }
                
                val authHeader = "Bearer $token"
                
                // Create request
                val request = AssignElderlyRequest(
                    caretakerId = caretakerId,
                    elderlyEmail = email
                )
                
                Log.d(TAG, "Sending assignment request: $request")
                
                // Make API call
                val response = elderlyApiService.assignElderlyByEmail(authHeader, request)
                
                Log.d(TAG, "Assignment response code: ${response.code()}")
                
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    Log.d(TAG, "Success response: $result")
                    if (result.success) {
                        callback(true, result.message)
                    } else {
                        callback(false, result.message)
                    }
                } else {
                    try {
                        // Try to parse the error message from the response
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.e(TAG, "Error response body: $errorBody")
                        
                        // Check if the error is in JSON format
                        if (errorBody.startsWith("{") && errorBody.contains("message")) {
                            val gson = com.google.gson.Gson()
                            val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                            val errorMsg = errorResponse.message ?: "Unknown error"
                            Log.e(TAG, "Parsed error message: $errorMsg")
                            callback(false, errorMsg)
                        } else {
                            Log.e(TAG, "Non-JSON error response: $errorBody")
                            callback(false, "Failed to assign elderly: $errorBody")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing response", e)
                        // If parsing fails, just return the raw error
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        callback(false, "Error: $errorBody")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error assigning elderly by email", e)
                callback(false, "Error: ${e.message}")
            }
        }
    }
    
    fun unassignElderly(elderlyId: String, callback: (success: Boolean, message: String) -> Unit) {
        viewModelScope.launch {
            try {
                val caretakerId = sessionManager.getUserId()
                if (caretakerId == null) {
                    callback(false, "User ID not found. Please login again.")
                    return@launch
                }
                
                val token = sessionManager.getToken()
                if (token == null) {
                    callback(false, "Authentication token not found. Please login again.")
                    return@launch
                }
                
                val authHeader = "Bearer $token"
                
                // Create request
                val request = UnassignElderlyRequest(elderlyId = elderlyId)
                
                Log.d(TAG, "Sending unassignment request: $request")
                
                // Make API call
                val response = elderlyApiService.unassignElderly(authHeader, request)
                
                Log.d(TAG, "Unassignment response code: ${response.code()}")
                
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    Log.d(TAG, "Success response: $result")
                    if (result.success) {
                        callback(true, result.message)
                    } else {
                        callback(false, result.message)
                    }
                } else {
                    try {
                        // Try to parse the error message from the response
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.e(TAG, "Error response body: $errorBody")
                        
                        // Check if the error is in JSON format
                        if (errorBody.startsWith("{") && errorBody.contains("message")) {
                            val gson = com.google.gson.Gson()
                            val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                            val errorMsg = errorResponse.message ?: "Unknown error"
                            Log.e(TAG, "Parsed error message: $errorMsg")
                            callback(false, errorMsg)
                        } else {
                            Log.e(TAG, "Non-JSON error response: $errorBody")
                            callback(false, "Failed to unassign elderly: $errorBody")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing response", e)
                        // If parsing fails, just return the raw error
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        callback(false, "Error: $errorBody")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error unassigning elderly", e)
                callback(false, "Error: ${e.message}")
            }
        }
    }
    
    private fun formatLastCheckInTime(timestamp: String?): String {
        if (timestamp == null) return "Unknown"
        
        try {
            val date = sdf.parse(timestamp) ?: return "Invalid date"
            val now = Date()
            
            val diffMillis = now.time - date.time
            val diffHours = TimeUnit.MILLISECONDS.toHours(diffMillis)
            
            return when {
                diffHours < 1 -> "Today, ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)}"
                diffHours < 24 -> "Today, ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)}"
                diffHours < 48 -> "Yesterday, ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)}"
                else -> "${TimeUnit.MILLISECONDS.toDays(diffMillis)} days ago"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date: $timestamp", e)
            return "Invalid date"
        }
    }

    private fun checkIfLastCheckInWasToday(timestamp: String?): Boolean {
        if (timestamp == null) return false

        try {
            val date = sdf.parse(timestamp) ?: return false
            val now = Date()

            val diffMillis = now.time - date.time
            val diffHours = TimeUnit.MILLISECONDS.toHours(diffMillis)

            return diffHours < 24
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date: $timestamp", e)
            return false
        }
    }
}
