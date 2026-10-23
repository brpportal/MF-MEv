package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.net.Uri
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.browser.CaptureManager
import com.example.browser.PlatformSessionManager
import com.example.browser.SessionStatus
import com.example.browser.SupportedPlatform
import com.example.data.crypto.ChecksumUtil
import com.example.data.crypto.ExportResult
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BatchEntity
import com.example.data.local.entity.ExportRecordEntity
import com.example.data.local.entity.LinkItemEntity
import com.example.data.local.entity.ScreenshotEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.EvidenceRepository
import com.example.data.repository.EvidenceUser
import com.example.data.repository.StorageMetrics
import com.example.data.repository.SyncRepository
import com.example.data.repository.SyncSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import android.util.Log

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository(application)

    val currentUser: StateFlow<EvidenceUser?> = repository.currentUser
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        repository.attemptSilentSignIn(viewModelScope)
    }

    fun signIn(activity: Activity) {
        _isLoading.value = true
        _errorMessage.value = null
        repository.signInWithGoogle(
            activity = activity,
            scope = viewModelScope,
            onSuccess = {
                _isLoading.value = false
            },
            onError = { err ->
                _isLoading.value = false
                _errorMessage.value = err
            },
            onCancelled = {
                _isLoading.value = false
            }
        )
    }

    fun signInWithEmail(email: String, password: String) {
        _isLoading.value = true
        _errorMessage.value = null
        repository.signInWithEmail(
            email = email,
            password = password,
            scope = viewModelScope,
            onSuccess = {
                _isLoading.value = false
            },
            onError = { err ->
                _isLoading.value = false
                _errorMessage.value = err
            }
        )
    }

    fun sendPasswordReset(email: String, onResetSent: () -> Unit) {
        _isLoading.value = true
        _errorMessage.value = null
        repository.sendPasswordReset(
            email = email,
            scope = viewModelScope,
            onSuccess = {
                _isLoading.value = false
                onResetSent()
            },
            onError = { err ->
                _isLoading.value = false
                _errorMessage.value = err
            }
        )
    }

    fun verifyAndAccessWithPairingCode(email: String, code: String, onSuccess: (com.example.data.repository.EvidenceUser) -> Unit = {}) {
        _isLoading.value = true
        _errorMessage.value = null
        repository.verifyAndAccessWithPairingCode(
            email = email,
            code = code,
            scope = viewModelScope,
            onSuccess = { user ->
                _isLoading.value = false
                onSuccess(user)
            },
            onError = { err ->
                _isLoading.value = false
                _errorMessage.value = err
            }
        )
    }

    fun signOut(onComplete: () -> Unit = {}) {
        repository.signOut(viewModelScope) {
            onComplete()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

class LinkingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = com.example.data.repository.LinkingRepository(application)
    private val authRepo = com.example.data.repository.AuthRepository(application)
    
    val registrationStatus = authRepo.currentUser.flatMapLatest { user ->
        val effectiveId = user?.uid ?: if (repository.isActivated()) "LOCAL_OFFICER" else ""
        if (effectiveId.isNotBlank()) {
            repository.getRegistrationStatus(effectiveId)
        } else {
            kotlinx.coroutines.flow.flowOf(com.example.data.repository.RegistrationStatus.NOT_LINKED)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        if (repository.isActivated()) com.example.data.repository.RegistrationStatus.ACTIVATED else com.example.data.repository.RegistrationStatus.CHECKING
    )

    private val _pairingError = MutableStateFlow<String?>(null)
    val pairingError: StateFlow<String?> = _pairingError.asStateFlow()

    private val _isPairing = MutableStateFlow(false)
    val isPairing: StateFlow<Boolean> = _isPairing.asStateFlow()

    fun pairApp(code: String, email: String? = null) {
        val effectiveEmail = email?.trim() ?: authRepo.currentUser.value?.email ?: ""
        if (effectiveEmail.isBlank()) {
            _pairingError.value = "Please enter your officer email address to verify pairing code."
            return
        }
        viewModelScope.launch {
            _isPairing.value = true
            _pairingError.value = null
            val result = repository.pairWithEmailAndCode(effectiveEmail, code)
            _isPairing.value = false
            if (result.isSuccess) {
                authRepo.attemptSilentSignIn(viewModelScope)
            } else {
                _pairingError.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to verify pairing code with Supabase."
            }
        }
    }

    fun activateOfflineMode() {
        val user = authRepo.currentUser.value
        val uid = user?.uid ?: "USR-OFFICER"
        repository.activateLocally(uid)
    }

    fun clearError() {
        _pairingError.value = null
    }
}


class BatchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EvidenceRepository(application)
    private val syncRepository = SyncRepository(application)

    val batches: StateFlow<List<BatchEntity>> = repository.getAllBatches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncCount: StateFlow<Int> = repository.getPendingSyncCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allExports: StateFlow<List<ExportRecordEntity>> = repository.getAllExports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedBatchId = MutableStateFlow<String?>(null)
    val selectedBatchId: StateFlow<String?> = _selectedBatchId.asStateFlow()

    private val _links = MutableStateFlow<List<LinkItemEntity>>(emptyList())
    val links: StateFlow<List<LinkItemEntity>> = _links.asStateFlow()

    private val _statusFilter = MutableStateFlow("ALL")
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    private val _platformFilter = MutableStateFlow("ALL")
    val platformFilter: StateFlow<String> = _platformFilter.asStateFlow()

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val _storageMetrics = MutableStateFlow<StorageMetrics?>(null)
    val storageMetrics: StateFlow<StorageMetrics?> = _storageMetrics.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    init {
        refreshStorageMetrics()
    }

    fun selectBatch(batchId: String) {
        _selectedBatchId.value = batchId
        loadLinksForBatch(batchId)
    }

    fun setStatusFilter(status: String) {
        _statusFilter.value = status
        _selectedBatchId.value?.let { loadLinksForBatch(it) }
    }

    fun setPlatformFilter(platform: String) {
        _platformFilter.value = platform
        _selectedBatchId.value?.let { loadLinksForBatch(it) }
    }

    private fun loadLinksForBatch(batchId: String) {
        viewModelScope.launch {
            repository.getLinksForBatch(batchId).collect { list ->
                var filtered = list
                if (_statusFilter.value != "ALL") {
                    filtered = filtered.filter { it.status == _statusFilter.value }
                }
                if (_platformFilter.value != "ALL") {
                    filtered = filtered.filter { it.platform.equals(_platformFilter.value, ignoreCase = true) }
                }
                _links.value = filtered
            }
        }
    }

    fun importBatchFromJson(jsonString: String, currentUserId: String) {
        viewModelScope.launch {
            _importState.value = ImportState.Loading
            try {
                val batch = repository.importBatchFromJson(jsonString, currentUserId)
                _importState.value = ImportState.Success(batch)
                selectBatch(batch.batchId)
                refreshStorageMetrics()
            } catch (e: Exception) {
                _importState.value = ImportState.Error(e.localizedMessage ?: "Failed to import JSON.")
            }
        }
    }

    fun importBatchFromUri(uri: Uri, currentUserId: String) {
        viewModelScope.launch {
            _importState.value = ImportState.Loading
            try {
                val batch = repository.importBatchFromUri(uri, currentUserId)
                _importState.value = ImportState.Success(batch)
                selectBatch(batch.batchId)
                refreshStorageMetrics()
            } catch (e: Exception) {
                _importState.value = ImportState.Error(e.localizedMessage ?: "Failed to import file.")
            }
        }
    }

    fun resetImportState() {
        _importState.value = ImportState.Idle
    }

    fun refreshStorageMetrics() {
        viewModelScope.launch {
            _storageMetrics.value = repository.getStorageMetrics()
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            try {
                val summary = syncRepository.executeSyncNow()
                _syncState.value = SyncState.Completed(summary)
            } catch (e: Exception) {
                _syncState.value = SyncState.Failed(e.localizedMessage ?: "Sync failed.")
            }
        }
    }
}

sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState()
    data class Success(val batch: BatchEntity) : ImportState()
    data class Error(val message: String) : ImportState()
}

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Completed(val summary: SyncSummary) : SyncState()
    data class Failed(val error: String) : SyncState()
}

class CaptureViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = EvidenceRepository(application)
    private val captureManager = CaptureManager(application, database)

    private val _currentLink = MutableStateFlow<LinkItemEntity?>(null)
    val currentLink: StateFlow<LinkItemEntity?> = _currentLink.asStateFlow()

    private val _screenshots = MutableStateFlow<List<ScreenshotEntity>>(emptyList())
    val screenshots: StateFlow<List<ScreenshotEntity>> = _screenshots.asStateFlow()

    private val _captureState = MutableStateFlow<CaptureState>(CaptureState.Idle)
    val captureState: StateFlow<CaptureState> = _captureState.asStateFlow()

    fun loadLink(batchId: String, linkId: String) {
        viewModelScope.launch {
            val link = repository.getLink(batchId, linkId)
            _currentLink.value = link
            repository.getScreenshotsForLink(batchId, linkId).collect {
                _screenshots.value = it
            }
        }
    }

    fun captureScreenshot(
        webView: WebView,
        userId: String
    ) {
        val link = _currentLink.value ?: return
        viewModelScope.launch {
            _captureState.value = CaptureState.Capturing
            try {
                val saved = captureManager.captureVisibleViewport(
                    webView = webView,
                    batchId = link.batchId,
                    linkId = link.linkId,
                    platform = link.platform,
                    originalUrl = link.url,
                    userId = userId
                )
                _captureState.value = CaptureState.Success(saved)
                // Reload link stats
                _currentLink.value = repository.getLink(link.batchId, link.linkId)
            } catch (e: Exception) {
                _captureState.value = CaptureState.Error(e.localizedMessage ?: "Capture failed.")
            }
        }
    }

    fun deleteScreenshot(screenshot: ScreenshotEntity) {
        viewModelScope.launch {
            repository.deleteScreenshot(screenshot)
            _currentLink.value?.let { link ->
                _currentLink.value = repository.getLink(link.batchId, link.linkId)
            }
        }
    }

    fun clearCaptureState() {
        _captureState.value = CaptureState.Idle
    }
}

