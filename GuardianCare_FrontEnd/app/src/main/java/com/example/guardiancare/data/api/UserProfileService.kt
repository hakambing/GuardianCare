package com.example.guardiancare.data.api

import com.example.guardiancare.data.models.ProfileResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface UserProfileService {
    @GET("/api/users/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Response<ProfileResponse>
    
    @GET("/api/users/{userId}")
    suspend fun getUserById(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<ProfileResponse>
}