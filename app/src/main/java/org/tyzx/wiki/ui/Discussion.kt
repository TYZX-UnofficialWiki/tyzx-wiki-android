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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import org.tyzx.wiki.net.Wiki

/**
 * 讨论页。MediaWiki 没有独立评论系统，条目讨论都在 Talk: 命名空间，
 * 所以这里做的是「阅读讨论页 + 新开一节」。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DiscussionScreen(
    title: String,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var html by remember(title) { mutableStateOf<String?>(null) }
    var error by remember(title) { mutableStateOf<String?>(null) }
    var reload by remember(title) { mutableIntStateOf(0) }

    var composer by remember { mutableStateOf(false) }
    var heading by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(title, reload) {
        error = null
        runCatching { Wiki.talkHtml(title) }
            .onSuccess { html = it }
            .onFailure { error = it.message ?: "加载失败" }
    }

    val doc = remember(html, cs) {
        html?.takeIf { it.isNotBlank() }?.let { wrapHtml(it, cs) }
    }

    Column(Modifier.fillMaxSize().background(cs.background).statusBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
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
            Column(Modifier.weight(1f)) {
                Text(title, color = cs.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("讨论页", color = cs.onSurfaceVariant, fontSize = 12.sp)
            }
            TextButton(onClick = { composer = !composer; msg = null }) {
                Text(if (composer) "收起" else "发言", fontSize = 14.sp)
            }
        }

        Box(Modifier.weight(1f)) {
            when {
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error!!, color = cs.onSurfaceVariant, fontSize = 13.sp)
                }
                html == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = cs.primary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
                doc == null -> Box(Modifier.fillMaxSize().padding(24.dp)) {
                    Text(
                        "还没有人讨论过这条。点右上角「发言」开个话题。",
                        color = cs.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
                else -> AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(0x00000000)
                            settings.javaScriptEnabled = true
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView,
                                    request: WebResourceRequest,
                                ): Boolean {
                                    val uri = request.url ?: return false
                                    if (uri.host?.endsWith("tyzx.org") == true) {
                                        val p = uri.path.orEmpty()
                                        if (p.startsWith("/wiki/")) {
                                            onOpen(Uri.decode(p.removePrefix("/wiki/")).replace('_', ' '))
                                            return true
                                        }
                                        return false
                                    }
                                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                    return true
                                }
                            }
                        }
                    },
                    update = { wv ->
                        val d = doc
                        if (d != null && wv.tag != d) {
                            wv.tag = d
                            wv.loadDataWithBaseURL(Wiki.HOST, d, "text/html", "utf-8", null)
                        }
                    },
                )
            }
        }

        if (composer) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (Wiki.user == null) {
                    Text(
                        "发言需要先登录（设置 → 账号）",
                        color = cs.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                Field("话题标题", heading, { heading = it }, cs)
                Spacer(Modifier.height(8.dp))
                Field("内容（支持 wikitext）", body, { body = it }, cs, singleLine = false)
                if (msg != null) {
                    Text(msg!!, color = cs.error, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (busy) return@Button
                        if (Wiki.user == null) {
                            msg = "请先登录"
                            return@Button
                        }
                        if (heading.isBlank() || body.isBlank()) {
                            msg = "标题和内容都要填"
                            return@Button
                        }
                        busy = true
                        msg = null
                        scope.launch {
                            val r = Wiki.addTopic(title, heading.trim(), body)
                            busy = false
                            if (r.ok) {
                                heading = ""
                                body = ""
                                composer = false
                                reload++
                            } else {
                                msg = r.message
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(23.dp),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(16.dp), color = cs.onPrimary, strokeWidth = 2.dp)
                    else Text("发布", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    cs: androidx.compose.material3.ColorScheme,
    singleLine: Boolean = true,
) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(cs.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(label, fontSize = 13.sp, color = cs.onSurfaceVariant)
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = singleLine,
            textStyle = TextStyle(fontSize = 14.sp, color = cs.onSurface),
            cursorBrush = SolidColor(cs.primary),
            modifier = Modifier.fillMaxWidth().height(if (singleLine) 20.dp else 96.dp),
        )
    }
}
