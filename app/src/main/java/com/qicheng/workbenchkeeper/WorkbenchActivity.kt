package com.qicheng.workbenchkeeper

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.qicheng.workbenchkeeper.model.KeepAwakeDuration
import com.qicheng.workbenchkeeper.ui.theme.WorkbenchKeeperTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WorkbenchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val accessUrl = intent.getStringExtra(EXTRA_ACCESS_URL).orEmpty()
        val keepAwakeDuration = intent.getStringExtra(EXTRA_KEEP_AWAKE_DURATION)
            ?.let { encoded -> runCatching { KeepAwakeDuration.valueOf(encoded) }.getOrNull() }
            ?: KeepAwakeDuration.default

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        lifecycleScope.launch {
            delay(keepAwakeDuration.timeoutMillis)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            Toast.makeText(
                this@WorkbenchActivity,
                "${keepAwakeDuration.label}常亮已结束，已恢复系统息屏控制",
                Toast.LENGTH_SHORT,
            ).show()
        }

        setContent {
            WorkbenchKeeperTheme {
                WorkbenchScreen(
                    url = accessUrl,
                    onExit = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_ACCESS_URL = "extra_access_url"
        const val EXTRA_KEEP_AWAKE_DURATION = "extra_keep_awake_duration"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkbenchScreen(
    url: String,
    onExit: () -> Unit,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by remember { mutableStateOf("工作页") }

    BackHandler {
        val currentWebView = webView
        if (currentWebView?.canGoBack() == true) {
            currentWebView.goBack()
        } else {
            onExit()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = pageTitle,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        val currentWebView = webView
                        if (currentWebView?.canGoBack() == true) {
                            currentWebView.goBack()
                        } else {
                            onExit()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { webView?.reload() }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "刷新",
                        )
                    }
                    IconButton(onClick = onExit) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "编辑参数",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            WorkbenchWebView(
                initialUrl = url,
                onTitleChanged = { title -> pageTitle = title.ifBlank { "工作页" } },
                onWebViewReady = { createdWebView -> webView = createdWebView },
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WorkbenchWebView(
    initialUrl: String,
    onTitleChanged: (String) -> Unit,
    onWebViewReady: (WebView) -> Unit,
) {
    val backgroundColor = MaterialTheme.colorScheme.background

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(backgroundColor.toArgb())
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                webChromeClient = object : WebChromeClient() {
                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        onTitleChanged(title.orEmpty())
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean = false

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onTitleChanged(view?.title.orEmpty())
                    }
                }

                loadUrl(initialUrl)
                onWebViewReady(this)
            }
        },
    )
}
