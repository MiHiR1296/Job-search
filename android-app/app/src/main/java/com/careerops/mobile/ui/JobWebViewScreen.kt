package com.careerops.mobile.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class PageCaptureBridge(
    private val mainHandler: Handler,
    private val onPageTextCaptured: (String) -> Unit,
    private val onVisibleTextCaptured: (String) -> Unit,
    private val onJsonLdCaptured: (String) -> Unit
) {
    @JavascriptInterface
    fun postPageText(text: String) {
        mainHandler.post { onPageTextCaptured(text) }
    }

    @JavascriptInterface
    fun postVisibleText(text: String) {
        mainHandler.post { onVisibleTextCaptured(text) }
    }

    @JavascriptInterface
    fun postJsonLdPayload(text: String) {
        mainHandler.post { onJsonLdCaptured(text) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun JobWebViewScreen(
    url: String,
    autoCaptureOnLoad: Boolean = true,
    onPageTextCaptured: (String) -> Unit,
    onVisibleTextCaptured: (String) -> Unit,
    onJsonLdCaptured: (String) -> Unit = {},
    onLoadError: (String) -> Unit = {},
    onClearLoadError: () -> Unit = {},
    onOpenCustomTab: () -> Unit = {},
    onCaptureAndGenerate: (() -> Unit)? = null
) {
    val isLoading = remember { mutableStateOf(true) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val bridge = remember(mainHandler, onPageTextCaptured, onVisibleTextCaptured, onJsonLdCaptured) {
        PageCaptureBridge(
            mainHandler = mainHandler,
            onPageTextCaptured = onPageTextCaptured,
            onVisibleTextCaptured = onVisibleTextCaptured,
            onJsonLdCaptured = onJsonLdCaptured
        )
    }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var oauthPopupWebView by remember { mutableStateOf<WebView?>(null) }
    val timeoutScope = remember { CoroutineScope(Dispatchers.Main) }
    var timeoutJob by remember { mutableStateOf<Job?>(null) }
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp

    fun requestCapture() {
        webViewRef?.evaluateJavascript(
            """
            (function() {
              var fullText = (document.body && document.body.innerText) ? document.body.innerText : "";
              var visibleText = fullText.slice(0, 12000);
              var jsonParts = [];
              var scripts = document.querySelectorAll('script[type="application/ld+json"]');
              for (var i = 0; i < scripts.length; i++) {
                var t = (scripts[i].textContent || "").trim();
                if (t) jsonParts.push(t);
              }
              window.CareerOpsBridge.postJsonLdPayload(jsonParts.join(String.fromCharCode(10) + "---JSONLD---" + String.fromCharCode(10)));
              window.CareerOpsBridge.postPageText(fullText || "");
              window.CareerOpsBridge.postVisibleText(visibleText || "");
            })();
            """.trimIndent(),
            null
        )
    }

    fun dismissOAuthPopup() {
        val wv = oauthPopupWebView ?: return
        (wv.parent as? ViewGroup)?.removeView(wv)
        runCatching { wv.stopLoading() }
        runCatching { wv.destroy() }
        oauthPopupWebView = null
    }

    oauthPopupWebView?.let { popupView ->
        Dialog(
            onDismissRequest = { dismissOAuthPopup() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(maxHeight = screenHeightDp * 9 / 10)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Sign-in or verification window",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { dismissOAuthPopup() }) {
                            Text("Close")
                        }
                    }
                    Text(
                        "Complete login here, then close. The job page stays open underneath.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    AndroidView(
                        factory = { ctx ->
                            FrameLayout(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                post {
                                    (popupView.parent as? ViewGroup)?.removeView(popupView)
                                    addView(
                                        popupView,
                                        FrameLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(screenHeightDp * 7 / 10)
                    )
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            tonalElevation = 1.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    "Job page",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            requestCapture()
                            onCaptureAndGenerate?.invoke()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (onCaptureAndGenerate != null) "Capture + Generate" else "Capture text")
                    }
                    Button(
                        onClick = { requestCapture() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Refresh suggestions")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onOpenCustomTab() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open in Custom Tab (recommended for LinkedIn / Google sign-in)")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "If the page stays white after tapping Log in, use Custom Tab — in-app WebView often cannot show every OAuth popup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                TextButton(
                    onClick = {
                        val target = webViewRef?.url ?: url
                        if (target.isNotBlank()) {
                            runCatching {
                                val context = webViewRef?.context ?: return@runCatching
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open same URL in external browser (last resort)")
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        webViewRef = this
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.setSupportMultipleWindows(true)
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.userAgentString = settings.userAgentString + " CareerOpsMobileWebView/1.0"
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.loadsImagesAutomatically = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        addJavascriptInterface(bridge, "CareerOpsBridge")
                        webChromeClient = object : WebChromeClient() {
                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: android.os.Message?
                            ): Boolean {
                                val parent = view ?: return false
                                val popup = WebView(parent.context).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.javaScriptCanOpenWindowsAutomatically = true
                                    settings.setSupportMultipleWindows(true)
                                    settings.userAgentString =
                                        settings.userAgentString + " CareerOpsMobileWebView/1.0"
                                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val target = request?.url?.toString().orEmpty()
                                            if (target.startsWith("http://") || target.startsWith("https://")) {
                                                return false
                                            }
                                            if (target.startsWith("intent:") || target.startsWith("linkedin://")) {
                                                runCatching {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
                                                }
                                                return true
                                            }
                                            return false
                                        }
                                    }
                                }
                                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                                transport.webView = popup
                                resultMsg.sendToTarget()
                                oauthPopupWebView = popup
                                return true
                            }

                            override fun onCloseWindow(window: WebView?) {
                                if (window != null && window === oauthPopupWebView) {
                                    dismissOAuthPopup()
                                }
                                super.onCloseWindow(window)
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val target = request?.url?.toString().orEmpty()
                                return if (target.startsWith("http://") || target.startsWith("https://")) {
                                    false
                                } else if (target.startsWith("intent:") || target.startsWith("linkedin://")) {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
                                    }
                                    true
                                } else if (target.isNotBlank()) {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
                                    }
                                    true
                                } else {
                                    false
                                }
                            }

                            override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, pageUrl, favicon)
                                isLoading.value = true
                                onClearLoadError()
                                timeoutJob?.cancel()
                                timeoutJob = timeoutScope.launch {
                                    delay(55_000)
                                    if (isLoading.value) {
                                        onLoadError("Page load is taking very long. Try Custom Tab if the screen stays blank.")
                                    }
                                }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    val msg = "${error?.errorCode}: ${error?.description ?: "unknown error"}"
                                    onLoadError(msg)
                                    isLoading.value = false
                                }
                            }

                            override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                super.onPageFinished(view, pageUrl)
                                isLoading.value = false
                                timeoutJob?.cancel()
                                timeoutJob = null
                                if (autoCaptureOnLoad) {
                                    requestCapture()
                                }
                            }
                        }
                        loadUrl(url)
                    }
                },
                update = { webView ->
                    webViewRef = webView
                    if (url.isNotBlank() && webView.url != url) {
                        isLoading.value = true
                        webView.loadUrl(url)
                    }
                }
            )

            if (isLoading.value) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    LaunchedEffect(url) {
        isLoading.value = true
    }

    DisposableEffect(Unit) {
        onDispose {
            timeoutJob?.cancel()
            dismissOAuthPopup()
        }
    }
}
