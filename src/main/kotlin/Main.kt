import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import java.awt.event.ActionEvent
import java.time.format.DateTimeFormatter
import javax.swing.SwingUtilities

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
fun main() = application {
    // 定义虚拟窗口尺寸和初始缩放比例
    val virtualWidth = 135.dp
    val virtualHeight = 30.dp
    val initialScale = 0.8f

    // 从配置加载初始窗口位置
    val initialX = AppConfig.getProperty("window.x", "0").toFloat()
    val initialY = AppConfig.getProperty("window.y", "0").toFloat()

    // 记住窗口状态，包括大小和位置
    val windowState = rememberWindowState(
        size = DpSize(virtualWidth * initialScale, virtualHeight * initialScale),
        position = WindowPosition(initialX.dp, initialY.dp)
    )

    // 定义各种状态变量
    var isHovered by remember { mutableStateOf(false) }
    val targetAlpha = if (isHovered) 0.0f else 0f
    val animatedAlpha by animateFloatAsState(targetValue = targetAlpha)
    var showContextMenu by remember { mutableStateOf(false) }
    var showHistoryPopup by remember { mutableStateOf(false) }
    var isWindowVisible by remember { mutableStateOf(true) } // 控制主窗口可见性

    // 记住 ViewModel
    val stockViewModel = remember { StockViewModel() }

    // 记住历史记录窗口的状态
    val historyWindowState = rememberDialogState(
        size = DpSize(240.dp, 45.dp)
    )

    // 定义关闭请求的处理逻辑
    val handleCloseRequest = {
        // 保存窗口位置
        if (windowState.position is WindowPosition.Absolute) {
            val pos = windowState.position as WindowPosition.Absolute
            AppConfig.setProperty("window.x", pos.x.value.toString())
            AppConfig.setProperty("window.y", pos.y.value.toString())
        }
        AppConfig.save() // 保存配置
        exitApplication() // 退出应用
    }

    // 使用 LaunchedEffect 监听主窗口位置变化，以同步更新历史窗口的位置
    LaunchedEffect(windowState.position) {
        val historyY = windowState.position.y - historyWindowState.size.height - 4.dp
        val historyX = windowState.position.x + (windowState.size.width - historyWindowState.size.width) / 2
        historyWindowState.position = WindowPosition(x = historyX, y = historyY)
    }

    // 托盘图标状态
    val trayState = rememberTrayState()

    // 托盘图标，加载 "app.ico"
    Tray(
        icon = painterResource("app.ico"),
        state = trayState,
        tooltip = "Vanish", // 鼠标悬停在托盘图标上时显示的工具提示
        onAction = {
            // 左键单击托盘图标时，切换主窗口的可见性
            isWindowVisible = !isWindowVisible
            println("托盘图标被点击，窗口可见性: $isWindowVisible")
        },
        menu = {
            Item("Exit", onClick = {
                println("菜单项 '退出' 被点击")
                handleCloseRequest()
            })
        }
    )

    // 仅当 isWindowVisible 为 true 时，才显示主窗口
    if (isWindowVisible) {
        Window(
            onCloseRequest = { isWindowVisible = false }, // 点击关闭按钮时隐藏窗口，而不是退出
            state = windowState,
            undecorated = true,
            transparent = true,
            alwaysOnTop = true,
            title = "Vanish",
            resizable = false,
            icon = painterResource("app.ico") // 设置窗口图标
        ) {
            // 设置窗口背景为透明
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

                    // 右键上下文菜单
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
                                            println("菜单背景被点击，已关闭")
                                        })
                                    },
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 历史记录按钮
                                IconButton(onClick = {
                                    showHistoryPopup = !showHistoryPopup
                                    showContextMenu = false
                                    println("历史按钮点击，当前状态: $showHistoryPopup")
                                }) {
                                    Icon(Icons.Default.History, "历史", tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                                // 设置按钮
                                IconButton(onClick = {
                                    showContextMenu = false
                                    println("设置按钮点击")
                                }) {
                                    Icon(
                                        Icons.Default.Settings,
                                        "设置",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                // 关闭按钮（隐藏窗口）
                                IconButton(onClick = { isWindowVisible = false }) {
                                    Icon(Icons.Default.Close, "关闭", tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    // 历史记录弹窗
    if (showHistoryPopup) {
        DialogWindow(
            onCloseRequest = { showHistoryPopup = false },
            undecorated = true,
            transparent = true,
            alwaysOnTop = true,
            resizable = false,
            state = historyWindowState
        ) {
            val listState = rememberLazyListState()
            var oldSize by remember { mutableStateOf(0) }

            // 当历史记录大小变化时，自动滚动到底部
            LaunchedEffect(stockViewModel.history.size) {
                val newSize = stockViewModel.history.size
                if (newSize == 0) return@LaunchedEffect

                val layoutInfo = listState.layoutInfo
                val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index

                val shouldScroll = lastVisibleItemIndex == null || (oldSize > 0 && lastVisibleItemIndex >= oldSize - 1)

                if (shouldScroll) {
                    listState.animateScrollToItem(newSize - 1)
                }

                oldSize = newSize
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            showHistoryPopup = false
                            println("历史面板被点击，已关闭")
                        })
                    }
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                ) {
                    items(stockViewModel.history) { data ->
                        HistoryRow(data)
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(scrollState = listState)
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(data: StockData) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm:ss") }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
        // 时间列
        Text(
            text = data.timestamp.format(timeFormatter),
            modifier = Modifier.width(50.dp),
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
        // 价格列
        Text(
            text = "%.2f".format(data.price),
            modifier = Modifier.width(46.dp),
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End
        )
        // 涨跌幅列
        val changeColor = if (data.changePercent >= 0) Color(0xFFd81e06) else Color(0xFF1aad19)
        Text(
            text = "%.2f%%".format(data.changePercent),
            modifier = Modifier.width(46.dp),
            color = changeColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End
        )
        // 涨速列
        val riseColor = if (data.rise >= 0) Color(0xFFd81e06) else Color(0xFF1aad19)
        Text(
            text = "%.2f%%".format(data.rise),
            modifier = Modifier.width(41.dp),
            color = riseColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End
        )
        // 大盘指数涨跌幅列
        val indexColor = if (data.indexPercent >= 0) Color(0xFFd81e06) else Color(0xFF1aad19)
        Text(
            text = "%.2f%%".format(data.indexPercent),
            modifier = Modifier.width(41.dp),
            color = indexColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End
        )
    }
}
