package org.tyzx.wiki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.tyzx.wiki.glass.BarItem
import org.tyzx.wiki.glass.LiquidBottomBar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App() }
    }
}

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    background = Color(0xFF0F0D13),
    surface = Color(0xFF0F0D13),
)

@Composable
fun App() {
    MaterialTheme(colorScheme = DarkColors) {
        val backdrop = rememberGraphicsLayer()
        var tab by remember { mutableIntStateOf(0) }

        Box(Modifier.fillMaxSize().background(Color(0xFF0F0D13))) {
            // 页面内容：录制成 backdrop，供底栏透镜采样
            Box(
                Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        backdrop.record { this@drawWithContent.drawContent() }
                        drawLayer(backdrop)
                    }
            ) {
                DemoPage(tab)
            }

            LiquidBottomBar(
                items = listOf(
                    BarItem("首页", Icons.Filled.Home),
                    BarItem("设置", Icons.Filled.Settings),
                ),
                selected = tab,
                onSelected = { tab = it },
                backdrop = backdrop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp),
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
        // 底栏附近放几个彩色圆斑，折射与色散才看得出来
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
            drawCircle(
                color = Color(0xFFFF7A45).copy(alpha = 0.30f),
                radius = 150.dp.toPx(),
                center = Offset(size.width * 0.52f, size.height * 0.92f),
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
    }
}
