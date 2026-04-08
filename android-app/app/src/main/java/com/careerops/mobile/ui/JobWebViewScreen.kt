package com.careerops.mobile.ui

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onPageTextCaptured: (String) -> Unit,
    onVisibleTextCaptured: (String) -> Unit
) {
    val isLoading = remember { mutableStateOf(true) }
    val bridge = remember { PageCaptureBridge(onPageTextCaptured, onVisibleTextCaptured) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
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
                            view?.evaluateJavascript(
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
                    }
                    loadUrl(url)
                }
            },
            update = { webView ->
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

    LaunchedEffect(url) {
        isLoading.value = true
    }
}
