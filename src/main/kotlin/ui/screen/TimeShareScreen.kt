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
import androidx.compose.runtime.*
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
import kotlinx.coroutines.delay
import viewmodel.TimeShareUiState
import viewmodel.TimeShareViewModel
import java.awt.Dialog
import kotlin.math.max

/**
 * 分时图屏幕，负责展示股票的分钟级价格走势。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimeShareScreen(
    code: String,
    dialogState: DialogState,
    onCloseRequest: () -> Unit
) {
    val viewModel = remember(code) { TimeShareViewModel(code) }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.loadTimeShareData()
            delay(5000)
        }
    }

    DialogWindow(
        onCloseRequest = onCloseRequest,
        state = dialogState,
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = false
    ) {
        // 核心修正：采用“组合拳”方案，彻底阻止窗口出现在任务切换器中
        LaunchedEffect(window) {
            window.modalExclusionType = Dialog.ModalExclusionType.APPLICATION_EXCLUDE
            window.focusableWindowState = false
        }

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
