package com.careerops.mobile.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class PageCaptureBridge(
    private val onPageTextCaptured: (String) -> Unit,
    private val onVisibleTextCaptured: (String) -> Unit,
    private val onJsonLdCaptured: (String) -> Unit
) {
    @JavascriptInterface
    fun postPageText(text: String) {
        onPageTextCaptured(text)
    }

    @JavascriptInterface
    fun postVisibleText(text: String) {
        onVisibleTextCaptured(text)
    }

    @JavascriptInterface
    fun postJsonLdPayload(text: String) {
        onJsonLdCaptured(text)
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
    val bridge = remember(onPageTextCaptured, onVisibleTextCaptured, onJsonLdCaptured) {
        PageCaptureBridge(
            onPageTextCaptured = onPageTextCaptured,
            onVisibleTextCaptured = onVisibleTextCaptured,
            onJsonLdCaptured = onJsonLdCaptured
        )
    }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val timeoutScope = remember { CoroutineScope(Dispatchers.Main) }
    var timeoutJob by remember { mutableStateOf<Job?>(null) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            requestCapture()
                            onCaptureAndGenerate?.invoke()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (onCaptureAndGenerate != null) "Capture + Generate" else "Capture page text")
                    }
                    Button(
                        onClick = { requestCapture() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Refresh suggestions")
                    }
                }
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
                        Text("Open current page in browser (fallback for difficult logins)")
                    }
                    TextButton(
                        onClick = { onOpenCustomTab() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open job in Custom Tab (Indeed / heavy logins)")
                    }
                }

            Spacer(modifier = Modifier.height(4.dp))

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
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val target = request?.url?.toString().orEmpty()
                                            if (target.startsWith("http://") || target.startsWith("https://")) {
                                                parent.loadUrl(target)
                                            } else if (target.isNotBlank()) {
                                                runCatching {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
                                                }
                                            }
                                            return true
                                        }
                                    }
                                }
                                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                                transport.webView = popup
                                resultMsg.sendToTarget()
                                return true
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
                                } else {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
                                    }
                                    true
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
        }

        if (isLoading.value) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }

    LaunchedEffect(url) {
        isLoading.value = true
    }

    DisposableEffect(Unit) {
        onDispose {
            timeoutJob?.cancel()
        }
    }
}
