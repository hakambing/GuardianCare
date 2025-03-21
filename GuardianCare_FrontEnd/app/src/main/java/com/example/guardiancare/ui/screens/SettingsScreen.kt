package com.example.guardiancare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.guardiancare.R
import com.example.guardiancare.ui.viewmodel.SettingsViewModel
import com.example.guardiancare.ui.viewmodel.SettingsViewModelFactory

/**
 * Main settings screen that routes to the appropriate settings screen based on user role
 */
@Composable
fun SettingsScreen(navController: NavController? = null) {
    // Setup ViewModel with proper factory
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))
    
    // Collect state from ViewModel
    val settingsState by viewModel.settingsState.collectAsState()
    
    // Route to appropriate screen based on user role
    when (settingsState.userType) {
        "caretaker" -> CaretakerSettingsScreen(viewModel, navController)
        else -> ElderlySettingsScreen(viewModel, navController)
    }
}

/**
 * Settings screen specifically for elderly users
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElderlySettingsScreen(viewModel: SettingsViewModel, navController: NavController? = null) {
    val settingsState by viewModel.settingsState.collectAsState()
    val scrollState = rememberScrollState()
    
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showServerUrlDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.settings),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // App Settings
        SettingsCategoryHeader(title = stringResource(id = R.string.app_settings))
        
        SwitchSettingsItem(
            icon = Icons.Default.Notifications,
            title = stringResource(id = R.string.notifications),
            isChecked = settingsState.notificationsEnabled,
            onCheckedChange = { viewModel.toggleNotifications() }
        )
        
        SwitchSettingsItem(
            icon = Icons.Default.DarkMode,
            title = stringResource(id = R.string.dark_mode),
            isChecked = settingsState.darkModeEnabled,
            onCheckedChange = { viewModel.toggleDarkMode() }
        )
        
        // Language selection
        SettingsItem(
            icon = Icons.Default.Language,
            title = stringResource(id = R.string.language),
            subtitle = settingsState.language,
            onClick = { showLanguageDialog = true }
        )
        
        SwitchSettingsItem(
            icon = Icons.Default.LocationOn,
            title = stringResource(id = R.string.location_sharing),
            subtitle = stringResource(id = R.string.allow_location_sharing),
            isChecked = settingsState.locationSharingEnabled,
            onCheckedChange = { viewModel.toggleLocationSharing() }
        )
        
        SwitchSettingsItem(
            icon = Icons.Default.Warning,
            title = stringResource(id = R.string.fall_detection),
            subtitle = stringResource(id = R.string.fall_detection_description),
            isChecked = settingsState.fallDetectionEnabled,
            onCheckedChange = { viewModel.toggleFallDetection() }
        )
        
        // Server URL setting (for development)
        SettingsItem(
            icon = Icons.Default.Cloud,
            title = "Server URL",
            subtitle = settingsState.serverUrl,
            onClick = { showServerUrlDialog = true }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Logout
        LogoutButton(onClick = { showLogoutDialog = true })
    }
    
    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(id = R.string.logout)) },
            text = { Text(stringResource(id = R.string.logout_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logout()
                        navController?.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                        showLogoutDialog = false
                    }
                ) {
                    Text(stringResource(id = R.string.logout))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
    
    // Language selection dialog
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = settingsState.language,
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { selectedLanguage ->
                viewModel.updateLanguage(selectedLanguage)
            }
        )
    }
    
    // Server URL dialog
    if (showServerUrlDialog) {
        ServerUrlDialog(
            currentUrl = settingsState.serverUrl,
            onDismiss = { showServerUrlDialog = false },
            onUrlUpdated = { newUrl ->
                viewModel.updateServerUrl(newUrl)
                showServerUrlDialog = false
            }
        )
    }
}

/**
 * Settings screen specifically for caretaker users
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaretakerSettingsScreen(viewModel: SettingsViewModel, navController: NavController? = null) {
    val settingsState by viewModel.settingsState.collectAsState()
    val scrollState = rememberScrollState()
    
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showServerUrlDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.settings),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // App Settings
        SettingsCategoryHeader(title = stringResource(id = R.string.app_settings))
        
        SwitchSettingsItem(
            icon = Icons.Default.Notifications,
            title = stringResource(id = R.string.notifications),
            subtitle = stringResource(id = R.string.receive_alerts),
            isChecked = settingsState.notificationsEnabled,
            onCheckedChange = { viewModel.toggleNotifications() }
        )
        
        SwitchSettingsItem(
            icon = Icons.Default.DarkMode,
            title = stringResource(id = R.string.dark_mode),
            isChecked = settingsState.darkModeEnabled,
            onCheckedChange = { viewModel.toggleDarkMode() }
        )
        
        // Language selection
        SettingsItem(
            icon = Icons.Default.Language,
            title = stringResource(id = R.string.language),
            subtitle = settingsState.language,
            onClick = { showLanguageDialog = true }
        )
        
        SettingsItem(
            icon = Icons.Default.Schedule,
            title = stringResource(id = R.string.alert_settings),
            subtitle = stringResource(id = R.string.configure_alerts),
            onClick = { /* Navigate to alert settings */ }
        )
        
        // Server URL setting (for development)
        SettingsItem(
            icon = Icons.Default.Cloud,
            title = "Server URL",
            subtitle = settingsState.serverUrl,
            onClick = { showServerUrlDialog = true }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Logout
        LogoutButton(onClick = { showLogoutDialog = true })
    }
    
    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(id = R.string.logout)) },
            text = { Text(stringResource(id = R.string.logout_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logout()
                        navController?.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                        showLogoutDialog = false
                    }
                ) {
                    Text(stringResource(id = R.string.logout))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
    
    // Language selection dialog
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = settingsState.language,
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { selectedLanguage ->
                viewModel.updateLanguage(selectedLanguage)
            }
        )
    }
    
    // Server URL dialog
    if (showServerUrlDialog) {
        ServerUrlDialog(
            currentUrl = settingsState.serverUrl,
            onDismiss = { showServerUrlDialog = false },
            onUrlUpdated = { newUrl ->
                viewModel.updateServerUrl(newUrl)
                showServerUrlDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerUrlDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onUrlUpdated: (String) -> Unit
) {
    var serverUrl by remember { mutableStateOf(currentUrl) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Server URL") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    "Enter the ngrok URL for your backend server:",
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("Server URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text(
                    "Example: https://a1b2c3d4e5f6.ngrok.io",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Text(
                    "Note: Make sure to include the protocol (http:// or https://) and trailing slash",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    // Ensure URL ends with a slash
                    var url = serverUrl
                    if (!url.endsWith("/")) {
                        url += "/"
                    }
                    onUrlUpdated(url)
                },
                enabled = serverUrl.isNotBlank() && 
                          (serverUrl.startsWith("http://") || serverUrl.startsWith("https://"))
            ) {
                Text(stringResource(id = R.string.apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
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
                .padding(start = 16.dp, end = 8.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }

    HorizontalDivider(
        modifier = Modifier.padding(start = 40.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
fun SwitchSettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
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
                .padding(start = 16.dp, end = 8.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }

    HorizontalDivider(
        modifier = Modifier.padding(start = 40.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
fun LogoutButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = "Logout",
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = stringResource(id = R.string.logout),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val languages = listOf(
        "English (US)",
        "Bahasa Melayu",
        "简体中文 (Simplified Chinese)",
        "हिन्दी (Hindi)"
    )
    
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.select_language)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                languages.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLanguage = language }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedLanguage == language,
                            onClick = { selectedLanguage = language }
                        )
                        
                        Text(
                            text = language,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                // First dismiss the dialog to prevent UI issues during language change
                onDismiss()
                // Then update the language
                onLanguageSelected(selectedLanguage)
                
                // Debug toast commented out - uncomment if needed for troubleshooting
                /*Toast.makeText(
                    context,
                    "Changing language to: $selectedLanguage",
                    Toast.LENGTH_SHORT
                ).show()*/
            }) {
                Text(stringResource(id = R.string.apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}