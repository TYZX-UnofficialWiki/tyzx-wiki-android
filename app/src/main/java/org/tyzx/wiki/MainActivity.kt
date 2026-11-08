package org.tyzx.wiki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.tyzx.wiki.glass.TyBottomBar
import org.tyzx.wiki.glass.TyTab
import org.tyzx.wiki.glass.liquidtabs.LiquidGlassStyle

// 四层表面（照 BiliTools AppSurfaces 的取值区间）
private val DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF1D192B),
    background = Color(0xFF121016),
    surface = Color(0xFF121016),
    surfaceContainerLowest = Color(0xFF0C0A10),
    surfaceContainerLow = Color(0xFF17141C),
    surfaceContainer = Color(0xFF1D1A22),
    surfaceContainerHigh = Color(0xFF26222C),
    surfaceContainerHighest = Color(0xFF2B2733),
    surfaceBright = Color(0xFF1D1A22),
    onSurface = Color(0xFFE8E3EC),
    onSurfaceVariant = Color(0xFFA9A3B5),
    outlineVariant = Color(0xFF3A3542),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFF5F5FA),
    surface = Color(0xFFF5F5FA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAFD),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFEDEDF3),
    surfaceContainerHighest = Color(0xFFE4E4EC),
    surfaceBright = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1B21),
    onSurfaceVariant = Color(0xFF5D5D68),
    outlineVariant = Color(0xFFD5D5DE),
)

private val Tabs = listOf(
    TyTab("首页", Icons.Filled.Home),
    TyTab("设置", Icons.Filled.Settings),
)

/** 演示数据：先不接 API，图片用站点自己的照片 */
private data class FeedItem(
    val title: String,
    val desc: String?,
    val tag: String,
    val image: String? = null,
    val ratio: Float = 1f,
)

private val Feed = listOf(
    FeedItem(
        "樱花节", "樱花季的校园活动与历届记录", "校园活动",
        "https://zh-wiki.tyzx.org/r2_file.php/public/5/55/%E6%A8%B1%E8%8A%B11.jpg", 0.86f,
    ),
    FeedItem(
        "江苏省天一中学", "官方名称、校区与办学信息", "学校",
        "https://zh-wiki.tyzx.org/r2_file.php/public/9/96/%E5%8D%97%E9%97%A8%E6%AD%A3%E9%9D%A20.jpg", 0.72f,
    ),
    FeedItem("校园梗", "只有天一人看得懂的梗百科", "校园梗"),
    FeedItem(
        "话剧节", "话剧节的剧目、排练与演出", "校园文化艺术节",
        "https://zh-wiki.tyzx.org/r2_file.php/public/0/00/%E4%BC%9A%E8%AE%AE%E4%B8%AD%E5%BF%831.jpg", 1.05f,
    ),
    FeedItem(
        "天一中学图灵社", "校内计算机社团的活动与作品", "学生社团",
        "https://zh-wiki.tyzx.org/r2_file.php/thumb/4/43/Turing-club.png/600px-Turing-club.png", 0.86f,
    ),
    FeedItem(
        "学校历史", "从建校到今天的完整时间线", "学校历史",
        "https://zh-wiki.tyzx.org/r2_file.php/public/b/b5/%E4%BF%A1%E6%81%AF%E6%A5%BC3%E6%A5%BC%E8%A5%BF%E4%BE%A7%E8%B5%B0%E5%BB%8A0.jpg", 0.95f,
    ),
    FeedItem("合唱节", "校园合唱节的历届记录", "校园文化艺术节"),
    FeedItem(
        "总集", "全部条目的汇总索引", "网站导航",
        "https://zh-wiki.tyzx.org/r2_file.php/public/e/ec/%E4%B9%A6%E4%B8%8E%E6%A8%B1%E8%8A%B10.jpg", 0.86f,
    ),
    FeedItem("AP课程", "开设科目、考试与学分说明", "课程"),
    FeedItem("运动会", "校运会的项目与成绩", "体育赛事"),
)

private val Categories = listOf("全部", "学校", "校园生活", "校园文化", "规范", "其他")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App() }
    }
}

@Composable
fun App() {
    var dark by remember { mutableStateOf(false) }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors) {
        val cs = MaterialTheme.colorScheme
        val backdrop = rememberLayerBackdrop()
        var tab by remember { mutableIntStateOf(0) }

        Box(Modifier.fillMaxSize().background(cs.background)) {
            Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                if (tab == 0) FeedPage() else SettingsPage(dark) { dark = it }
            }
            TyBottomBar(
                tabs = Tabs,
                selectedTabIndex = { tab },
                onTabSelected = { tab = it },
                backdrop = backdrop,
                glassStyle = LiquidGlassStyle(
                    blurRadiusDp = 6f,
                    refractionHeightDp = 20f,
                    refractionAmountFrac = 0.35f,
                    chromaticAberration = true,
                ),
                surfaceAlpha = if (dark) 0.30f else 0.55f,
            )
        }
    }
}

