import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
fun main() = application {
    val virtualWidth = 130.dp
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
    val targetAlpha = if (isHovered) 0.6f else 0f
    val animatedAlpha by animateFloatAsState(targetValue = targetAlpha)
    var showMenu by remember { mutableStateOf(false) }

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
