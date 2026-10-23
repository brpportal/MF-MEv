package com.example.data.crypto

import android.content.Context
import com.example.data.model.EvidenceManifest
import com.example.data.model.ManifestLinkItem
import com.example.data.model.ManifestScreenshotItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File
import java.io.IOException

data class ExportResult(
    val exportId: String,
    val batchId: String,
    val zipFile: File,
    val manifestSha256: String,
    val archiveSha256: String,
    val fileSizeBytes: Long,
    val linkCount: Int,
    val screenshotCount: Int,
    val manifest: EvidenceManifest
)

class ZipEncryptionManager(private val context: Context) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val manifestAdapter = moshi.adapter(EvidenceManifest::class.java)

    /**
     * Builds and exports a genuinely AES-256 encrypted ZIP archive.
     */
    fun createEncryptedZip(
        batchId: String,
        exportId: String,
        exportingUserUid: String,
        password: String,
        links: List<Pair<com.example.data.local.entity.LinkItemEntity, List<Pair<com.example.data.local.entity.ScreenshotEntity, File>>>>
    ): ExportResult {
        if (password.length < 8) {
            throw IllegalArgumentException("Archive password must be at least 8 characters.")
        }

        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val tempDir = File(context.cacheDir, "temp_export_$exportId").apply { mkdirs() }

        val safeBatchId = batchId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val safeExportId = exportId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val zipFileName = "MF_Evidence_${safeBatchId}_${safeExportId}.zip"
        val targetZipFile = File(exportDir, zipFileName)

        if (targetZipFile.exists()) {
            targetZipFile.delete()
        }

        try {
            // Check available disk space (require at least 50MB free)
            val freeBytes = context.filesDir.freeSpace
            if (freeBytes < 50 * 1024 * 1024) {
                throw IOException("Insufficient local storage space for evidence export.")
            }

            // 1. Prepare Manifest items
            val manifestLinks = mutableListOf<ManifestLinkItem>()
            var totalScreenshots = 0

            for ((linkEntity, screenshots) in links) {
                val manifestScreenshots = mutableListOf<ManifestScreenshotItem>()
                for ((scrEntity, file) in screenshots) {
                    if (file.exists() && file.isFile) {
                        val checksum = scrEntity.sha256Checksum.ifBlank {
                            ChecksumUtil.computeSha256(file)
                        }
                        manifestScreenshots.add(
                            ManifestScreenshotItem(
                                screenshotId = scrEntity.screenshotId,
                                fileName = file.name,
                                capturedAt = scrEntity.capturedAt,
                                fileSizeBytes = file.length(),
                                sha256Checksum = checksum,
                                mimeType = scrEntity.mimeType
                            )
                        )
                        totalScreenshots++
                    }
                }

                manifestLinks.add(
                    ManifestLinkItem(
                        linkId = linkEntity.linkId,
                        url = linkEntity.url,
                        platform = linkEntity.platform,
                        profileId = linkEntity.profileId,
                        requiredCount = linkEntity.requiredCount,
                        actualCount = manifestScreenshots.size,
                        status = linkEntity.status,
                        screenshots = manifestScreenshots
                    )
                )
            }

            val manifest = EvidenceManifest(
                manifestVersion = "1.0.0",
                exportId = exportId,
                batchId = batchId,
                exportingUserUid = exportingUserUid,
                exportTimestamp = System.currentTimeMillis(),
                totalLinks = links.size,
                totalScreenshots = totalScreenshots,
                encryptionAlgorithm = "AES-256",
                links = manifestLinks
            )

            // Write manifest.json
            val manifestJsonString = manifestAdapter.indent("  ").toJson(manifest)
            val manifestFile = File(tempDir, "manifest.json")
            manifestFile.writeText(manifestJsonString)
            val manifestSha256 = ChecksumUtil.computeSha256(manifestFile)

            // 2. Initialize Zip4j with AES-256
            val zipFile = ZipFile(targetZipFile, password.toCharArray())
            val zipParameters = ZipParameters().apply {
                isEncryptFiles = true
                encryptionMethod = EncryptionMethod.AES
                aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                compressionMethod = CompressionMethod.DEFLATE
                compressionLevel = CompressionLevel.NORMAL
            }

            // Add manifest.json at root of archive
            zipParameters.fileNameInZip = "manifest.json"
            zipFile.addFile(manifestFile, zipParameters)

            // Add screenshot files under links/<linkId>/
            for ((linkEntity, screenshots) in links) {
                val safeLinkId = linkEntity.linkId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                for (index in screenshots.indices) {
                    val (scrEntity, file) = screenshots[index]
                    if (file.exists() && file.isFile) {
                        // Safe formatted sequence e.g., screenshot_0001.jpg
                        val extension = file.extension.ifBlank { "jpg" }
                        val formattedName = "screenshot_%04d.%s".format(index + 1, extension)
                        zipParameters.fileNameInZip = "links/$safeLinkId/$formattedName"
                        zipFile.addFile(file, zipParameters)
                    }
                }
            }

            // 3. Validate newly created encrypted archive
            if (!targetZipFile.exists() || targetZipFile.length() == 0L) {
                throw IOException("Encryption failed: Output ZIP file is empty or was not created.")
            }

            val validationZip = ZipFile(targetZipFile, password.toCharArray())
            if (!validationZip.isValidZipFile) {
                targetZipFile.delete()
                throw IOException("Integrity check failed: Created ZIP file is not a valid archive.")
            }

            if (!validationZip.isEncrypted) {
                targetZipFile.delete()
                throw SecurityException("Security violation: ZIP file was created without encryption!")
            }

            // Test extraction of manifest with password to verify decryptability
            val testExtractDir = File(tempDir, "test_verify").apply { mkdirs() }
            try {
                validationZip.extractFile("manifest.json", testExtractDir.absolutePath)
                val extractedManifest = File(testExtractDir, "manifest.json")
                if (!extractedManifest.exists() || extractedManifest.length() == 0L) {
                    throw IOException("Decryption validation test failed.")
                }
            } finally {
                testExtractDir.deleteRecursively()
            }

            val archiveSha256 = ChecksumUtil.computeSha256(targetZipFile)

            return ExportResult(
                exportId = exportId,
                batchId = batchId,
                zipFile = targetZipFile,
                manifestSha256 = manifestSha256,
                archiveSha256 = archiveSha256,
                fileSizeBytes = targetZipFile.length(),
                linkCount = links.size,
                screenshotCount = totalScreenshots,
                manifest = manifest
            )
        } finally {
            // Secure cleanup of temporary directory
            tempDir.deleteRecursively()
        }
    }

    /**
     * Decrypts and validates an archive with the given password.
     * Enforces path traversal protection.
     */
    fun decryptAndValidateArchive(
        zipFile: File,
        password: String,
        destinationDir: File
    ): EvidenceManifest {
        val zip = ZipFile(zipFile, password.toCharArray())
        if (!zip.isValidZipFile) {
            throw IOException("File is not a valid ZIP archive.")
        }
        if (!zip.isEncrypted) {
            throw SecurityException("Archive is not encrypted.")
        }

        destinationDir.mkdirs()

        // Path traversal protection check
        val fileHeaders = zip.fileHeaders
        for (header in fileHeaders) {
            val fileName = header.fileName
            if (fileName.contains("..") || fileName.startsWith("/") || fileName.startsWith("\\")) {
                throw SecurityException("Illegal archive entry detected (Path Traversal attempt): $fileName")
            }
        }

        // Extract all entries safely
        zip.extractAll(destinationDir.absolutePath)

        val manifestFile = File(destinationDir, "manifest.json")
        if (!manifestFile.exists()) {
            throw IOException("manifest.json not found in decrypted archive.")
        }

        val manifestJson = manifestFile.readText()
        val manifest = manifestAdapter.fromJson(manifestJson)
            ?: throw IOException("Failed to parse manifest.json from archive.")

        // Verify SHA-256 of all screenshots in the manifest
        for (link in manifest.links) {
            for (scr in link.screenshots) {
                // Link directory
                val expectedFile = File(destinationDir, "links/${link.linkId}/${scr.fileName}")
                if (expectedFile.exists()) {
                    val actualSha = ChecksumUtil.computeSha256(expectedFile)
                    if (scr.sha256Checksum.isNotBlank() && !actualSha.equals(scr.sha256Checksum, ignoreCase = true)) {
                        throw SecurityException("Checksum mismatch for ${scr.fileName}: expected ${scr.sha256Checksum}, got $actualSha")
                    }
                }
            }
        }

        return manifest
    }
}
