package org.tyzx.wiki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.tyzx.wiki.glass.TyBottomBar
import org.tyzx.wiki.glass.TyTab
import org.tyzx.wiki.glass.liquidtabs.LiquidGlassStyle

// 四层表面语义（照 BiliTools AppSurfaces 的取值区间）：页面 / 卡片 / 内嵌 / 模态
private val DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF1D192B),
    background = Color(0xFF121016),        // page  T94
    surface = Color(0xFF121016),
    surfaceContainerLowest = Color(0xFF0C0A10),
    surfaceContainerLow = Color(0xFF17141C),
    surfaceContainer = Color(0xFF1D1A22),  // card  T98
    surfaceContainerHigh = Color(0xFF26222C), // inset T95
    surfaceContainerHighest = Color(0xFF2B2733), // modal T90
    surfaceBright = Color(0xFF1D1A22),
    onSurface = Color(0xFFE8E3EC),
    onSurfaceVariant = Color(0xFFA9A3B5),
    outlineVariant = Color(0xFF3A3542),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFF5F5FA),        // page
    surface = Color(0xFFF5F5FA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAFD),
    surfaceContainer = Color(0xFFFFFFFF),  // card
    surfaceContainerHigh = Color(0xFFEDEDF3), // inset
    surfaceContainerHighest = Color(0xFFE4E4EC), // modal
    surfaceBright = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1B21),
    onSurfaceVariant = Color(0xFF5D5D68),
    outlineVariant = Color(0xFFD5D5DE),
)

private val Tabs = listOf(
    TyTab("首页", Icons.Filled.Home),
    TyTab("设置", Icons.Filled.Settings),
)

/** 中文站现有的全部条目 */
private val Entries = listOf(
    "首页" to "站点导航与近期更新",
    "天一中学" to "学校的概况、历史与现状",
    "学校历史" to "从建校到今天的完整时间线",
    "江苏省天一中学" to "官方名称、校区与办学信息",
    "天一中学AP课程班" to "AP 课程班的课程设置与招录",
    "AP课程" to "开设科目、考试与学分说明",
    "天一中学图灵社" to "校内计算机社团的活动与作品",
    "总集" to "全部条目的汇总索引",
    "生成式AI使用方针" to "校内对生成式 AI 的使用规范",
    "合唱节" to "校园合唱节的历届记录",
    "科技节" to "科技节的展项与比赛",
    "话剧节" to "话剧节的剧目与演出",
    "樱花节" to "樱花季的校园活动",
    "运动会" to "校运会的项目与成绩",
    "数学“π”节" to "数学节的活动与题目",
    "Bad Apple!!" to "社团制作的影响影绘作品",
)

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
                if (tab == 0) HomePage() else SettingsPage(dark) { dark = it }
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
private fun HomePage() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 140.dp),
    ) {
        item { PageHeader("天一维基", "江苏省天一中学 · 非官方维基") }
        item { Spacer(Modifier.height(20.dp)) }
        item { SearchField("搜索条目…") }
        item { Spacer(Modifier.height(28.dp)) }
        item { SectionLabel("条目 ${Entries.size}") }
        item {
            GroupCard {
                Entries.forEachIndexed { i, (title, desc) ->
                    if (i > 0) Hairline()
                    EntryRow(title, desc)
                }
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
        item {
            Text(
                text = "本站内容来自 wiki.tyzx.org，本应用为非官方客户端",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun SettingsPage(dark: Boolean, onDarkChange: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 140.dp),
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
                SettingRow("只读模式", "不提供编辑入口", "开")
                Hairline()
                SettingRow("外链", "在应用内打开", "开")
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
        item { SectionLabel("离线") }
        item {
            GroupCard {
                SettingRow("已缓存", "0 个页面", "")
                Hairline()
                SettingRow("清除缓存", "连同缩略图一起删除", "")
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
        item { SectionLabel("关于") }
        item {
            GroupCard {
                SettingRow("版本", "0.0.4-debug", "")
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
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = cs.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
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
private fun EntryRow(title: String, desc: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = cs.onSurface, fontSize = 16.sp)
            Spacer(Modifier.height(3.dp))
            Text(desc, color = cs.onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
        }
        Icon(
            Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = cs.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp),
        )
    }
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
