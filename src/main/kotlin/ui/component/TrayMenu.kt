package ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import java.awt.event.WindowFocusListener

/**
 * 系统托盘的自定义菜单
 * @param showTrayMenu 控制菜单可见性的状态
 * @param trayMenuPosition 菜单的显示位置
 * @param onCloseRequest 回调：当请求关闭应用时触发
 */
@Composable
fun TrayMenu(
    showTrayMenu: MutableState<Boolean>,
    trayMenuPosition: WindowPosition,
    onCloseRequest: () -> Unit
) {
    // 仅当 showTrayMenu 为 true 时显示 DialogWindow 作为菜单
    if (showTrayMenu.value) {
        DialogWindow(
            onCloseRequest = { showTrayMenu.value = false },
            state = rememberDialogState(
                position = trayMenuPosition,
                size = androidx.compose.ui.unit.DpSize(50.dp, Dp.Unspecified) // 宽度固定，高度自适应
            ),
            undecorated = true,
            transparent = true,
            resizable = false,
            alwaysOnTop = true,
            focusable = true
        ) {
            // 使用 DisposableEffect 来添加和移除窗口焦点监听器
            // 这是确保菜单在失去焦点时能自动关闭的关键
            DisposableEffect(window) {
                val listener = object : WindowFocusListener {
                    override fun windowGainedFocus(e: java.awt.event.WindowEvent?) {}
                    override fun windowLostFocus(e: java.awt.event.WindowEvent?) {
                        // 当窗口失去焦点时，隐藏菜单
                        showTrayMenu.value = false
                        println("菜单窗口失去焦点，已自动关闭")
                    }
                }
                window.addWindowFocusListener(listener)
                // onDispose 用于在 Composable 销毁时清理资源
                onDispose {
                    window.removeWindowFocusListener(listener)
                }
            }

            // 设置窗口背景为完全透明
            window.background = java.awt.Color(0, 0, 0, 0)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.85f)) // 半透明黑色背景
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
                            showTrayMenu.value = false
                            onCloseRequest() // 调用关闭回调
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
        }
    }
}
