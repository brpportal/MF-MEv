package com.example

import com.example.data.model.BatchImportJson
import com.example.data.repository.EvidenceRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BatchImportJsonTest {

    @Test
    fun testParseValidBatchJson() {
        val sampleJson = """
            {
              "batchId": "BATCH-2026-ALPHA",
              "batchName": "Social Ads Investigation",
              "createdAt": "2026-10-15T00:00:00Z",
              "createdBy": "admin@mfmpanel.com",
              "links": [
                {
                  "linkId": "L01",
                  "url": "https://www.instagram.com/p/DA123456789/",
                  "platform": "Instagram",
                  "profileId": "@brand",
                  "requiredScreenshots": 2,
                  "notes": "Verify banner"
                },
                {
                  "linkId": "L02",
                  "url": "https://www.tiktok.com/@creator/video/123",
                  "platform": "TikTok",
                  "profileId": "@creator",
                  "requiredScreenshots": 1
                }
              ]
            }
        """.trimIndent()

        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(BatchImportJson::class.java)

        val batch = adapter.fromJson(sampleJson)
        assertNotNull(batch)
        assertEquals("BATCH-2026-ALPHA", batch?.batchId)
        assertEquals("Social Ads Investigation", batch?.batchName)
        assertEquals(2, batch?.links?.size)

        val link1 = batch?.links?.get(0)
        assertEquals("L01", link1?.linkId)
        assertEquals("Instagram", link1?.platform)
        assertEquals(2, link1?.requiredScreenshots)

        val link2 = batch?.links?.get(1)
        assertEquals("L02", link2?.linkId)
        assertEquals("TikTok", link2?.platform)
    }

    @Test
    fun testPlatformDetectionFromUrl() {
        val context = RuntimeEnvironment.getApplication()
        val repository = EvidenceRepository(context)

        assertEquals("Instagram", repository.detectPlatformFromUrl("https://www.instagram.com/reel/123"))
        assertEquals("Facebook", repository.detectPlatformFromUrl("https://www.facebook.com/watch/?v=456"))
        assertEquals("TikTok", repository.detectPlatformFromUrl("https://www.tiktok.com/@user/video/789"))
        assertEquals("YouTube", repository.detectPlatformFromUrl("https://www.youtube.com/watch?v=abc"))
        assertEquals("YouTube", repository.detectPlatformFromUrl("https://youtu.be/abc"))
        assertEquals("X", repository.detectPlatformFromUrl("https://x.com/official/status/123"))
        assertEquals("Other", repository.detectPlatformFromUrl("https://example.com/page"))
    }
}
