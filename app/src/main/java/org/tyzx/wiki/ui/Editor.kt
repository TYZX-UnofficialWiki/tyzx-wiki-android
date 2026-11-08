package org.tyzx.wiki.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.tyzx.wiki.net.Wiki

/**
 * 源码编辑器。走 action=edit，带 baserevid 做冲突检测。
 */
@Composable
fun EditorScreen(
    title: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    var original by remember(title) { mutableStateOf<String?>(null) }
    var text by remember(title) { mutableStateOf("") }
    var baseRev by remember(title) { mutableStateOf(0L) }
    var summary by remember(title) { mutableStateOf("") }
    var watch by remember(title) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var askDiscard by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    LaunchedEffect(title) {
        runCatching { Wiki.pageSource(title) }
            .onSuccess { (src, rev) ->
                original = src
                text = src
                baseRev = rev
            }
            .onFailure { error = it.message ?: "加载源码失败" }
    }

    val dirty = original != null && text != original

    BackHandler { if (dirty) askDiscard = true else onBack() }

    fun save() {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            val r = Wiki.save(title, text, summary.trim(), baseRev, watch)
            busy = false
            if (r.ok) {
                original = text
                onSaved()
            } else {
                error = r.message
            }
        }
    }

    Column(Modifier.fillMaxSize().background(cs.background).statusBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(50)).background(cs.surfaceContainer)
                    .clickable { if (dirty) askDiscard = true else onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ArrowBack, null, tint = cs.onSurface, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = cs.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    if (dirty) "已修改 · 未保存" else "源代码",
                    color = if (dirty) cs.primary else cs.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
            Button(
                onClick = { save() },
                enabled = !busy && original != null && dirty,
                shape = RoundedCornerShape(50),
                modifier = Modifier.height(40.dp),
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(16.dp), color = cs.onPrimary, strokeWidth = 2.dp)
                else Text("保存", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        if (error != null) {
            Text(
                error!!,
                color = cs.error,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
            val src = original
            if (src == null && error == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = cs.primary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
            } else {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        fontFamily = FontFamily.Monospace,
                        color = cs.onSurface,
                    ),
                    cursorBrush = SolidColor(cs.primary),
                    modifier = Modifier.fillMaxSize().verticalScroll(scroll),
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(cs.surfaceContainerHigh)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                if (summary.isEmpty()) {
                    Text("编辑摘要（选填）", fontSize = 13.sp, color = cs.onSurfaceVariant)
                }
                BasicTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp, color = cs.onSurface),
                    cursorBrush = SolidColor(cs.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(checked = watch, onCheckedChange = { watch = it })
                Text("监视", fontSize = 10.sp, color = cs.onSurfaceVariant)
            }
        }
    }

    if (askDiscard) {
        AlertDialog(
            onDismissRequest = { askDiscard = false },
            title = { Text("放弃修改？", fontSize = 18.sp) },
            text = { Text("你改的内容还没保存，退出就没了。", fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { askDiscard = false; onBack() }) { Text("放弃") }
            },
            dismissButton = {
                TextButton(onClick = { askDiscard = false }) { Text("继续编辑") }
            },
        )
    }
}
