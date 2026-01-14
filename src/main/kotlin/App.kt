import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.window.rememberWindowState
import config.AppConfig
import ui.component.TrayMenu
import ui.screen.HistoryScreen
import ui.screen.MainScreen
import ui.screen.WatchlistScreen
import util.TrayManager
import viewmodel.StockViewModel

/**
 * 应用程序的主 Composable 函数，负责整体的结构和状态管理。
 * @param onExit 一个回调函数，用于在需要时触发应用程序的退出逻辑。
 */
@Composable
fun App(onExit: () -> Unit) {
    // 定义UI的常量尺寸
    val virtualWidth = 135.dp
    val virtualHeight = 30.dp
    val initialScale = 0.8f

    // 从配置文件加载并记住窗口的初始位置
    val initialX = AppConfig.getProperty("window.x", "0").toFloat()
    val initialY = AppConfig.getProperty("window.y", "0").toFloat()

    // 记住主窗口的状态（大小、位置）
    val windowState = rememberWindowState(
        size = androidx.compose.ui.unit.DpSize(virtualWidth * initialScale, virtualHeight * initialScale),
        position = WindowPosition(initialX.dp, initialY.dp)
    )

    // 使用 MutableState 来管理所有需要在不同组件间共享的UI状态
    val showHistoryPopup = remember { mutableStateOf(false) }
    val showWatchlistPopup = remember { mutableStateOf(false) }
    val isWindowVisible = remember { mutableStateOf(true) }
    val showTrayMenu = remember { mutableStateOf(false) }
    val showContextMenu = remember { mutableStateOf(false) }
    // 修复：明确指定 trayMenuPosition 的状态类型为通用的 WindowPosition
    // 这是为了防止 Kotlin 类型推断将其默认为 WindowPosition.Absolute，从而在赋值时产生类型不匹配的编译错误。
    val trayMenuPosition = remember { mutableStateOf<WindowPosition>(WindowPosition(0.dp, 0.dp)) }

    // 记住 ViewModel 的单一实例
    val stockViewModel = remember { StockViewModel() }

    // 记住各个弹窗的状态
    val historyWindowState = rememberDialogState(size = androidx.compose.ui.unit.DpSize(240.dp, 45.dp))
    val watchlistWindowState = rememberDialogState(size = androidx.compose.ui.unit.DpSize(200.dp, 70.dp))

    // 定义关闭应用程序时的处理逻辑
    val handleCloseRequest = {
        // 在保存位置前，必须检查 position 是否是 Absolute 类型
        val currentPosition = windowState.position
        if (currentPosition is WindowPosition.Absolute) {
            // 确认是绝对位置后，安全地访问 x 和 y
            AppConfig.setProperty("window.x", currentPosition.x.value.toString())
            AppConfig.setProperty("window.y", currentPosition.y.value.toString())
        }
        AppConfig.save() // 保存配置到文件
        onExit() // 调用从 main 传递过来的退出函数
    }

    // 定义切换主窗口可见性的逻辑
    val changeVisible = {
        isWindowVisible.value = !isWindowVisible.value
        // 当主窗口隐藏时，确保所有关联的弹窗也一并隐藏
        if (!isWindowVisible.value) {
            showHistoryPopup.value = false
            showWatchlistPopup.value = false
        }
    }

    // 当主窗口位置或大小变化时，自动调整弹窗的位置，使其与主窗口保持相对固定
    LaunchedEffect(windowState.position, windowState.size) {
        val currentPosition = windowState.position
        if (currentPosition is WindowPosition.Absolute) {
            val historyY = currentPosition.y - historyWindowState.size.height - 4.dp
            val historyX = currentPosition.x + (windowState.size.width - historyWindowState.size.width) / 2
            historyWindowState.position = WindowPosition(x = historyX, y = historyY)

            val watchlistY = currentPosition.y - watchlistWindowState.size.height - 4.dp
            val watchlistX = currentPosition.x + (windowState.size.width - watchlistWindowState.size.width) / 2
            watchlistWindowState.position = WindowPosition(x = watchlistX, y = watchlistY)
        }
    }

    // 监听自选股弹窗的可见性变化，以启动或停止对应的数据监控
    LaunchedEffect(showWatchlistPopup.value) {
        if (showWatchlistPopup.value) {
            stockViewModel.startWatchlistMonitor()
            println("自选股监控已启动")
        } else {
            stockViewModel.stopWatchlistMonitor()
            println("自选股监控已停止")
        }
    }

    // 管理系统托盘图标及其交互
    TrayManager(
        onTrayIconClick = { changeVisible() },
        onTrayIconRightClick = { position ->
            trayMenuPosition.value = position
            showTrayMenu.value = true
        }
    )

    // 显示自定义的托盘菜单
    TrayMenu(
        showTrayMenu = showTrayMenu,
        trayMenuPosition = trayMenuPosition.value,
        onCloseRequest = handleCloseRequest
    )

    // 获取股票数据
    val stockData = stockViewModel.stockData.value

    // 根据可见性状态决定是否渲染主窗口
    // 只有在 stockData 非空时才渲染主窗口，避免传入 null
    if (isWindowVisible.value) {
        MainScreen(
            windowState = windowState,
            stockData = stockData, // 此处 stockData 已确保非空
            virtualWidth = virtualWidth,
            virtualHeight = virtualHeight,
            initialScale = initialScale,
            showContextMenu = showContextMenu,
            onShowHistory = { showHistoryPopup.value = !showHistoryPopup.value },
            onShowWatchlist = { showWatchlistPopup.value = !showWatchlistPopup.value },
            onCloseRequest = handleCloseRequest,
            onVisibilityChange = { isWindowVisible.value = it }
        )
    }

    // 渲染历史记录弹窗（如果其状态为可见）
    HistoryScreen(
        showHistoryPopup = showHistoryPopup,
        stockViewModel = stockViewModel,
        historyWindowState = historyWindowState
    )

    // 渲染自选股列表弹窗（如果其状态为可见）
    WatchlistScreen(
        showWatchlistPopup = showWatchlistPopup,
        stockViewModel = stockViewModel,
        watchlistWindowState = watchlistWindowState
    )
}
