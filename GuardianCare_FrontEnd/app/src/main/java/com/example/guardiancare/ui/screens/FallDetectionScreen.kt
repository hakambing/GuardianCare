package com.example.guardiancare.ui.screens

/**
 * Fall Detection Screen
 * ====================
 * This screen provides the UI for detecting falls through the M5StickC device.
 * It manages BLE scanning and connection to the M5StickC device, displays connection
 * status, and handles notifications when falls are detected.
 * 
 * Key functionalities:
 * - Automatically scans for M5StickC device on screen load
 * - Displays current connection status
 * - Shows fall detection alerts
 * - Provides button to restart Bluetooth scanning
 * - Manages BLE connections through the BleManager
 */

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.guardiancare.R
import com.example.guardiancare.utils.BleManager
import com.example.guardiancare.utils.NotificationHelper

/**
 * Fall Detection Screen UI
 * 
 * This screen manages the connection to the M5StickC device for fall detection.
 * It displays connection status and provides controls for Bluetooth scanning.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FallDetectionScreen() {
    val context = LocalContext.current
    val bleManager = remember { BleManager.getInstance(context) }
    val notificationHelper = remember { NotificationHelper.getInstance(context) }

    // Store string resources that will be used in non-composable contexts
    val connectedMessage = context.getString(R.string.connected_to_m5stickc)
    val scanningMessage = context.getString(R.string.scanning_for_bluetooth_device)
    val disconnectedMessage = context.getString(R.string.disconnected_from_m5stickc)
    val checkingMessage = context.getString(R.string.checking_bluetooth_connection)
    
    var statusMessage by remember { mutableStateOf(checkingMessage) }

    /**
     * Effect to handle BLE connection management
     * 
     * This runs when the screen is first displayed:
     * 1. Checks if already connected to a device
     * 2. If not connected, starts scanning for devices
     * 3. Sets up callbacks for connection state changes and data reception
     */
    LaunchedEffect(Unit) {
        // Check current connection state
        if (bleManager.isConnected()) {
            statusMessage = connectedMessage
        } else {
            statusMessage = scanningMessage
            bleManager.startScanning(context as Activity) // Start scanning if not connected
        }

        // Listen for connection changes
        bleManager.setConnectionCallback { isConnected ->
            statusMessage = if (isConnected) {
                connectedMessage
            } else {
                disconnectedMessage
            }
        }

        // Listen for fall detection messages
        bleManager.setDataCallback { message ->
            statusMessage = message
            if (message.contains("Fall detected", ignoreCase = true)) {
                notificationHelper.showFallDetectionNotification()
            }
        }
    }

    // UI Layout
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display current connection status
        Text(text = statusMessage)

        Spacer(modifier = Modifier.height(16.dp))

        // Button to restart BLE scanning
        Button(onClick = {
            bleManager.startScanning(context as Activity)
        }) {
            Text(stringResource(R.string.restart_bluetooth_scan))
        }
    }
}