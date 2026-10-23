package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.crypto.ExportResult
import com.example.data.crypto.ZipEncryptionManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BatchEntity
import com.example.data.local.entity.ExportRecordEntity
import com.example.data.local.entity.LinkItemEntity
import com.example.data.local.entity.ScreenshotEntity
import com.example.data.local.entity.SyncOutboxEntity
import com.example.data.model.BatchImportJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader
import java.util.UUID

class EvidenceRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context)
) {
    private val zipManager = ZipEncryptionManager(context)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val batchAdapter = moshi.adapter(BatchImportJson::class.java)

    fun getAllBatches(): Flow<List<BatchEntity>> = database.batchDao().getAllBatchesFlow()

    fun getLinksForBatch(batchId: String): Flow<List<LinkItemEntity>> =
        database.linkDao().getLinksByBatchFlow(batchId)

    fun getLinksForBatchAndStatus(batchId: String, status: String): Flow<List<LinkItemEntity>> =
        database.linkDao().getLinksByBatchAndStatusFlow(batchId, status)

    fun getScreenshotsForLink(batchId: String, linkId: String): Flow<List<ScreenshotEntity>> =
        database.screenshotDao().getScreenshotsForLinkFlow(batchId, linkId)

    fun getAllExports(): Flow<List<ExportRecordEntity>> = database.exportDao().getAllExportsFlow()

    fun getPendingSyncCount(): Flow<Int> = database.syncOutboxDao().getPendingCountFlow()

    suspend fun getBatch(batchId: String): BatchEntity? = database.batchDao().getBatchById(batchId)

    suspend fun getLink(batchId: String, linkId: String): LinkItemEntity? =
        database.linkDao().getLink(batchId, linkId)

    /**
     * Imports a JSON batch into Room database.
     */
    suspend fun importBatchFromJson(jsonString: String, currentUserId: String): BatchEntity =
        withContext(Dispatchers.IO) {
            val batchData = try {
                batchAdapter.fromJson(jsonString)
            } catch (e: Exception) {
                // If it's a raw list of links
                val listType = com.squareup.moshi.Types.newParameterizedType(
                    List::class.java,
                    com.example.data.model.LinkImportJson::class.java
                )
                val listAdapter = moshi.adapter<List<com.example.data.model.LinkImportJson>>(listType)
                val list = listAdapter.fromJson(jsonString)
                if (list != null) {
                    BatchImportJson(
                        batchId = "BATCH-${System.currentTimeMillis() % 100000}",
                        batchName = "Imported Batch",
                        links = list
                    )
                } else {
                    throw IllegalArgumentException("Invalid batch JSON format: ${e.message}")
                }
            } ?: throw IllegalArgumentException("Failed to parse batch JSON.")

            val batchId = batchData.batchId?.ifBlank { null }
                ?: "BATCH-${System.currentTimeMillis() % 100000}"
            val batchName = batchData.batchName?.ifBlank { null } ?: "Batch $batchId"
            val linksList = batchData.links ?: emptyList()

            // Map links
            val linkEntities = linksList.map { linkJson ->
                val detectedPlatform = linkJson.platform ?: detectPlatformFromUrl(linkJson.url)
                LinkItemEntity(
                    linkId = linkJson.linkId.trim(),
                    batchId = batchId,
                    url = linkJson.url.trim(),
                    platform = detectedPlatform,
                    profileId = linkJson.profileId,
                    postIdentifier = linkJson.postIdentifier,
                    notes = linkJson.notes,
                    screenshotCount = 0,
                    requiredCount = linkJson.requiredScreenshots ?: 1,
                    status = "PENDING",
                    lastUpdatedAt = System.currentTimeMillis()
                )
            }

            val batchEntity = BatchEntity(
                batchId = batchId,
                name = batchName,
                importedAt = System.currentTimeMillis(),
                totalLinks = linkEntities.size,
                completedLinks = 0,
                status = "PENDING",
                createdByUid = currentUserId,
                rawJson = jsonString
            )

            // Save to Room
            database.batchDao().insertBatch(batchEntity)
            database.linkDao().insertLinks(linkEntities)

            // Update completed count
            database.batchDao().updateBatchCompletedCount(batchId)

            // Enqueue sync event
            val syncPayload = """{"batchId":"$batchId","name":"$batchName","totalLinks":${linkEntities.size}}"""
            database.syncOutboxDao().insertOperation(
                SyncOutboxEntity(
                    operationId = UUID.randomUUID().toString(),
                    eventType = "BATCH_IMPORTED",
                    batchId = batchId,
                    payloadJson = syncPayload
                )
            )

            batchEntity
        }

    suspend fun importBatchFromUri(uri: Uri, currentUserId: String): BatchEntity =
        withContext(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Cannot open file from Uri.")
            val jsonString = InputStreamReader(inputStream).use { it.readText() }
            importBatchFromJson(jsonString, currentUserId)
        }

    fun detectPlatformFromUrl(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains("instagram.com") -> "Instagram"
            lower.contains("facebook.com") || lower.contains("fb.watch") -> "Facebook"
            lower.contains("tiktok.com") -> "TikTok"
            lower.contains("youtube.com") || lower.contains("youtu.be") -> "YouTube"
            lower.contains("twitter.com") || lower.contains("x.com") -> "X"
            else -> "Other"
        }
    }

    /**
     * Deletes a screenshot file and updates Room stats.
     */
    suspend fun deleteScreenshot(screenshot: ScreenshotEntity): Unit = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, screenshot.localFilePath)
        if (file.exists()) {
            file.delete()
        }
        database.screenshotDao().softDeleteScreenshot(screenshot.screenshotId)

        val remainingCount = database.screenshotDao().getScreenshotCountForLink(screenshot.batchId, screenshot.linkId)
        val link = database.linkDao().getLink(screenshot.batchId, screenshot.linkId)
        val newStatus = if (link != null && remainingCount >= link.requiredCount) {
            "COMPLETED"
        } else if (remainingCount > 0) {
            "IN_PROGRESS"
        } else {
            "PENDING"
        }

        database.linkDao().updateLinkScreenshotStats(
            batchId = screenshot.batchId,
            linkId = screenshot.linkId,
            count = remainingCount,
            status = newStatus,
            updatedAt = System.currentTimeMillis()
        )
        database.batchDao().updateBatchCompletedCount(screenshot.batchId)
    }

    /**
     * Exports batch evidence into an AES-256 encrypted ZIP.
     */
    suspend fun exportBatchEvidence(
        batchId: String,
        password: String,
        currentUserId: String
    ): ExportResult = withContext(Dispatchers.IO) {
        val links = database.linkDao().getLinksByBatchFlow(batchId)
        // Collect links synchronously for export
        val linkList = mutableListOf<LinkItemEntity>()
        // Read from DB directly
        val rawBatch = database.batchDao().getBatchById(batchId)
            ?: throw IllegalArgumentException("Batch $batchId not found.")

        // We can query links directly
        val allBatchScreenshots = database.screenshotDao().getAllScreenshotsForBatch(batchId)
        val screenshotsByLink = allBatchScreenshots.groupBy { it.linkId }

        // Fetch all links for this batch
        val allLinks = mutableListOf<Pair<LinkItemEntity, List<Pair<ScreenshotEntity, File>>>>()

        // Get link items through DAO
        val linksFromDb = mutableListOf<LinkItemEntity>()
        // Query links directly
        val count = database.linkDao().getCountByBatch(batchId)
        // Load links
        // We'll query links for batch via a direct helper
        val linksInBatch = allBatchScreenshots.map { it.linkId }.distinct()

        for (linkId in linksInBatch) {
            val linkItem = database.linkDao().getLink(batchId, linkId)
            if (linkItem != null) {
                val scrs = screenshotsByLink[linkId] ?: emptyList()
                val scrsWithFiles = scrs.mapNotNull { scr ->
                    val file = File(context.filesDir, scr.localFilePath)
                    if (file.exists()) scr to file else null
                }
                allLinks.add(linkItem to scrsWithFiles)
            }
        }

        if (allLinks.isEmpty()) {
            throw IllegalStateException("No captured evidence found in batch $batchId to export.")
        }

        val exportId = "EXP-${System.currentTimeMillis() % 1000000}"
        val result = zipManager.createEncryptedZip(
            batchId = batchId,
            exportId = exportId,
            exportingUserUid = currentUserId,
            password = password,
            links = allLinks
        )

        // Save export record
        val exportRecord = ExportRecordEntity(
            exportId = exportId,
            batchId = batchId,
            fileName = result.zipFile.name,
            filePath = result.zipFile.absolutePath,
            createdAt = System.currentTimeMillis(),
            linkCount = result.linkCount,
            screenshotCount = result.screenshotCount,
            fileSizeBytes = result.fileSizeBytes,
            encryptionAlgorithm = "AES-256",
            manifestSha256 = result.manifestSha256,
            archiveSha256 = result.archiveSha256,
            validationResult = "VALID",
            syncStatus = "PENDING"
        )

        database.exportDao().insertExport(exportRecord)

        // Update batch status
        database.batchDao().updateBatch(rawBatch.copy(status = "EXPORTED"))

        // Enqueue sync event
        val syncPayload = """
            {
                "exportId": "$exportId",
                "batchId": "$batchId",
                "fileName": "${result.zipFile.name}",
                "archiveSha256": "${result.archiveSha256}",
                "manifestSha256": "${result.manifestSha256}",
                "linkCount": ${result.linkCount},
                "screenshotCount": ${result.screenshotCount},
                "fileSize": ${result.fileSizeBytes}
            }
        """.trimIndent()

        database.syncOutboxDao().insertOperation(
            SyncOutboxEntity(
                operationId = UUID.randomUUID().toString(),
                eventType = "EXPORT_CREATED",
                batchId = batchId,
                payloadJson = syncPayload
            )
        )

        result
    }

    suspend fun markExportSharedToWhatsApp(exportId: String) = withContext(Dispatchers.IO) {
        database.exportDao().markSharedToWhatsApp(exportId)
    }

    suspend fun markExportSavedToStorage(exportId: String) = withContext(Dispatchers.IO) {
        database.exportDao().markSavedToStorage(exportId)
    }

    /**
     * Calculates storage metrics for Evidence & Exports
     */
    suspend fun getStorageMetrics(): StorageMetrics = withContext(Dispatchers.IO) {
        val evidenceDir = File(context.filesDir, "evidence")
        val exportsDir = File(context.filesDir, "exports")

        val evidenceBytes = getFolderSize(evidenceDir)
        val exportsBytes = getFolderSize(exportsDir)
        val freeBytes = context.filesDir.freeSpace
        val totalBytes = context.filesDir.totalSpace

        StorageMetrics(
            evidenceSizeBytes = evidenceBytes,
            exportsSizeBytes = exportsBytes,
            freeDiskBytes = freeBytes,
            totalDiskBytes = totalBytes
        )
    }

    private fun getFolderSize(dir: File): Long {
        var size = 0L
        if (dir.exists()) {
            dir.walkTopDown().forEach { file ->
                if (file.isFile) size += file.length()
            }
        }
        return size
    }
}

data class StorageMetrics(
    val evidenceSizeBytes: Long,
    val exportsSizeBytes: Long,
    val freeDiskBytes: Long,
    val totalDiskBytes: Long
)
