package ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup

/**
 * 主窗口的上下文菜单（右键菜单）
 * @param showContextMenu 控制菜单可见性的状态
 * @param virtualWidth 虚拟宽度，用于确定菜单的尺寸
 * @param virtualHeight 虚拟高度，用于确定菜单的尺寸
 * @param initialScale 初始缩放比例
 * @param onShowHistory 回调：当点击“历史”按钮时触发
 * @param onShowWatchlist 回调：当点击“自选列表”按钮时触发
 * @param onShowTimeShare 新增回调：当点击“分时图”按钮时触发
 * @param onCloseRequest 回调：当点击“关闭”按钮时触发
 */
@Composable
fun ContextMenu(
    showContextMenu: MutableState<Boolean>,
    virtualWidth: Dp,
    virtualHeight: Dp,
    initialScale: Float,
    onShowHistory: () -> Unit,
    onShowWatchlist: () -> Unit,
    onShowTimeShare: () -> Unit, // 新增参数
    onCloseRequest: () -> Unit
) {
    // 仅当 showContextMenu 为 true 时显示 Popup
    if (showContextMenu.value) {
        Popup(
            alignment = Alignment.Center,
            offset = IntOffset(0, 0),
            onDismissRequest = { showContextMenu.value = false }
        ) {
            Row(
                modifier = Modifier
                    .width(virtualWidth * initialScale)
                    .height(virtualHeight * initialScale)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 4.dp)
                    // 添加点击手势，点击菜单背景会关闭菜单
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            showContextMenu.value = false
                            println("菜单背景被点击，已关闭")
                        })
                    },
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // “历史”按钮
                IconButton(onClick = {
                    onShowHistory()
                    showContextMenu.value = false
                    println("历史按钮点击")
                }) {
                    Icon(
                        Icons.Default.History,
                        "历史",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
                // “自选列表”按钮
                IconButton(onClick = {
                    onShowWatchlist()
                    showContextMenu.value = false
                    println("自选列表按钮点击")
                }) {
                    Icon(
                        Icons.Default.List,
                        "自选列表",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
                // 核心修正：将分时图的触发器从文字按钮改为图标按钮
                // 原理：使用 IconButton 和 Icon 组件，选择一个能代表“图表”或“时间线”的图标（如 Timeline），以实现UI风格的统一。
                // 注意：contentDescription 是为辅助功能服务的，对视力正常的用来说不可见，但良好的实践是提供它。
                IconButton(onClick = {
                    onShowTimeShare()
                    showContextMenu.value = false
                    println("分时图按钮点击")
                }) {
                    Icon(
                        imageVector = Icons.Default.Timeline, // 使用 Timeline 图标，更形象
                        contentDescription = "分时图",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
                // “关闭”按钮
                IconButton(onClick = onCloseRequest) {
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
