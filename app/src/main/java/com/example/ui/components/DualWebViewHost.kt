package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.LiveStreamStatus
import com.example.ui.theme.BestValueGold
import com.example.ui.theme.CopilotEmerald
import com.example.ui.theme.GeminiCyan
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated

private const val DOM_EXTRACT_JS = """
    (function() {
        try {
            var text = document.body ? document.body.innerText : '';
            return text ? text.substring(0, 4500) : '';
        } catch(e) {
            return '';
        }
    })();
"""

private fun unescapeJsString(raw: String?): String {
    if (raw.isNullOrBlank() || raw == "null") return ""
    return raw.trim('"')
        .replace("\\n", "\n")
        .replace("\\t", " ")
        .replace("\\\"", "\"")
        .replace("\\u003C", "<")
        .replace(Regex("\\s+"), " ")
        .trim()
}

/**
 * Background headless collector that runs both Primary (Gemini Search) and Secondary (Copilot Search)
 * WebViews simultaneously even when the user is viewing the Unified Best Value screen, so live DOM text
 * is continuously harvested without requiring the user to switch screens.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BackgroundDualWebViewCollector(
    primaryUrl: String,
    secondaryUrl: String,
    onPrimaryProgress: (Int, Boolean) -> Unit,
    onSecondaryProgress: (Int, Boolean) -> Unit,
    onPrimaryDomCaptured: (String) -> Unit,
    onSecondaryDomCaptured: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .size(1.dp)
            .alpha(0.01f)
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    configureSynapseWebView(
                        onProgress = onPrimaryProgress,
                        onDomExtracted = onPrimaryDomCaptured
                    )
                    loadUrl(primaryUrl)
                }
            },
            update = { webView ->
                if (webView.url != primaryUrl && primaryUrl.isNotBlank()) {
                    webView.loadUrl(primaryUrl)
                }
            }
        )
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    configureSynapseWebView(
                        onProgress = onSecondaryProgress,
                        onDomExtracted = onSecondaryDomCaptured
                    )
                    loadUrl(secondaryUrl)
                }
            },
            update = { webView ->
                if (webView.url != secondaryUrl && secondaryUrl.isNotBlank()) {
                    webView.loadUrl(secondaryUrl)
                }
            }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configureSynapseWebView(
    onProgress: (Int, Boolean) -> Unit,
    onDomExtracted: (String) -> Unit
) {
    CookieManager.getInstance().setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        loadsImagesAutomatically = true
        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        userAgentString =
            "Mozilla/5.0 (Linux; Android 15; Pixel 9 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
    }
    webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            onProgress(newProgress, newProgress < 100)
        }
    }
    webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            onProgress(15, true)
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean = false

        override fun onPageFinished(view: WebView?, url: String?) {
            onProgress(100, false)
            view?.evaluateJavascript(DOM_EXTRACT_JS) { raw ->
                val text = unescapeJsString(raw)
                if (text.isNotBlank()) onDomExtracted(text)
            }
            // Second pass after 1.8s to capture asynchronous SPA content from Gemini / Copilot
            view?.postDelayed({
                view.evaluateJavascript(DOM_EXTRACT_JS) { rawDelayed ->
                    val textDelayed = unescapeJsString(rawDelayed)
                    if (textDelayed.isNotBlank()) onDomExtracted(textDelayed)
                }
            }, 1800L)
        }
    }
}

/**
 * Interactive Split-Screen Dual Live Streams view allowing the user to see both
 * Primary (https://gemini.google.com/search?q=%s) and Secondary (https://copilot.microsoft.com/search?q=%s)
 * simultaneously on one screen without switching tabs, and re-arbitrate live DOM on demand.
 */
