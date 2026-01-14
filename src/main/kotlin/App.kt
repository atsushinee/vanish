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

    // 目的: 通过 AppConfig 的类型安全属性获取窗口初始位置。
    // 原理: 直接访问 AppConfig.windowX 和 AppConfig.windowY，它们返回 Float 类型，无需关心转换和默认值。
    val initialX = AppConfig.windowX
    val initialY = AppConfig.windowY

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
    val trayMenuPosition = remember { mutableStateOf<WindowPosition>(WindowPosition(0.dp, 0.dp)) }

    // 记住 ViewModel 的单一实例
    val stockViewModel = remember { StockViewModel() }

    // 记住各个弹窗的状态
    val historyWindowState = rememberDialogState(size = androidx.compose.ui.unit.DpSize(240.dp, 45.dp))
    val watchlistWindowState = rememberDialogState(size = androidx.compose.ui.unit.DpSize(200.dp, 70.dp))

    // 定义关闭应用程序时的处理逻辑
    val handleCloseRequest = {
        val currentPosition = windowState.position
        if (currentPosition is WindowPosition.Absolute) {
            // 目的: 通过 AppConfig 的类型安全属性保存窗口位置。
            // 原理: 直接为 AppConfig.windowX 和 AppConfig.windowY 赋值，其 setter 会处理持久化。
            AppConfig.windowX = currentPosition.x.value
            AppConfig.windowY = currentPosition.y.value
        }
        // 目的: 统一调用 save 方法，将所有变更一次性写入文件。
        // 注意: 这是确保所有配置（包括 ViewModel 中可能修改的）都被保存的关键。
        AppConfig.save()
        onExit()
    }

    // 定义切换主窗口可见性的逻辑
    val changeVisible = {
        isWindowVisible.value = !isWindowVisible.value
        if (!isWindowVisible.value) {
            showHistoryPopup.value = false
            showWatchlistPopup.value = false
        }
    }

    // 当主窗口位置或大小变化时，自动调整弹窗的位置
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

    // 监听自选股弹窗的可见性变化，以启动或停止数据监控
    LaunchedEffect(showWatchlistPopup.value) {
        if (showWatchlistPopup.value) {
            stockViewModel.startWatchlistMonitor()
            println("自选股监控已启动")
        } else {
            stockViewModel.stopWatchlistMonitor()
            println("自选股监控已停止")
        }
    }

    // 管理系统托盘图标
    TrayManager(
        onTrayIconClick = { changeVisible() },
        onTrayIconRightClick = { position ->
            trayMenuPosition.value = position
            showTrayMenu.value = true
        }
    )

    // 显示托盘菜单
    TrayMenu(
        showTrayMenu = showTrayMenu,
        trayMenuPosition = trayMenuPosition.value,
        onCloseRequest = handleCloseRequest
    )

    val stockData = stockViewModel.stockData.value

    // 主窗口渲染
    if (isWindowVisible.value) {
        MainScreen(
            windowState = windowState,
            stockData = stockData,
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

    // 历史记录弹窗
    HistoryScreen(
        showHistoryPopup = showHistoryPopup,
        stockViewModel = stockViewModel,
        historyWindowState = historyWindowState
    )

    // 自选股列表弹窗
    WatchlistScreen(
        showWatchlistPopup = showWatchlistPopup,
        stockViewModel = stockViewModel,
        watchlistWindowState = watchlistWindowState
    )
}
