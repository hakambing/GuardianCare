package com.example.guardiancare.ui.screens

import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.guardiancare.R
import com.example.guardiancare.data.api.RetrofitClient
import com.example.guardiancare.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, context: Context, authViewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val localContext = LocalContext.current
    
    // Server URL dialog state
    var showServerUrlDialog by remember { mutableStateOf(false) }
    
    // Get current server URL from SharedPreferences
    val sharedPreferences = remember { 
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) 
    }
    var serverUrl by remember { 
        mutableStateOf(
            sharedPreferences.getString("server_url", RetrofitClient.baseUrl) ?: RetrofitClient.baseUrl
        ) 
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Settings icon for server URL configuration
        IconButton(
            onClick = { showServerUrlDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.server_settings),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App logo and title
            Spacer(modifier = Modifier.height(40.dp))
            Image(
                painter = painterResource(id = R.drawable.login),
                contentDescription = stringResource(R.string.app_logo),
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
            
            Text(
                text = stringResource(R.string.guardian_care),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            Text(
                text = stringResource(R.string.sign_in_to_account),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = stringResource(R.string.email_icon),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.password_icon),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            // Login button
            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        authViewModel.loginUser(email, password, navController)
                    } else {
                        Toast.makeText(localContext, localContext.getString(R.string.all_fields_required), Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(R.string.login),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.weight(1f))
            
            // Register link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dont_have_account),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                TextButton(onClick = { navController.navigate("register") }) {
                    Text(
                        text = stringResource(R.string.register),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            // Server URL info
            Text(
                text = stringResource(R.string.server, serverUrl),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
    
    // Server URL dialog
    if (showServerUrlDialog) {
        LoginServerUrlDialog(
            currentUrl = serverUrl,
            onDismiss = { showServerUrlDialog = false },
            onUrlUpdated = { newUrl ->
                serverUrl = newUrl
                // Update RetrofitClient
                RetrofitClient.updateBaseUrl(newUrl)
                // Save to SharedPreferences
                sharedPreferences.edit().putString("server_url", newUrl).apply()
                showServerUrlDialog = false
                
                Toast.makeText(context, context.getString(R.string.server_url_updated, newUrl), Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginServerUrlDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onUrlUpdated: (String) -> Unit
) {
    var serverUrl by remember { mutableStateOf(currentUrl) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.server_url)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    stringResource(R.string.enter_ngrok_url),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text(stringResource(R.string.server_url)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text(
                    stringResource(R.string.server_url_example),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Text(
                    stringResource(R.string.server_url_note),
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

@Composable
fun SocialLoginButton(
    icon: Int,
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .size(64.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = stringResource(R.string.login_with, text),
            modifier = Modifier
                .size(24.dp)
        )
    }
}