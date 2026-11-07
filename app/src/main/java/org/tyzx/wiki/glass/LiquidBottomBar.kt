package org.tyzx.wiki.glass

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader as AndroidShader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

data class BarItem(val label: String, val icon: ImageVector)

private val TrackColor = Color(0xFF16151A)
private val TrackBorder = Color(0x1FFFFFFF)
private val PillColor = Color(0xFF2C2833)
private val Accent = Color(0xFFD0BCFF)
private val Inactive = Color(0xFF8E8A99)

private val BAR_H = 96.dp
private val TRACK_H = 64.dp
private val PILL_H = 52.dp
private val LENS_H = 78.dp
private val H_PAD = 14.dp

/**
 * 底栏：静止是 Material 3 Expressive 的 tonal 胶囊，按下才“液化”成玻璃透镜，松手凝固回平涂。
 *
 * 通过 [backdrop] 拿到下层内容的录制结果，透镜据此做模糊 + 折射 + 色散。
 */
@Composable
fun LiquidBottomBar(
    items: List<BarItem>,
    selected: Int,
    onSelected: (Int) -> Unit,
    backdrop: GraphicsLayer,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var barOrigin by remember { mutableStateOf(Offset.Zero) }

    val press = remember { Animatable(0f) }
    val lensX = remember { Animatable(0f) }
    var stretchTarget by remember { mutableFloatStateOf(1f) }
    val stretch by animateFloatAsState(
        targetValue = stretchTarget,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 900f),
        label = "stretch",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(BAR_H)
            .onGloballyPositioned { barOrigin = it.positionInRoot() }
    ) {
        val n = items.size
        val wPx = with(density) { maxWidth.toPx() }
        val hPadPx = with(density) { H_PAD.toPx() }
        val tabW = (wPx - hPadPx * 2f) / n
        val pillWPx = tabW - with(density) { 12.dp.toPx() }
        val pillHPx = with(density) { PILL_H.toPx() }
        val lensHPx = with(density) { LENS_H.toPx() }

        fun centerOf(i: Int) = hPadPx + tabW * (i + 0.5f)

        LaunchedEffect(Unit) {
            lensX.snapTo(centerOf(selected.coerceIn(0, n - 1)))
        }

        // 布局尺寸变化时重新对齐
        LaunchedEffect(wPx, n) {
            if (press.value < 0.5f) lensX.snapTo(centerOf(selected.coerceIn(0, n - 1)))
        }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(wPx, n) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startX = down.position.x.coerceIn(centerOf(0), centerOf(n - 1))
                        press.snapTo(1f)
                        lensX.snapTo(startX)

                        var lastX = startX
                        var lastUptime = down.uptimeMillis
                        var dragged = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            val dx = change.position.x - lastX
                            val dt = (change.uptimeMillis - lastUptime).coerceAtLeast(1L)
                            if (abs(dx) > 2f) dragged = true
                            if (dragged) {
                                lastX = change.position.x
                                lensX.snapTo(lastX)
                                val speed = abs(dx) / dt.toFloat()
                                stretchTarget = (1f + speed * 0.22f).coerceIn(1f, 1.30f)
                                change.consume()
                            }
                            lastUptime = change.uptimeMillis
                        }

                        val target = ((lastX - hPadPx) / tabW).toInt().coerceIn(0, n - 1)
                        stretchTarget = 1f
                        press.animateTo(0f, tween(durationMillis = 200))
                        lensX.animateTo(
                            targetValue = centerOf(target),
                            animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
                        )
                        if (target != selected) onSelected(target)
                    }
                }
        ) {
            // 1. 轨道 + 平涂胶囊
            Canvas(Modifier.fillMaxSize()) {
                val trackTop = (size.height - TRACK_H.toPx()) / 2f
                val trackSize = Size(size.width - hPadPx * 2f, TRACK_H.toPx())
                val radius = TRACK_H.toPx() / 2f
                drawRoundRect(
                    color = TrackColor,
                    topLeft = Offset(hPadPx, trackTop),
                    size = trackSize,
                    cornerRadius = CornerRadius(radius, radius),
                )
                drawRoundRect(
                    color = TrackBorder,
                    topLeft = Offset(hPadPx, trackTop),
                    size = trackSize,
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = 1.dp.toPx()),
                )

                val flat = 1f - press.value
                if (flat > 0.01f) {
                    val pillW = pillWPx * stretch
                    drawRoundRect(
                        color = PillColor.copy(alpha = flat),
                        topLeft = Offset(lensX.value - pillW / 2f, (size.height - pillHPx) / 2f),
                        size = Size(pillW, pillHPx),
                        cornerRadius = CornerRadius(pillHPx / 2f, pillHPx / 2f),
                    )
                }
            }

            // 2. 图标与文字
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(BAR_H)
                    .padding(horizontal = H_PAD),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { i, item ->
                    val active = i == selected
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (active) Accent else Inactive,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = item.label,
                            color = if (active) Accent else Inactive,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            // 3. 玻璃透镜
            val p = press.value
            if (p > 0.01f) {
                val lensW = (pillWPx * stretch * (1f + 0.06f * p)).coerceAtLeast(1f)
                val lensH = pillHPx + (lensHPx - pillHPx) * p
                val left = lensX.value - lensW / 2f
                val top = (with(density) { BAR_H.toPx() } - lensH) / 2f

                GlassLens(
                    backdrop = backdrop,
                    absoluteLeft = barOrigin.x + left,
                    absoluteTop = barOrigin.y + top,
                    widthPx = lensW,
                    heightPx = lensH,
                    progress = p,
                    modifier = Modifier
                        .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                        .size(
                            width = with(density) { lensW.toDp() },
                            height = with(density) { lensH.toDp() },
                        ),
                )
            }
        }
    }
}

