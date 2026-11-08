package org.tyzx.wiki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.tyzx.wiki.glass.TyBottomBar
import org.tyzx.wiki.glass.TyTab
import org.tyzx.wiki.glass.liquidtabs.LiquidGlassStyle
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import kotlinx.coroutines.launch
import org.tyzx.wiki.net.Wiki
import org.tyzx.wiki.ui.AuthScreen
import org.tyzx.wiki.ui.DiscussionScreen
import org.tyzx.wiki.ui.EditorScreen
import org.tyzx.wiki.ui.ExploreScreen
import org.tyzx.wiki.ui.HomeScreen
import org.tyzx.wiki.ui.ReaderScreen
import org.tyzx.wiki.ui.VisualEditorScreen

private val DarkColors = darkColorScheme(
    primary = Color(0xFFCFBCFF),
    onPrimary = Color(0xFF36246B),
    primaryContainer = Color(0xFF4B3A85),
    onPrimaryContainer = Color(0xFFE8DDFF),
    secondaryContainer = Color(0xFF332C4C),
    onSecondaryContainer = Color(0xFFE6DEFF),
    background = Color(0xFF111016),
    surface = Color(0xFF111016),
    surfaceContainerLowest = Color(0xFF0B0A0F),
    surfaceContainerLow = Color(0xFF16141D),
    surfaceContainer = Color(0xFF1C1A24),
    surfaceContainerHigh = Color(0xFF262231),
    surfaceContainerHighest = Color(0xFF302B3D),
    surfaceBright = Color(0xFF2A2536),
    onSurface = Color(0xFFE9E4F2),
    onSurfaceVariant = Color(0xFFA9A1BC),
    outlineVariant = Color(0xFF3B3448),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6C5BD4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4DBFF),
    onPrimaryContainer = Color(0xFF221A4E),
    secondaryContainer = Color(0xFFEDE6FF),
    onSecondaryContainer = Color(0xFF2A2350),
    background = Color(0xFFF6F4FC),
    surface = Color(0xFFF6F4FC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1EEFA),
    surfaceContainer = Color(0xFFE9E4F8),
    surfaceContainerHigh = Color(0xFFDFD8F2),
    surfaceContainerHighest = Color(0xFFD4CCEC),
    surfaceBright = Color(0xFFFBF9FF),
    onSurface = Color(0xFF1B1725),
    onSurfaceVariant = Color(0xFF5C5470),
    outlineVariant = Color(0xFFCFC6E4),
)

