package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.LinkItemEntity
import com.example.data.local.entity.ScreenshotEntity
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PurpleLight
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.BatchViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ProjectTab {
    LINKS,
    SCREENSHOTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkQueueScreen(
    batchId: String,
    batchViewModel: BatchViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCapture: (batchId: String, linkId: String, url: String, platform: String) -> Unit,
    onNavigateToExport: (batchId: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(batchId) {
        batchViewModel.selectBatch(batchId)
    }

    val batches by batchViewModel.batches.collectAsState()
    val batch = batches.find { it.batchId == batchId }
    val links by batchViewModel.links.collectAsState()
    val screenshots by batchViewModel.screenshots.collectAsState()
    val statusFilter by batchViewModel.statusFilter.collectAsState()

    var selectedTab by remember { mutableStateOf(ProjectTab.LINKS) }
    var fullScreenScreenshot by remember { mutableStateOf<ScreenshotEntity?>(null) }
    var screenshotToDelete by remember { mutableStateOf<ScreenshotEntity?>(null) }

    val progress = if ((batch?.totalLinks ?: 0) > 0) {
        (batch?.completedLinks ?: 0).toFloat() / (batch?.totalLinks ?: 1).toFloat()
    } else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = batch?.name ?: "Project Execution",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Batch ID: $batchId • ${(progress * 100).toInt()}% Done",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                actions = {
                    // "Get Links" action button linking directly to https://mf-panel.vercel.app/
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://mf-panel.vercel.app/")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open browser", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .height(34.dp)
                            .testTag("project_get_links_button")
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Get Links", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Export button
                    IconButton(
                        onClick = { onNavigateToExport(batchId) },
                        modifier = Modifier.testTag("queue_export_button")
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = "Export", tint = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            // Project Bottom Navigation Bar: Two buttons [Links] and [Screenshots]
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = selectedTab == ProjectTab.LINKS,
                    onClick = { selectedTab = ProjectTab.LINKS },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Links"
                        )
                    },
                    label = {
                        Text(
                            text = "Links (${links.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == ProjectTab.LINKS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = PurplePrimary,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.testTag("tab_project_links")
                )

                NavigationBarItem(
                    selected = selectedTab == ProjectTab.SCREENSHOTS,
                    onClick = { selectedTab = ProjectTab.SCREENSHOTS },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Screenshots"
                        )
                    },
                    label = {
                        Text(
                            text = "Screenshots (${screenshots.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == ProjectTab.SCREENSHOTS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = PurplePrimary,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.testTag("tab_project_screenshots")
                )
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                ProjectTab.LINKS -> {
                    SingleLinkPagerView(
                        batchId = batchId,
                        links = links,
                        statusFilter = statusFilter,
                        onFilterChange = { batchViewModel.setStatusFilter(it) },
                        onCapture = { linkId, url, platform ->
                            onNavigateToCapture(batchId, linkId, url, platform)
                        },
                        onMarkFailed = { linkId ->
                            batchViewModel.markLinkAsFailed(batchId, linkId)
                            Toast.makeText(context, "Link marked as FAILED", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                ProjectTab.SCREENSHOTS -> {
                    ProjectScreenshotsView(
                        screenshots = screenshots,
                        onSelectScreenshot = { fullScreenScreenshot = it },
                        onDeleteScreenshot = { screenshotToDelete = it },
                        onRecapture = { scr ->
                            onNavigateToCapture(scr.batchId, scr.linkId, scr.originalUrl, scr.platform)
                        }
                    )
                }
            }
        }
    }

    // Full Screen Screenshot Viewer Dialog
    fullScreenScreenshot?.let { scr ->
        FullScreenScreenshotDialog(
            screenshot = scr,
            onDismiss = { fullScreenScreenshot = null },
            onDelete = {
                screenshotToDelete = scr
                fullScreenScreenshot = null
            },
            onRecapture = {
                val bId = scr.batchId
                val lId = scr.linkId
                val url = scr.originalUrl
                val plat = scr.platform
                fullScreenScreenshot = null
                onNavigateToCapture(bId, lId, url, plat)
            }
        )
    }

    // Delete confirmation dialog
    screenshotToDelete?.let { scr ->
        AlertDialog(
            onDismissRequest = { screenshotToDelete = null },
            title = {
                Text("Delete Screenshot?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Are you sure you want to delete ${scr.screenshotId} for Link ${scr.linkId}? This local evidence image will be removed permanently.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        batchViewModel.deleteScreenshot(scr)
                        screenshotToDelete = null
                        Toast.makeText(context, "Screenshot deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { screenshotToDelete = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Single Link Card View with Horizontal Swipe Pager & Next/Previous navigation
 */
@Composable
fun SingleLinkPagerView(
    batchId: String,
    links: List<LinkItemEntity>,
    statusFilter: String,
    onFilterChange: (String) -> Unit,
    onCapture: (linkId: String, url: String, platform: String) -> Unit,
    onMarkFailed: (linkId: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Status Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val filters = listOf("ALL", "PENDING", "IN_PROGRESS", "COMPLETED", "FAILED")
            filters.forEach { f ->
                FilterChip(
                    selected = statusFilter == f,
                    onClick = { onFilterChange(f) },
                    label = { Text(f, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PurplePrimary,
                        selectedLabelColor = Color.White,
                        containerColor = DarkSurface,
                        labelColor = Color(0xFF94A3B8)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = statusFilter == f,
                        borderColor = Color(0xFF334155),
                        selectedBorderColor = PurpleLight
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (links.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No links in this view",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try switching the status filter above.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { links.size })

            // Pager Navigation Header: [ < Prev ]   Link X of Y   [ Next > ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        if (pagerState.currentPage > 0) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    },
                    enabled = pagerState.currentPage > 0,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Prev", fontSize = 12.sp)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceVariant,
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text(
                        text = "Link ${pagerState.currentPage + 1} of ${links.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleLight,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                OutlinedButton(
                    onClick = {
                        if (pagerState.currentPage < links.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    enabled = pagerState.currentPage < links.size - 1,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Next", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // The Swipe Pager presenting ONE link card on screen at a time
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val link = links[page]
                SingleLinkCard(
                    link = link,
                    totalLinks = links.size,
                    currentIndex = page + 1,
                    onCapture = { onCapture(link.linkId, link.url, link.platform) },
                    onMarkFailed = {
                        onMarkFailed(link.linkId)
                        if (page < links.size - 1) {
                            coroutineScope.launch { pagerState.animateScrollToPage(page + 1) }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Dedicated Full-Featured Single Link Card
 */
@Composable
fun SingleLinkCard(
    link: LinkItemEntity,
    totalLinks: Int,
    currentIndex: Int,
    onCapture: () -> Unit,
    onMarkFailed: () -> Unit
) {
    val context = LocalContext.current
    var selectedCountry by remember { mutableStateOf("United States (US)") }
    var countryMenuExpanded by remember { mutableStateOf(false) }

    val countries = listOf(
        "United States (US)",
        "United Kingdom (UK)",
        "Canada (CA)",
        "Australia (AU)",
        "Germany (DE)",
        "United Arab Emirates (UAE)",
        "Pakistan (PK)",
        "India (IN)",
        "Global / Default"
    )

    Card(
        modifier = Modifier
            .fillMaxSize()
            .testTag("single_link_card_${link.linkId}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Top Header: Link ID & Platform Tag & Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E1B4B),
                            border = BorderStroke(1.dp, Color(0xFFA855F7))
                        ) {
                            Text(
                                text = "ID: ${link.linkId}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceVariant
                        ) {
                            Text(
                                text = link.platform,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PurpleLight,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    LinkStatusBadge(status = link.status)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Profile and Post Identifier Section
                if (!link.profileId.isNullOrBlank()) {
                    Text(
                        text = "Target Account / Profile:",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = link.profileId,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (!link.postIdentifier.isNullOrBlank()) {
                    Text(
                        text = "Post ID / Reference:",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = link.postIdentifier,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE2E8F0)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Target URL Card with 1-Tap Copy
                Text(
                    text = "Target URL:",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = link.url,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF38BDF8),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("URL", link.url)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Verification Country Selection (Drop-down selector)
                Text(
                    text = "Inspection Country Region:",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { countryMenuExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = PurpleLight, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedCountry,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                            Text("Change", fontSize = 11.sp, color = PurpleLight, fontWeight = FontWeight.Bold)
                        }
                    }

                    DropdownMenu(
                        expanded = countryMenuExpanded,
                        onDismissRequest = { countryMenuExpanded = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        countries.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c, color = Color.White, fontSize = 13.sp) },
                                onClick = {
                                    selectedCountry = c
                                    countryMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Optional Postage Links Advisory
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF131B2E),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Linked Postage Links: 3 recommended (Optional). Screenshot capture is required.",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }

                if (!link.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Notes: ${link.notes}",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Bottom Action Controls: FAILED vs WORKING (Capture)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Failed Button
                    Button(
                        onClick = onMarkFailed,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7F1D1D),
                            contentColor = Color(0xFFFCA5A5)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("link_failed_${link.linkId}")
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Failed", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    // Working (Capture) Button
                    Button(
                        onClick = onCapture,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (link.screenshotCount > 0) Color(0xFF047857) else Color(0xFF6D28D9),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(46.dp)
                            .testTag("link_working_${link.linkId}")
                    ) {
                        Icon(
                            imageVector = if (link.screenshotCount > 0) Icons.Default.Check else Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (link.screenshotCount > 0) "Working (Retake)" else "Working (Capture)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Project Screenshots Gallery View
 */
@Composable
fun ProjectScreenshotsView(
    screenshots: List<ScreenshotEntity>,
    onSelectScreenshot: (ScreenshotEntity) -> Unit,
    onDeleteScreenshot: (ScreenshotEntity) -> Unit,
    onRecapture: (ScreenshotEntity) -> Unit
) {
    val context = LocalContext.current

    if (screenshots.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Screenshots Captured Yet",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Open any link in the 'Links' tab and tap 'Working (Capture)' to save evidence here.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(screenshots, key = { it.screenshotId }) { scr ->
                ScreenshotCardItem(
                    screenshot = scr,
                    onClick = { onSelectScreenshot(scr) },
                    onDelete = { onDeleteScreenshot(scr) }
                )
            }
        }
    }
}

@Composable
fun ScreenshotCardItem(
    screenshot: ScreenshotEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(screenshot.localFilePath) {
        val file = File(context.filesDir, screenshot.localFilePath)
        if (file.exists()) {
            try {
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    val dateFormatted = remember(screenshot.capturedAt) {
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        sdf.format(Date(screenshot.capturedAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("screenshot_card_${screenshot.screenshotId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = screenshot.screenshotId,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Delete quick button on top right of card
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .background(Color(0x99000000), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFF87171),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = screenshot.screenshotId,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Link: ${screenshot.linkId} • ${screenshot.platform}",
                    fontSize = 11.sp,
                    color = PurpleLight
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateFormatted,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

/**
 * High-Resolution Full-Screen Screenshot Viewer Dialog
 */
@Composable
fun FullScreenScreenshotDialog(
    screenshot: ScreenshotEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onRecapture: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(screenshot.localFilePath) {
        val file = File(context.filesDir, screenshot.localFilePath)
        if (file.exists()) {
            try {
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    val dateFormatted = remember(screenshot.capturedAt) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.format(Date(screenshot.capturedAt))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Fullscreen Screenshot Image
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = screenshot.screenshotId,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 60.dp)
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Image unavailable", color = Color.White)
                }
            }

            // Top Header Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding(),
                color = Color(0xCC0F172A)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = screenshot.screenshotId,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Link: ${screenshot.linkId} • $dateFormatted",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }

            // Bottom Action Bar: [ Delete ]   [ Recapture ]
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
                color = Color(0xCC0F172A)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                        border = BorderStroke(1.dp, Color(0xFFDC2626)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Image", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onRecapture,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Recapture", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LinkStatusBadge(status: String) {
    val (bg, fg, label) = when (status.uppercase()) {
        "COMPLETED" -> Triple(Color(0xFF065F46), Color(0xFF34D399), "Completed")
        "FAILED" -> Triple(Color(0xFF7F1D1D), Color(0xFFF87171), "Failed")
        "IN_PROGRESS" -> Triple(Color(0xFF1E3A8A), Color(0xFF93C5FD), "In Progress")
        else -> Triple(Color(0xFF334155), Color(0xFF94A3B8), "Pending")
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
