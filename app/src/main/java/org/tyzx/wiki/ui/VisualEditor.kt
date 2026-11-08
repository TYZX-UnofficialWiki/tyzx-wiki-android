package org.tyzx.wiki.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import org.json.JSONTokener
import org.tyzx.wiki.net.Wiki

/**
 * 可视化编辑。直接把 Parsoid 的 HTML 放进 contenteditable，
 * 保存时把改过的 HTML 交回 /transform/html/to/wikitext 转成 wikitext——
 * 和 VisualEditor 网页版走的是同一条路。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VisualEditorScreen(
    title: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var askDiscard by remember { mutableStateOf(false) }
    BackHandler(enabled = !askDiscard) { askDiscard = true }

    var doc by remember(title) { mutableStateOf<String?>(null) }
    var baseRev by remember(title) { mutableStateOf(0L) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var web by remember { mutableStateOf<WebView?>(null) }
    var linking by remember { mutableStateOf(false) }
    var linkUrl by remember { mutableStateOf("") }

    LaunchedEffect(title) {
        runCatching {
            val html = Wiki.pageHtml(title)
            val (_, rev) = Wiki.pageSource(title)
            baseRev = rev
            doc = wrapForEditing(html, cs)
        }.onFailure { error = it.message ?: "加载失败" }
    }

    fun js(code: String) {
        web?.evaluateJavascript(code, null)
    }

    fun save() {
        val w = web ?: return
        if (busy) return
        busy = true
        error = null
        w.evaluateJavascript(
            "(function(){var e=document.getElementById('content');return e?e.innerHTML:'';})()",
        ) { raw ->
            scope.launch {
                val html = runCatching {
                    val v = JSONTokener(raw).nextValue()
                    if (v is String) v else ""
                }.getOrDefault("")
                if (html.isBlank()) {
                    busy = false
                    error = "读不到编辑内容"
                    return@launch
                }
                val wiki = runCatching { Wiki.htmlToWikitext(title, html) }
                if (wiki.isFailure) {
                    busy = false
                    error = wiki.exceptionOrNull()?.message ?: "转换失败"
                    return@launch
                }
                val r = Wiki.save(title, wiki.getOrThrow(), "可视化编辑", baseRev, false)
                busy = false
                if (r.ok) onSaved() else error = r.message
            }
        }
    }

    Column(Modifier.fillMaxSize().background(cs.background).statusBarsPadding()) {
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
                Text("可视化编辑", color = cs.onSurfaceVariant, fontSize = 12.sp)
            }
            Button(
                onClick = { save() },
                enabled = !busy && doc != null,
                shape = RoundedCornerShape(50),
                modifier = Modifier.height(40.dp),
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(16.dp), color = cs.onPrimary, strokeWidth = 2.dp)
                else Text("保存", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tool("加粗") { js("document.execCommand('bold')") }
            Tool("斜体") { js("document.execCommand('italic')") }
            Tool("标题") { js("document.execCommand('formatBlock', false, 'H2')") }
            Tool("正文") { js("document.execCommand('formatBlock', false, 'P')") }
            Tool("列表") { js("document.execCommand('insertUnorderedList')") }
            Tool("链接") { linking = true }
            Tool("撤销") { js("document.execCommand('undo')") }
        }

        if (error != null) {
            Text(
                error!!,
                color = cs.error,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            val d = doc
            if (d == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = cs.primary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(0x00000000)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            loadDataWithBaseURL(Wiki.HOST + "/", d, "text/html", "utf-8", null)
                            web = this
                        }
                    },
                )
            }
        }
    }

    if (askDiscard) {
        AlertDialog(
            onDismissRequest = { askDiscard = false },
            title = { Text("放弃这次编辑？", fontSize = 18.sp) },
            text = { Text("没有保存的内容会丢失。", fontSize = 14.sp, color = cs.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    askDiscard = false
                    onBack()
                }) { Text("放弃") }
            },
            dismissButton = {
                TextButton(onClick = { askDiscard = false }) { Text("继续编辑") }
            },
        )
    }

    if (linking) {
        AlertDialog(
            onDismissRequest = { linking = false },
            title = { Text("插入链接", fontSize = 18.sp) },
            text = {
                Column {
                    Text("先选中文字，再填目标。", fontSize = 13.sp, color = cs.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = linkUrl,
                        onValueChange = { linkUrl = it },
                        singleLine = true,
                        label = { Text("条目名或网址", fontSize = 13.sp) },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = linkUrl.trim()
                    linking = false
                    if (target.isNotEmpty()) {
                        val quoted = org.json.JSONObject.quote(target)
                        js("document.execCommand('createLink', false, $quoted)")
                    }
                }) { Text("插入") }
            },
            dismissButton = { TextButton(onClick = { linking = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun Tool(label: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.height(36.dp).clip(RoundedCornerShape(50)).background(cs.surfaceContainerHigh)
            .clickable(onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 13.sp, color = cs.onSurface)
    }
    Spacer(Modifier.width(8.dp))
}

/** 只取 Parsoid <body> 的内容，套上编辑用的外壳。 */
private fun wrapForEditing(html: String, cs: androidx.compose.material3.ColorScheme): String {
    val body = Regex("<body[^>]*>([\\s\\S]*)</body>", RegexOption.IGNORE_CASE)
        .find(html)?.groupValues?.get(1) ?: html
    val shell = wrapHtml("<!--BODY-->", cs)
    val editable = body.replace(
        Regex("^"), ""
    )
    return shell.replace(
        "<!--BODY-->",
        "<div id=\"content\" contenteditable=\"true\" spellcheck=\"false\">$editable</div>" +
            "<style>#content{outline:none;min-height:60vh;}</style>",
    )
}
