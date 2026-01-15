package util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.WindowPosition
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.imageio.ImageIO
import javax.swing.SwingUtilities

/**
 * 管理系统托盘图标的 Composable
 * @param onTrayIconClick 回调：当左键点击托盘图标时触发
 * @param onTrayIconRightClick 回调：当右键点击托盘图标时触发，并传递点击位置
 */
@Composable
fun TrayManager(
    onTrayIconClick: () -> Unit,
    onTrayIconRightClick: (WindowPosition) -> Unit
) {
    // 获取当前屏幕密度，用于将像素坐标转换为 Dp
    val density = LocalDensity.current

    // 使用 DisposableEffect 来管理 AWT TrayIcon 的生命周期
    // 这确保了托盘图标只在 Composable 存在时显示，并在其销毁时被移除
    DisposableEffect(Unit) {
        // 声明一个变量来持有 TrayIcon 实例，以便在 onDispose 中可以访问它
        var trayIcon: TrayIcon? = null
        // AWT/Swing 操作必须在 EDT (Event Dispatch Thread) 上执行
        SwingUtilities.invokeLater {
            // 检查当前系统是否支持托盘
            if (!SystemTray.isSupported()) {
                println("系统不支持托盘")
                return@invokeLater
            }

            // 获取系统托盘实例
            val tray = SystemTray.getSystemTray()
            // 从资源加载托盘图标
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

            trayIcon = TrayIcon(image, "Vanish").apply {
                isImageAutoSize = true // 自动调整图标大小

                addMouseListener(object : MouseAdapter() {
                    override fun mousePressed(e: MouseEvent) {
                        // 判断是右键点击还是左键点击
                        if (e.isPopupTrigger || SwingUtilities.isRightMouseButton(e)) {
                            val position = with(density) {
                                WindowPosition(e.x.toDp(), e.y.toDp())
                            }
                            onTrayIconRightClick(position)
                            println("托盘图标被右键点击，显示自定义菜单")
                        } else {
                            onTrayIconClick()
                            println("托盘图标被左键点击")
                        }
                    }
                })
            }


            try {
                // 将托盘图标添加到系统托盘
                tray.add(trayIcon)
                println("自定义托盘图标已添加")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        onDispose {
            SwingUtilities.invokeLater {
                trayIcon?.let {
                    SystemTray.getSystemTray().remove(it)
                    println("自定义托盘图标已移除")
                }
            }
        }
    }
}
