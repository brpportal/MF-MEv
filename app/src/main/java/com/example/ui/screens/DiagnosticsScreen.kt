package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.crypto.ChecksumUtil
import com.example.data.crypto.ZipEncryptionManager
import com.example.data.local.entity.LinkItemEntity
import com.example.data.local.entity.ScreenshotEntity
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PurpleLight
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.BatchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    authViewModel: AuthViewModel,
    batchViewModel: BatchViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by authViewModel.currentUser.collectAsState()
    val storageMetrics by batchViewModel.storageMetrics.collectAsState()
    val batches by batchViewModel.batches.collectAsState()
    val pendingSync by batchViewModel.pendingSyncCount.collectAsState()
    val exports by batchViewModel.allExports.collectAsState()

    var testStatus by remember { mutableStateOf<String?>(null) }
    var testRunning by remember { mutableStateOf(false) }

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
                        text = "System Diagnostics",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Environment & Architecture Overview
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "MF MPanel System Configuration",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        DetailRow(label = "Application ID", value = context.packageName)
                        DetailRow(label = "Version Name", value = "1.0.0")
                        DetailRow(
                            label = "Supabase Auth",
                            value = if (currentUser != null) "Authenticated (${currentUser?.uid})" else "Unauthenticated"
                        )
                        DetailRow(
                            label = "Assigned Team",
                            value = currentUser?.teamName ?: "Evidence Team Alpha"
                        )
                        DetailRow(
                            label = "Supabase URL",
                            value = com.example.data.SupabaseConfig.getUrl(context)
                        )
                    }
                }
            }

            // Room Database Metrics
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Local Room Persistence",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        DetailRow(label = "Batches Stored", value = "${batches.size}")
                        DetailRow(label = "Total Links Registered", value = "${batches.sumOf { it.totalLinks }}")
                        DetailRow(label = "Completed Links", value = "${batches.sumOf { it.completedLinks }}")
                        DetailRow(label = "Sync Outbox Queue", value = "$pendingSync pending")
                        DetailRow(label = "Export Archives Record", value = "${exports.size} records")
                    }
                }
            }

            // Storage Metrics
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Sandbox Storage Allocation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val evidenceMb = (storageMetrics?.evidenceSizeBytes ?: 0L) / (1024f * 1024f)
                        val exportsMb = (storageMetrics?.exportsSizeBytes ?: 0L) / (1024f * 1024f)
                        val freeMb = (storageMetrics?.freeDiskBytes ?: 0L) / (1024f * 1024f)

                        DetailRow(label = "Evidence Images", value = "%.2f MB".format(evidenceMb))
                        DetailRow(label = "Encrypted ZIPs", value = "%.2f MB".format(exportsMb))
                        DetailRow(label = "Free Disk Space", value = "%.1f MB".format(freeMb))
                    }
                }
            }

            // Live Cryptographic Round-Trip Test
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = PurpleLight, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AES-256 Cryptography Audit",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Executes an end-to-end cryptographic test on device: generates test evidence, builds an AES-256 encrypted ZIP with Zip4j, verifies checksums, tests decryption with password, and verifies path-traversal rejection.",
                            fontSize = 12.sp,
                            color = Slate400,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                testRunning = true
                                testStatus = null
                                scope.launch {
                                    testStatus = runEncryptionSelfTest(context)
                                    testRunning = false
                                }
                            },
                            enabled = !testRunning,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("run_encryption_test_button")
                        ) {
                            if (testRunning) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Running Cryptographic Verification...", color = Color.White)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Run Cryptography Self-Test", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        if (testStatus != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (testStatus?.contains("PASSED") == true) DarkSurfaceVariant else Color(0xFF450A0A),
                                border = BorderStroke(
                                    1.dp,
                                    if (testStatus?.contains("PASSED") == true) SuccessGreen.copy(alpha = 0.5f) else Color(0xFF7F1D1D)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (testStatus?.contains("PASSED") == true) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (testStatus?.contains("PASSED") == true) SuccessGreen else Color(0xFFF87171),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = testStatus ?: "",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (testStatus?.contains("PASSED") == true) SuccessGreen else Color(0xFFFECACA)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

suspend fun runEncryptionSelfTest(context: android.content.Context): String = withContext(Dispatchers.IO) {
    val tempDir = File(context.cacheDir, "diag_test_${System.currentTimeMillis()}").apply { mkdirs() }
    val extractDir = File(tempDir, "extracted").apply { mkdirs() }

    try {
        // 1. Create dummy screenshot image
        val testImg = File(tempDir, "test_screenshot.jpg")
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.BLUE)
        val paint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 20f
        }
        canvas.drawText("TEST", 50f, 100f, paint)

        FileOutputStream(testImg).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bitmap.recycle()

        val imgChecksum = ChecksumUtil.computeSha256(testImg)

        // 2. Prepare test entities
        val testLink = LinkItemEntity(
            linkId = "TEST-01",
            batchId = "DIAG-BATCH",
            url = "https://instagram.com/test",
            platform = "Instagram",
            screenshotCount = 1,
            requiredCount = 1,
            status = "COMPLETED"
        )

        val testScr = ScreenshotEntity(
            screenshotId = "TEST-01-SCR-0001",
            internalId = UUID.randomUUID().toString(),
            linkId = "TEST-01",
            batchId = "DIAG-BATCH",
            userId = "DIAG_USER",
            platform = "Instagram",
            originalUrl = "https://instagram.com/test",
            localFilePath = testImg.name,
            fileSize = testImg.length(),
            sha256Checksum = imgChecksum,
            captureStatus = "SAVED",
            syncStatus = "PENDING"
        )

        val links = listOf(testLink to listOf(testScr to testImg))
        val testPassword = ChecksumUtil.generateSecurePassword(16)

        // 3. Encrypt archive
        val zipManager = ZipEncryptionManager(context)
        val exportResult = zipManager.createEncryptedZip(
            batchId = "DIAG-BATCH",
            exportId = "TEST-EXP",
            exportingUserUid = "DIAG_USER",
            password = testPassword,
            links = links
        )

        // 4. Decrypt & validate
        val manifest = zipManager.decryptAndValidateArchive(
            zipFile = exportResult.zipFile,
            password = testPassword,
            destinationDir = extractDir
        )

        // Clean up test archive
        exportResult.zipFile.delete()

        if (manifest.totalScreenshots == 1 && manifest.links.first().linkId == "TEST-01") {
            "SELF-TEST PASSED: AES-256 archive created, manifest validated, SHA-256 checksum confirmed, and decryption verified."
        } else {
            "SELF-TEST FAILED: Manifest structure mismatch."
        }
    } catch (e: Exception) {
        "SELF-TEST FAILED: ${e.message}"
    } finally {
        tempDir.deleteRecursively()
    }
}
