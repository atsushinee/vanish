package ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.DialogWindow
import data.model.TimeSharePoint
import viewmodel.TimeShareUiState
import viewmodel.TimeShareViewModel
import kotlin.math.max

/**
 * 核心重构：将此 Composable 重新定义为“屏幕”（Screen）。
 * 目的：统一项目架构，明确其作为独立UI展示单元的职责，为未来扩展（如日K、周K切换）做准备。
 * 原理：通过重命名文件和函数，并将其移动到 `ui.screen` 包下，使其在项目结构中的定位更加清晰。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimeShareScreen( // 函数名从 TimeShareWindow 改为 TimeShareScreen
    code: String,
    dialogState: DialogState,
    onCloseRequest: () -> Unit
) {
    val viewModel = remember(code) { TimeShareViewModel(code) }
    val uiState by viewModel.uiState.collectAsState()

    DialogWindow(
        onCloseRequest = onCloseRequest,
        state = dialogState,
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = false
    ) {
        MaterialTheme(colors = darkColors()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .combinedClickable(
                        onClick = {},
                        onDoubleClick = { onCloseRequest() }
                    )
            ) {
                when (val state = uiState) {
                    is TimeShareUiState.Loading -> CenteredText("加载中...")
                    is TimeShareUiState.NoData -> CenteredText("暂无数据")
                    is TimeShareUiState.Error -> CenteredText(state.message, Color.Red)
                    is TimeShareUiState.Success -> {
                        if (state.points.isEmpty()) {
                            CenteredText("暂无数据")
                        } else {
                            TimeShareChart(state.points, state.preClosePrice)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredText(text: String, color: Color = Color.White) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = color, fontSize = 12.sp)
    }
}

/**
 * 分时图的绘制组件。
 * 未来可以扩展，根据传入的类型（如日K、周K）改变其绘制逻辑。
 */
@Composable
fun TimeShareChart(points: List<TimeSharePoint>, preClosePrice: Float) {
    val upColor = Color(0xFFd81e06)
    val downColor = Color(0xFF1aad19)
    val midLineColor = Color.Gray

    Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        if (points.size < 2) return@Canvas

        val maxDiff = points.maxOf { max(it.price - preClosePrice, preClosePrice - it.price) }
        val priceRange = (maxDiff * 2).coerceAtLeast(0.01f)
        val minPrice = preClosePrice - maxDiff

        val xScale = size.width / (points.size - 1)
        val yScale = size.height / priceRange

        val midY = size.height / 2
        drawLine(
            color = midLineColor,
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
        )

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]

            val x1 = i * xScale
            val y1 = size.height - (p1.price - minPrice) * yScale
            val x2 = (i + 1) * xScale
            val y2 = size.height - (p2.price - minPrice) * yScale

            val color = if (p2.price >= preClosePrice) upColor else downColor

            drawLine(
                color = color,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 1.5f
            )
        }
    }
}
