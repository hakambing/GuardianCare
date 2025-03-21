package com.example.guardiancare.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.content.ContextCompat
import com.example.guardiancare.R
import com.example.guardiancare.data.api.ElderlyApiService
import com.example.guardiancare.data.api.RetrofitClient
import com.example.guardiancare.data.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Tab selection state
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Voice", "Text")

    var checkInText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var audioFilePath by remember { mutableStateOf<String?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var hasRecordingPermission by remember { mutableStateOf(false) }

    var isSubmitting by remember { mutableStateOf(false) }
    var isSubmitted by remember { mutableStateOf(false) }

    val sessionManager = remember { SessionManager.getInstance(context) }
    val elderlyApiService = remember { RetrofitClient.createService(ElderlyApiService::class.java) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        hasRecordingPermission = allGranted

        if (!allGranted) {
            Toast.makeText(
                context,
                "Audio recording permission is required",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        val requiredPermissions = mutableListOf(Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            requiredPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            requiredPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isEmpty()) {
            hasRecordingPermission = true
        } else {
            requestPermissionLauncher.launch(permissionsToRequest)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopRecording(recorder)
            recorder = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.daily_check_in),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        if (isSubmitted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(64.dp)
                            .padding(bottom = 16.dp)
                    )

                    Text(
                        text = stringResource(R.string.check_in_submitted),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.check_in_success_message),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = {
                            // Reset form for a new check-in
                            isSubmitted = false
                            checkInText = ""
                            audioFilePath = null
                            selectedTabIndex = 0 // Return to voice tab
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.new_check_in),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.new_check_in))
                    }
                }
            }
        } else {
            // Content based on selected tab
            when (selectedTabIndex) {
                0 -> {
                    // Voice Tab Content
                    VoiceTabContent(
                        isRecording = isRecording,
                        isSubmitting = isSubmitting,
                        audioFilePath = audioFilePath,
                        hasRecordingPermission = hasRecordingPermission,
                        onRecordingStateChanged = { recording, path ->
                            isRecording = recording
                            if (path != null) {
                                audioFilePath = path
                            }
                        },
                        onRecorderCreated = { newRecorder ->
                            recorder = newRecorder
                        },
                        onRecorderStopped = {
                            stopRecording(recorder)
                            isSubmitting = true
                            uploadAudioFile(
                                filePath = audioFilePath!!,
                                elderlyApiService = elderlyApiService,
                                sessionManager = sessionManager,
                                onSuccess = {
                                    isSubmitted = true
                                    isSubmitting = false
                                },
                                onError = { errorMessage ->
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                    isSubmitting = false
                                }
                            )
                            recorder = null
                        },
                        context = context
                    )
                }
                1 -> {
                    // Text Tab Content
                    TextTabContent(
                        text = checkInText,
                        onTextChanged = { checkInText = it },
                        isSubmitting = isSubmitting,
                        onSubmit = {
                            isSubmitting = true
                            uploadTextCheckIn(
                                text = checkInText,
                                elderlyApiService = elderlyApiService,
                                sessionManager = sessionManager,
                                onSuccess = {
                                    isSubmitted = true
                                    isSubmitting = false
                                },
                                onError = { errorMessage ->
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                    isSubmitting = false
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

// Function to create a MediaRecorder instance
private fun createRecorder(context: Context): MediaRecorder {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
    } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
    }
}

// Function to start recording audio
private fun startRecording(recorder: MediaRecorder, context: Context): String {
    val fileName = "${context.externalCacheDir?.absolutePath}/audio.m4a"
    
    try {
        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioChannels(1) 
            setAudioSamplingRate(44100) 
            setAudioEncodingBitRate(128000)
            setOutputFile(fileName)
            prepare()
            start()
        }
        
        // Log success
        android.util.Log.d("AudioRecording", "Started recording to $fileName")
    } catch (e: IOException) {
        android.util.Log.e("AudioRecording", "Error starting recording: ${e.message}", e)
        e.printStackTrace()
        return ""
    }
    
    return fileName
}

// Function to stop recording
private fun stopRecording(recorder: MediaRecorder?) {
    try {
        recorder?.apply {
            stop()
            release()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Voice Tab Content
@Composable
fun VoiceTabContent(
    isRecording: Boolean,
    isSubmitting: Boolean,
    audioFilePath: String?,
    hasRecordingPermission: Boolean,
    onRecordingStateChanged: (Boolean, String?) -> Unit,
    onRecorderCreated: (MediaRecorder) -> Unit,
    onRecorderStopped: () -> Unit,
    context: Context
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Record your check-in message",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "Tap the microphone button to start recording your daily check-in. Tap again to stop and send.",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Large recording button
        Button(
            onClick = {
                if (isRecording) {
                    onRecorderStopped()
                    onRecordingStateChanged(false, audioFilePath)
                } else {
                    if (hasRecordingPermission) {
                        val newRecorder = createRecorder(context)
                        onRecorderCreated(newRecorder)
                        val filePath = startRecording(newRecorder, context)
                        onRecordingStateChanged(true, filePath)
                    } else {
                        Toast.makeText(
                            context,
                            "Audio recording permission is required",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            modifier = Modifier.size(160.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ),
            enabled = !isSubmitting
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                    modifier = Modifier.size(64.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (isSubmitting) "Sending recording..." 
                  else if (isRecording) "Recording... Tap to stop and send" 
                  else "Tap to record",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// Text Tab Content
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextTabContent(
    text: String,
    onTextChanged: (String) -> Unit,
    isSubmitting: Boolean,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Type your check-in message",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            placeholder = { Text("Enter your check-in message here...") },
            enabled = !isSubmitting
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = text.isNotBlank() && !isSubmitting,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Submit",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Submit",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Function to upload audio file to server
private fun uploadAudioFile(
    filePath: String,
    elderlyApiService: ElderlyApiService,
    sessionManager: SessionManager,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val file = File(filePath)
    if (!file.exists()) {
        onError("Audio file not found")
        return
    }
    
    val token = sessionManager.getToken()
    if (token.isNullOrEmpty()) {
        onError("Authentication required")
        return
    }
    
    // Log file details
    android.util.Log.d("AudioUpload", "Uploading file: $filePath, size: ${file.length()} bytes")
    
    val requestFile = file.asRequestBody("audio/mp4a-latm".toMediaTypeOrNull())
    val audioPart = MultipartBody.Part.createFormData("audio", "audio.m4a", requestFile)

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = elderlyApiService.uploadAudioForTranscription(
                "Bearer $token",
                audioPart
            )
            
            CoroutineScope(Dispatchers.Main).launch {
                if (response.isSuccessful) {
                    android.util.Log.d("AudioUpload", "Upload successful")
                    onSuccess()
                } else {
                    android.util.Log.e("AudioUpload", "Upload failed: ${response.message()}")
                    onError("Upload failed: ${response.message()}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioUpload", "Upload error: ${e.message}", e)
            CoroutineScope(Dispatchers.Main).launch {
                onError("Error: ${e.message}")
            }
        }
    }
}

// Function to upload text check-in to server
private fun uploadTextCheckIn(
    text: String,
    elderlyApiService: ElderlyApiService,
    sessionManager: SessionManager,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val token = sessionManager.getToken()
    if (token.isNullOrEmpty()) {
        onError("Authentication required")
        return
    }
    
    val requestBody = text.toRequestBody("text/plain".toMediaTypeOrNull())

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = elderlyApiService.uploadTextCheckIn(
                "Bearer $token",
                requestBody
            )
            
            CoroutineScope(Dispatchers.Main).launch {
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError("Upload failed: ${response.message()}")
                }
            }
        } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch {
                onError("Error: ${e.message}")
            }
        }
    }
}
