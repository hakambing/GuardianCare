package com.example.guardiancare.data.models

object UserType {
    const val ELDERLY = "elderly"
    const val CARETAKER = "caretaker"
}

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val token: String, val user: User)

data class RegisterRequest(
    val user_type: String,
    val name: String,
    val email: String,
    val password: String,
    val dob: String? = null,
    val address: String? = null,
    val medical_history: List<String>? = null,
    val point_of_contact: PointOfContact? = null
)

data class RegisterResponse(val message: String)

data class User(
    val id: String,
    val user_type: String,
    val name: String,
    val email: String,
    val dob: String?,
    val address: String?,
    val medical_history: List<String>?,
    val point_of_contact: PointOfContact?
)

data class PointOfContact(
    val name: String,
    val phone: String,
    val relationship: String
)

data class ProfileResponse(
    val _id: String,
    val user_type: String,
    val name: String,
    val email: String,
    val dob: String? = null,
    val medical_history: List<String>? = null,
    val caretaker_id: String? = null,
    val address: String? = null,
    val emergency_contact: String? = null,
    val created_at: String,
    val updated_at: String
)

data class UpdateProfileRequest(
    val name: String? = null,
    val dob: String? = null,
    val medical_history: List<String>? = null,
    val phone: String? = null,
    val address: String? = null
)

data class UpdateProfileResponse(
    val message: String,
    val user: User
)
