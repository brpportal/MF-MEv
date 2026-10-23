package com.example

import com.example.data.crypto.ChecksumUtil
import com.example.data.crypto.ZipEncryptionManager
import com.example.data.local.entity.LinkItemEntity
import com.example.data.local.entity.ScreenshotEntity
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class EvidenceCryptographyTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testChecksumCalculation() {
        val sampleBytes = "MF_MPANEL_EVIDENCE_TEST_PAYLOAD".toByteArray(Charsets.UTF_8)
        val hash = ChecksumUtil.computeSha256(sampleBytes)
        assertNotNull(hash)
        assertEquals(64, hash.length) // 64 hex chars for SHA-256

        val testFile = tempFolder.newFile("sample.txt")
        testFile.writeBytes(sampleBytes)
        val fileHash = ChecksumUtil.computeSha256(testFile)
        assertEquals(hash, fileHash)
    }

    @Test
    fun testSecurePasswordGeneration() {
        val pwd = ChecksumUtil.generateSecurePassword(16)
        assertEquals(16, pwd.length)
        // Verify contains mixed characters
        assertTrue(pwd.any { it.isUpperCase() })
        assertTrue(pwd.any { it.isLowerCase() })
        assertTrue(pwd.any { it.isDigit() })
    }

    @Test
    fun testAes256EncryptionAndDecryptionRoundTrip() {
        val context = RuntimeEnvironment.getApplication()
        val zipManager = ZipEncryptionManager(context)

        // Create dummy screenshot file
        val dummyImgFile = tempFolder.newFile("screenshot_0001.jpg")
        val content = "FAKE_JPEG_IMAGE_CONTENT_FOR_CRYPTO_TEST".toByteArray()
        dummyImgFile.writeBytes(content)
        val imgSha = ChecksumUtil.computeSha256(dummyImgFile)

        val link = LinkItemEntity(
            linkId = "A01",
            batchId = "BATCH-TEST",
            url = "https://instagram.com/sample",
            platform = "Instagram",
            screenshotCount = 1,
            requiredCount = 1,
            status = "COMPLETED"
        )

        val screenshot = ScreenshotEntity(
            screenshotId = "A01-SCR-0001",
            internalId = UUID.randomUUID().toString(),
            linkId = "A01",
            batchId = "BATCH-TEST",
            userId = "USER-123",
            platform = "Instagram",
            originalUrl = "https://instagram.com/sample",
            localFilePath = dummyImgFile.name,
            fileSize = dummyImgFile.length(),
            sha256Checksum = imgSha,
            captureStatus = "SAVED",
            syncStatus = "PENDING"
        )

        val password = "StrongEvidenceKey@2026!"
        val links = listOf(link to listOf(screenshot to dummyImgFile))

        // Create AES-256 encrypted archive
        val exportResult = zipManager.createEncryptedZip(
            batchId = "BATCH-TEST",
            exportId = "EXP-999",
            exportingUserUid = "USER-123",
            password = password,
            links = links
        )

        assertNotNull(exportResult.zipFile)
        assertTrue(exportResult.zipFile.exists())
        assertTrue(exportResult.fileSizeBytes > 0)
        assertEquals(1, exportResult.linkCount)
        assertEquals(1, exportResult.screenshotCount)

        // Verify that ZIP is genuinely encrypted
        val rawZip = ZipFile(exportResult.zipFile)
        assertTrue(rawZip.isEncrypted)

        // Decrypt archive with correct password
        val extractDir = tempFolder.newFolder("extracted_correct")
        val manifest = zipManager.decryptAndValidateArchive(
            zipFile = exportResult.zipFile,
            password = password,
            destinationDir = extractDir
        )

        assertEquals("1.0.0", manifest.manifestVersion)
        assertEquals("BATCH-TEST", manifest.batchId)
        assertEquals(1, manifest.links.size)
        assertEquals("A01", manifest.links.first().linkId)
        assertEquals(1, manifest.links.first().screenshots.size)
        assertEquals("screenshot_0001.jpg", manifest.links.first().screenshots.first().fileName)

        // Attempt decryption with wrong password must fail
        val wrongExtractDir = tempFolder.newFolder("extracted_wrong")
        assertThrows(ZipException::class.java) {
            val wrongZip = ZipFile(exportResult.zipFile, "WrongPassword#999".toCharArray())
            wrongZip.extractAll(wrongExtractDir.absolutePath)
        }
    }
}
