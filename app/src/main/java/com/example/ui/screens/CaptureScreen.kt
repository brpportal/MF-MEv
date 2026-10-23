package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.ui.theme.Navy900
import com.example.ui.theme.PurpleLight
import com.example.ui.theme.PurplePrimary
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
    val keyboardController = LocalSoftwareKeyboardController.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val captureState by captureViewModel.captureState.collectAsState()
    val linkEntity by captureViewModel.currentLink.collectAsState()

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var swipeRefreshInstance by remember { mutableStateOf<SwipeRefreshLayout?>(null) }
    var pageProgress by remember { mutableFloatStateOf(0f) }
    var isPageLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var addressInput by remember { mutableStateOf(initialUrl) }
    var successToastText by remember { mutableStateOf<String?>(null) }
    var hasRendererCrashed by remember { mutableStateOf(false) }

    // Smooth spinning animation for the explicit refresh button when loading
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )
    val refreshIconRotation = if (isPageLoading) spinAngle else 0f

    LaunchedEffect(batchId, linkId) {
        captureViewModel.loadLink(batchId, linkId)
    }

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

    fun navigateToInput(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return

        val targetUrl = when {
            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> {
                val encoded = Uri.encode(trimmed)
                "https://www.google.com/search?q=$encoded"
            }
        }
        addressInput = targetUrl
        currentUrl = targetUrl
        try {
            isPageLoading = true
            webViewInstance?.loadUrl(targetUrl)
        } catch (e: Exception) {
            Toast.makeText(context, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Safe Search & Address Header Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            color = Color(0xFF1E293B),
            shadowElevation = 4.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back arrow icon
                    IconButton(
                        onClick = {
                            if (webViewInstance?.canGoBack() == true) {
                                webViewInstance?.goBack()
                            } else {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Forward button
                    IconButton(
                        onClick = {
                            if (webViewInstance?.canGoForward() == true) {
                                webViewInstance?.goForward()
                            }
                        },
                        modifier = Modifier.size(32.dp),
                        enabled = webViewInstance?.canGoForward() == true
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (webViewInstance?.canGoForward() == true) Color.White else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Search / URL Input Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(20.dp))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (currentUrl.contains("google.com/search")) Icons.Default.Search else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (currentUrl.startsWith("https://")) Color(0xFF10B981) else Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            BasicTextField(
                                value = addressInput,
                                onValueChange = { addressInput = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(Color(0xFF8B5CF6)),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        keyboardController?.hide()
                                        navigateToInput(addressInput)
                                    }
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            if (addressInput.isNotEmpty()) {
                                IconButton(
                                    onClick = { addressInput = "" },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Explicit Refresh Button in Header Bar
                    IconButton(
                        onClick = {
                            hasRendererCrashed = false
                            isPageLoading = true
                            swipeRefreshInstance?.isRefreshing = true
                            webViewInstance?.reload()
                            Toast.makeText(context, "Refreshing page...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(1.dp, if (isPageLoading) PurplePrimary else Color(0xFF334155), CircleShape)
                            .testTag("browser_header_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = if (isPageLoading) "Refreshing page" else "Refresh page",
                            tint = if (isPageLoading) PurpleLight else Color.White,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(refreshIconRotation)
                        )
                    }

                    // Reset to initial URL button
                    IconButton(
                        onClick = {
                            addressInput = initialUrl
                            currentUrl = initialUrl
                            isPageLoading = true
                            webViewInstance?.loadUrl(initialUrl)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home Link",
                            tint = PurpleLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Loading bar
                if (isPageLoading) {
                    LinearProgressIndicator(
                        progress = { pageProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = PurplePrimary,
                        trackColor = Color(0xFF1E293B)
                    )
                }
            }
        }

        // Web view container with pull-to-refresh, crash recovery & capture controls
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
        ) {
            if (hasRendererCrashed) {
                // Crash recovery display instead of app crash
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Web Page Recovered Safely",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "The web renderer ran out of memory on complex scripts. The app safely caught this event.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PurplePrimary,
                            onClick = {
                                hasRendererCrashed = false
                                isPageLoading = true
                                webViewInstance?.loadUrl(currentUrl)
                            }
                        ) {
                            Text(
                                text = "Reload Page",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            } else {
                // Integrated SwipeRefreshLayout wrapping the WebView for native pull-to-refresh
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("browser_swipe_refresh_layout"),
                    factory = { ctx ->
                        SwipeRefreshLayout(ctx).apply {
                            swipeRefreshInstance = this
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setColorSchemeColors(
                                android.graphics.Color.parseColor("#8B5CF6"), // PurplePrimary
                                android.graphics.Color.parseColor("#3B82F6")  // BluePrimary
                            )
                            setProgressBackgroundColorSchemeColor(android.graphics.Color.parseColor("#1E293B"))

                            val webView = WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )

                                // Sandboxed configuration
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    setSupportZoom(true)
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    setSupportMultipleWindows(false)
                                    javaScriptCanOpenWindowsAutomatically = false
                                    allowFileAccess = false
                                    allowContentAccess = false
                                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    userAgentString = settings.userAgentString.replace("; wv", "")
                                }

                                // Cookies enabled safely
                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        isPageLoading = true
                                        url?.let {
                                            currentUrl = it
                                            addressInput = it
                                        }
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isPageLoading = false
                                        swipeRefreshInstance?.isRefreshing = false
                                        url?.let {
                                            currentUrl = it
                                            addressInput = it
                                        }
                                        cookieManager.flush()
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val urlStr = request?.url?.toString() ?: return false
                                        // Protect against intent://, market://, app links crashing WebView
                                        if (!urlStr.startsWith("http://", ignoreCase = true) &&
                                            !urlStr.startsWith("https://", ignoreCase = true)
                                        ) {
                                            return try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlStr))
                                                view?.context?.startActivity(intent)
                                                true
                                            } catch (e: Exception) {
                                                true
                                            }
                                        }
                                        return false
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            isPageLoading = false
                                            swipeRefreshInstance?.isRefreshing = false
                                        }
                                    }

                                    override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: RenderProcessGoneDetail?
                                    ): Boolean {
                                        hasRendererCrashed = true
                                        isPageLoading = false
                                        swipeRefreshInstance?.isRefreshing = false
                                        return true
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        pageProgress = newProgress / 100f
                                        if (newProgress >= 100) {
                                            isPageLoading = false
                                            swipeRefreshInstance?.isRefreshing = false
                                        }
                                    }
                                }

                                loadUrl(initialUrl)
                            }

                            webViewInstance = webView
                            addView(webView)

                            setOnRefreshListener {
                                hasRendererCrashed = false
                                isPageLoading = true
                                webView.reload()
                            }
                        }
                    },
                    update = { swipeRefresh ->
                        swipeRefresh.isRefreshing = isPageLoading
                    }
                )
            }

            // Minimal Capture Button Floating Bar
            val isCapturing = captureState is CaptureState.Capturing

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 18.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(28.dp))
                    .testTag("minimal_capture_button"),
                shape = RoundedCornerShape(28.dp),
                color = if (isCapturing) Color.White else Color(0xFF1E1B4B),
                border = BorderStroke(1.dp, PurpleLight),
                onClick = {
                    if (!isCapturing && webViewInstance != null) {
                        val uid = currentUser?.uid ?: "USR-OFFICER"
                        captureViewModel.captureScreenshot(webViewInstance!!, uid)
                    }
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
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
                            tint = Color(0xFFE9D5FF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Capture Evidence ($linkId)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Success Confirmation Pill
            if (successToastText != null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0F172A),
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = successToastText ?: "",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                swipeRefreshInstance?.removeAllViews()
                swipeRefreshInstance = null
                webViewInstance?.let { wv ->
                    wv.stopLoading()
                    wv.webChromeClient = null
                    wv.webViewClient = WebViewClient()
                    (wv.parent as? ViewGroup)?.removeView(wv)
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
