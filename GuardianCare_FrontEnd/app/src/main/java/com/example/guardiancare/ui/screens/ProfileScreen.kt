package com.example.guardiancare.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.guardiancare.R
import com.example.guardiancare.ui.viewmodel.ProfileViewModel
import com.example.guardiancare.ui.viewmodel.ProfileViewModelFactory

/**
 * Main profile screen that displays user profile information
 */
@Composable
fun ProfileScreen() {
    // Setup ViewModel with proper factory
    val context = LocalContext.current
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(context))
    
    // Collect state from ViewModel
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val updateSuccess by viewModel.updateSuccess.collectAsState()
    
    val scrollState = rememberScrollState()
    
    // Track if edit dialog should be shown
    var showEditDialog by remember { mutableStateOf(false) }
    
    // Show success message when update succeeds
    LaunchedEffect(updateSuccess) {
        if (updateSuccess) {
            Toast.makeText(context, context.getString(R.string.profile_updated_successfully), Toast.LENGTH_SHORT).show()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Profile Image
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = stringResource(R.string.profile_picture),
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // User Name
                    Text(
                        text = userProfile.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    // User Type
                    Text(
                        text = capitalizeUserType(userProfile.userType),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Profile Information
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.personal_information),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    ProfileInfoItem(
                        icon = Icons.Default.Email,
                        title = stringResource(R.string.email),
                        value = userProfile.email
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    if (userProfile.dob.isNotEmpty()) {
                        ProfileInfoItem(
                            icon = Icons.Default.DateRange,
                            title = stringResource(R.string.date_of_birth),
                            value = userProfile.dob
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                    
                    ProfileInfoItem(
                        icon = Icons.Default.Person,
                        title = stringResource(R.string.user_id),
                        value = userProfile.id
                    )
                    
                    if (userProfile.caretakerId != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        ProfileInfoItem(
                            icon = Icons.Default.Group,
                            title = stringResource(R.string.caretaker_id),
                            value = userProfile.caretakerId!!
                        )
                    }
                }
            }
            
            // Medical History (for Elderly users)
            if (userProfile.userType == "elderly" && userProfile.medicalHistory.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.medical_history),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        userProfile.medicalHistory.forEach { condition ->
                            ProfileInfoItem(
                                icon = Icons.Default.MedicalServices,
                                title = stringResource(R.string.condition),
                                value = condition
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
            
            // Account Information
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.account_information),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    ProfileInfoItem(
                        icon = Icons.Default.CalendarToday,
                        title = stringResource(R.string.created_at),
                        value = userProfile.createdAt
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    ProfileInfoItem(
                        icon = Icons.Default.Update,
                        title = stringResource(R.string.last_updated),
                        value = userProfile.updatedAt
                    )
                }
            }
            
            // Edit Profile Button
            Button(
                onClick = { showEditDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = stringResource(R.string.edit_profile),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Spacer
            Spacer(modifier = Modifier.height(16.dp))
            
            // Test Notification Button
            Button(
                onClick = { viewModel.sendTestNotification() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = stringResource(R.string.notifications),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = stringResource(R.string.test_fcm_notification),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Help text for test notification
            Text(
                text = stringResource(R.string.fcm_notification_help),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
        }
        
        // Edit Profile Dialog
        if (showEditDialog) {
            EditProfileDialog(
                currentName = userProfile.name,
                currentDob = userProfile.dob,
                onDismiss = { showEditDialog = false },
                onSave = { name, dob, medicalHistory ->
                    viewModel.updateProfile(name, dob, medicalHistory)
                    showEditDialog = false
                }
            )
        }
        
        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        // Error message
        error?.let { errorMessage ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = stringResource(R.string.error),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(bottom = 8.dp)
                        )
                        
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Button(
                            onClick = { viewModel.loadUserProfile() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.try_again))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    currentName: String,
    currentDob: String,
    onDismiss: () -> Unit,
    onSave: (name: String, dob: String, medicalHistory: List<String>?) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var dob by remember { mutableStateOf(currentDob) }
    var medicalCondition by remember { mutableStateOf("") }
    val medicalHistory = remember { mutableStateListOf<String>() }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_profile)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    singleLine = true
                )
                
                // Date of Birth Field
                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text(stringResource(R.string.date_of_birth_format)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    singleLine = true
                )
                
                // Medical History Section (for Elderly users)
                Text(
                    text = stringResource(R.string.medical_conditions),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                
                // Add new condition
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = medicalCondition,
                        onValueChange = { medicalCondition = it },
                        label = { Text(stringResource(R.string.add_condition)) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        singleLine = true
                    )
                    
                    IconButton(
                        onClick = {
                            if (medicalCondition.isNotEmpty()) {
                                medicalHistory.add(medicalCondition)
                                medicalCondition = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_condition)
                        )
                    }
                }
                
                // List of added conditions
                medicalHistory.forEachIndexed { index, condition ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = condition,
                            modifier = Modifier.weight(1f)
                        )
                        
                        IconButton(
                            onClick = { medicalHistory.removeAt(index) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.remove)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, dob, medicalHistory.toList()) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun capitalizeUserType(userType: String): String {
    return when (userType.lowercase()) {
        "elderly" -> "Elderly"
        "caretaker" -> "Caretaker"
        else -> userType.replaceFirstChar { it.uppercase() }
    }
}