private val Tabs = listOf(
    TyTab("首页", Icons.Filled.Home),
    TyTab("设置", Icons.Filled.Settings),
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
    val context = LocalContext.current
    var dark by remember { mutableStateOf(false) }
    var dynamic by remember { mutableStateOf(false) }
    val scheme = when {
        dynamic && dark -> dynamicDarkColorScheme(context)
        dynamic -> dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }

    MaterialTheme(colorScheme = scheme) {
        val cs = MaterialTheme.colorScheme
        val backdrop = rememberLayerBackdrop()
        val scope = rememberCoroutineScope()
        var tab by remember { mutableIntStateOf(0) }
        var reading by remember { mutableStateOf<String?>(null) }
        var exploring by remember { mutableStateOf(false) }
        var authing by remember { mutableStateOf(false) }
        var editing by remember { mutableStateOf<String?>(null) }
        var visual by remember { mutableStateOf<String?>(null) }
        var editPick by remember { mutableStateOf<String?>(null) }
        var discussing by remember { mutableStateOf<String?>(null) }
        var tick by remember { mutableIntStateOf(0) }
        var account by remember { mutableStateOf(Wiki.user) }

        BackHandler(enabled = reading != null && editing == null && discussing == null) { reading = null }
        BackHandler(enabled = discussing != null && editing == null) { discussing = null }
        BackHandler(enabled = reading == null && exploring && editing == null) { exploring = false }
        BackHandler(enabled = reading == null && !exploring && authing) { authing = false }

        Box(Modifier.fillMaxSize().background(cs.background)) {
            Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                val title = reading
                val editingTitle = editing
                val discussingTitle = discussing
                if (discussingTitle != null) {
                    DiscussionScreen(
                        title = discussingTitle,
                        onBack = { discussing = null },
                        onOpen = { discussing = null; reading = it },
                    )
                } else if (visual != null) {
                    VisualEditorScreen(
                        title = visual!!,
                        onBack = { visual = null },
                        onSaved = { visual = null; tick++ },
                    )
                } else if (editingTitle != null) {
                    EditorScreen(
                        title = editingTitle,
                        onBack = { editing = null },
                        onSaved = { editing = null; tick++ },
                    )
                } else if (title != null) {
                    androidx.compose.runtime.key(tick) {
                        ReaderScreen(
                            title = title,
                            onBack = { reading = null },
                            onOpen = { reading = it },
                            onEdit = { editPick = title },
                            onDiscuss = { discussing = title },
                            onShare = {
                                val url = "https://wiki.tyzx.org/wiki/" +
                                    java.net.URLEncoder.encode(title.replace(' ', '_'), "UTF-8")
                                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, "$title\n$url")
                                }
                                context.startActivity(
                                    android.content.Intent.createChooser(send, "分享条目")
                                )
                            },
                        )
                    }
                } else if (authing) {
                    AuthScreen(onBack = { authing = false }, onDone = { account = it; authing = false })
                } else if (exploring) {
                    ExploreScreen(onBack = { exploring = false }, onOpen = { reading = it })
                } else if (tab == 0) {
                    HomeScreen(onOpen = { reading = it }, onSearch = { exploring = true })
                } else {
                    SettingsPage(
                        dark = dark,
                        dynamic = dynamic,
                        onDarkChange = { dark = it },
                        onDynamicChange = { dynamic = it },
                        account = account,
                        onAuth = { authing = true },
                        onLogout = {
                            scope.launch { Wiki.logout() }
                            account = null
                        },
                    )
                }
            }
            if (reading == null && !exploring && !authing && editing == null && discussing == null) {
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
}

@Composable
private fun SettingsPage(
    dark: Boolean,
    dynamic: Boolean,
    onDarkChange: (Boolean) -> Unit,
    onDynamicChange: (Boolean) -> Unit,
    account: String?,
    onAuth: () -> Unit,
    onLogout: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 150.dp),
    ) {
        item { PageHeader("设置", "阅读、缓存与关于") }
        item { Spacer(Modifier.height(20.dp)) }
        item { SearchField("搜索设置…") }

        item { Spacer(Modifier.height(28.dp)) }
        item { SectionLabel("账号") }
        item {
            GroupCard {
                if (account == null) {
                    SettingRow(
                        title = "登录 / 注册",
                        subtitle = "编辑条目、上传图片、讨论页发言都需要账号",
                        trailing = "去登录",
                        onClick = onAuth,
                    )
                } else {
                    SettingRow(
                        title = account!!,
                        subtitle = "已登录，可编辑与上传",
                        trailing = "退出",
                        onClick = onLogout,
                    )
                }
            }
        }

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
                SettingRow(
                    title = "动态取色",
                    subtitle = if (dynamic) "跟随壁纸（Material You）" else "使用天一紫",
                    trailing = if (dynamic) "开" else "关",
                    onClick = { onDynamicChange(!dynamic) },
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
                SettingRow("版本", "0.2.0", "")
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
        Text(title, color = cs.onSurface, fontSize = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = cs.onSurfaceVariant, fontSize = 14.sp)
    }
}

@Composable
private fun SearchField(hint: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp))
            .background(cs.surfaceContainerHigh).padding(horizontal = 18.dp),
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
        content = content,
    )
}

@Composable
private fun Hairline() {
    Box(
        Modifier.fillMaxWidth().padding(start = 16.dp).height(1.dp)
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
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
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