@Composable
private fun GlassLens(
    backdrop: GraphicsLayer,
    absoluteLeft: Float,
    absoluteTop: Float,
    widthPx: Float,
    heightPx: Float,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val shader = remember { RuntimeShader(LENS_AGSL) }
    val shape = RoundedCornerShape(percent = 50)

    Box(modifier) {
        Canvas(
            Modifier
                .fillMaxSize()
                .clip(shape)
        ) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            shader.setFloatUniform("size", w, h)
            shader.setFloatUniform("cornerRadii", h / 2f, h / 2f, h / 2f, h / 2f)
            shader.setFloatUniform("refractionHeight", with(density) { 34.dp.toPx() })
            shader.setFloatUniform("refractionAmount", with(density) { 20.dp.toPx() })
            shader.setFloatUniform("depthEffect", 0.22f)
            shader.setFloatUniform("chromaticAberration", 1.15f)

            val effect = RenderEffect.createChainEffect(
                RenderEffect.createRuntimeShaderEffect(shader, "content"),
                RenderEffect.createBlurEffect(
                    with(density) { 16.dp.toPx() },
                    with(density) { 16.dp.toPx() },
                    AndroidShader.TileMode.CLAMP,
                ),
            )

            drawIntoCanvas { canvas ->
                val paint = Paint()
                paint.asFrameworkPaint().setRenderEffect(effect)
                canvas.saveLayer(Rect(0f, 0f, w, h), paint)
                translate(-absoluteLeft, -absoluteTop) {
                    drawLayer(backdrop)
                }
                canvas.restore()
            }
        }

        // 彩虹描边 + 高光
        Canvas(Modifier.fillMaxSize()) {
            val alpha = progress
            val rimWidth = 2.2.dp.toPx()
            val brush = Brush.sweepGradient(
                0.00f to Color(0xFFFF8A4C),
                0.14f to Color(0xFFFF3D6E),
                0.30f to Color(0xFF8B5CF6),
                0.48f to Color(0xFF2DD4FF),
                0.64f to Color(0xFF3B82F6),
                0.80f to Color(0xFFA3E635),
                1.00f to Color(0xFFFF8A4C),
            )
            drawRoundRect(
                brush = brush,
                alpha = alpha * 0.85f,
                style = Stroke(width = rimWidth),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.16f * alpha),
                style = Stroke(width = 1.dp.toPx()),
            )
            // 顶部内高光
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.10f * alpha), Color.Transparent),
                ),
                style = Stroke(width = 3.dp.toPx()),
            )
        }
    }
}
