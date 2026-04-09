package com.careerops.mobile.ui

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private class PageCaptureBridge(
    private val onPageTextCaptured: (String) -> Unit,
    private val onVisibleTextCaptured: (String) -> Unit
) {
    @JavascriptInterface
    fun postPageText(text: String) {
        onPageTextCaptured(text)
    }

    @JavascriptInterface
    fun postVisibleText(text: String) {
        onVisibleTextCaptured(text)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun JobWebViewScreen(
    url: String,
    autoCaptureOnLoad: Boolean = true,
    onPageTextCaptured: (String) -> Unit,
    onVisibleTextCaptured: (String) -> Unit,
    onCaptureAndGenerate: (() -> Unit)? = null
) {
    val isLoading = remember { mutableStateOf(true) }
    val bridge = remember(onPageTextCaptured, onVisibleTextCaptured) {
        PageCaptureBridge(
            onPageTextCaptured = onPageTextCaptured,
            onVisibleTextCaptured = onVisibleTextCaptured
        )
    }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun requestCapture() {
        webViewRef?.evaluateJavascript(
            """
            (function() {
              var fullText = (document.body && document.body.innerText) ? document.body.innerText : "";
              var visibleText = fullText.slice(0, 4000);
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
            }

            Spacer(modifier = Modifier.height(4.dp))

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        webViewRef = this
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.loadsImagesAutomatically = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        addJavascriptInterface(bridge, "CareerOpsBridge")
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                super.onPageFinished(view, pageUrl)
                                isLoading.value = false
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
}
