package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.SupabaseClient
import com.example.data.local.AppDatabase
import com.example.data.local.entity.SyncOutboxEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SyncRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context)
) {
    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val PREFS_SYNC = "mf_sync_settings"
        private const val KEY_AUTO_SYNC = "is_auto_sync_enabled"

        fun isAutoSync(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_AUTO_SYNC, true) // default is automatic
        }

        fun setAutoSync(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
        }
    }

    fun scheduleBackgroundSync() {
        if (!isAutoSync(context)) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSyncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "EvidenceMetadataSync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
    }

    suspend fun queueOperation(
        eventType: String,
        batchId: String?,
        linkId: String?,
        screenshotId: String?,
        payloadJson: String
    ) = withContext(Dispatchers.IO) {
        val op = SyncOutboxEntity(
            operationId = java.util.UUID.randomUUID().toString(),
            eventType = eventType,
            batchId = batchId ?: "",
            linkId = linkId,
            screenshotId = screenshotId,
            payloadJson = payloadJson,
            createdAt = System.currentTimeMillis(),
            status = "PENDING"
        )
        database.syncOutboxDao().insertOperation(op)
    }

    fun triggerImmediateSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            "ImmediateEvidenceSync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    suspend fun executeSyncNow(): SyncSummary = withContext(Dispatchers.IO) {
        val pendingOps = database.syncOutboxDao().getPendingOperations(50)
        var successful = 0
        var failed = 0

        for (op in pendingOps) {
            try {
                val json = JSONObject(op.payloadJson)
                val map = mutableMapOf<String, Any>()
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = json.get(key)
                }
                map["operation_id"] = op.operationId
                map["batch_id"] = op.batchId ?: ""

                val targetTable = when (op.eventType) {
                    "SCREENSHOT_CAPTURED" -> "evidence_screenshots"
                    "EXPORT_CREATED" -> "evidence_exports"
                    "BATCH_IMPORTED" -> "evidence_batches"
                    else -> "evidence_workflow_events"
                }

                // Sync to Supabase PostgREST table
                val ok = SupabaseClient.syncRecord(context, targetTable, map)

                if (ok) {
                    database.syncOutboxDao().updateStatus(
                        opId = op.operationId,
                        status = "SUCCESS",
                        timestamp = System.currentTimeMillis(),
                        error = null
                    )

                    if (!op.screenshotId.isNullOrBlank()) {
                        database.screenshotDao().updateSyncStatus(op.screenshotId, "SYNCED")
                    }
                    successful++
                } else {
                    failed++
                }
            } catch (e: Exception) {
                Log.e("SyncRepository", "Sync failed for op: ${op.operationId}", e)
                database.syncOutboxDao().updateStatus(
                    opId = op.operationId,
                    status = "FAILED",
                    timestamp = System.currentTimeMillis(),
                    error = e.localizedMessage
                )
                failed++
            }
        }

        SyncSummary(
            totalProcessed = pendingOps.size,
            successfulCount = successful,
            failedCount = failed
        )
    }
}

data class SyncSummary(
    val totalProcessed: Int,
    val successfulCount: Int,
    val failedCount: Int
)

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val syncRepo = SyncRepository(applicationContext)
        return try {
            val summary = syncRepo.executeSyncNow()
            if (summary.failedCount > 0 && summary.successfulCount == 0) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error in sync worker", e)
            Result.retry()
        }
    }
}
