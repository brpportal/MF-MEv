package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "batches")
data class BatchEntity(
    @PrimaryKey val batchId: String,
    val name: String,
    val importedAt: Long = System.currentTimeMillis(),
    val totalLinks: Int = 0,
    val completedLinks: Int = 0,
    val status: String = "PENDING", // PENDING, IN_PROGRESS, COMPLETED, EXPORTED
    val createdByUid: String = "",
    val rawJson: String? = null
)

@Entity(
    tableName = "link_items",
    primaryKeys = ["batchId", "linkId"],
    indices = [
        Index(value = ["batchId"]),
        Index(value = ["status"]),
        Index(value = ["platform"])
    ]
)
data class LinkItemEntity(
    val linkId: String,
    val batchId: String,
    val url: String,
    val platform: String = "Other",
    val profileId: String? = null,
    val postIdentifier: String? = null,
    val notes: String? = null,
    val screenshotCount: Int = 0,
    val requiredCount: Int = 1,
    val status: String = "PENDING", // PENDING, IN_PROGRESS, COMPLETED, FAILED
    val lastUpdatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "screenshots",
    indices = [
        Index(value = ["linkId"]),
        Index(value = ["batchId"]),
        Index(value = ["syncStatus"])
    ]
)
data class ScreenshotEntity(
    @PrimaryKey val screenshotId: String, // e.g., A01-SCR-0001
    val internalId: String, // UUID
    val linkId: String,
    val batchId: String,
    val userId: String,
    val platform: String,
    val originalUrl: String,
    val capturedAt: Long = System.currentTimeMillis(),
    val localFilePath: String, // Relative path inside app files dir
    val mimeType: String = "image/jpeg",
    val fileSize: Long = 0L,
    val sha256Checksum: String = "",
    val captureStatus: String = "SAVED", // SAVED, CORRUPTED, DELETED
    val syncStatus: String = "PENDING" // PENDING, SYNCED, FAILED
)

@Entity(
    tableName = "sync_outbox",
    indices = [
        Index(value = ["status"]),
        Index(value = ["batchId"])
    ]
)
data class SyncOutboxEntity(
    @PrimaryKey val operationId: String, // UUID
    val eventType: String, // SCREENSHOT_CAPTURED, LINK_COMPLETED, EXPORT_CREATED, BATCH_IMPORTED
    val batchId: String,
    val linkId: String? = null,
    val screenshotId: String? = null,
    val payloadJson: String,
    val status: String = "PENDING", // PENDING, IN_PROGRESS, SUCCESS, FAILED
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAttemptAt: Long = 0L,
    val errorMessage: String? = null
)

@Entity(
    tableName = "export_records",
    indices = [
        Index(value = ["batchId"])
    ]
)
data class ExportRecordEntity(
    @PrimaryKey val exportId: String,
    val batchId: String,
    val fileName: String,
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis(),
    val linkCount: Int,
    val screenshotCount: Int,
    val fileSizeBytes: Long,
    val encryptionAlgorithm: String = "AES-256",
    val manifestSha256: String = "",
    val archiveSha256: String = "",
    val validationResult: String = "VALID", // VALID, FAILED
    val syncStatus: String = "PENDING",
    val sharedToWhatsApp: Boolean = false,
    val savedToStorage: Boolean = false
)
