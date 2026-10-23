package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.RenderProcessGoneDetail
import androidx.compose.runtime.key
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.Navy900
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.Slate500
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.CaptureState
import com.example.ui.viewmodel.CaptureViewModel
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CaptureScreen(
    batchId: String,
    linkId: String,
    initialUrl: String,
    platform: String,
    authViewModel: AuthViewModel,
    captureViewModel: CaptureViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val captureState by captureViewModel.captureState.collectAsState()
    val linkEntity by captureViewModel.currentLink.collectAsState()

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var pageProgress by remember { mutableFloatStateOf(0f) }
    var isPageLoading by remember { mutableStateOf(true) }
    var successToastText by remember { mutableStateOf<String?>(null) }
    var browserError by remember { mutableStateOf<String?>(null) }
    var browserReloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(batchId, linkId) {
        captureViewModel.loadLink(batchId, linkId)
    }

    // Handle subtle transient success confirmation
    LaunchedEffect(captureState) {
        if (captureState is CaptureState.Success) {
            val scr = (captureState as CaptureState.Success).screenshot
            successToastText = "Saved: ${scr.screenshotId}"
            delay(1800)
            successToastText = null
            captureViewModel.clearCaptureState()
        }
    }

    // System back navigation handling
    BackHandler {
        try {
            if (webViewInstance?.canGoBack() == true) {
                webViewInstance?.goBack()
            } else {
                onNavigateBack()
            }
        } catch (e: Throwable) {
            onNavigateBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Full screen webpage WebView
        key(browserReloadKey) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Robust configuration for modern platforms
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        cacheMode = WebSettings.LOAD_DEFAULT
                        userAgentString = settings.userAgentString.replace("; wv", "")
                    }

                    // Persistent cookie handling
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val target = request?.url ?: return false
                            // Keep browsing in-app only for secure web URLs; reject unsupported schemes.
                            return target.scheme != "https" && target.scheme != "http"
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isPageLoading = true
                            browserError = null
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isPageLoading = false
                            runCatching { cookieManager.flush() }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                isPageLoading = false
                                browserError = "This page could not be loaded. Check your connection and retry."
                            }
                        }

                        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                            // Android may kill a renderer under memory pressure. Release the dead WebView
                            // and offer a controlled recreation instead of letting the screen crash.
                            isPageLoading = false
                            browserError = "The browser recovered from a page crash. Tap Reload to continue."
                            webViewInstance = null
                            runCatching {
                                (view?.parent as? ViewGroup)?.removeView(view)
                                view?.removeAllViews()
                                view?.destroy()
                            }
                            return true
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            pageProgress = newProgress / 100f
                            if (newProgress >= 100) {
                                isPageLoading = false
                            }
                        }
                    }

                    runCatching {
                        val safeUrl = initialUrl.trim()
                        require(safeUrl.startsWith("https://") || safeUrl.startsWith("http://"))
                        loadUrl(safeUrl)
                    }.onFailure {
                        isPageLoading = false
                        browserError = "The requested link is not a valid web address."
                    }
                    webViewInstance = this
                }
            },
            update = {
                // Keep WebView state owned by this screen; avoid reloading on every recomposition.
            }
        )
        }

        if (browserError != null) {
            Surface(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Browser temporarily unavailable", fontWeight = FontWeight.Bold, color = Navy900)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(browserError ?: "Please try again.", color = Slate500, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.material3.Button(
                        onClick = {
                            browserError = null
                            isPageLoading = true
                            pageProgress = 0f
                            browserReloadKey++
                        }
                    ) { Text("Reload page") }
                }
            }
        }

        // Web Page Loading Bar (minimal 2dp line)
        if (isPageLoading) {
            LinearProgressIndicator(
                progress = { pageProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .align(Alignment.TopCenter)
                    .statusBarsPadding(),
                color = BluePrimary,
                trackColor = Color.Transparent
            )
        }

        // ==========================================
        // MINIMAL CAPTURE INTERFACE: ONLY 2 CONTROLS
        // 1. Back button
        // 2. Capture button
        // ==========================================

        // Control 1: Minimal Back Control (Top-Left translucent circular chip)
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 12.dp, start = 14.dp)
                .size(44.dp)
                .shadow(elevation = 4.dp, shape = CircleShape)
                .testTag("minimal_browser_back_button"),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.92f),
            shadowElevation = 4.dp,
            onClick = {
                if (webViewInstance?.canGoBack() == true) {
                    webViewInstance?.goBack()
                } else {
                    onNavigateBack()
                }
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Navy900,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Control 2: Minimal Capture Control (Bottom-Center subtle translucent pill)
        val isCapturing = captureState is CaptureState.Capturing

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(28.dp))
                .testTag("minimal_capture_button"),
            shape = RoundedCornerShape(28.dp),
            color = if (isCapturing) Color.White else Navy900,
            onClick = {
                if (!isCapturing && webViewInstance != null) {
                    val uid = currentUser?.uid ?: "USR-OFFICER"
                    captureViewModel.captureScreenshot(webViewInstance!!, uid)
                }
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Navy900,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Saving...",
                        color = Navy900,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Capture",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Capture ($linkId)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Subtle transient success notification (disappears cleanly)
        AnimatedVisibility(
            visible = successToastText != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = successToastText ?: "",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Navy900
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                webViewInstance?.let { wv ->
                    wv.stopLoading()
                    wv.webChromeClient = null
                    wv.webViewClient = WebViewClient()
                    (wv.parent as? android.view.ViewGroup)?.removeView(wv)
                    wv.removeAllViews()
                    wv.destroy()
                }
            } catch (e: Throwable) {
                // Ignore disposal errors
            }
            webViewInstance = null
        }
    }
}
