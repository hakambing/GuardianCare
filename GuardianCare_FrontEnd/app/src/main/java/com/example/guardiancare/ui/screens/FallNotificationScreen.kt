package com.example.guardiancare.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.guardiancare.R
import com.example.guardiancare.data.session.SessionManager
import com.example.guardiancare.navigation.Routes
import com.example.guardiancare.ui.viewmodel.NotificationViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FallNotificationScreen(
    navController: NavController? = null,
    viewModel: NotificationViewModel = viewModel(
        factory = NotificationViewModelFactory(SessionManager.getInstance(LocalContext.current))
    )
) {
    // Collect state from ViewModel
    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.fall_notifications),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (notifications.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_fall_notifications),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.check_for_notifications),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // List of notifications
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notifications) { notification ->
                        NotificationCard(
                            notification = notification, 
                            viewModel = viewModel,
                            navController = navController
                        )
                    }
                }
            }
            
            // Loading indicator
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
            
            // Error message
            if (error != null) {
                Text(
                    text = error ?: "An error occurred",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )
            }
            
            // Refresh button
            FloatingActionButton(
                onClick = { viewModel.loadNotifications() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.refresh)
                )
            }
        }
    }
}

// ViewModel Factory for NotificationViewModel
class NotificationViewModelFactory(
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationViewModel(sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NotificationCard(
    notification: FallNotification,
    viewModel: NotificationViewModel,
    navController: NavController? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Navigate to elderly profile screen when clicked
                navController?.navigate("${Routes.Elderly}Profile/${notification.elderlyId}")
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Elderly name with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp)
                )
                
                Text(
                    text = notification.elderlyName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Date and time with icon
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp)
                )
                
                Text(
                    text = viewModel.formatDateTime(notification.timestamp),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Data class for the UI
data class FallNotification(
    val id: String,
    val elderlyId: String,  // Added elderlyId field
    val elderlyName: String,
    val timestamp: Long
)