@Composable
fun DualLiveStreamsScreen(
    liveStatus: LiveStreamStatus,
    isSynthesizing: Boolean,
    onPrimaryProgress: (Int, Boolean) -> Unit,
    onSecondaryProgress: (Int, Boolean) -> Unit,
    onPrimaryDomCaptured: (String) -> Unit,
    onSecondaryDomCaptured: (String) -> Unit,
    onReArbitrateLiveDom: () -> Unit,
    onReturnToSynthesis: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 0 = 50/50 split, 1 = Primary 70%, 2 = Secondary 70%
    var splitMode by remember { mutableIntStateOf(0) }
    var primaryWebViewRef by remember { mutableStateOf<WebView?>(null) }
    var secondaryWebViewRef by remember { mutableStateOf<WebView?>(null) }

    val primaryWeight = when (splitMode) {
        1 -> 0.68f
        2 -> 0.32f
        else -> 0.5f
    }
    val secondaryWeight = 1f - primaryWeight

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Control Bar for Dual Live Streams
        Surface(
            color = ObsidianSurfaceElevated,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewColumn,
                        contentDescription = "Dual Live Streams",
                        tint = GeminiCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Simultaneous Dual-Engine Live View",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Primary (${liveStatus.primaryDomCharsExtracted} chars) • Secondary (${liveStatus.secondaryDomCharsExtracted} chars)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { splitMode = (splitMode + 1) % 3 },
                        modifier = Modifier.testTag("toggle_split_ratio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Adjust Split Ratio",
                            tint = BestValueGold
                        )
                    }

                    Button(
                        onClick = {
                            primaryWebViewRef?.evaluateJavascript(DOM_EXTRACT_JS) { raw ->
                                val t = unescapeJsString(raw)
                                if (t.isNotBlank()) onPrimaryDomCaptured(t)
                            }
                            secondaryWebViewRef?.evaluateJavascript(DOM_EXTRACT_JS) { raw ->
                                val t = unescapeJsString(raw)
                                if (t.isNotBlank()) onSecondaryDomCaptured(t)
                            }
                            onReArbitrateLiveDom()
                        },
                        enabled = !isSynthesizing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GeminiCyan,
                            contentColor = Color(0xFF001E30)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("fuse_live_dom_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pull & Fuse Best Info",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 700.dp

            if (isWideScreen) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiveEngineStreamPane(
                        title = "PRIMARY: Gemini Search",
                        endpointUrl = liveStatus.primaryUrl,
                        accentColor = GeminiCyan,
                        progress = liveStatus.primaryProgress,
                        isLoading = liveStatus.primaryLoading,
                        extractedChars = liveStatus.primaryDomCharsExtracted,
                        onProgress = onPrimaryProgress,
                        onDomCaptured = onPrimaryDomCaptured,
                        onWebViewCreated = { primaryWebViewRef = it },
                        modifier = Modifier
                            .weight(primaryWeight)
                            .fillMaxHeight()
                    )

                    LiveEngineStreamPane(
                        title = "SECONDARY: Copilot Search",
                        endpointUrl = liveStatus.secondaryUrl,
                        accentColor = CopilotEmerald,
                        progress = liveStatus.secondaryProgress,
                        isLoading = liveStatus.secondaryLoading,
                        extractedChars = liveStatus.secondaryDomCharsExtracted,
                        onProgress = onSecondaryProgress,
                        onDomCaptured = onSecondaryDomCaptured,
                        onWebViewCreated = { secondaryWebViewRef = it },
                        modifier = Modifier
                            .weight(secondaryWeight)
                            .fillMaxHeight()
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiveEngineStreamPane(
                        title = "PRIMARY: Gemini Search",
                        endpointUrl = liveStatus.primaryUrl,
                        accentColor = GeminiCyan,
                        progress = liveStatus.primaryProgress,
                        isLoading = liveStatus.primaryLoading,
                        extractedChars = liveStatus.primaryDomCharsExtracted,
                        onProgress = onPrimaryProgress,
                        onDomCaptured = onPrimaryDomCaptured,
                        onWebViewCreated = { primaryWebViewRef = it },
                        modifier = Modifier
                            .weight(primaryWeight)
                            .fillMaxWidth()
                    )

                    LiveEngineStreamPane(
                        title = "SECONDARY: Copilot Search",
                        endpointUrl = liveStatus.secondaryUrl,
                        accentColor = CopilotEmerald,
                        progress = liveStatus.secondaryProgress,
                        isLoading = liveStatus.secondaryLoading,
                        extractedChars = liveStatus.secondaryDomCharsExtracted,
                        onProgress = onSecondaryProgress,
                        onDomCaptured = onSecondaryDomCaptured,
                        onWebViewCreated = { secondaryWebViewRef = it },
                        modifier = Modifier
                            .weight(secondaryWeight)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveEngineStreamPane(
    title: String,
    endpointUrl: String,
    accentColor: Color,
    progress: Int,
    isLoading: Boolean,
    extractedChars: Int,
    onProgress: (Int, Boolean) -> Unit,
    onDomCaptured: (String) -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    var localWebView by remember { mutableStateOf<WebView?>(null) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianSurface)
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurfaceElevated)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = endpointUrl,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = accentColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${extractedChars}c pulled",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
                IconButton(
                    onClick = { localWebView?.reload() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload $title",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (isLoading && progress < 100) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    configureSynapseWebView(
                        onProgress = onProgress,
                        onDomExtracted = onDomCaptured
                    )
                    onWebViewCreated(this)
                    localWebView = this
                    loadUrl(endpointUrl)
                }
            },
            update = { webView ->
                localWebView = webView
                onWebViewCreated(webView)
                if (webView.url != endpointUrl && endpointUrl.isNotBlank()) {
                    webView.loadUrl(endpointUrl)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        )
    }
}

/**
 * Interactive Browser Reader view when the user opens a specific citation or URL.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SingleWebReaderScreen(
    url: String,
    onCloseReader: () -> Unit,
    onDistillUrlAsQuery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember(url) { mutableStateOf(url) }
    var progress by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Surface(
            color = ObsidianSurfaceElevated,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (webViewRef?.canGoBack() == true) webViewRef?.goBack()
                        else onCloseReader()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = { if (webViewRef?.canGoForward() == true) webViewRef?.goForward() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = "Interactive Source Reader",
                        style = MaterialTheme.typography.labelLarge,
                        color = GeminiCyan
                    )
                    Text(
                        text = currentUrl,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = BestValueGold.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .clickable { onDistillUrlAsQuery(currentUrl) }
                        .testTag("distill_reader_page_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Distill Page",
                            tint = BestValueGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Synthesize",
                            style = MaterialTheme.typography.labelLarge,
                            color = BestValueGold
                        )
                    }
                }

                IconButton(onClick = onCloseReader) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Return to Unified Best Value",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                color = GeminiCyan,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .height(2.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    configureSynapseWebView(
                        onProgress = { p, _ -> progress = p },
                        onDomExtracted = {}
                    )
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, newUrl: String?, favicon: Bitmap?) {
                            if (!newUrl.isNullOrBlank()) currentUrl = newUrl
                        }
                    }
                    webViewRef = this
                    loadUrl(url)
                }
            },
            update = { webView ->
                webViewRef = webView
                if (webView.url != url && currentUrl == url) {
                    webView.loadUrl(url)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
        )
    }
}
