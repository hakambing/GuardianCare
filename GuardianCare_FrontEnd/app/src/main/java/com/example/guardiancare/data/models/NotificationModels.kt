package com.example.guardiancare.data.models

import com.google.gson.annotations.SerializedName

data class DeviceRegistrationRequest(
    @SerializedName("userId")
    val userId: String,
    
    @SerializedName("deviceToken")
    val deviceToken: String,
    
    @SerializedName("deviceType")
    val deviceType: String
)

data class DeviceRegistrationResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: DeviceData? = null
)

data class DeviceData(
    @SerializedName("userId")
    val userId: String,
    
    @SerializedName("deviceToken")
    val deviceToken: String,
    
    @SerializedName("deviceType")
    val deviceType: String,
    
    @SerializedName("createdAt")
    val createdAt: String
)

// Fall Notification Models
data class NotificationResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("data")
    val data: List<NotificationData>
)

data class NotificationData(
    @SerializedName("_id")
    val id: String?,
    
    @SerializedName("type")
    val type: String,
    
    @SerializedName("elderlyId")
    val elderlyId: String,
    
    @SerializedName("priority")
    val priority: String,
    
    @SerializedName("timestamp")
    val timestamp: String?,
    
    @SerializedName("content")
    val content: NotificationContent?,
    
    @SerializedName("recipients")
    val recipients: List<NotificationRecipient>?
)

data class NotificationContent(
    @SerializedName("title")
    val title: String?,
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("data")
    val data: Map<String, Any>?,
    
    @SerializedName("elderlyName")
    val elderlyName: String?,
    
    @SerializedName("location")
    val location: String?,
    
    @SerializedName("contactNumber")
    val contactNumber: String?
)

data class NotificationRecipient(
    @SerializedName("userId")
    val userId: String,
    
    @SerializedName("role")
    val role: String,
    
    @SerializedName("deviceTokens")
    val deviceTokens: List<String>?,
    
    @SerializedName("notificationSent")
    val notificationSent: Boolean?,
    
    @SerializedName("readTimestamp")
    val readTimestamp: String?
)

enum class NotificationType(val value: String) {
    FALL_DETECTION("FALL_DETECTION"),
    DEVICE_STATUS("DEVICE_STATUS")
}
