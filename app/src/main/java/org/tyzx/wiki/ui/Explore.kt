package org.tyzx.wiki.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.tyzx.wiki.net.Wiki

@Composable
fun ExploreScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Wiki.Hit>>(emptyList()) }
    var cats by remember { mutableStateOf<List<Wiki.Cat>>(emptyList()) }
    var members by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { Wiki.categoriesWithSize() }
            .onSuccess { cats = it }
            .onFailure { error = it.message }
    }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.isBlank()) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(280)
        loading = true
        runCatching { Wiki.search(q, 20) }
            .onSuccess { results = it; error = null }
            .onFailure { error = it.message }
        loading = false
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(50)).clickable {
                    if (members != null) members = null else onBack()
                },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ArrowBack, null, tint = cs.onSurface, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.size(4.dp))
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(50))
                    .background(cs.surfaceContainerHigh)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("搜索条目、分类…", style = TextStyle(fontSize = 15.sp), color = cs.onSurfaceVariant)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it; members = null },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp, color = cs.onSurface),
                        cursorBrush = SolidColor(cs.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (loading) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = cs.primary, strokeWidth = 2.dp)
                }
            }
        }

        when {
            query.isNotBlank() -> ResultList(results, error, onOpen)
            members != null -> MemberList(members!!, onOpen)
            else -> CategoryList(cats, error) { c -> members = c }
        }
    }
}

@Composable
private fun CategoryList(cats: List<Wiki.Cat>, error: String?, onPick: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "浏览分类",
                style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
                color = cs.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text("${cats.sumOf { it.size }} 个条目分布在 ${cats.size} 个分类里", fontSize = 13.sp, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
        }
        if (error != null) {
            item { Text("加载失败：$error", fontSize = 13.sp, color = cs.error) }
        }
        items(cats) { c ->
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cs.surfaceContainerHigh)
                    .clickable { onPick(c.name) }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(c.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = cs.onSurface)
                    Spacer(Modifier.height(3.dp))
                    Text("${c.size} 个条目", fontSize = 12.5.sp, color = cs.onSurfaceVariant)
                }
                Icon(Icons.Filled.KeyboardArrowRight, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun MemberList(cat: String, onOpen: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    var list by remember(cat) { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(cat) {
        runCatching { Wiki.categoryMembers(cat) }.onSuccess { list = it }.onFailure { list = emptyList() }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                cat,
                style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
                color = cs.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text("分类下的条目", fontSize = 13.sp, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
        }
        val itemsList = list
        if (itemsList == null) {
            item { CircularProgressIndicator(Modifier.size(20.dp), color = cs.primary, strokeWidth = 2.dp) }
        } else if (itemsList.isEmpty()) {
            item { Text("这个分类里没有条目", fontSize = 13.sp, color = cs.onSurfaceVariant) }
        } else {
            items(itemsList) { title ->
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(cs.surfaceContainerHigh)
                        .clickable { onOpen(title) }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = cs.onSurface)
                    Icon(Icons.Filled.KeyboardArrowRight, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun ResultList(results: List<Wiki.Hit>, error: String?, onOpen: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (error != null) {
            item { Text("搜索失败：$error", fontSize = 13.sp, color = cs.error) }
        }
        if (results.isEmpty() && error == null) {
            item {
                Spacer(Modifier.height(60.dp))
                Text("没有找到条目", fontSize = 14.sp, color = cs.onSurfaceVariant)
            }
        }
        items(results) { r ->
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cs.surfaceContainerHigh)
                    .clickable { onOpen(r.title) }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Text(r.title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = cs.onSurface)
                if (r.snippet.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(r.snippet, fontSize = 12.5.sp, color = cs.onSurfaceVariant, maxLines = 2)
                }
            }
        }
    }
}
