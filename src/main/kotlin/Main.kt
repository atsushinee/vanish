import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
fun main() = application {
    val virtualWidth = 135.dp
    val virtualHeight = 30.dp
    val initialScale = 0.8f

    // 从配置中加载窗口位置，如果不存在则使用默认值
    val initialX = AppConfig.getProperty("window.x", "0").toFloat()
    val initialY = AppConfig.getProperty("window.y", "0").toFloat()

    val windowState = rememberWindowState(
        size = DpSize(virtualWidth * initialScale, virtualHeight * initialScale),
        // 使用加载的或默认的位置
        position = WindowPosition(initialX.dp, initialY.dp)
    )
    var isHovered by remember { mutableStateOf(false) }
    val targetAlpha = if (isHovered) 0.0f else 0f
    val animatedAlpha by animateFloatAsState(targetValue = targetAlpha)
    // 右键菜单显示状态
    var showContextMenu by remember { mutableStateOf(false) }

    val stockViewModel = remember { StockViewModel() }

    Window(
        // 在关闭请求时保存窗口位置并退出
        onCloseRequest = {
            // 保存窗口的 x 和 y 坐标
            AppConfig.setProperty("window.x", windowState.position.x.value.toString())
            AppConfig.setProperty("window.y", windowState.position.y.value.toString())
            // 保存配置
            AppConfig.save()
            // 退出应用程序
            exitApplication()
        },
        state = windowState,
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        title = "Vanish",
        resizable = false
    ) {
        // 启动时设置窗口背景为全透明
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
                                        // 检查是否是鼠标右键点击
                                        if (event.buttons.isSecondaryPressed) {
                                            // 显示右键菜单
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

                // 如果 showContextMenu 为 true，则显示自定义的右键菜单
                if (showContextMenu) {
                    // 使用 Popup 实现菜单，它不会在任务栏创建新窗口，并且可以定位在主窗口之外
                    Popup(
                        alignment = Alignment.Center,
                        offset = IntOffset(0, 0),
                        onDismissRequest = { showContextMenu = false }
                    ) {
                        // 菜单内容区域
                        Row(
                            modifier = Modifier
                                .width(virtualWidth * initialScale)
                                .height(virtualHeight * initialScale)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(horizontal = 4.dp)
                                // 关键：为菜单背景添加手势检测
                                .pointerInput(Unit) {
                                    // 使用 detectTapGestures 来监听点击手势
                                    // onTap 回调会在检测到单击时触发
                                    detectTapGestures(onTap = {
                                        // 当用户点击的是 Row 的背景区域（非按钮部分）时，关闭菜单
                                        showContextMenu = false
                                        println("点击菜单背景，已关闭")
                                    })
                                },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 菜单项 1：刷新按钮
                            IconButton(onClick = {
                                stockViewModel.refresh()
                                showContextMenu = false
                                println("刷新按钮点击")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "刷新",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // 菜单项 2：设置按钮（占位符）
                            IconButton(onClick = {
                                showContextMenu = false
                                println("设置按钮点击")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "设置",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
