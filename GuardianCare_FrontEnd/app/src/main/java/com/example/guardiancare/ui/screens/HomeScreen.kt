package com.example.guardiancare.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.outlined.Emergency
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.guardiancare.R
import com.example.guardiancare.data.session.SessionManager
import com.example.guardiancare.navigation.Routes
import com.example.guardiancare.ui.viewmodel.ElderlyViewModel
import com.example.guardiancare.ui.viewmodel.NotificationViewModel
import com.example.guardiancare.ui.viewmodel.ProfileViewModel
import com.example.guardiancare.ui.viewmodel.ProfileViewModelFactory
import com.example.guardiancare.utils.BleManager
import com.example.guardiancare.utils.FallTracker

// Helper function to check BLE permissions
fun checkBlePermissions(context: Context): Boolean {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    
    return permissions.all { permission ->
        ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun WifiConfigDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit,
    onRemoveDevice: () -> Unit
) {
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.configure_wifi_connection),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = { Text(stringResource(R.string.wifi_name_ssid)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.wifi_password)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                
                // Email field removed - will use the logged-in user's email
                Text(
                    stringResource(R.string.your_account_email_will_be_used),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = { onSubmit(ssid, password) },
                        enabled = ssid.isNotBlank() && password.isNotBlank()
                    ) {
                        Text(stringResource(R.string.connect))
                    }
                }
                
                // Add a divider and Remove Device button
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onRemoveDevice,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.remove_device),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.remove_device))
                }
            }
        }
    }
}

/**
 * Home screen specifically for caretaker users
 */
