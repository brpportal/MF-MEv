package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.BatchEntity
import com.example.data.local.entity.ExportRecordEntity
import com.example.data.local.entity.LinkItemEntity
import com.example.data.local.entity.ScreenshotEntity
import com.example.data.local.entity.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BatchDao {
    @Query("SELECT * FROM batches ORDER BY importedAt DESC")
    fun getAllBatchesFlow(): Flow<List<BatchEntity>>

    @Query("SELECT * FROM batches WHERE batchId = :batchId LIMIT 1")
    suspend fun getBatchById(batchId: String): BatchEntity?

    @Query("SELECT * FROM batches WHERE batchId = :batchId LIMIT 1")
    fun getBatchByIdFlow(batchId: String): Flow<BatchEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: BatchEntity)

    @Update
    suspend fun updateBatch(batch: BatchEntity)

    @Query("UPDATE batches SET completedLinks = (SELECT COUNT(*) FROM link_items WHERE batchId = :batchId AND status = 'COMPLETED') WHERE batchId = :batchId")
    suspend fun updateBatchCompletedCount(batchId: String)

    @Query("DELETE FROM batches WHERE batchId = :batchId")
    suspend fun deleteBatch(batchId: String)
}

@Dao
interface LinkDao {
    @Query("SELECT * FROM link_items WHERE batchId = :batchId ORDER BY linkId ASC")
    fun getLinksByBatchFlow(batchId: String): Flow<List<LinkItemEntity>>

    @Query("SELECT * FROM link_items WHERE batchId = :batchId AND status = :status ORDER BY linkId ASC")
    fun getLinksByBatchAndStatusFlow(batchId: String, status: String): Flow<List<LinkItemEntity>>

    @Query("SELECT * FROM link_items WHERE batchId = :batchId AND platform = :platform ORDER BY linkId ASC")
    fun getLinksByBatchAndPlatformFlow(batchId: String, platform: String): Flow<List<LinkItemEntity>>

    @Query("SELECT * FROM link_items WHERE batchId = :batchId AND linkId = :linkId LIMIT 1")
    suspend fun getLink(batchId: String, linkId: String): LinkItemEntity?

    @Query("SELECT * FROM link_items WHERE batchId = :batchId AND linkId = :linkId LIMIT 1")
    fun getLinkFlow(batchId: String, linkId: String): Flow<LinkItemEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLinks(links: List<LinkItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLink(link: LinkItemEntity)

    @Update
    suspend fun updateLink(link: LinkItemEntity)

    @Query("UPDATE link_items SET screenshotCount = :count, status = :status, lastUpdatedAt = :updatedAt WHERE batchId = :batchId AND linkId = :linkId")
    suspend fun updateLinkScreenshotStats(batchId: String, linkId: String, count: Int, status: String, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM link_items WHERE batchId = :batchId")
    suspend fun getCountByBatch(batchId: String): Int

    @Query("SELECT COUNT(*) FROM link_items WHERE batchId = :batchId AND status = 'COMPLETED'")
    suspend fun getCompletedCountByBatch(batchId: String): Int
}

@Dao
interface ScreenshotDao {
    @Query("SELECT * FROM screenshots WHERE batchId = :batchId AND linkId = :linkId AND captureStatus = 'SAVED' ORDER BY capturedAt ASC")
    fun getScreenshotsForLinkFlow(batchId: String, linkId: String): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots WHERE batchId = :batchId AND linkId = :linkId AND captureStatus = 'SAVED' ORDER BY capturedAt ASC")
    suspend fun getScreenshotsForLink(batchId: String, linkId: String): List<ScreenshotEntity>

    @Query("SELECT * FROM screenshots WHERE batchId = :batchId AND captureStatus = 'SAVED' ORDER BY capturedAt DESC")
    fun getAllScreenshotsForBatchFlow(batchId: String): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots WHERE batchId = :batchId AND captureStatus = 'SAVED'")
    suspend fun getAllScreenshotsForBatch(batchId: String): List<ScreenshotEntity>

    @Query("SELECT COUNT(*) FROM screenshots WHERE batchId = :batchId AND linkId = :linkId AND captureStatus = 'SAVED'")
    suspend fun getScreenshotCountForLink(batchId: String, linkId: String): Int

    @Query("SELECT COUNT(*) FROM screenshots WHERE batchId = :batchId AND captureStatus = 'SAVED'")
    suspend fun getTotalScreenshotsForBatch(batchId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreenshot(screenshot: ScreenshotEntity)

    @Query("UPDATE screenshots SET captureStatus = 'DELETED' WHERE screenshotId = :screenshotId")
    suspend fun softDeleteScreenshot(screenshotId: String)

    @Query("UPDATE screenshots SET syncStatus = :syncStatus WHERE screenshotId = :screenshotId")
    suspend fun updateSyncStatus(screenshotId: String, syncStatus: String)

    @Query("SELECT * FROM screenshots WHERE screenshotId = :screenshotId LIMIT 1")
    suspend fun getScreenshotById(screenshotId: String): ScreenshotEntity?
}

@Dao
interface SyncOutboxDao {
    @Query("SELECT * FROM sync_outbox WHERE status IN ('PENDING', 'FAILED') ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingOperations(limit: Int = 50): List<SyncOutboxEntity>

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE status IN ('PENDING', 'FAILED')")
    fun getPendingCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(op: SyncOutboxEntity)

    @Update
    suspend fun updateOperation(op: SyncOutboxEntity)

    @Query("UPDATE sync_outbox SET status = :status, lastAttemptAt = :timestamp, errorMessage = :error WHERE operationId = :opId")
    suspend fun updateStatus(opId: String, status: String, timestamp: Long, error: String? = null)

    @Query("DELETE FROM sync_outbox WHERE status = 'SUCCESS'")
    suspend fun purgeCompletedOperations()
}

@Dao
interface ExportDao {
    @Query("SELECT * FROM export_records ORDER BY createdAt DESC")
    fun getAllExportsFlow(): Flow<List<ExportRecordEntity>>

    @Query("SELECT * FROM export_records WHERE exportId = :exportId LIMIT 1")
    suspend fun getExportById(exportId: String): ExportRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExport(export: ExportRecordEntity)

    @Update
    suspend fun updateExport(export: ExportRecordEntity)

    @Query("UPDATE export_records SET sharedToWhatsApp = 1 WHERE exportId = :exportId")
    suspend fun markSharedToWhatsApp(exportId: String)

    @Query("UPDATE export_records SET savedToStorage = 1 WHERE exportId = :exportId")
    suspend fun markSavedToStorage(exportId: String)
}
