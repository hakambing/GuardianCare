package com.example.guardiancare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import androidx.navigation.NavController
import com.example.guardiancare.R
import com.example.guardiancare.ui.viewmodel.ElderlyViewModel

/**
 * Data class for patient/elderly
 */
data class Patient(
    val id: String,
    val name: String,
    val status: String = "",
    val lastCheckIn: String = "",
    val hasAlert: Boolean = false,
    // Check-in data
    val priority: Int = 0,
    val mood: Int = 0,
    val summary: String = "",
    val transcript: String? = "",
    val hasCheckedInToday: Boolean = true
)

/**
 * This screen is used by Caretaker/Member users to manage and view their elderly patients
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElderlyScreen(
    navController: NavController? = null,
    viewModel: ElderlyViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val patientsData by viewModel.patientsData.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val error by viewModel.error.observeAsState(null)

    // Load data when screen is first displayed
    LaunchedEffect(key1 = true) {
        viewModel.loadPatientsData()
    }

    // Filter patients based on search query
    val filteredPatients = patientsData.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Screen header
        Text(
            text = stringResource(R.string.elderly_management),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = { Text(stringResource(R.string.search_patients)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        // Add patient button and dialog state
        var showAddPatientDialog by remember { mutableStateOf(false) }
        var emailInput by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        Button(
            onClick = { showAddPatientDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = stringResource(R.string.add_new_patient),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }


        // Patients list
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                when {
                    isLoading -> {
                        // Loading state
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    error != null -> {
                        // Error state
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = error ?: "Unknown error occurred",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    filteredPatients.isEmpty() -> {
                        // Empty state
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_patients_found),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        // List of patients
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredPatients) { patient ->
                                PatientListItem(
                                    patient = patient,
                                    viewModel = viewModel,
                                    onItemClick = {
                                        // Navigate to elderly profile when clicked
                                        navController?.navigate("elderlyProfile/${patient.id}")
                                    }
                                )
                            }
                        }
                    }
                }
            }



        // Add Patient Dialog
        if (showAddPatientDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAddPatientDialog = false
                    emailInput = ""
                    errorMessage = null
                },
                title = { Text("Add New Elderly") },
                text = {
                    Column {
                        Text(
                            "Enter the email address of the elderly person you want to add.",
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = {
                                emailInput = it
                                errorMessage = null
                            },
                            label = { Text("Email Address") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            ),
                            isError = errorMessage != null,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        if (isSubmitting) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (emailInput.isBlank()) {
                                errorMessage = "Please enter an email address"
                                return@Button
                            }

                            // Start submission process
                            isSubmitting = true
                            errorMessage = null

                            // Call ViewModel method to assign elderly
                            viewModel.assignElderlyByEmail(emailInput) { success, message ->
                                isSubmitting = false
                                if (success) {
                                    // Close dialog and refresh data
                                    showAddPatientDialog = false
                                    emailInput = ""
                                    viewModel.loadPatientsData()
                                } else {
                                    // Show error message
                                    errorMessage = message
                                }
                            }
                        },
                        enabled = !isSubmitting
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showAddPatientDialog = false
                            emailInput = ""
                            errorMessage = null
                        },
                        enabled = !isSubmitting
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun PatientListItem(
    patient: Patient,
    viewModel: ElderlyViewModel,
    onItemClick: () -> Unit = {}) {
    // Get priority-based colors
    val priorityColor = when (patient.priority) {
        4 -> Color(0xFFB71C1C)
        3 -> Color(0xFFF44336)
        2 -> Color(0xFFFF9800)
        1 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onItemClick() },
        shape = RoundedCornerShape(12.dp),
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
                    .background(priorityColor.copy(alpha = 0.1f))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Patient info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = patient.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        val priorityText = when (patient.priority) {
                            4 -> "EMERGENCY"
                            3 -> "High"
                            2 -> "Medium"
                            1 -> "Low"
                            else -> "Normal"
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    color = priorityColor,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 9.dp, vertical = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (patient.priority == 4) {
                                    Icon(
                                        imageVector = Icons.Rounded.Error,
                                        contentDescription = "No Check-In Today",
                                        tint = Color.White
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                Text(
                                    text = priorityText,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

//                    Row(
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Box(
//                            modifier = Modifier
//                                .background(
//                                    color = MaterialTheme.colorScheme.secondaryContainer,
//                                    shape = RoundedCornerShape(10.dp)
//                                )
//                                .padding(horizontal = 9.dp, vertical = 3.dp)
//                        ) {
//                            Text(
//                                text = patient.status,
//                                color = Color.White,
//                                fontSize = 10.sp,
//                                fontWeight = FontWeight.Bold
//                            )
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.height(8.dp))

                    Row {
                        Text(
                            text = patient.lastCheckIn,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        if (!patient.hasCheckedInToday) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "No Check-In Today",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    
                    // Summary section with improved visibility
                    if (patient.summary.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = "Summary",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = patient.summary,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3
                            )
                        }
                    }
                }
                
                // Action buttons
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
//                    IconButton(onClick = { /* Open chat */ }) {
//                        Icon(
//                            imageVector = Icons.AutoMirrored.Filled.Message,
//                            contentDescription = "Chat",
//                            tint = MaterialTheme.colorScheme.primary
//                        )
//                    }
//
//                    IconButton(onClick = { /* Show emergency options */ }) {
//                        Icon(
//                            imageVector = Icons.Default.Notifications,
//                            contentDescription = "Emergency",
//                            tint = if (patient.hasAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                    }
                    
                    // More options menu
                    var showMenu by remember { mutableStateOf(false) }
                    
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Unassign") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    viewModel.unassignElderly(patient.id) { success: Boolean, message: String ->
                                        if (success) {
                                            // Refresh the list
                                            viewModel.loadPatientsData()
                                        } else {
                                            // Show error message - in a production app, you'd use a snackbar or toast
                                            Log.e("ElderlyScreen", "Failed to unassign: $message")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
