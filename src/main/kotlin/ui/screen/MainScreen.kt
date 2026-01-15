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
import java.awt.Dialog

/**
 * 应用的主UI屏幕，被包裹在一个特殊的 DialogWindow 中。
 */
@Composable
fun MainScreen(
    dialogState: DialogState,
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

    DialogWindow(
        onCloseRequest = { onVisibilityChange(false) },
        state = dialogState,
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = false
    ) {
        // 核心修正：采用“组合拳”方案，彻底阻止窗口出现在任务切换器中
        LaunchedEffect(window) {
            // 第一招：设置模态排除类型，建议操作系统不要将其包含在任务列表中。
            window.modalExclusionType = Dialog.ModalExclusionType.APPLICATION_EXCLUDE
            // 第二招：明确设置窗口不可聚焦。一个不能被聚焦的窗口，通常不会被任务切换器所关心。
            // 这是解决某些操作系统或桌面环境依然显示窗口问题的关键一步。
            window.focusableWindowState = false
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
