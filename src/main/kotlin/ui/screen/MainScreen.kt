package ui.screen

import StockInfo
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import data.model.StockData
import ui.component.ContextMenu

/**
 * 应用的主窗口界面
 * @param windowState 主窗口的状态，控制大小和位置
 * @param stockData 当前要显示的股票数据
 * @param virtualWidth 虚拟宽度，用于布局
 * @param virtualHeight 虚拟高度，用于布局
 * @param initialScale 初始缩放比例
 * @param showContextMenu 控制上下文菜单可见性的状态
 * @param onShowHistory 回调：当请求显示历史记录时触发
 * @param onShowWatchlist 回调：当请求显示自选股列表时触发
 * @param onShowTimeShare 新增回调：当请求显示分时图时触发
 * @param onCloseRequest 回调：当请求关闭应用时触发
 * @param onVisibilityChange 回调：当窗口可见性需要改变时触发
 */
@Composable
fun MainScreen(
    windowState: WindowState,
    stockData: StockData?,
    virtualWidth: Dp,
    virtualHeight: Dp,
    initialScale: Float,
    showContextMenu: MutableState<Boolean>,
    onShowHistory: () -> Unit,
    onShowWatchlist: () -> Unit,
    onShowTimeShare: () -> Unit, // 新增参数
    onCloseRequest: () -> Unit,
    onVisibilityChange: (Boolean) -> Unit
) {
    // 控制鼠标悬浮状态
    var isHovered by remember { mutableStateOf(false) }
    // 根据悬浮状态计算目标透明度，实现淡入淡出效果
    val targetAlpha = if (isHovered) 0.0f else 0f
    val animatedAlpha by animateFloatAsState(targetValue = targetAlpha)

    Window(
        onCloseRequest = { onVisibilityChange(false) }, // 关闭窗口时仅改变可见性
        state = windowState,
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        title = "Vanish",
        resizable = false,
        icon = painterResource("app.ico")
    ) {
        // 启动时设置窗口背景为透明
        LaunchedEffect(Unit) {
            window.background = java.awt.Color(0, 0, 0, 0)
        }

        // 用于检测双击事件的状态
        var lastTapTime by remember { mutableStateOf(0L) }
        var lastTapPosition by remember { mutableStateOf(Offset.Zero) }
        val density = LocalDensity.current
        // 定义双击的像素容差范围
        val tapTolerancePx = with(density) { 24.dp.toPx() }

        // 使整个窗口可拖动
        WindowDraggableArea {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 使用 pointerInput 处理复杂的指针事件
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    // 鼠标进入和退出时更新悬浮状态
                                    PointerEventType.Enter -> isHovered = true
                                    PointerEventType.Exit -> isHovered = false
                                    // 鼠标按下事件
                                    PointerEventType.Press -> {
                                        if (event.buttons.isSecondaryPressed) {
                                            // 右键按下，显示上下文菜单
                                            showContextMenu.value = true
                                        } else if (event.buttons.isPrimaryPressed) {
                                            // 左键按下，处理双击逻辑
                                            val currentTime = System.currentTimeMillis()
                                            val currentPos = event.changes.first().position

                                            // 判断是否在短时间内（300ms）且在小范围内连续点击
                                            if (currentTime - lastTapTime < 300 &&
                                                (currentPos - lastTapPosition).getDistance() <= tapTolerancePx
                                            ) {
                                                // 触发双击事件，切换自选股列表的可见性
                                                onShowWatchlist()
                                                lastTapTime = 0 // 重置时间，避免连续触发
                                            } else {
                                                // 记录本次点击的时间和位置
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
                    .background(Color.Black.copy(alpha = animatedAlpha)) // 应用动画透明度
            ) {
                // 显示主要的股票信息
                StockInfo(stockData)

                // 显示上下文菜单（如果需要）
                ContextMenu(
                    showContextMenu = showContextMenu,
                    virtualWidth = virtualWidth,
                    virtualHeight = virtualHeight,
                    initialScale = initialScale,
                    onShowHistory = onShowHistory,
                    onShowWatchlist = onShowWatchlist,
                    onShowTimeShare = onShowTimeShare, // 传递回调
                    onCloseRequest = onCloseRequest
                )
            }
        }
    }
}
