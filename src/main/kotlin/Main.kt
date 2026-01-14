import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowFocusListener
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
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
    // 控制自定义托盘菜单的可见性
    var showTrayMenu by remember { mutableStateOf(false) }
    // 存储托盘菜单的显示位置（使用 Dp 单位）
    var trayMenuPosition by remember { mutableStateOf(WindowPosition(0.dp, 0.dp)) }
    // 获取当前屏幕的密度，用于将 AWT 的像素坐标转换为 Compose 的 Dp 坐标
    val density = LocalDensity.current


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

    val changeVisible = {
        isWindowVisible = !isWindowVisible
        if (!isWindowVisible) {
            showHistoryPopup = false
        }
    }

    // 使用 LaunchedEffect 监听主窗口位置变化，以同步更新历史窗口的位置
    LaunchedEffect(windowState.position) {
        val historyY = windowState.position.y - historyWindowState.size.height - 4.dp
        val historyX = windowState.position.x + (windowState.size.width - historyWindowState.size.width) / 2
        historyWindowState.position = WindowPosition(x = historyX, y = historyY)
    }

    // 使用 DisposableEffect 管理 AWT TrayIcon 的生命周期
    DisposableEffect(Unit) {
        SwingUtilities.invokeLater {
            if (!SystemTray.isSupported()) {
                println("系统不支持托盘")
                return@invokeLater
            }

            val tray = SystemTray.getSystemTray()
            val image = try {
                val resourceStream = Thread.currentThread().contextClassLoader.getResourceAsStream("app.ico")
                ImageIO.read(resourceStream)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            if (image == null) {
                println("加载托盘图标失败")
                return@invokeLater
            }

            val trayIcon = TrayIcon(image, "Vanish")
            trayIcon.isImageAutoSize = true

            trayIcon.addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    if (e.isPopupTrigger || SwingUtilities.isRightMouseButton(e)) {
                        // 使用从 Compose 获取的 density 对象，将鼠标事件的像素(px)坐标转换为 Dp
                        trayMenuPosition = with(density) {
                            WindowPosition(e.x.toDp(), e.y.toDp())
                        }
                        // 触发菜单显示
                        showTrayMenu = true
                        println("托盘图标被右键点击，显示自定义菜单")
                    } else {
                        changeVisible()
                        println("托盘图标被左键点击，窗口可见性: $isWindowVisible")
                    }
                }
            })

            try {
                tray.add(trayIcon)
                println("自定义托盘图标已添加")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        onDispose {
            // 清理逻辑
            println("托盘图标清理逻辑（示意）")
        }
    }

    // 当 showTrayMenu 为 true 时，使用 DialogWindow 显示自定义菜单
    if (showTrayMenu) {
        DialogWindow(
            onCloseRequest = { showTrayMenu = false },
            state = rememberDialogState(
                position = trayMenuPosition,
                size = DpSize(50.dp, Dp.Unspecified)
            ),
            undecorated = true,
            transparent = true,
            resizable = false,
            alwaysOnTop = true,
            focusable = true
        ) {
            // 关键修复：使用 DisposableEffect 来安全地添加和移除 AWT 窗口焦点监听器。
            // 这是解决“菜单在失去焦点后无法点击”问题的核心。
            DisposableEffect(window) {
                // 创建一个窗口焦点监听器实例
                val listener = object : WindowFocusListener {
                    override fun windowGainedFocus(e: java.awt.event.WindowEvent?) {}

                    // 当菜单窗口失去系统焦点时（例如用户点击了任务栏或其他程序），此方法会被调用
                    override fun windowLostFocus(e: java.awt.event.WindowEvent?) {
                        // 将 showTrayMenu 状态设置为 false，从而以编程方式关闭菜单
                        showTrayMenu = false
                        println("菜单窗口失去焦点，已自动关闭")
                    }
                }
                // 将监听器添加到 DialogWindow 的底层 AWT 窗口上
                window.addWindowFocusListener(listener)

                // onDispose 回调是 DisposableEffect 的一部分，在组件销毁时执行
                onDispose {
                    // 移除监听器，以防止在窗口关闭后发生内存泄漏
                    window.removeWindowFocusListener(listener)
                }
            }

            // 必须为 DialogWindow 的 AWT 窗口设置透明背景
            window.background = java.awt.Color(0, 0, 0, 0)

            // 菜单的容器，定义了其暗色、紧凑、圆角的样式
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = "退出",
                    color = Color.White,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            println("自定义菜单项 '退出' 被点击")
                            showTrayMenu = false
                            handleCloseRequest()
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
        }
    }


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
            var lastTapTime by remember { mutableStateOf(0L) }
            var lastTapPosition by remember { mutableStateOf(Offset.Zero) }
            val density = LocalDensity.current
            val tapTolerancePx = with(density) { 24.dp.toPx() }

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
                                            } else if (event.buttons.isPrimaryPressed) {
                                                val currentTime = System.currentTimeMillis()
                                                val currentPos = event.changes.first().position

                                                if (currentTime - lastTapTime < 300 &&
                                                    (currentPos - lastTapPosition).getDistance() <= tapTolerancePx
                                                ) {
                                                    // 双击
                                                    showHistoryPopup = !showHistoryPopup
                                                    lastTapTime = 0 // 防止三连击触发第二次双击
                                                } else {
                                                    lastTapTime = currentTime
                                                    lastTapPosition = currentPos
                                                }
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
                                    Icon(
                                        Icons.Default.History,
                                        "历史",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
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
                                IconButton(onClick = handleCloseRequest) {
                                    Icon(
                                        Icons.Default.Close,
                                        "关闭",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
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
