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
    // 定义上涨、下跌和中线的颜色
    val upColor = Color(0xFFd81e06)
    val downColor = Color(0xFF1aad19)
    val midLineColor = Color.Gray

    // Canvas是Compose中的一个可组合函数，它提供了一个可以在其中进行自定义2D图形绘制的区域。
    // 我们在这里使用它来绘制分时图的背景网格和价格线。
    // Modifier.padding用于在Canvas周围添加一些空间，以避免图形紧贴边缘。
    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
        if (points.isEmpty()) return@Canvas

        // --- Y轴计算 ---
        val maxDiff = points.maxOfOrNull { max(it.price - preClosePrice, preClosePrice - it.price) } ?: 0.01f
        val priceRange = (maxDiff * 2).coerceAtLeast(0.01f)
        val minPrice = preClosePrice - maxDiff
        val yScale = size.height / priceRange

        // --- X轴计算 ---
        // 1. 定义总分钟数：
        //    集合竞价 (9:15-9:30) 15分钟 + 上午盘 (9:30-11:30) 120分钟 + 下午盘 (13:00-15:00) 120分钟 = 255分钟。
        //    这为从9:15开始的数据提供了固定的时间轴。
        val totalMinutes = 255f
        // 2. 计算X轴的缩放比例：将Canvas的宽度映射到总交易分钟数上。
        val xScale = size.width / totalMinutes

        // --- 绘制背景网格 ---

        // 绘制水平中线（昨日收盘价线）
        val midY = size.height / 2
        drawLine(
            color = midLineColor,
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
        )

        // 定义时间刻度的分钟位置
        // 分钟数是相对于9:15开始的总时长计算的。
        val timeMarkersMinutes = listOf(
            0,    // 9:15
            15,   // 9:30
            75,   // 10:30
            135,  // 11:30/13:00
            195,  // 14:00
            255   // 15:00
        )

        // 遍历时间刻度，绘制所有垂直虚线，包括起始和结束位置。
        timeMarkersMinutes.forEach { minute ->
            val x = minute * xScale
            drawLine(
                color = midLineColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
            )
        }

        // --- 绘制价格线 ---
        // 检查是否有足够的数据点来绘制线条
        if (points.size < 2) return@Canvas

        // 遍历所有数据点，绘制价格走势
        // 数据从9:15开始，因此第一个数据点（索引0）应绘制在图表的起始位置（x=0）。
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]

            // 根据数据点在列表中的索引（代表时间流逝）计算x坐标。
            val x1 = i * xScale
            val y1 = size.height - (p1.price - minPrice) * yScale
            val x2 = (i + 1) * xScale
            val y2 = size.height - (p2.price - minPrice) * yScale

            // 根据价格决定颜色
            val color = if (p2.price >= preClosePrice) upColor else downColor

            // 绘制线段
            drawLine(
                color = color,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 1.5f
            )
        }
    }
}
