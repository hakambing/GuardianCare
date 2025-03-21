package com.example.guardiancare.data.api

import com.example.guardiancare.data.models.DeviceRegistrationRequest
import com.example.guardiancare.data.models.DeviceRegistrationResponse
import com.example.guardiancare.data.models.NotificationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface NotificationApiService {
    /**
     * Get notifications for a user
     */
    @GET("api/notifications")
    suspend fun getNotifications(
        @Header("Authorization") authHeader: String,
        @Query("userId") userId: String,
        @Query("limit") limit: Int = 20,
        @Query("skip") skip: Int = 0,
        @Query("unreadOnly") unreadOnly: Boolean = false
    ): Response<NotificationResponse>
    @POST("api/devices/register")
    suspend fun registerDevice(
        @Header("Authorization") authHeader: String,
        @Body request: DeviceRegistrationRequest
    ): Response<DeviceRegistrationResponse>
    
    @POST("api/devices/unregister")
    suspend fun unregisterDevice(
        @Header("Authorization") authHeader: String,
        @Body request: DeviceRegistrationRequest
    ): Response<DeviceRegistrationResponse>
    
    /**
     * Send a test notification to a specific device
     */
    @POST("api/test/send-notification")
    suspend fun sendTestNotification(
        @Header("Authorization") authHeader: String,
        @Body request: DeviceRegistrationRequest
    ): Response<DeviceRegistrationResponse>
    
    /**
     * Simulate a fall detection event
     */
    @POST("api/test/fall-detection")
    suspend fun testFallDetection(
        @Header("Authorization") authHeader: String,
        @Body request: Map<String, Any>
    ): Response<Map<String, Any>>
}
