package com.example.guardiancare.ui.screens

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
//import com.example.guardiancare.design_system.charts.CheckInDetailCard
import com.example.guardiancare.design_system.charts.MoodScoreGraph
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.guardiancare.R
import com.example.guardiancare.data.models.CheckInResponse
import com.example.guardiancare.data.models.ElderlyResponse
import com.example.guardiancare.ui.viewmodel.ElderlyProfileViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.random.Random

private val TAG = "ElderlyProfileScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElderlyProfileScreen(
    navController: NavController,
    elderlyId: String,
    viewModel: ElderlyProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val elderlyData by viewModel.elderlyData.observeAsState()
    val checkInsData by viewModel.checkInsData.observeAsState(emptyList())
    val latestCheckIn by viewModel.latestCheckIn.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState(true)
    val error by viewModel.error.observeAsState()
    
    // Load data when screen is first displayed
    LaunchedEffect(key1 = elderlyId) {
        viewModel.loadElderlyData(elderlyId)
        viewModel.loadCheckIns(elderlyId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Elderly Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Service Unavailable",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = error ?: "Unknown error",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    // If we still have elderly data, show it anyway
                    if (elderlyData != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            ProfileInfoCard(elderlyData!!)
                        }
                    }
                }
                elderlyData != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        ProfileInfoCard(elderlyData!!)

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Singapore location map
                        SingaporeLocationMap()
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        NoCheckInTodayCard(checkInsData)
                        
                        if (latestCheckIn != null) {
                            LatestCheckInCard(latestCheckIn!!)
                            Spacer(modifier = Modifier.height(16.dp))
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No Check-Ins Available",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Text(
                                        text = "This elderly person has not completed any check-ins yet.",
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Check-In History",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Mood score graph and check-in details
                            if (checkInsData.size > 1) {
                                var selectedCheckIn by remember {
                                    mutableStateOf<CheckInResponse?>(
                                        null
                                    )
                                }

                                MoodScoreGraph(
                                    checkIns = checkInsData,
                                    modifier = Modifier.fillMaxWidth(),
                                    onDataPointSelected = { selectedCheckIn = it }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Show check-in details if a data point is selected
                                if (selectedCheckIn != null) {
//                                CheckInDetailCard(
//                                    checkIn = selectedCheckIn,
//                                    onDismiss = { selectedCheckIn = null }
//                                )
                                }
                            } else {
                                Text(
                                    text = "The user hasn't checked in yet.",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                else -> {
                    Text(
                        text = "No data available",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SingaporeLocationMap(modifier: Modifier = Modifier) {
    // Singapore bounds (approximate)
    val minLat = 1.315
    val maxLat = 1.412
    val minLng = 103.71
    val maxLng = 103.93
    
    // Generate random coordinates within Singapore
    val randomLat = minLat + (maxLat - minLat) * Random.nextDouble()
    val randomLng = minLng + (maxLng - minLng) * Random.nextDouble()
    val singaporeLocation = LatLng(randomLat, randomLng)
    
    // Set up camera position
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(singaporeLocation, 12f)
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "Current Location",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Coordinates display
            Text(
                text = "Lat: ${String.format("%.4f", randomLat)}° N, Lng: ${String.format("%.4f", randomLng)}° E",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Google Map
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f/9f),
                cameraPositionState = cameraPositionState
            ) {
                Marker(
                    state = MarkerState(position = singaporeLocation),
                    title = "Current Location"
                )
            }
        }
    }
}

@Composable
fun ProfileInfoCard(elderly: ElderlyResponse) {
    Column(
        modifier = Modifier
    ) {
        Row {
            Text(
                text = elderly.name,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Contact info section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text("Email", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        elderly.email,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    elderly.dob?.let {
                                        val formattedDate = try {
                                            val inputFormat = SimpleDateFormat(
                                                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                                                Locale.getDefault()
                                            )
                                            val outputFormat =
                                                SimpleDateFormat(
                                                    "MMMM d, yyyy",
                                                    Locale.getDefault()
                                                )
                                            val date = inputFormat.parse(it)
                                            date?.let {
                                                outputFormat.format(date)
                                            } ?: "Unknown"
                                        } catch (e: Exception) {
                                            "Unknown"
                                        }

                                        Text(
                                            "Date of Birth",
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = formattedDate,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Emergency Contact Section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Emergency Contact",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    elderly.emergency_contact ?: "No emergency contact recorded",
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Medical History Section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Medical History",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (elderly.medical_history.isNullOrEmpty()) {
                                    Text("No medical history recorded", fontSize = 14.sp)
                                } else {
                                    Column {
                                        elderly.medical_history.forEach { condition ->
                                            Row(
                                                modifier = Modifier.padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(condition, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoCheckInTodayCard(checkIns: List<CheckInResponse>) {
    if (checkIns.isNotEmpty()) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        var checkInToday = false

        run outer@{
            checkIns.forEach loop@{ checkIn ->
                if (checkIn.created_at == null) return@loop

                try {
                    val date = sdf.parse(checkIn.created_at) ?: return@loop
                    val now = Date()

                    val diffMillis = now.time - date.time
                    val diffHours = TimeUnit.MILLISECONDS.toHours(diffMillis)

                    if (diffHours < 24) {
                        checkInToday = true
                        return@outer
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing date: $checkIn.created_at", e)
                    return@loop
                }
            }
        }

        if (!checkInToday) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error)
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Error,
                            contentDescription = "No Check-In Today!",
                            tint = Color.White
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "No Check-In Today!",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun LatestCheckInCard(checkIn: CheckInResponse) {
    // Get priority-based colors
    val priorityColor = when (checkIn.priority) {
        4 -> Color(0xFFB71C1C)
        3 -> Color(0xFFF44336)
        2 -> Color(0xFFFF9800)
        1 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }
    
    val priorityText = when (checkIn.priority) {
        4 -> "Emergency"
        3 -> "High Priority"
        2 -> "Medium Priority"
        1 -> "Low Priority"
        else -> "Normal"
    }
    
    // Get mood-based emoji and text
    val (moodEmoji, moodText) = when (checkIn.mood) {
        3 -> "😄" to "Very Positive"
        2 -> "😐" to "Positive"
        1 -> "🙁" to "Slightly Positive"
        0 -> "😞" to "Neutral"
        -1 -> "😄" to "Slightly Negative"
        -2 -> "🙂" to "Negative"
        -3 -> "😐" to "Very Negative"
        else -> "❓" to "Unknown"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            // Priority indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(priorityColor)
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header with timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(priorityColor)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = "Latest Check-In",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Format and display the date
                    checkIn.created_at?.let {
                        val formattedDate = try {
                            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                            val outputFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                            val date = inputFormat.parse(it)
                            date?.let { outputFormat.format(date) } ?: "Unknown"
                        } catch (e: Exception) {
                            "Unknown"
                        }
                        
                        Text(
                            text = formattedDate,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Status and priority section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Priority card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = priorityColor.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Priority",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = priorityText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Mood card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                        ) {
                            Text(
                                text = "Mood",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = moodText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Summary section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Summary",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Text(
                            text = checkIn.summary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Transcript section
                checkIn.transcript?.let { transcript ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Transcript",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = transcript,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun InfoRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        
        Text(
            text = value,
            fontSize = 14.sp,
            color = valueColor
        )
    }
}
