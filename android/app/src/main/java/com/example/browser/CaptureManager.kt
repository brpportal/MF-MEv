package com.example.browser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.WebView
import com.example.data.crypto.ChecksumUtil
import com.example.data.local.AppDatabase
import com.example.data.local.entity.LinkItemEntity
import com.example.data.local.entity.ScreenshotEntity
import com.example.data.local.entity.SyncOutboxEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

class CaptureManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val captureMutex = Mutex()

    /**
     * Captures visible webpage viewport from WebView and saves it locally.
     * Generates collision-resistant internal ID and sequential display ID.
     */
    suspend fun captureVisibleViewport(
        webView: WebView,
        batchId: String,
        linkId: String,
        platform: String,
        originalUrl: String,
        userId: String
    ): ScreenshotEntity = captureMutex.withLock {
        withContext(Dispatchers.Main) {
            val width = webView.width
            val height = webView.height
            if (width <= 0 || height <= 0) {
                throw IllegalStateException("WebView has invalid dimensions ($width x $height).")
            }

            // Create bitmap matching the visible viewport
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            webView.draw(canvas)

            withContext(Dispatchers.IO) {
                try {
                    // Check disk storage (require at least 20MB free)
                    if (context.filesDir.freeSpace < 20 * 1024 * 1024) {
                        throw IOException("Insufficient local storage space for screenshot capture.")
                    }

                    // Count existing screenshots for this link to generate sequential ID
                    val existingCount = database.screenshotDao().getScreenshotCountForLink(batchId, linkId)
                    val nextSeq = existingCount + 1
                    val safeLinkId = linkId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                    val screenshotId = "%s-SCR-%04d".format(safeLinkId, nextSeq)
                    val internalUuid = UUID.randomUUID().toString()

                    val safeBatchId = batchId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                    val relativeDir = "evidence/$safeBatchId/$safeLinkId"
                    val evidenceDir = File(context.filesDir, relativeDir).apply { mkdirs() }

                    val fileName = "${screenshotId}.jpg"
                    val tempFile = File(evidenceDir, "${screenshotId}_temp.jpg")
                    val finalFile = File(evidenceDir, fileName)

                    // Write bitmap atomically
                    FileOutputStream(tempFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                        out.flush()
                    }

                    if (!tempFile.exists() || tempFile.length() == 0L) {
                        throw IOException("Failed to write screenshot image file.")
                    }

                    if (finalFile.exists()) {
                        finalFile.delete()
                    }

                    if (!tempFile.renameTo(finalFile)) {
                        // Fallback copy & delete
                        tempFile.copyTo(finalFile, overwrite = true)
                        tempFile.delete()
                    }

                    // Compute SHA-256
                    val sha256 = ChecksumUtil.computeSha256(finalFile)
                    val fileSize = finalFile.length()
                    val relativeFilePath = "$relativeDir/$fileName"

                    val entity = ScreenshotEntity(
                        screenshotId = screenshotId,
                        internalId = internalUuid,
                        linkId = linkId,
                        batchId = batchId,
                        userId = userId,
                        platform = platform,
                        originalUrl = originalUrl,
                        capturedAt = System.currentTimeMillis(),
                        localFilePath = relativeFilePath,
                        mimeType = "image/jpeg",
                        fileSize = fileSize,
                        sha256Checksum = sha256,
                        captureStatus = "SAVED",
                        syncStatus = "PENDING"
                    )

                    // Atomic database write
                    database.screenshotDao().insertScreenshot(entity)

                    // Update link status and screenshot count
                    val currentLink = database.linkDao().getLink(batchId, linkId)
                    val newCount = nextSeq
                    val newStatus = if (currentLink != null && newCount >= currentLink.requiredCount) {
                        "COMPLETED"
                    } else {
                        "IN_PROGRESS"
                    }

                    database.linkDao().updateLinkScreenshotStats(
                        batchId = batchId,
                        linkId = linkId,
                        count = newCount,
                        status = newStatus,
                        updatedAt = System.currentTimeMillis()
                    )

                    // Update batch completion count
                    database.batchDao().updateBatchCompletedCount(batchId)

                    // Queue sync operation for metadata
                    val payload = """
                        {
                            "screenshotId": "$screenshotId",
                            "internalId": "$internalUuid",
                            "linkId": "$linkId",
                            "batchId": "$batchId",
                            "userId": "$userId",
                            "platform": "$platform",
                            "capturedAt": ${entity.capturedAt},
                            "sha256": "$sha256",
                            "fileSize": $fileSize
                        }
                    """.trimIndent()

                    database.syncOutboxDao().insertOperation(
                        SyncOutboxEntity(
                            operationId = UUID.randomUUID().toString(),
                            eventType = "SCREENSHOT_CAPTURED",
                            batchId = batchId,
                            linkId = linkId,
                            screenshotId = screenshotId,
                            payloadJson = payload,
                            status = "PENDING"
                        )
                    )

                    entity
                } finally {
                    bitmap.recycle()
                }
            }
        }
    }
}
