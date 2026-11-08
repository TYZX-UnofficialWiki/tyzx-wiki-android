package org.tyzx.wiki.glass

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.RuntimeShader
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

data class BarItem(val label: String, val icon: ImageVector)

private val TrackColor = Color(0xFF16151A)
private val TrackBorder = Color(0x1FFFFFFF)
private val PillColor = Color(0xFF2A2630)
private val Accent = Color(0xFFD0BCFF)
private val Inactive = Color(0xFF8E8A99)

private val BAR_H = 84.dp
private val TRACK_H = 64.dp
private val PILL_H = 52.dp
private val LENS_H = 74.dp
private val H_PAD = 12.dp

/** 截图缩放比：先降采样再用双线性放大，等于一层便宜的模糊 */
private const val SNAPSHOT_SCALE = 0.5f

/**
 * 底栏：静止是 Material 3 Expressive 的 tonal 胶囊；按下才“液化”成玻璃透镜，
 * 拖动时跟着手指走，松手凝固回平涂。
 *
 * 玻璃的取景方式是：按下瞬间把 [backdrop]（下层页面）抓成一张位图，
 * 再用 AGSL 把这张位图按折射法线重新采样。不依赖任何层的实时回读，
 * 采样坐标完全由 Matrix 决定，行为可预期。
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
    val scope = rememberCoroutineScope()
    var barOrigin by remember { mutableStateOf(Offset.Zero) }

    var pressed by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var stretchTarget by remember { mutableFloatStateOf(1f) }
    var snapshot by remember { mutableStateOf<Bitmap?>(null) }

    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(durationMillis = if (pressed) 80 else 220),
        label = "press",
    )
    val stretch by animateFloatAsState(
        targetValue = stretchTarget,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 900f),
        label = "stretch",
    )

    val lensX = remember { Animatable(0f) }
    val currentX = if (dragging) dragX else lensX.value

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
        val pillWPx = tabW - with(density) { 10.dp.toPx() }
        val pillHPx = with(density) { PILL_H.toPx() }
        val lensHPx = with(density) { LENS_H.toPx() }
        val barHPx = with(density) { BAR_H.toPx() }

        fun centerOf(i: Int) = hPadPx + tabW * (i + 0.5f)

        LaunchedEffect(wPx, n, selected) {
            if (!pressed && !dragging) {
                lensX.snapTo(centerOf(selected.coerceIn(0, n - 1)))
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(wPx, n) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startX = down.position.x.coerceIn(centerOf(0), centerOf(n - 1))
                        pressed = true
                        dragging = true
                        dragX = startX

                        // 按下即抓一帧页面，玻璃后面就是这一帧。
                        // 旧帧先留着当占位，回读完成再替换，避免玻璃闪一下白。
                        scope.launch {
                            val fresh = runCatching {
                                val full = backdrop.toImageBitmap().asAndroidBitmap()
                                Bitmap.createScaledBitmap(
                                    full,
                                    (full.width * SNAPSHOT_SCALE).toInt(),
                                    (full.height * SNAPSHOT_SCALE).toInt(),
                                    true,
                                )
                            }.getOrNull()
                            if (fresh != null) snapshot = fresh
                        }

                        var lastX = startX
                        var lastUptime = down.uptimeMillis
                        var moved = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            val dx = change.position.x - lastX
                            val dt = (change.uptimeMillis - lastUptime).coerceAtLeast(1L)
                            if (abs(dx) > 2f) moved = true
                            if (moved) {
                                lastX = change.position.x
                                dragX = lastX
                                val speed = abs(dx) / dt.toFloat()
                                stretchTarget = (1f + speed * 0.22f).coerceIn(1f, 1.30f)
                                change.consume()
                            }
                            lastUptime = change.uptimeMillis
                        }

                        val target = ((dragX - hPadPx) / tabW).toInt().coerceIn(0, n - 1)
                        stretchTarget = 1f
                        pressed = false
                        dragging = false
                        scope.launch {
                            lensX.snapTo(dragX)
                            lensX.animateTo(
                                targetValue = centerOf(target),
                                animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
                            )
                        }
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

                val flat = 1f - press
                if (flat > 0.01f) {
                    val pillW = pillWPx * stretch
                    drawRoundRect(
                        color = PillColor.copy(alpha = flat),
                        topLeft = Offset(currentX - pillW / 2f, (size.height - pillHPx) / 2f),
                        size = Size(pillW, pillHPx),
                        cornerRadius = CornerRadius(pillHPx / 2f, pillHPx / 2f),
                    )
                }
            }

            // 2. 图标与文字（在玻璃下面，会被玻璃一起折射）
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
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = item.label,
                            color = if (active) Accent else Inactive,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            // 3. 玻璃透镜
            if (press > 0.01f) {
                val lensW = (pillWPx * stretch * (1f + 0.05f * press)).coerceAtLeast(1f)
                val lensH = pillHPx + (lensHPx - pillHPx) * press
                val left = currentX - lensW / 2f
                val top = (barHPx - lensH) / 2f

                GlassLens(
                    snapshot = snapshot,
                    absoluteLeft = barOrigin.x + left,
                    absoluteTop = barOrigin.y + top,
                    alpha = press,
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
    snapshot: Bitmap?,
    absoluteLeft: Float,
    absoluteTop: Float,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val shader = remember { runCatching { RuntimeShader(LENS_AGSL) }.getOrNull() }
    val bitmapShader = remember(snapshot) {
        snapshot?.let { BitmapShader(it, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP) }
    }

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            if (shader == null || bitmapShader == null) {
                // 还没抓到帧 / 着色器不可用：先给一层磨砂，不闪不崩
                drawRoundRect(color = Color(0x22FFFFFF))
                return@Canvas
            }

            val matrix = Matrix().apply {
                setScale(SNAPSHOT_SCALE, SNAPSHOT_SCALE)
                postTranslate(absoluteLeft * SNAPSHOT_SCALE, absoluteTop * SNAPSHOT_SCALE)
            }
            bitmapShader.setLocalMatrix(matrix)

            shader.setInputShader("content", bitmapShader)
            shader.setFloatUniform("size", w, h)
            shader.setFloatUniform("cornerRadii", h / 2f, h / 2f, h / 2f, h / 2f)
            shader.setFloatUniform("refractionHeight", 26.dp.toPx())
            shader.setFloatUniform("refractionAmount", 14.dp.toPx())
            shader.setFloatUniform("depthEffect", 0.18f)
            shader.setFloatUniform("chromaticAberration", 0.9f)

            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    setShader(shader)
                    isAntiAlias = true
                    this.alpha = (alpha * 255).toInt()
                }
                val r = h / 2f
                canvas.nativeCanvas.drawRoundRect(0f, 0f, w, h, r, r, paint)
            }

            // 玻璃自身的奶白，别让折射把页面压暗
            drawRoundRect(color = Color(0x0DFFFFFF), alpha = alpha)
        }

        // 彩虹描边 + 内高光
        Canvas(Modifier.fillMaxSize()) {
            val rimWidth = 1.6.dp.toPx()
            val brush = Brush.sweepGradient(
                0.00f to Color(0xFFFF9A5C),
                0.13f to Color(0xFFFF4D7A),
                0.29f to Color(0xFF9B6BFF),
                0.47f to Color(0xFF35D6FF),
                0.63f to Color(0xFF4C8DFF),
                0.79f to Color(0xFFB6E85B),
                1.00f to Color(0xFFFF9A5C),
            )
            drawRoundRect(
                brush = brush,
                alpha = alpha * 0.55f,
                style = Stroke(width = rimWidth),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.22f * alpha),
                style = Stroke(width = 1.dp.toPx()),
            )
            // 顶部内高光
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.12f * alpha), Color.Transparent),
                ),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}
