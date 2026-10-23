package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BatchImportJson(
    @Json(name = "batchId") val batchId: String? = null,
    @Json(name = "batchName") val batchName: String? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "createdBy") val createdBy: String? = null,
    @Json(name = "links") val links: List<LinkImportJson>? = null
)

@JsonClass(generateAdapter = true)
data class LinkImportJson(
    @Json(name = "linkId") val linkId: String,
    @Json(name = "url") val url: String,
    @Json(name = "platform") val platform: String? = null,
    @Json(name = "profileId") val profileId: String? = null,
    @Json(name = "postIdentifier") val postIdentifier: String? = null,
    @Json(name = "requiredScreenshots") val requiredScreenshots: Int? = 1,
    @Json(name = "notes") val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class EvidenceManifest(
    @Json(name = "manifestVersion") val manifestVersion: String = "1.0.0",
    @Json(name = "exportId") val exportId: String,
    @Json(name = "batchId") val batchId: String,
    @Json(name = "exportingUserUid") val exportingUserUid: String,
    @Json(name = "exportTimestamp") val exportTimestamp: Long,
    @Json(name = "totalLinks") val totalLinks: Int,
    @Json(name = "totalScreenshots") val totalScreenshots: Int,
    @Json(name = "encryptionAlgorithm") val encryptionAlgorithm: String = "AES-256",
    @Json(name = "links") val links: List<ManifestLinkItem>
)

@JsonClass(generateAdapter = true)
data class ManifestLinkItem(
    @Json(name = "linkId") val linkId: String,
    @Json(name = "url") val url: String,
    @Json(name = "platform") val platform: String,
    @Json(name = "profileId") val profileId: String?,
    @Json(name = "requiredCount") val requiredCount: Int,
    @Json(name = "actualCount") val actualCount: Int,
    @Json(name = "status") val status: String,
    @Json(name = "screenshots") val screenshots: List<ManifestScreenshotItem>
)

@JsonClass(generateAdapter = true)
data class ManifestScreenshotItem(
    @Json(name = "screenshotId") val screenshotId: String,
    @Json(name = "fileName") val fileName: String,
    @Json(name = "capturedAt") val capturedAt: Long,
    @Json(name = "fileSizeBytes") val fileSizeBytes: Long,
    @Json(name = "sha256Checksum") val sha256Checksum: String,
    @Json(name = "mimeType") val mimeType: String = "image/jpeg"
)
