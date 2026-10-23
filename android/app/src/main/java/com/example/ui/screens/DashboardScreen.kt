package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.BatchEntity
import com.example.ui.theme.Blue50
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberBg
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.BatchViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutLinearInEasing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun StatusIndicator(isLinked: Boolean) {
    val indicatorColor = if (isLinked) Color(0xFF10B981) else Color(0xFFEF4444)
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(750, easing = androidx.compose.animation.core.FastOutLinearInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ), label = "alpha"
    )

    val currentAlpha = if (!isLinked) alpha else 1f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isLinked) Color(0xFFECFDF5) else Color(0xFFFEF2F2))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("backend_status_indicator")
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(indicatorColor.copy(alpha = currentAlpha))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isLinked) "Linked" else "Unlinked",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isLinked) Color(0xFF047857) else Color(0xFFB91C1C)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel,
    batchViewModel: BatchViewModel,
    onNavigateToBatchImport: () -> Unit,
    onNavigateToLinkQueue: (String) -> Unit,
    onNavigateToPlatforms: () -> Unit,
    onNavigateToExport: (String) -> Unit,
    onNavigateToExportHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDiagnostics: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val batches by batchViewModel.batches.collectAsState()
    val pendingSync by batchViewModel.pendingSyncCount.collectAsState()
    val storageMetrics by batchViewModel.storageMetrics.collectAsState()
    
    val linkingViewModel: com.example.ui.viewmodel.LinkingViewModel = viewModel()
    val registrationStatus by linkingViewModel.registrationStatus.collectAsState()
    val isLinked = registrationStatus == com.example.data.repository.RegistrationStatus.ACTIVATED

    val context = androidx.compose.ui.platform.LocalContext.current
    var lastBackPressTime by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0L) }
    
    // ... (rest of DashboardScreen content)

    androidx.activity.compose.BackHandler {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < 2000L) {
            (context as? android.app.Activity)?.finish()
        } else {
            lastBackPressTime = now
            android.widget.Toast.makeText(context, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Navy900
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Navy900),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MF-MEv Evidence",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                            Text(
                                text = currentUser?.teamName ?: "Evidence Team",
                                fontSize = 11.sp,
                                color = BluePrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
                    StatusIndicator(isLinked = isLinked)
                    Spacer(modifier = Modifier.width(16.dp))
                    if (pendingSync > 0) {
                        IconButton(onClick = { batchViewModel.syncNow() }) {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = "Sync",
                                    tint = WarningAmber
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(WarningAmber)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }
                    IconButton(onClick = onNavigateToDiagnostics) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Diagnostics",
                            tint = Slate500
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Slate600
                        )
                    }
                }
            )
        },
        containerColor = Slate50
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Team Welcome Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy900)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Welcome, ${currentUser?.displayName?.split(" ")?.firstOrNull() ?: "Officer"}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0x33FFFFFF)
                            ) {
                                Text(
                                    text = "AES-256 Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Assigned: ${currentUser?.teamName} (${currentUser?.role})",
                            fontSize = 12.sp,
                            color = Slate400
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onNavigateToBatchImport,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dashboard_import_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import Batch", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = onNavigateToPlatforms,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dashboard_platforms_button")
                            ) {
                                Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Platforms", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Quick Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val totalLinks = batches.sumOf { it.totalLinks }
                    val completedLinks = batches.sumOf { it.completedLinks }

                    StatCard(
                        title = "Batches",
                        value = "${batches.size}",
                        subtitle = "$totalLinks links",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Completed",
                        value = "$completedLinks",
                        subtitle = "Ready to export",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Pending Sync",
                        value = "$pendingSync",
                        subtitle = if (pendingSync == 0) "All synced" else "Queued",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Section Header: Batches
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Evidence Batches",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                    Text(
                        text = "Export History",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BluePrimary,
                        modifier = Modifier
                            .clickable { onNavigateToExportHistory() }
                            .padding(4.dp)
                    )
                }
            }

            // Batches List
            if (batches.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No Evidence Batches Yet",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Navy900
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Import a batch JSON file generated by the MF MPanel website to begin collecting evidence.",
                                fontSize = 12.sp,
                                color = Slate500,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = onNavigateToBatchImport,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                            ) {
                                Text("Import JSON Batch")
                            }
                        }
                    }
                }
            } else {
                items(batches) { batch ->
                    BatchItemCard(
                        batch = batch,
                        onOpenQueue = { onNavigateToLinkQueue(batch.batchId) },
                        onExport = { onNavigateToExport(batch.batchId) }
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Navy900)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 10.sp, color = Slate400)
        }
    }
}

@Composable
fun BatchItemCard(
    batch: BatchEntity,
    onOpenQueue: () -> Unit,
    onExport: () -> Unit
) {
    val progress = if (batch.totalLinks > 0) {
        batch.completedLinks.toFloat() / batch.totalLinks.toFloat()
    } else 0f

    val dateFormat = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(batch.importedAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenQueue() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = batch.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "ID: ${batch.batchId} • $dateString",
                        fontSize = 11.sp,
                        color = Slate500
                    )
                }

                StatusBadge(status = batch.status)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Evidence Progress",
                    fontSize = 11.sp,
                    color = Slate500
                )
                Text(
                    text = "${batch.completedLinks} / ${batch.totalLinks} links (${(progress * 100).toInt()}%)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Navy900
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (progress >= 1f) SuccessGreen else BluePrimary,
                trackColor = Slate100
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onExport,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy900),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export ZIP", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onOpenQueue,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Open Queue", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bg, text, label) = when (status.uppercase()) {
        "COMPLETED" -> Triple(SuccessGreenBg, SuccessGreen, "Completed")
        "EXPORTED" -> Triple(Blue50, BluePrimary, "Exported")
        "IN_PROGRESS" -> Triple(WarningAmberBg, WarningAmber, "In Progress")
        else -> Triple(Slate100, Slate600, "Pending")
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