sealed class CaptureState {
    object Idle : CaptureState()
    object Capturing : CaptureState()
    data class Success(val screenshot: ScreenshotEntity) : CaptureState()
    data class Error(val message: String) : CaptureState()
}

class ExportViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EvidenceRepository(application)
    private val context = application

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _generatedPassword = MutableStateFlow(ChecksumUtil.generateSecurePassword())
    val generatedPassword: StateFlow<String> = _generatedPassword.asStateFlow()

    private val _isAdminPasswordManaged = MutableStateFlow(true)
    val isAdminPasswordManaged: StateFlow<Boolean> = _isAdminPasswordManaged.asStateFlow()

    init {
        viewModelScope.launch {
            val adminPwd = com.example.data.SupabaseClient.getAdminZipPassword(context)
            if (!adminPwd.isNullOrBlank()) {
                _generatedPassword.value = adminPwd
                _isAdminPasswordManaged.value = true
            }
        }
    }

    fun regeneratePassword() {
        _generatedPassword.value = ChecksumUtil.generateSecurePassword()
    }

    fun setCustomPassword(pwd: String) {
        _generatedPassword.value = pwd
    }

    fun exportBatch(batchId: String, password: String, userId: String) {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting
            try {
                val result = repository.exportBatchEvidence(
                    batchId = batchId,
                    password = password,
                    currentUserId = userId
                )
                _exportState.value = ExportState.Success(result)

                // Sync export details directly to Supabase table evidence_exports for Admin Panel extraction & management
                try {
                    val exportDoc = mapOf(
                        "export_id" to result.exportId,
                        "batch_id" to batchId,
                        "file_name" to result.zipFile.name,
                        "manifest_sha256" to result.manifestSha256,
                        "archive_sha256" to result.archiveSha256,
                        "file_size_bytes" to result.fileSizeBytes,
                        "link_count" to result.linkCount,
                        "screenshot_count" to result.screenshotCount,
                        "created_by_user" to userId,
                        "admin_manageable_only" to true,
                        "encryption_algorithm" to "AES-256"
                    )
                    com.example.data.SupabaseClient.syncRecord(context, "evidence_exports", exportDoc)
                } catch (e: Exception) {
                    Log.w("ExportViewModel", "Failed to sync export record to Supabase: ${e.message}")
                }

            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.localizedMessage ?: "Export failed.")
            }
        }
    }

    fun markSharedToWhatsApp(exportId: String) {
        viewModelScope.launch {
            repository.markExportSharedToWhatsApp(exportId)
        }
    }

    fun markSavedToStorage(exportId: String) {
        viewModelScope.launch {
            repository.markExportSavedToStorage(exportId)
        }
    }

    fun resetState() {
        _exportState.value = ExportState.Idle
    }
}

sealed class ExportState {
    object Idle : ExportState()
    object Exporting : ExportState()
    data class Success(val result: ExportResult) : ExportState()
    data class Error(val message: String) : ExportState()
}

class PlatformViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = PlatformSessionManager(application)

    val platforms: StateFlow<List<SupportedPlatform>> = sessionManager.platforms

    fun refreshSessions() {
        sessionManager.refreshAllSessionStatuses()
    }

    fun logoutPlatform(platformId: String) {
        sessionManager.logoutPlatform(platformId)
    }

    fun clearAllBrowserSessions(onComplete: () -> Unit) {
        sessionManager.clearAllBrowserSessions(onComplete)
    }
}
