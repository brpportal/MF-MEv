package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PurpleLight
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.BatchViewModel
import com.example.ui.viewmodel.ImportState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchImportScreen(
    authViewModel: AuthViewModel,
    batchViewModel: BatchViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToQueue: (String) -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val importState by batchViewModel.importState.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var jsonText by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedFileUri = uri
        uri?.let {
            currentUser?.uid?.let { uid ->
                batchViewModel.importBatchFromUri(it, uid)
            }
        }
    }

    LaunchedEffect(importState) {
        if (importState is ImportState.Success) {
            val batch = (importState as ImportState.Success).batch
            batchViewModel.resetImportState()
            onNavigateToQueue(batch.batchId)
        }
    }

    val sampleJson = """
{
  "batchId": "BATCH-${System.currentTimeMillis() % 10000}",
  "batchName": "Sponsored Content Verification Q4",
  "createdAt": "2026-10-15T09:00:00Z",
  "createdBy": "officer@mfmpanel.com",
  "links": [
    {
      "linkId": "A01",
      "url": "https://www.instagram.com/instagram/",
      "platform": "Instagram",
      "profileId": "@instagram",
      "requiredScreenshots": 1,
      "notes": "Verify promotional bio disclosure"
    },
    {
      "linkId": "A02",
      "url": "https://www.tiktok.com/@tiktok",
      "platform": "TikTok",
      "profileId": "@tiktok",
      "requiredScreenshots": 1,
      "notes": "Capture pinned campaign video"
    },
    {
      "linkId": "A03",
      "url": "https://www.youtube.com/@YouTube",
      "platform": "YouTube",
      "profileId": "@YouTube",
      "requiredScreenshots": 1,
      "notes": "Verify community post sponsor tag"
    },
    {
      "linkId": "A04",
      "url": "https://x.com/X",
      "platform": "X",
      "profileId": "@X",
      "requiredScreenshots": 1,
      "notes": "Verify official account banner"
    }
  ]
}
    """.trimIndent()

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
                    Text(
                        text = "Import Evidence Batch",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = PurpleLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MF MPanel JSON Compatibility",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Imports link queue files produced by the MF MPanel web console. Preserves original Link IDs, platform tags, profile IDs, and required capture counts.",
                        fontSize = 12.sp,
                        color = Slate400,
                        lineHeight = 18.sp
                    )
                }
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DarkSurface,
                contentColor = PurpleLight,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            "Choose File",
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedTabIndex == 0) PurpleLight else Slate400
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            "Paste JSON",
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedTabIndex == 1) PurpleLight else Slate400
                        )
                    }
                )
            }

            if (selectedTabIndex == 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = null,
                                tint = PurpleLight,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Select Batch JSON File",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Supports standard .json export files from MF MPanel",
                            fontSize = 12.sp,
                            color = Slate400
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { filePickerLauncher.launch("application/json") },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            modifier = Modifier.testTag("file_picker_button")
                        ) {
                            Text("Browse Files...", fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "JSON Payload",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color.White
                            )

                            OutlinedButton(
                                onClick = { jsonText = sampleJson },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, DarkBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleLight),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Load Sample", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = jsonText,
                            onValueChange = { jsonText = it },
                            placeholder = { Text("Paste JSON batch content here...", color = Slate500) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .testTag("json_paste_field"),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant,
                                focusedIndicatorColor = PurplePrimary,
                                unfocusedIndicatorColor = DarkBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (jsonText.isNotBlank()) {
                                    currentUser?.uid?.let { uid ->
                                        batchViewModel.importBatchFromJson(jsonText, uid)
                                    }
                                }
                            },
                            enabled = jsonText.isNotBlank() && importState !is ImportState.Loading,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PurplePrimary,
                                disabledContainerColor = DarkSurfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("import_submit_button")
                        ) {
                            if (importState is ImportState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Import Batch Now", fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = importState is ImportState.Error) {
                if (importState is ImportState.Error) {
                    val msg = (importState as ImportState.Error).message
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                        border = BorderStroke(1.dp, Color(0xFF7F1D1D))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = msg, color = Color(0xFFFECACA), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
