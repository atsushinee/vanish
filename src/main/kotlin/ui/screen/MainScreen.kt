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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.DialogWindow
import data.model.StockData
import ui.component.ContextMenu

/**
 * 应用的主UI屏幕，现在被包裹在一个Dialog中。
 * @param dialogState 主对话框的状态，控制大小和位置
 * @param stockData 当前要显示的股票数据
 * @param virtualWidth 虚拟宽度，用于布局
 * @param virtualHeight 虚拟高度，用于布局
 * @param initialScale 初始缩放比例
 * @param showContextMenu 控制上下文菜单可见性的状态
 * @param onShowHistory 回调：当请求显示历史记录时触发
 * @param onShowWatchlist 回调：当请求显示自选股列表时触发
 * @param onShowTimeShare 回调：当请求显示分时图时触发
 * @param onDoubleClick 回调：当双击主UI时触发
 * @param onCloseRequest 回调：当请求关闭应用时触发
 * @param onVisibilityChange 回调：当窗口可见性需要改变时触发
 */
@Composable
fun MainScreen(
    dialogState: DialogState, // 核心修正：接收 DialogState 而不是 WindowState
    stockData: StockData?,
    virtualWidth: Dp,
    virtualHeight: Dp,
    initialScale: Float,
    showContextMenu: MutableState<Boolean>,
    onShowHistory: () -> Unit,
    onShowWatchlist: () -> Unit,
    onShowTimeShare: () -> Unit,
    onDoubleClick: () -> Unit,
    onCloseRequest: () -> Unit,
    onVisibilityChange: (Boolean) -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    val targetAlpha = if (isHovered) 0.0f else 0f
    val animatedAlpha by animateFloatAsState(targetValue = targetAlpha)

    // 核心修正：使用 Dialog 组件替换 Window 组件
    // 原理：Dialog 默认不会在任务栏显示图标，完美符合需求。
    //      同时，保持所有样式属性（undecorated, transparent, alwaysOnTop）不变，以维持原有的视觉效果。
    DialogWindow(
        onCloseRequest = { onVisibilityChange(false) }, // 关闭对话框时仅改变可见性
        state = dialogState, // 应用从 App.kt 传入的 DialogState
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = false
    ) {
        // 用于检测双击事件的状态
        var lastTapTime by remember { mutableStateOf(0L) }
        var lastTapPosition by remember { mutableStateOf(Offset.Zero) }
        val density = LocalDensity.current
        val tapTolerancePx = with(density) { 24.dp.toPx() }

        // 使整个对话框内容可拖动
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
                                            showContextMenu.value = true
                                        } else if (event.buttons.isPrimaryPressed) {
                                            val currentTime = System.currentTimeMillis()
                                            val currentPos = event.changes.first().position
                                            if (currentTime - lastTapTime < 300 &&
                                                (currentPos - lastTapPosition).getDistance() <= tapTolerancePx
                                            ) {
                                                onDoubleClick()
                                                lastTapTime = 0
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
                StockInfo(stockData)

                ContextMenu(
                    showContextMenu = showContextMenu,
                    virtualWidth = virtualWidth,
                    virtualHeight = virtualHeight,
                    initialScale = initialScale,
                    onShowHistory = onShowHistory,
                    onShowWatchlist = onShowWatchlist,
                    onShowTimeShare = onShowTimeShare,
                    onCloseRequest = onCloseRequest
                )
            }
        }
    }
}
