package org.tyzx.wiki.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.tyzx.wiki.net.Wiki
import java.time.Duration
import java.time.Instant

private val Chips = listOf("全部", "学校", "校园活动", "校园文化艺术节", "校园梗", "行为方针")

@Composable
fun HomeScreen(onOpen: (String) -> Unit, onSearch: () -> Unit = {}) {
    val cs = MaterialTheme.colorScheme
    var cat by remember { mutableIntStateOf(0) }
    var reload by remember { mutableIntStateOf(0) }
    var feed by remember { mutableStateOf<List<Wiki.Feed>>(emptyList()) }
    var recent by remember { mutableStateOf<List<Wiki.Recent>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(cat, reload) {
        loading = true
        error = null
        runCatching {
            val list = if (cat == 0) Wiki.random(18) else Wiki.byCategory(Chips[cat], 18)
            val rec = runCatching { Wiki.recent(6) }.getOrDefault(emptyList())
            list to rec
        }.onSuccess { (l, r) ->
            feed = l
            recent = r
            loading = false
        }.onFailure {
            error = it.message ?: "网络错误"
            loading = false
        }
    }

    val hero = feed.firstOrNull { it.thumb != null }
    val cards = feed.filter { it !== hero }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 150.dp),
        verticalItemSpacing = 10.dp,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = StaggeredGridItemSpan.FullLine) { HomeTopBar(onRandom = { reload++ }, onSearch = onSearch) }

        if (hero != null) {
            item(span = StaggeredGridItemSpan.FullLine) {
                HeroCard(hero) { onOpen(hero.title) }
            }
            item(span = StaggeredGridItemSpan.FullLine) { Spacer(Modifier.height(16.dp)) }
        }

        if (recent.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) { RecentSection(recent, onOpen) }
            item(span = StaggeredGridItemSpan.FullLine) { Spacer(Modifier.height(18.dp)) }
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            CategoryRow(selected = cat, onSelect = { cat = it }, onShuffle = { reload++ })
        }
        item(span = StaggeredGridItemSpan.FullLine) { Spacer(Modifier.height(14.dp)) }

        if (loading) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = cs.primary, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
                }
            }
        } else if (error != null) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Column(Modifier.fillMaxWidth().padding(vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error!!, color = cs.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("重试", color = cs.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { reload++ })
                }
            }
        } else {
            items(cards) { item -> FeedCard(item) { onOpen(item.title) } }
            if (cards.isEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text("这个分类还没有条目", color = cs.onSurfaceVariant, fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp))
                }
            }
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            Box(Modifier.fillMaxWidth().padding(top = 18.dp), contentAlignment = Alignment.Center) {
                Text("换一批 · 随机条目", color = cs.primary, fontSize = 13.sp,
                    modifier = Modifier.clickable { reload++ })
            }
        }
    }
}

@Composable
private fun HomeTopBar(onRandom: () -> Unit, onSearch: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("天一维基", color = cs.onSurface, fontSize = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp)
                Spacer(Modifier.height(4.dp))
                Text("江苏省天一中学 · 非官方维基", color = cs.onSurfaceVariant, fontSize = 13.sp)
            }
            CircleBtn(onClick = onSearch) { Icon(Icons.Filled.Search, null, tint = cs.onSurface, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(8.dp))
            CircleBtn(onClick = onRandom) { DiceGlyph() }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun CircleBtn(onClick: () -> Unit = {}, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(cs.surfaceContainerHigh).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun HeroCard(item: Wiki.Feed, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxWidth().height(212.dp).clip(RoundedCornerShape(26.dp))
            .background(cs.surfaceContainerHigh).clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = item.thumb,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)), startY = 90f),
            ),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.22f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) { Text("随机条目", color = Color.White, fontSize = 11.sp, letterSpacing = 0.6.sp) }
            Spacer(Modifier.height(8.dp))
            Text(item.title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
            item.extract?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, color = Color.White.copy(alpha = 0.85f), fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun RecentSection(list: List<Wiki.Recent>, onOpen: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("最近编辑", color = cs.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("实时", color = cs.onSurfaceVariant, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list) { r ->
                Column(
                    Modifier.width(158.dp).clip(RoundedCornerShape(20.dp))
                        .background(cs.surfaceContainerHigh).clickable { onOpen(r.title) }
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                ) {
                    Text(r.title, color = cs.onSurface, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(ago(r.timestamp), color = cs.onSurfaceVariant, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(selected: Int, onSelect: (Int) -> Unit, onShuffle: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        items(Chips.size) { i ->
            val on = i == selected
            Box(
                Modifier.clip(RoundedCornerShape(50))
                    .then(
                        if (on) Modifier.background(cs.primaryContainer)
                        else Modifier.border(1.dp, cs.outlineVariant, RoundedCornerShape(50))
                    )
                    .clickable { onSelect(i) }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Text(
                    Chips[i],
                    color = if (on) cs.onPrimaryContainer else cs.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun FeedCard(item: Wiki.Feed, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(cs.surfaceContainer)
            .clickable(onClick = onClick),
    ) {
        item.thumb?.let { thumb ->
            AsyncImage(
                model = thumb,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(0.92f).background(cs.surfaceContainerHigh),
            )
        }
        Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)) {
            if (item.thumb == null) {
                Text("条目", color = cs.primary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
            }
            Text(
                item.title,
                color = cs.onSurface,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            item.extract?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = cs.onSurfaceVariant, fontSize = 12.5.sp, lineHeight = 17.sp,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun DiceGlyph() {
    val c = MaterialTheme.colorScheme.onSurface
    androidx.compose.foundation.Canvas(Modifier.size(20.dp)) {
        drawRoundRect(
            color = c,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.22f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.minDimension * 0.09f),
        )
        listOf(0.3f to 0.3f, 0.7f to 0.7f, 0.3f to 0.7f, 0.7f to 0.3f).forEach { (x, y) ->
            drawCircle(c, size.minDimension * 0.09f,
                androidx.compose.ui.geometry.Offset(size.width * x, size.height * y))
        }
    }
}

private fun ago(ts: String): String = runCatching {
    val then = Instant.parse(ts)
    val d = Duration.between(then, Instant.now())
    when {
        d.toMinutes() < 1 -> "刚刚"
        d.toMinutes() < 60 -> "${d.toMinutes()} 分钟前"
        d.toHours() < 24 -> "${d.toHours()} 小时前"
        d.toDays() < 30 -> "${d.toDays()} 天前"
        else -> "${d.toDays() / 30} 个月前"
    }
}.getOrDefault("")