@Composable
fun CaretakerHomeScreen(
    navController: NavController? = null,
    elderlyViewModel: ElderlyViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel(
        factory = NotificationViewModelFactory(SessionManager.getInstance(LocalContext.current))
    )
) {
    // Initialize ProfileViewModel to access user profile data
    val context = LocalContext.current
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(context)
    )
    
    // Collect user profile state
    val userProfile by profileViewModel.userProfile.collectAsState()
    
    // Collect elderly data
    val patientsData by elderlyViewModel.patientsData.observeAsState(emptyList())
    val isLoadingElderly by elderlyViewModel.isLoading.observeAsState(false)
    val elderlyError by elderlyViewModel.error.observeAsState(null)

    // Collect notification data
    val notifications by notificationViewModel.notifications.collectAsState()
    val isLoadingNotifications by notificationViewModel.isLoading.collectAsState()
    val notificationError by notificationViewModel.error.collectAsState()
    
    // Load data when screen is first displayed
    LaunchedEffect(key1 = true) {
        elderlyViewModel.loadPatientsData()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationViewModel.loadNotifications()
        }
    }
    
    // Get the latest notification and high priority patient
    val latestPatient = patientsData.firstOrNull()
    val emergencyPatients = patientsData
        .filter { it.priority >= 2 || !it.hasCheckedInToday }
        .sortedByDescending { it.priority }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            // Personalized greeting with user's name if available
            Text(
                text = if (userProfile.name.isNotEmpty()) 
                       stringResource(R.string.hello_user, userProfile.name) 
                       else stringResource(R.string.caretaker_dashboard),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Current date and time
            val currentDateTime = remember { SimpleDateFormat("EEEE, MMMM d, yyyy • h:mm a", Locale.getDefault()).format(Date()) }
            Text(
                text = currentDateTime,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Dashboard Summary Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Elderly Count Card
                DashboardCard(
                    title = stringResource(R.string.elderly),
                    value = patientsData.size.toString(),
                    subtitle = stringResource(R.string.assigned),
                    icon = Icons.Default.Person,
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                
                // Alerts Summary Card
                DashboardCard(
                    title = stringResource(R.string.alerts),
                    value = emergencyPatients.size.toString(),
                    subtitle = stringResource(R.string.active),
                    icon = Icons.Default.Warning,
                    backgroundColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            // Latest Patient Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                    Text(
                        text = stringResource(R.string.latest_alert),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (isLoadingElderly) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else if (elderlyError != null) {
                        Text(
                            text = stringResource(R.string.error_loading_patients, elderlyError ?: ""),
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (patientsData.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_patients_assigned),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        latestPatient?.let { patient ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Priority color
                                val priorityColor = when (patient.priority) {
                                    4 -> Color(0xFFB71C1C)
                                    3 -> Color(0xFFF44336)
                                    2 -> Color(0xFFFF9800)
                                    1 -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.primary
                                }

                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(priorityColor.copy(alpha = 0.2f), CircleShape)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = priorityColor
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = patient.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = priorityColor,
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                                .padding(horizontal = 12.dp)
                                        ) {
                                            Text(
                                                text = patient.status,
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = patient.lastCheckIn,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {

                                        Text(
                                            text = patient.summary,
                                            fontSize = 14.sp,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        navController?.navigate("${Routes.Elderly}Profile/${patient.id}")
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = stringResource(R.string.view_profile)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Latest Fall Alert Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.needs_attention),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (notifications.isNotEmpty()) {
                            TextButton(
                                onClick = { 
                                    navController?.navigate(Routes.FallNotification) 
                                }
                            ) {
                                Text(stringResource(R.string.view_all))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (isLoadingElderly) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else if (elderlyError != null) {
                        Text(
                            text = stringResource(R.string.error_loading_alerts, elderlyError ?: ""),
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (emergencyPatients.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                            text = stringResource(R.string.no_alerts),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        emergencyPatients.forEach { patient ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Priority color
                                val priorityColor = when (patient.priority) {
                                    4 -> Color(0xFFB71C1C)
                                    3 -> Color(0xFFF44336)
                                    2 -> Color(0xFFFF9800)
                                    1 -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.primary
                                }

                                Box(
                                    modifier = Modifier
                                        .size(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!patient.hasCheckedInToday) {
                                        Icon(
                                            imageVector = Icons.Filled.Emergency,
                                            contentDescription = null,
                                            tint = Color(0xFFB71C1C)
                                        )
                                    } else {
                                        if (patient.priority == 2) {
                                            Icon(
                                                imageVector = Icons.Outlined.Warning,
                                                contentDescription = null,
                                                tint = priorityColor
                                            )
                                        } else if (patient.priority == 3) {
                                            Icon(
                                                imageVector = Icons.Outlined.Warning,
                                                contentDescription = null,
                                                tint = priorityColor
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.Emergency,
                                                contentDescription = null,
                                                tint = priorityColor
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = patient.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        if (patient.hasCheckedInToday) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = priorityColor,
                                                        shape = RoundedCornerShape(20.dp)
                                                    )
                                                    .padding(horizontal = 12.dp)
                                            ) {
                                                Text(
                                                    text = patient.status,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    if (!patient.hasCheckedInToday) {
                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .width(IntrinsicSize.Max)
                                                .horizontalScroll(rememberScrollState())
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = Color(0xFFB71C1C),
                                                        shape = RoundedCornerShape(20.dp)
                                                    )
                                                    .padding(horizontal = 12.dp)
                                            ) {
                                                Text(
                                                    text = "Last check-in: ${patient.lastCheckIn}",
                                                    color = MaterialTheme.colorScheme.onError,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = priorityColor,
                                                        shape = RoundedCornerShape(20.dp)
                                                    )
                                                    .padding(horizontal = 12.dp)
                                            ) {
                                                Text(
                                                    text = patient.status,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Last check-in: ${patient.lastCheckIn}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        navController?.navigate("${Routes.Elderly}Profile/${patient.id}")
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = stringResource(R.string.view_profile)
                                    )
                                }

                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
            
            // Manage Elderly Button
            Button(
                onClick = { navController?.navigate(Routes.Elderly) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = stringResource(R.string.manage_elderly),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        // Show loading indicator if both data sources are loading
        if (isLoadingElderly && isLoadingNotifications) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
        }
    }
}

/**
 * HomeScreen that conditionally displays different content based on user role
 */
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val userType = SessionManager.getInstance(context).getUserType() ?: "elderly"
    
    when (userType) {
        "caretaker" -> CaretakerHomeScreen(navController = navController)
        else -> ElderlyHomeScreen()
    }
}

/**
 * Home screen specifically for elderly users
 */
@Composable
fun ElderlyHomeScreen() {
    val context = LocalContext.current
    val sessionManager = SessionManager.getInstance(context)
    val bleManager = remember { BleManager.getInstance(context) }
    var isConnected by remember { mutableStateOf(bleManager.isConnected()) }
    var showWifiDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(bleManager.isScanning()) }
    var fallTracker by remember { mutableStateOf(FallTracker(0, false)) }
    
    // Get string resources that will be used in non-composable contexts
    val scanningStoppedText = context.getString(R.string.scanning_stopped)
    val wifiConfigSentText = context.getString(R.string.wifi_config_sent_successfully)
    val failedToSendWifiText = context.getString(R.string.failed_to_send_wifi_config)
    val couldNotRetrieveEmailText = context.getString(R.string.could_not_retrieve_email)
    val deviceRemovedText = context.getString(R.string.device_removed_successfully)
    val failedToRemoveDeviceText = context.getString(R.string.failed_to_remove_device)
    
    // Initialize ProfileViewModel to access user profile data
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(context)
    )
    
    // Collect user profile state
    val userProfile by profileViewModel.userProfile.collectAsState()
    
    // Track previous connection state to detect when connection is established
    var wasConnected by remember { mutableStateOf(isConnected) }
    
    // Set up connection state callback
    LaunchedEffect(Unit) {
        bleManager.setConnectionCallback { connected ->
            // Check if this is a new connection
            val newConnection = !wasConnected && connected
            
            // Update connection state
            isConnected = connected
            wasConnected = connected
            
            if (connected) {
                isScanning = false
                
                // If this is a new connection, show WiFi dialog automatically
                if (newConnection) {
                    showWifiDialog = true
                }
            }
        }
        
        // Set up data callback to receive device status updates
        bleManager.setDataCallback { data ->
            if (data.startsWith("FallTracker:")) {
                val parts = data.split(":")
                if (parts.size >= 3) {
                    val batteryLevel = parts[1].toIntOrNull() ?: 0
                    val wifiConnected = parts[2].toBoolean()
                    fallTracker = FallTracker(batteryLevel, wifiConnected)
                }
            }
        }
    }
    
    // Update scanning state periodically
    LaunchedEffect(Unit) {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(object : Runnable {
            override fun run() {
                val currentlyConnected = bleManager.isConnected()
                
                // Check if this is a new connection
                if (!isConnected && currentlyConnected) {
                    showWifiDialog = true
                }
                
                // Update states
                isConnected = currentlyConnected
                isScanning = bleManager.isScanning()
                
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this, 1000)
            }
        }, 1000)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Personalized greeting with user's name if available
            Text(
                text = if (userProfile.name.isNotEmpty()) 
                       stringResource(R.string.hello_user, userProfile.name) 
                       else stringResource(R.string.welcome_home),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Current date and time
            val currentDateTime = remember { SimpleDateFormat("EEEE, MMMM d, yyyy • h:mm a", Locale.getDefault()).format(Date()) }
            Text(
                text = currentDateTime,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Bluetooth Connection Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!isConnected && !isScanning) {
                                if (checkBlePermissions(context)) {
                                    try {
                                        bleManager.startScanning(context as Activity)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    showPermissionDialog = true
                                }
                            } else if (isConnected) {
                                showWifiDialog = true
                            } else if (isScanning) {
                                // If already scanning, stop the scan
                                bleManager.stopScanning()
                                Toast.makeText(context, scanningStoppedText, Toast.LENGTH_SHORT).show()
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                when {
                                    isConnected -> MaterialTheme.colorScheme.primaryContainer
                                    isScanning -> MaterialTheme.colorScheme.tertiaryContainer
                                    else -> MaterialTheme.colorScheme.errorContainer
                                }
                            )
                            .padding(16.dp)
                    ) {
                        // Main connection info row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bluetooth,
                                    contentDescription = stringResource(R.string.bluetooth),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column {
                                    Text(
                                        text = when {
                                            isConnected -> stringResource(R.string.connected_to_guardiancare)
                                            isScanning -> stringResource(R.string.scanning_for_guardiancare)
                                            else -> stringResource(R.string.connect_to_guardiancare)
                                        },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = when {
                                            isConnected -> stringResource(R.string.tap_to_configure_wifi)
                                            isScanning -> stringResource(R.string.tap_to_stop_scanning)
                                            else -> stringResource(R.string.tap_to_start_scanning)
                                        },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Icon(
                                imageVector = when {
                                    isConnected -> Icons.Filled.Wifi
                                    isScanning -> Icons.Filled.Warning
                                    else -> Icons.Filled.Bluetooth
                                },
                                contentDescription = when {
                                    isConnected -> stringResource(R.string.configure)
                                    isScanning -> stringResource(R.string.scanning)
                                    else -> stringResource(R.string.connect)
                                },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
//                        // Device status info (only shown when connected)
//                        if (isConnected) {
//                            Spacer(modifier = Modifier.height(16.dp))
//                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
//                            Spacer(modifier = Modifier.height(16.dp))
//
//                            // Battery status
//                            Row(
//                                modifier = Modifier.fillMaxWidth(),
//                                verticalAlignment = Alignment.CenterVertically
//                            ) {
//                                val batteryIcon = when {
//                                    fallTracker.batteryLevel > 75 -> Icons.Filled.BatteryFull
//                                    fallTracker.batteryLevel > 50 -> Icons.Filled.Battery6Bar
//                                    fallTracker.batteryLevel > 25 -> Icons.Filled.Battery2Bar
//                                    else -> Icons.Filled.Battery0Bar
//                                }
//
//                                val batteryColor = MaterialTheme.colorScheme.onSurfaceVariant
//
//                                Icon(
//                                    imageVector = batteryIcon,
//                                    contentDescription = "Battery Level",
//                                    tint = batteryColor,
//                                    modifier = Modifier.size(24.dp)
//                                )
//
//                                Spacer(modifier = Modifier.width(4.dp))
//
//                                Text(
//                                    text = "${fallTracker.batteryLevel}%",
//                                    fontSize = 14.sp,
//                                    fontWeight = FontWeight.Medium,
//                                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                                )
//
//                                Spacer(modifier = Modifier.width(20.dp))
//
//                                Icon(
//                                    imageVector = if (fallTracker.wifiConnected)
//                                        Icons.Filled.Wifi else Icons.Filled.WifiOff,
//                                    contentDescription = "WiFi Status",
//                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
//                                    modifier = Modifier.size(24.dp)
//                                )
//
//                                Spacer(modifier = Modifier.width(4.dp))
//
//                                Text(
//                                    text = if (fallTracker.wifiConnected)
//                                        "Connected" else "Disconnected",
//                                    fontSize = 14.sp,
//                                    fontWeight = FontWeight.Medium,
//                                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                                )
//                            }
//                        }
                    }
                }
            }
            
            // Connection status text
            if (isConnected) {
//                Text(
//                    text = stringResource(R.string.device_connected_successfully),
//                    fontSize = 14.sp,
//                    color = Color.Green.copy(alpha = 0.8f),
//                    fontWeight = FontWeight.Bold,
//                    modifier = Modifier.padding(top = 8.dp)
//                )
            } else {
                // Debug info text
                Text(
                    text = stringResource(R.string.ensure_device_powered_on),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
    
    // WiFi Configuration Dialog
    if (showWifiDialog) {
        // Load user profile if not already loaded
        LaunchedEffect(showWifiDialog) {
            if (userProfile.email.isEmpty()) {
                profileViewModel.loadUserProfile()
            }
        }
        
        WifiConfigDialog(
            onDismiss = { showWifiDialog = false },
            onSubmit = { ssid, password ->
                // Use email from user profile
                val email = userProfile.email
                if (email.isNotEmpty()) {
                    // Get JWT token from SessionManager
                    val jwtToken = sessionManager.getToken() ?: ""
                    // Get username from profile
                    val username = userProfile.name
                    
                    // Include JWT token and username in the message
                    val message = "WIFI:$ssid,$password,$email,$jwtToken,$username"
                    val success = bleManager.sendMessage(message)
                    if (success) {
                        Toast.makeText(context, wifiConfigSentText, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, failedToSendWifiText, Toast.LENGTH_SHORT).show()
                    }
                    showWifiDialog = false
                } else {
                    Toast.makeText(context, couldNotRetrieveEmailText, Toast.LENGTH_SHORT).show()
                    // Try to reload profile
                    profileViewModel.loadUserProfile()
                }
            },
            onRemoveDevice = {
                val success = bleManager.removeDevice()
                if (success) {
                    Toast.makeText(context, deviceRemovedText, Toast.LENGTH_SHORT).show()
                    isConnected = false
                } else {
                    Toast.makeText(context, failedToRemoveDeviceText, Toast.LENGTH_SHORT).show()
                }
                showWifiDialog = false
            }
        )
    }
    
    // Permission Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(stringResource(R.string.bluetooth_permissions_required)) },
            text = { 
                Text(stringResource(R.string.bluetooth_permissions_explanation))
            },
            confirmButton = {
                Button(onClick = {
                    // Open app settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", context.packageName, null)
                    intent.data = uri
                    context.startActivity(intent)
                    showPermissionDialog = false
                }) {
                    Text(stringResource(R.string.open_settings))
                }
            },
            dismissButton = {
                Button(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
