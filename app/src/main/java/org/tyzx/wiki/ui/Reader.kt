package org.tyzx.wiki.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.tyzx.wiki.net.Wiki

/**
 * 阅读页。用 WebView 渲染 Parsoid 输出的 HTML——MediaWiki 内容里 HTML 标签很多，
 * 自己解析成 Compose 组件兼容性差，这里改成网页渲染 + 注入主题 CSS。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReaderScreen(
    title: String,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onEdit: () -> Unit = {},
    onDiscuss: () -> Unit = {},
    onShare: () -> Unit = {},
    fontScale: Float = 1f,
    readonly: Boolean = false,
    externalInApp: Boolean = true,
) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    var html by remember(title) { mutableStateOf<String?>(null) }
    var error by remember(title) { mutableStateOf<String?>(null) }
    var reload by remember(title) { mutableIntStateOf(0) }
    var progress by remember(title) { mutableIntStateOf(0) }

    LaunchedEffect(title, reload) {
        html = null
        error = null
        progress = 0
        runCatching { Wiki.pageHtml(title) }
            .onSuccess { html = it }
            .onFailure { error = it.message ?: "加载失败" }
    }

    val doc = remember(html, cs, fontScale) {
        html?.let { wrapHtml(it, cs, fontScale) }
    }

    Column(Modifier.fillMaxSize().background(cs.background)) {
        Spacer(Modifier.padding(top = 0.dp))
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(50)).background(cs.surfaceContainer)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ArrowBack, null, tint = cs.onSurface, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                color = cs.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier.height(34.dp).clip(RoundedCornerShape(50)).background(cs.surfaceContainer)
                    .clickable { onDiscuss() }.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("讨论", fontSize = 13.sp, color = cs.onSurface, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(50)).clickable { onShare() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Share, null, tint = cs.onSurface, modifier = Modifier.size(19.dp))
            }
            if (!readonly) {
                Spacer(Modifier.width(2.dp))
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(50)).clickable { onEdit() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Edit, null, tint = cs.onSurface, modifier = Modifier.size(19.dp))
                }
            }
        }

        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = cs.primary,
                trackColor = Color.Transparent,
            )
        }

        when {
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error!!, color = cs.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "重试",
                        color = cs.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { reload++ },
                    )
                }
            }
            doc == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = cs.primary, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
            }
            else -> AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(0x00000000)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadsImagesAutomatically = true
                        settings.blockNetworkImage = false
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean {
                                val url = request.url ?: return false
                                return route(url)
                            }

                            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                                route(Uri.parse(url))

                            private fun route(uri: Uri): Boolean {
                                val host = uri.host.orEmpty()
                                if (host == "tyzx.org" || host.endsWith(".tyzx.org")) {
                                    val path = uri.path.orEmpty()
                                    if (path.startsWith("/wiki/")) {
                                        val t = Uri.decode(path.removePrefix("/wiki/")).replace('_', ' ')
                                        if (t.isNotBlank()) onOpen(t)
                                        return true
                                    }
                                    if (path.startsWith("/index.php")) {
                                        val t = uri.getQueryParameter("title")
                                        if (t != null && uri.getQueryParameter("action") == null) {
                                            onOpen(t)
                                            return true
                                        }
                                    }
                                    return false
                                }
                                if (externalInApp) return false
                                runCatching {
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                }
                                return true
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                progress = 100
                            }

                            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                                progress = 10
                            }
                        }
                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                progress = newProgress
                            }
                        }
                    }
                },
                update = { wv ->
                    val d = doc
                    if (d != null && wv.tag != d) {
                        wv.tag = d
                        wv.loadDataWithBaseURL(Wiki.HOST + "/", d, "text/html", "utf-8", null)
                    }
                },
                onRelease = { wv ->
                    wv.stopLoading()
                    wv.destroy()
                },
            )
        }
    }
}

/** 注入主题 CSS：背景透明、配色跟 App 一致、图片自适应、表格可横滚。 */
internal fun wrapHtml(body: String, cs: androidx.compose.material3.ColorScheme, fontScale: Float = 1f) = buildString {
    val bg = cs.background.toHex()
    val fg = cs.onSurface.toHex()
    val muted = cs.onSurfaceVariant.toHex()
    val link = cs.primary.toHex()
    val card = cs.surfaceContainerHigh.toHex()
    val size = (16f * fontScale).toInt()
    append("<!doctype html><html><head><meta charset=\"utf-8\">")
    append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=3\">")
    append("<style>")
    append("html,body{background:$bg;color:$fg;margin:0;padding:0;}")
    append("body{font-family:system-ui,-apple-system,\"Noto Sans SC\",sans-serif;font-size:${size}px;line-height:1.75;")
    append("padding:4px 20px 48px;word-break:break-word;overflow-x:hidden;}")
    append("a{color:$link;text-decoration:none;}")
    append("h1,h2,h3,h4,h5,h6{color:$fg;line-height:1.35;margin:1.4em 0 .5em;font-weight:650;}")
    append("h1{font-size:1.7em}h2{font-size:1.35em;border-bottom:1px solid ${muted}33;padding-bottom:.25em}")
    append("h3{font-size:1.15em}h4{font-size:1.02em}")
    append("p{margin:.7em 0;}")
    append("ul,ol{padding-left:1.4em;margin:.6em 0;}")
    append("li{margin:.3em 0;}")
    append("img,video{max-width:100%;height:auto;border-radius:14px;}")
    append("figure{margin:1.1em 0;}")
    append("figcaption{font-size:.8em;color:$muted;margin-top:.4em;}")
    append("table{display:block;overflow-x:auto;border-collapse:collapse;font-size:.88em;margin:1em 0;}")
    append("th,td{border:1px solid ${muted}44;padding:8px 10px;}")
    append("th{background:$card;}")
    append("blockquote{border-left:3px solid $link;margin:1em 0;padding:.2em 0 .2em 1em;color:$muted;}")
    append("pre,code{background:$card;border-radius:10px;font-size:.86em;}")
    append("pre{padding:12px;overflow-x:auto;}")
    append("code{padding:2px 6px;}")
    append(".mw-editsection,.navbox,.metadata,.ambox,.sistersitebox{display:none;}")
    append(".thumb,.thumbinner,.thumbcaption{background:transparent;border:0;}")
    append("</style></head><body>")
    append(body)
    append("</body></html>")
}

internal fun Color.toHex(): String {
    val a = (alpha * 255).toInt()
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    return if (a == 255) String.format("#%02X%02X%02X", r, g, b)
    else String.format("#%02X%02X%02X%02X", a, r, g, b)
}
