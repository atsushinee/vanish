import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
fun main() = application {
    val virtualWidth = 135.dp
    val virtualHeight = 30.dp
    val initialScale = 0.8f

    val initialX = AppConfig.getProperty("window.x", "0").toFloat()
    val initialY = AppConfig.getProperty("window.y", "0").toFloat()

    val windowState = rememberWindowState(
        size = DpSize(virtualWidth * initialScale, virtualHeight * initialScale),
        position = WindowPosition(initialX.dp, initialY.dp)
    )
    var isHovered by remember { mutableStateOf(false) }
    val targetAlpha = if (isHovered) 0.0f else 0f
    val animatedAlpha by animateFloatAsState(targetValue = targetAlpha)
    var showContextMenu by remember { mutableStateOf(false) }
    var showHistoryPopup by remember { mutableStateOf(false) }

    val stockViewModel = remember { StockViewModel() }

    Window(
        onCloseRequest = {
            AppConfig.setProperty("window.x", windowState.position.x.value.toString())
            AppConfig.setProperty("window.y", windowState.position.y.value.toString())
            AppConfig.save()
            exitApplication()
        },
        state = windowState,
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        title = "Vanish",
        resizable = false
    ) {
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
                                            showContextMenu = true
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

                if (showContextMenu) {
                    Popup(
                        alignment = Alignment.Center,
                        offset = IntOffset(0, 0),
                        onDismissRequest = { showContextMenu = false }
                    ) {
                        Row(
                            modifier = Modifier
                                .width(virtualWidth * initialScale)
                                .height(virtualHeight * initialScale)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(horizontal = 4.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(onTap = {
                                        showContextMenu = false
                                        println("点击菜单背景，已关闭")
                                    })
                                },
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                stockViewModel.refresh()
                                showContextMenu = false
                                println("刷新按钮点击")
                            }) {
                                Icon(Icons.Default.Refresh, "刷新", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                showHistoryPopup = true
                                showContextMenu = false
                                println("历史按钮点击")
                            }) {
                                Icon(Icons.Default.History, "历史", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                showContextMenu = false
                                println("设置按钮点击")
                            }) {
                                Icon(Icons.Default.Settings, "设置", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // 关键：将历史窗口的逻辑移到主窗口之外，并使用 DialogWindow
    if (showHistoryPopup) {
        // 使用 DialogWindow 创建一个真正的、独立的窗口，但它不会在任务栏显示图标
        DialogWindow(
            onCloseRequest = { showHistoryPopup = false }, // 当窗口请求关闭时（例如失去焦点），隐藏它
            undecorated = true,
            transparent = true,
            alwaysOnTop = true,
            resizable = false,
            state = rememberDialogState(
                // 关键：精确定位，使其紧贴主窗口右侧
                position = WindowPosition(
                    x = windowState.position.x + windowState.size.width, // 主窗口X坐标 + 主窗口宽度
                    y = windowState.position.y // 与主窗口Y坐标对齐
                ),
                size = DpSize(320.dp, 300.dp) // 设置固定的尺寸
            )
        ) {
            val listState = rememberLazyListState()
            LaunchedEffect(stockViewModel.history.size) {
                if (stockViewModel.history.isNotEmpty()) {
                    listState.animateScrollToItem(stockViewModel.history.size - 1)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                ) {
                    item {
                        Row(Modifier.padding(bottom = 8.dp)) {
                            Text("时间", Modifier.width(70.dp), color = Color.LightGray, fontSize = 10.sp)
                            Text("代码", Modifier.width(60.dp), color = Color.LightGray, fontSize = 10.sp)
                            Text("现价", Modifier.width(50.dp), color = Color.LightGray, fontSize = 10.sp)
                            Text("涨幅", Modifier.width(50.dp), color = Color.LightGray, fontSize = 10.sp)
                            Text("涨速", Modifier.width(50.dp), color = Color.LightGray, fontSize = 10.sp)
                            Text("大盘", Modifier.width(40.dp), color = Color.LightGray, fontSize = 10.sp)
                        }
                    }
                    items(stockViewModel.history) { data ->
                        HistoryRow(data)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(data: StockData) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm:ss") }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(data.timestamp.format(timeFormatter), Modifier.width(70.dp), color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(data.code, Modifier.width(60.dp), color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text("%.2f".format(data.price), Modifier.width(50.dp), color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        val changeColor = if (data.changePercent >= 0) Color(0xFFd81e06) else Color(0xFF1aad19)
        Text("%.2f%%".format(data.changePercent), Modifier.width(50.dp), color = changeColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        val riseColor = if (data.rise >= 0) Color(0xFFd81e06) else Color(0xFF1aad19)
        Text("%.2f%%".format(data.rise), Modifier.width(50.dp), color = riseColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        val indexColor = if (data.indexPercent >= 0) Color(0xFFd81e06) else Color(0xFF1aad19)
        Text("%.2f%%".format(data.indexPercent), Modifier.width(40.dp), color = indexColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}
