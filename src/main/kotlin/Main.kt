import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
fun main() = application {
    val virtualWidth = 165.dp
    val virtualHeight = 30.dp
    val initialScale = 0.8f

    val windowState = rememberWindowState(
        size = DpSize(virtualWidth * initialScale, virtualHeight * initialScale),
        position = WindowPosition(Alignment.Center)
    )
    var isHovered by remember { mutableStateOf(false) }
    val targetAlpha = if (isHovered) 0.6f else 0f
    val animatedAlpha by animateFloatAsState(targetValue = targetAlpha)
    var showMenu by remember { mutableStateOf(false) }

    val stockViewModel = remember { StockViewModel() }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        title = "Vanish",
        // 核心修正：将 resizable 设置为 false
        // 目的：完全禁止用户通过拖拽边缘或角落来调整窗口大小。
        // 原理：这是 Window Composable 提供的原生参数，它会直接通知操作系统窗口管理器锁定窗口尺寸。
        resizable = false
    ) {
        // 既然窗口大小固定，就不再需要复杂的缩放逻辑了
        // 我们可以直接使用 Box 来显示内容

        // 仅在窗口创建时执行一次，用于防止背景闪烁
        LaunchedEffect(Unit) {
            window.background = java.awt.Color(0, 0, 0, 0)
        }

        WindowDraggableArea {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Enter -> isHovered = true
                                    PointerEventType.Exit -> isHovered = false
                                    PointerEventType.Press -> {
                                        if (event.buttons.isSecondaryPressed) {
                                            showMenu = true
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = animatedAlpha))
            ) {
                StockInfo(stockViewModel.stockData.value)

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(onClick = {
                        stockViewModel.refresh()
                        showMenu = false
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("刷新")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StockInfo(stockData: StockData?) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stockData?.price?.let { "%.2f".format(it) } ?: "--.--",
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = stockData?.changePercent?.let { "%.2f%%".format(it) } ?: "--.--%",
            color = when {
                stockData == null -> Color.White
                stockData.changePercent >= 0 -> Color(0xFFd81e06)
                else -> Color(0xFF1aad19)
            },
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = stockData?.rise?.let { "%.2f%%".format(it) } ?: "--.--%",
            color = when {
                stockData == null -> Color.White
                stockData.rise > 0.1 -> Color(0xFFd81e06)
                stockData.rise < -0.1 -> Color(0xFF1aad19)
                else -> Color.White
            },
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = stockData?.indexPercent?.let { "%.2f%%".format(it) } ?: "--.--%",
            color = when {
                stockData == null -> Color.White
                stockData.indexPercent >= 0 -> Color(0xFFd81e06)
                else -> Color(0xFF1aad19)
            },
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Preview
@Composable
fun PreviewStockInfo() {
    MaterialTheme {
        Box(Modifier.background(Color.Black).size(240.dp * 0.8f, 40.dp * 0.8f)) {
            StockInfo(
                stockData = StockData(price = 12.34, changePercent = 1.23, rise = 0.5, indexPercent = -0.25)
            )
        }
    }
}