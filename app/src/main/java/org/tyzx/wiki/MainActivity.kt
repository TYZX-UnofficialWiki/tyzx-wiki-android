package org.tyzx.wiki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.tyzx.wiki.glass.TyBottomBar
import org.tyzx.wiki.glass.TyTab
import org.tyzx.wiki.glass.liquidtabs.LiquidGlassStyle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App() }
    }
}

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onSurfaceVariant = Color(0xFF0E4C52),
    background = Color(0xFF0F0D13),
    surface = Color(0xFF0F0D13),
)

private val Tabs = listOf(
    TyTab("首页", Icons.Filled.Home),
    TyTab("设置", Icons.Filled.Settings),
)

@Composable
fun App() {
    MaterialTheme(colorScheme = DarkColors) {
        val backdrop = rememberLayerBackdrop()
        var tab by remember { mutableIntStateOf(0) }

        Box(Modifier.fillMaxSize().background(Color(0xFF0F0D13))) {
            // 页面内容录进 backdrop，底栏玻璃从这里采样
            Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                DemoPage(tab)
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
                surfaceAlpha = 0.22f,
            )
        }
    }
}

@Composable
private fun DemoPage(tab: Int) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF2B1B45), Color(0xFF0F0D13)),
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFF7C4DFF).copy(alpha = 0.55f),
                radius = 260.dp.toPx(),
                center = Offset(size.width * 0.18f, size.height * 0.74f),
            )
            drawCircle(
                color = Color(0xFF00C2FF).copy(alpha = 0.38f),
                radius = 200.dp.toPx(),
                center = Offset(size.width * 0.88f, size.height * 0.82f),
            )
        }

        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(96.dp))
            Text(
                text = if (tab == 0) "底栏试验" else "设置",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "按住底栏：液化\n拖动：跟着手指走\n松手：凝固回平涂",
                color = Color(0xFFB9B4C7),
                fontSize = 15.sp,
                lineHeight = 24.sp,
            )
            Spacer(Modifier.height(28.dp))
            repeat(6) { i ->
                Text(
                    text = "示例条目 ${i + 1} · 这里以后是维基内容",
                    color = Color(0x66FFFFFF),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
            }
        }

        // 底栏正下方放带文字的卡片：玻璃要有边界才看得出折射
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 30.dp)
        ) {
            repeat(3) { i ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1C1922))
                        .padding(14.dp)
                ) {
                    Text("示例条目 ${i + 4}", color = Color(0xFFEDE7F6), fontSize = 15.sp)
                    Text("这段文字会被玻璃折射成两层", color = Color(0x99EDE7F6), fontSize = 12.sp)
                }
            }
        }
    }
}
