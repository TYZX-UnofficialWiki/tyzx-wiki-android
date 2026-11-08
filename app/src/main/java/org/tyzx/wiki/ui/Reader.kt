package org.tyzx.wiki.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import coil3.compose.AsyncImage
import org.tyzx.wiki.net.Wiki

@Composable
fun ReaderScreen(title: String, onBack: () -> Unit, onOpen: (String) -> Unit, onEdit: () -> Unit = {}) {
    val cs = MaterialTheme.colorScheme
    var article by remember(title) { mutableStateOf<Article?>(null) }
    var error by remember(title) { mutableStateOf<String?>(null) }
    var reload by remember(title) { mutableStateOf(0) }

    LaunchedEffect(title, reload) {
        error = null
        article = null
        runCatching { Html.parse(title, Wiki.pageHtml(title)) }
            .onSuccess { article = it }
            .onFailure { error = it.message ?: "加载失败" }
    }

    Column(Modifier.fillMaxSize().background(cs.background)) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip100().background(cs.surfaceContainer).clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = cs.onSurface, modifier = Modifier.size(20.dp))
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
                Modifier.size(40.dp).clip100().background(cs.surfaceContainer).clickable { onEdit() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = cs.onSurface, modifier = Modifier.size(18.dp))
            }
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
            article == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = cs.primary, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
            }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 40.dp),
            ) {
                items(article!!.blocks) { b -> BlockView(b, onOpen) }
            }
        }
    }
}

private fun Modifier.clip100() = clip(CircleShape)

@Composable
private fun BlockView(block: Block, onOpen: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    when (block) {
        is Block.Heading -> {
            Spacer(Modifier.height(if (block.level <= 2) 22.dp else 16.dp))
            Text(
                block.text,
                color = cs.onSurface,
                fontSize = when (block.level) {
                    1, 2 -> 22.sp
                    3 -> 18.sp
                    else -> 16.sp
                },
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }

        is Block.Paragraph -> {
            Text(
                runsToAnnotated(block.runs, cs.primary, onOpen),
                color = cs.onSurface,
                fontSize = 16.sp,
                lineHeight = 27.sp,
            )
            Spacer(Modifier.height(14.dp))
        }

        is Block.Bullets -> {
            Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                block.items.forEachIndexed { i, item ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text(
                            if (block.ordered) "${i + 1}." else "•",
                            color = cs.onSurfaceVariant,
                            fontSize = 16.sp,
                            modifier = Modifier.width(20.dp),
                        )
                        Text(
                            runsToAnnotated(item, cs.primary, onOpen),
                            color = cs.onSurface,
                            fontSize = 16.sp,
                            lineHeight = 26.sp,
                        )
                    }
                }
            }
        }

        is Block.Figure -> {
            Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                AsyncImage(
                    model = block.src,
                    contentDescription = block.caption,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .clip1002(RoundedCornerShape(16.dp))
                        .background(cs.surfaceContainer),
                )
                if (block.caption != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(block.caption, color = cs.onSurfaceVariant, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }

        is Block.Quote -> {
            Row(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                Box(Modifier.width(3.dp).height(20.dp).background(cs.outlineVariant, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(12.dp))
                Text(
                    runsToAnnotated(block.runs, cs.primary, onOpen),
                    color = cs.onSurfaceVariant,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                )
            }
        }

        is Block.TableView -> {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(bottom = 14.dp)
                    .clip1002(RoundedCornerShape(14.dp))
                    .background(cs.surfaceContainer),
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        @SuppressLint("SetJavaScriptEnabled")
                        settings.javaScriptEnabled = false
                    }
                },
                update = { wv ->
                    val dark = cs.surface.lum() < 0.5f
                    val fg = if (dark) "#E9E4F2" else "#1B1725"
                    val css = """
                        body{margin:0;padding:12px;font-family:sans-serif;font-size:13px;color:$fg;}
                        table{border-collapse:collapse;width:100%;}
                        td,th{border:1px solid rgba(128,128,128,.35);padding:6px;text-align:left;}
                    """.trimIndent()
                    val html = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'>" +
                        "<style>$css</style></head><body>" + block.html + "</body></html>"
                    wv.loadDataWithBaseURL(Wiki.HOST, html, "text/html", "utf-8", null)
                },
            )
        }
    }
}

private fun Modifier.clip1002(shape: androidx.compose.ui.graphics.Shape) = clip(shape)

private fun androidx.compose.ui.graphics.Color.lum(): Float =
    0.299f * red + 0.587f * green + 0.114f * blue

@Composable
private fun runsToAnnotated(runs: List<Run>, linkColor: Color, onOpen: (String) -> Unit): AnnotatedString {
    return buildAnnotatedString {
        runs.forEach { r ->
            val style = SpanStyle(
                fontWeight = if (r.bold) FontWeight.Bold else null,
                fontStyle = if (r.italic) FontStyle.Italic else null,
                fontFamily = if (r.code) FontFamily.Monospace else null,
                color = if (r.href != null) linkColor else Color.Unspecified,
            )
            if (r.href != null) {
                val link = LinkAnnotation.Clickable(
                    tag = r.href,
                    styles = TextLinkStyles(style = style),
                ) {
                    if (r.href.startsWith("wiki:")) onOpen(r.href.removePrefix("wiki:"))
                }
                withLink(link) { append(r.text) }
            } else {
                withStyle(style) { append(r.text) }
            }
        }
    }
}
