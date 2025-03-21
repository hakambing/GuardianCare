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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guardiancare.R
import com.example.guardiancare.data.session.SessionManager

/**
 * HelpScreen component that provides user guide and emergency contacts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen() {
    val context = LocalContext.current
    val sessionManager = SessionManager.getInstance(context)
    val userType = sessionManager.getUserType() ?: "elderly"
    
    val scrollState = rememberScrollState()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.user_guide),
        stringResource(R.string.emergency_contacts),
        stringResource(R.string.support)
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.help_and_support),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tab content
        when (selectedTab) {
            0 -> UserGuideContent(userType)
            1 -> EmergencyContactsContent(userType)
            2 -> SupportContent()
        }
    }
}

@Composable
fun rememberScrollState(): androidx.compose.foundation.ScrollState {
    return androidx.compose.foundation.rememberScrollState()
}

@Composable
fun UserGuideContent(userType: String) {
    val guides = if (userType == "caretaker") {
        // Guides for caretakers
        listOf(
            Guide(
                title = stringResource(R.string.caretaker_guide_title_1),
                description = stringResource(R.string.caretaker_guide_desc_1),
                icon = Icons.Default.People
            ),
            Guide(
                title = stringResource(R.string.caretaker_guide_title_2),
                description = stringResource(R.string.caretaker_guide_desc_2),
                icon = Icons.Default.Warning
            ),
            Guide(
                title = stringResource(R.string.caretaker_guide_title_3),
                description = stringResource(R.string.caretaker_guide_desc_3),
                icon = Icons.AutoMirrored.Filled.Assignment
            )
        )
    } else {
        // Guides for elderly users
        listOf(
            Guide(
                title = stringResource(R.string.elderly_guide_title_1),
                description = stringResource(R.string.elderly_guide_desc_1),
                icon = Icons.Default.CheckCircle
            ),
            Guide(
                title = stringResource(R.string.elderly_guide_title_2),
                description = stringResource(R.string.elderly_guide_desc_2),
                icon = Icons.Default.AddAlert
            ),
            Guide(
                title = stringResource(R.string.elderly_guide_title_3),
                description = stringResource(R.string.elderly_guide_desc_3),
                icon = Icons.Default.Medication
            )
        )
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(guides) { guide ->
            GuideItem(guide)
        }
    }
}

@Composable
fun EmergencyContactsContent(userType: String) {
    // Show only Singapore Police Force and Singapore Civil Defence Force for all users
    val emergencyContacts = listOf(
        EmergencyContact(
            name = stringResource(R.string.emergency_contact_police),
            phone = stringResource(R.string.emergency_number_police),
            role = stringResource(R.string.emergency_service_police),
            isPrimary = true
        ),
        EmergencyContact(
            name = stringResource(R.string.emergency_contact_ambulance),
            phone = stringResource(R.string.emergency_number_ambulance),
            role = stringResource(R.string.emergency_service_ambulance)
        )
    )
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.emergency_contact_intro),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        emergencyContacts.forEach { contact ->
            EmergencyContactItem(contact)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (userType == "elderly") {
            Button(
                onClick = { /* Add emergency call functionality */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = stringResource(R.string.emergency_call),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SupportContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Help,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(64.dp)
                .padding(bottom = 16.dp)
        )
        
        Text(
            text = stringResource(R.string.need_help),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = stringResource(R.string.support_description),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        SupportOption(
            title = stringResource(R.string.contact_support),
            description = stringResource(R.string.contact_support_description),
            icon = Icons.Default.Email
        )
    }
}

@Composable
fun GuideItem(guide: Guide) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { /* Navigate to detailed guide */ },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = guide.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 16.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = guide.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = guide.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmergencyContactItem(contact: EmergencyContact) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (contact.isPrimary) 
                MaterialTheme.colorScheme.errorContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (contact.isPrimary) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (contact.isPrimary) 
                        Icons.Default.Emergency 
                    else 
                        Icons.Default.ContactPhone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = contact.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = contact.role,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = contact.phone,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (contact.isPrimary) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            IconButton(
                onClick = { /* Call this contact */ }
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = if (contact.isPrimary) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SupportOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { /* Open support option */ },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Data classes for UI
data class Guide(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

data class EmergencyContact(
    val name: String,
    val phone: String,
    val role: String,
    val isPrimary: Boolean = false
)