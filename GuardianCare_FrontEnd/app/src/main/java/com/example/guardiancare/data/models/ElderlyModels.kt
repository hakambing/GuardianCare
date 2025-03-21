package com.example.guardiancare.data.models

import java.util.Date

data class ElderlyResponse(
    val _id: String,
    val name: String,
    val user_type: String,
    val email: String,
    val dob: String?,
    val medical_history: List<String>?,
    val caretaker_id: String?,
    val address: String? = null,
    val emergency_contact: String? = null
)

data class CheckInResponse(
    val _id: String,
    val elderly_id: String,
    val summary: String,
    val priority: Int,
    val mood: Int,
    val status: String,
    val transcript: String?,
    val created_at: String? = null,
    val updated_at: String? = null,
    val created_at_date: Date? = null,
    val updated_at_date: Date? = null,
)

data class AssignElderlyRequest(
    val caretakerId: String,
    val elderlyEmail: String
) {
    override fun toString(): String {
        return "AssignElderlyRequest(caretakerId=$caretakerId, elderlyEmail=$elderlyEmail)"
    }
}

data class AssignElderlyResponse(
    val success: Boolean,
    val message: String,
    val elderly: ElderlyResponse? = null
) {
    override fun toString(): String {
        return "AssignElderlyResponse(success=$success, message='$message', elderly=$elderly)"
    }
}

data class UnassignElderlyRequest(
    val elderlyId: String
) {
    override fun toString(): String {
        return "UnassignElderlyRequest(elderlyId=$elderlyId)"
    }
}

data class UnassignElderlyResponse(
    val success: Boolean,
    val message: String
) {
    override fun toString(): String {
        return "UnassignElderlyResponse(success=$success, message='$message')"
    }
}

data class ErrorResponse(
    val message: String? = null,
    val success: Boolean = false
)
