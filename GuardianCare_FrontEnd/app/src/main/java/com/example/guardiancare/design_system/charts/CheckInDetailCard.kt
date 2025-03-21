//package com.example.guardiancare.design_system.charts
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Close
//import androidx.compose.material3.Card
//import androidx.compose.material3.CardDefaults
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.example.guardiancare.data.models.CheckInResponse
//
///**
// * A reusable composable that displays details for a selected check-in
// *
// * @param checkIn The check-in to display details for
// * @param modifier Modifier for the composable
// * @param onDismiss Callback when the card is dismissed
// */
//@Composable
//fun CheckInDetailCard(
//    checkIn: CheckInResponse?,
//    modifier: Modifier = Modifier,
//    onDismiss: () -> Unit = {}
//) {
//    if (checkIn == null) return
//
//    // Get priority-based colors
//    val priorityColor = when (checkIn.priority) {
//        4 -> Color(0xFFB71C1C)  // Urgent Red
//        3 -> Color(0xFFF44336)  // High Priority Red
//        2 -> Color(0xFFFF9800)  // Medium Priority Orange
//        1 -> Color(0xFFFFEB3B)  // Low Priority Yellow
//        else -> Color(0xFF4CAF50)  // Normal Priority Green
//    }
//
//    val priorityText = when (checkIn.priority) {
//        4 -> "Urgent Attention Required"
//        3 -> "High Priority"
//        2 -> "Medium Priority"
//        1 -> "Low Priority"
//        else -> "Normal"
//    }
//
//    // Get mood-based text
//    val moodText = when (checkIn.mood) {
//        3 -> "Very Positive"
//        2 -> "Positive"
//        1 -> "Slightly Positive"
//        0 -> "Neutral"
//        -1 -> "Slightly Negative"
//        -2 -> "Negative"
//        -3 -> "Very Negative"
//        else -> "Unknown"
//    }
//
//    Card(
//        modifier = modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(12.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surface
//        )
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .background(MaterialTheme.colorScheme.surfaceContainer)
//        ) {
//            // Priority indicator
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(4.dp)
//                    .background(priorityColor)
//            )
//
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp)
//            ) {
//                // Header with timestamp and close button
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Box(
//                            modifier = Modifier
//                                .size(12.dp)
//                                .clip(CircleShape)
//                                .background(priorityColor)
//                        )
//
//                        Spacer(modifier = Modifier.width(8.dp))
//
//                        Text(
//                            text = "Check-In Details",
//                            fontSize = 18.sp,
//                            fontWeight = FontWeight.Bold
//                        )
//                    }
//
//                    IconButton(onClick = onDismiss) {
//                        Icon(
//                            imageVector = Icons.Default.Close,
//                            contentDescription = "Close",
//                            tint = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                    }
//                }
//
//                // Format and display the date
//                checkIn.created_at?.let {
//                    val formattedDate = ChartDataTransformer.formatDate(it)
//
//                    Text(
//                        text = formattedDate,
//                        fontSize = 14.sp,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant,
//                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
//                    )
//                }
//
//                // Status and priority section
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    // Priority card
//                    Card(
//                        modifier = Modifier.weight(1f),
//                        shape = RoundedCornerShape(8.dp),
//                        colors = CardDefaults.cardColors(
//                            containerColor = priorityColor.copy(alpha = 0.2f)
//                        )
//                    ) {
//                        Column(
//                            modifier = Modifier.padding(12.dp),
//                            horizontalAlignment = Alignment.CenterHorizontally
//                        ) {
//                            Text(
//                                text = "Priority",
//                                fontSize = 12.sp,
//                                fontWeight = FontWeight.Medium,
//                                color = MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                            Spacer(modifier = Modifier.height(4.dp))
//                            Text(
//                                text = priorityText,
//                                fontSize = 16.sp,
//                                fontWeight = FontWeight.Bold,
//                                color = MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.width(8.dp))
//
//                    // Mood card
//                    Card(
//                        modifier = Modifier.weight(1f),
//                        shape = RoundedCornerShape(8.dp),
//                        colors = CardDefaults.cardColors(
//                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
//                        )
//                    ) {
//                        Column(
//                            modifier = Modifier.padding(12.dp),
//                            horizontalAlignment = Alignment.CenterHorizontally
//                        ) {
//                            Text(
//                                text = "Mood",
//                                fontSize = 12.sp,
//                                fontWeight = FontWeight.Medium,
//                                color = MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                            Spacer(modifier = Modifier.height(4.dp))
//                            Text(
//                                text = moodText,
//                                fontSize = 16.sp,
//                                fontWeight = FontWeight.Bold
//                            )
//                        }
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Summary section
//                Card(
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = RoundedCornerShape(8.dp),
//                    colors = CardDefaults.cardColors(
//                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
//                    )
//                ) {
//                    Column(
//                        modifier = Modifier.padding(16.dp)
//                    ) {
//                        Text(
//                            text = "Summary",
//                            fontSize = 16.sp,
//                            fontWeight = FontWeight.SemiBold,
//                            modifier = Modifier.padding(bottom = 8.dp)
//                        )
//
//                        Text(
//                            text = checkIn.summary,
//                            fontSize = 14.sp,
//                            lineHeight = 20.sp
//                        )
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Transcript section
//                Card(
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = RoundedCornerShape(8.dp),
//                    colors = CardDefaults.cardColors(
//                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
//                    )
//                ) {
//                    Column(
//                        modifier = Modifier.padding(16.dp)
//                    ) {
//                        Text(
//                            text = "Full Transcript",
//                            fontSize = 16.sp,
//                            fontWeight = FontWeight.SemiBold,
//                            modifier = Modifier.padding(bottom = 8.dp)
//                        )
//
//                        Text(
//                            text = checkIn.transcript,
//                            fontSize = 14.sp,
//                            lineHeight = 20.sp
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