@Composable
private fun FeedPage() {
    var selectedCat by remember { mutableIntStateOf(0) }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 150.dp),
        verticalItemSpacing = 10.dp,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = StaggeredGridItemSpan.FullLine) { TopBar() }
        item(span = StaggeredGridItemSpan.FullLine) { RecentStrip() }
        item(span = StaggeredGridItemSpan.FullLine) { Spacer(Modifier.height(18.dp)) }
        item(span = StaggeredGridItemSpan.FullLine) {
            CategoryChips(selectedCat) { selectedCat = it }
        }
        item(span = StaggeredGridItemSpan.FullLine) { Spacer(Modifier.height(14.dp)) }

        items(Feed.size) { i ->
            val item = Feed[i]
            FeedCard(item)
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            Box(Modifier.fillMaxWidth().padding(top = 18.dp), contentAlignment = Alignment.Center) {
                Text(
                    "已经到底了 · 下拉换一批",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun TopBar() {
    val cs = MaterialTheme.colorScheme
    Column {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("天一维基", color = cs.onSurface, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("江苏省天一中学 · 非官方维基", color = cs.onSurfaceVariant, fontSize = 13.sp)
            }
            CircleIcon(onClick = {}) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = cs.onSurface, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            CircleIcon(onClick = {}) { DiceGlyph() }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun CircleIcon(onClick: () -> Unit, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(cs.surfaceContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun DiceGlyph() {
    val c = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.size(20.dp)) {
        val r = size.minDimension * 0.18f
        drawRoundRect(
            color = c,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.22f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.minDimension * 0.09f),
        )
        val pts = listOf(0.3f to 0.3f, 0.7f to 0.7f, 0.3f to 0.7f, 0.7f to 0.3f)
        pts.forEach { (x, y) ->
            drawCircle(c, r, Offset(size.width * x, size.height * y))
        }
    }
}

@Composable
private fun RecentStrip() {
    val cs = MaterialTheme.colorScheme
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("最近更新", color = cs.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text("全部", color = cs.onSurfaceVariant, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(3) { i ->
                val t = listOf("首页", "樱花节", "总集")[i]
                val sub = listOf("2 小时前", "昨天", "3 天前")[i]
                Column(
                    Modifier
                        .width(150.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cs.surfaceContainer)
                        .padding(12.dp),
                ) {
                    Text(t, color = cs.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("有编辑 · $sub", color = cs.onSurfaceVariant, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun CategoryChips(selected: Int, onSelected: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(Categories.size) { i ->
            val on = i == selected
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .then(
                        if (on) Modifier.background(cs.primary.copy(alpha = 0.16f))
                        else Modifier.border(1.dp, cs.outlineVariant, RoundedCornerShape(50))
                    )
                    .clickable { onSelected(i) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Text(
                    Categories[i],
                    color = if (on) cs.primary else cs.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = if (on) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun FeedCard(item: FeedItem) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cs.surfaceContainer),
    ) {
        if (item.image != null) {
            AsyncImage(
                model = item.image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(item.ratio)
                    .background(cs.surfaceContainerHigh),
            )
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            if (item.image == null) {
                Text(
                    item.tag,
                    color = cs.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(6.dp))
            }
            Text(
                item.title,
                color = cs.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.desc != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    item.desc,
                    color = cs.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.tag, color = cs.onSurfaceVariant.copy(alpha = 0.85f), fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = cs.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsPage(dark: Boolean, onDarkChange: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 150.dp),
    ) {
        item { PageHeader("设置", "阅读、缓存与关于") }
        item { Spacer(Modifier.height(20.dp)) }
        item { SearchField("搜索设置…") }

        item { Spacer(Modifier.height(28.dp)) }
        item { SectionLabel("外观") }
        item {
            GroupCard {
                SettingRow(
                    title = "主题",
                    subtitle = if (dark) "深色" else "浅色",
                    trailing = "切换",
                    onClick = { onDarkChange(!dark) },
                )
                Hairline()
                SettingRow("字号", "跟随系统", "标准")
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
        item { SectionLabel("阅读") }
        item {
            GroupCard {
                SettingRow("只读模式", "不提供编辑入口", "关")
                Hairline()
                SettingRow("外链", "在应用内打开", "开")
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
        item { SectionLabel("关于") }
        item {
            GroupCard {
                SettingRow("版本", "0.1.0-debug", "")
                Hairline()
                SettingRow("开源许可", "GPL-3.0 · 含 BiliTools、Backdrop", "")
                Hairline()
                SettingRow("非官方声明", "与天一中学官方无关", "")
            }
        }
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String) {
    val cs = MaterialTheme.colorScheme
    Column {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Spacer(Modifier.height(28.dp))
        Text(title, color = cs.onSurface, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = cs.onSurfaceVariant, fontSize = 14.sp)
    }
}

@Composable
private fun SearchField(hint: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(cs.surfaceContainerHigh)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(hint, color = cs.onSurfaceVariant, fontSize = 15.sp)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
        content = content,
    )
}

@Composable
private fun Hairline() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    )
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    trailing: String,
    onClick: (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = cs.onSurface, fontSize = 16.sp)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = cs.onSurfaceVariant, fontSize = 12.sp)
        }
        Text(trailing, color = cs.onSurfaceVariant, fontSize = 15.sp)
    }
}
