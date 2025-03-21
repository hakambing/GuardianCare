package com.example.guardiancare.utils

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.guardiancare.MainActivity
import com.example.guardiancare.R

@RequiresApi(Build.VERSION_CODES.O)
class NotificationHelper private constructor(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val fallChannelId = "fall_detection_channel"
    private val generalChannelId = "general_notification_channel"
    private var notificationId = 1

    companion object {
        private const val REQUEST_ENABLE_NOTIFICATIONS = 1

        @Volatile
        private var instance: NotificationHelper? = null

        fun getInstance(context: Context): NotificationHelper {
            return instance ?: synchronized(this) {
                instance ?: NotificationHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        createNotificationChannels()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannels() {
        // Fall detection channel
        val fallName = "Fall Detection"
        val fallDescriptionText = "Notifications for fall detection alerts"
        val fallImportance = NotificationManager.IMPORTANCE_HIGH
        val fallChannel = NotificationChannel(fallChannelId, fallName, fallImportance).apply {
            description = fallDescriptionText
            enableVibration(true)
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(fallChannel)
        
        // General notifications channel
        val generalName = "General Notifications"
        val generalDescriptionText = "General notifications from GuardianCare"
        val generalImportance = NotificationManager.IMPORTANCE_DEFAULT
        val generalChannel = NotificationChannel(generalChannelId, generalName, generalImportance).apply {
            description = generalDescriptionText
            enableVibration(true)
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(generalChannel)
    }

    fun checkNotificationPermissions(activity: Activity): Boolean {
        val permissions = arrayOf(
            Manifest.permission.POST_NOTIFICATIONS
        )

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, permissions, REQUEST_ENABLE_NOTIFICATIONS)
            return false
        } else {
            return true
        }
    }

    fun showFallDetectionNotification(userId: String = "") {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", "fall_detection")
            putExtra("user_id", userId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = if (userId.isNotEmpty()) {
            "Fall detected for user $userId!"
        } else {
            "Fall detected!"
        }

        val notification = NotificationCompat.Builder(context, fallChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Fall Detection Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(getNextNotificationId(), notification)
    }
    
    fun showGenericNotification(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", "generic")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, generalChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(getNextNotificationId(), notification)
    }
    
    private fun getNextNotificationId(): Int {
        return notificationId++
    }
}
