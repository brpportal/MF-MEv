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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.LinkItemEntity
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
import com.example.ui.viewmodel.BatchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkQueueScreen(
    batchId: String,
    batchViewModel: BatchViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCapture: (batchId: String, linkId: String, url: String, platform: String) -> Unit,
    onNavigateToExport: (batchId: String) -> Unit
) {
    LaunchedEffect(batchId) {
        batchViewModel.selectBatch(batchId)
    }

    val batches by batchViewModel.batches.collectAsState()
    val batch = batches.find { it.batchId == batchId }
    val links by batchViewModel.links.collectAsState()
    val statusFilter by batchViewModel.statusFilter.collectAsState()
    val platformFilter by batchViewModel.platformFilter.collectAsState()

    val progress = if ((batch?.totalLinks ?: 0) > 0) {
        (batch?.completedLinks ?: 0).toFloat() / (batch?.totalLinks ?: 1).toFloat()
    } else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Navy900
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Navy900
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = batch?.name ?: "Link Queue",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Batch ID: $batchId",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { onNavigateToExport(batchId) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(34.dp)
                            .testTag("queue_export_button")
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontSize = 12.sp)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Batch Progress Summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Evidence Completion",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Navy900
                            )
                            Text(
                                text = "${batch?.completedLinks ?: 0} / ${batch?.totalLinks ?: 0} links (${(progress * 100).toInt()}%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (progress >= 1f) SuccessGreen else BluePrimary,
                            trackColor = Slate100
                        )
                    }
                }
            }

            // Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val statuses = listOf("ALL", "PENDING", "IN_PROGRESS", "COMPLETED")
                        items(statuses) { status ->
                            FilterChip(
                                selected = statusFilter == status,
                                onClick = { batchViewModel.setStatusFilter(status) },
                                label = { Text(status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Navy900,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val platforms = listOf("ALL", "Instagram", "TikTok", "YouTube", "X", "Other")
                        items(platforms) { platform ->
                            FilterChip(
                                selected = platformFilter == platform,
                                onClick = { batchViewModel.setPlatformFilter(platform) },
                                label = { Text(platform) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BluePrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Link Items
            if (links.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No links found matching filter",
                                fontSize = 14.sp,
                                color = Slate500
                            )
                        }
                    }
                }
            } else {
                items(links) { link ->
                    LinkCard(
                        link = link,
                        onCaptureClicked = {
                            onNavigateToCapture(link.batchId, link.linkId, link.url, link.platform)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LinkCard(
    link: LinkItemEntity,
    onCaptureClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCaptureClicked() }
            .testTag("link_item_${link.linkId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Navy900
                    ) {
                        Text(
                            text = link.linkId,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = getPlatformChipBg(link.platform)
                    ) {
                        Text(
                            text = link.platform,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = getPlatformChipColor(link.platform),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                StatusBadge(status = link.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!link.profileId.isNullOrBlank()) {
                Text(
                    text = "Profile: ${link.profileId}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Navy900
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            Text(
                text = link.url,
                fontSize = 12.sp,
                color = Slate500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!link.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Notes: ${link.notes}",
                    fontSize = 11.sp,
                    color = Slate600
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = if (link.screenshotCount >= link.requiredCount) SuccessGreen else Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${link.screenshotCount} / ${link.requiredCount} captured",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (link.screenshotCount >= link.requiredCount) SuccessGreen else Navy900
                    )
                }

                Button(
                    onClick = onCaptureClicked,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (link.screenshotCount > 0) Slate100 else BluePrimary,
                        contentColor = if (link.screenshotCount > 0) Navy900 else Color.White
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (link.screenshotCount > 0) "Open Browser" else "Capture",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

fun getPlatformChipColor(platform: String): Color {
    return when (platform.lowercase()) {
        "instagram" -> Color(0xFFC13584)
        "tiktok" -> Color(0xFF000000)
        "youtube" -> Color(0xFFDC2626)
        "facebook" -> Color(0xFF1877F2)
        "x" -> Color(0xFF0F172A)
        else -> BluePrimary
    }
}

fun getPlatformChipBg(platform: String): Color {
    return when (platform.lowercase()) {
        "instagram" -> Color(0xFFFCE7F3)
        "tiktok" -> Color(0xFFF1F5F9)
        "youtube" -> Color(0xFFFEE2E2)
        "facebook" -> Color(0xFFEFF6FF)
        "x" -> Color(0xFFF1F5F9)
        else -> Blue50
    }
}
