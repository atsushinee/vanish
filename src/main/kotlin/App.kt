import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import config.AppConfig
import ui.component.TrayMenu
import ui.screen.HistoryScreen
import ui.screen.MainScreen
import ui.screen.TimeShareScreen
import ui.screen.WatchlistScreen
import util.TrayManager
import viewmodel.StockViewModel

/**
 * 应用程序的主 Composable 函数，负责整体的结构和状态管理。
 */
@Composable
fun App(onExit: () -> Unit) {
    Window(
        onCloseRequest = onExit, // 将其关闭请求连接到顶级退出函数，作为安全保障
        visible = false, // 确保对话框永远不可见
        undecorated = true // 一个不可见的对话框也应该是无边框的
    ) {}

    // 定义UI的常量尺寸
    val virtualWidth = 135.dp
    val virtualHeight = 30.dp
    val initialScale = 0.8f

    val initialX = AppConfig.windowX
    val initialY = AppConfig.windowY

    val mainDialogState = rememberDialogState(
        size = androidx.compose.ui.unit.DpSize(virtualWidth * initialScale, virtualHeight * initialScale),
        position = WindowPosition(initialX.dp, initialY.dp)
    )

    // 使用 MutableState 来管理所有需要在不同组件间共享的UI状态
    val showHistoryPopup = remember { mutableStateOf(false) }
    val showWatchlistPopup = remember { mutableStateOf(false) }
    val showTimeShareWindow = remember { mutableStateOf(false) }
    val isWindowVisible = remember { mutableStateOf(true) }
    val showTrayMenu = remember { mutableStateOf(false) }
    val showContextMenu = remember { mutableStateOf(false) }
    val trayMenuPosition = remember { mutableStateOf<WindowPosition>(WindowPosition(0.dp, 0.dp)) }

    // 记住 ViewModel 的单一实例
    val stockViewModel = remember { StockViewModel() }

    // 为各个对话框创建并记住 DialogState
    val historyWindowState = rememberDialogState(size = androidx.compose.ui.unit.DpSize(240.dp, 45.dp))
    val watchlistWindowState = rememberDialogState(size = androidx.compose.ui.unit.DpSize(200.dp, 70.dp))
    val timeShareDialogState = rememberDialogState(size = androidx.compose.ui.unit.DpSize(200.dp, 70.dp))


    val handleCloseRequest = {
        val currentPosition = mainDialogState.position
        AppConfig.windowX = currentPosition.x.value
        AppConfig.windowY = currentPosition.y.value
        AppConfig.save()
        onExit()
    }

    // 定义切换主窗口可见性的逻辑
    val changeVisible = {
        isWindowVisible.value = !isWindowVisible.value
        if (!isWindowVisible.value) {
            showHistoryPopup.value = false
            showWatchlistPopup.value = false
            showTimeShareWindow.value = false
        }
    }

    LaunchedEffect(mainDialogState.position, mainDialogState.size) {
        val currentPosition = mainDialogState.position
        // 更新历史记录窗口位置
        val historyY = currentPosition.y - historyWindowState.size.height - 4.dp
        val historyX = currentPosition.x + (mainDialogState.size.width - historyWindowState.size.width) / 2
        historyWindowState.position = WindowPosition(x = historyX, y = historyY)

        // 更新自选列表窗口位置
        val watchlistY = currentPosition.y - watchlistWindowState.size.height - 4.dp
        val watchlistX = currentPosition.x + (mainDialogState.size.width - watchlistWindowState.size.width) / 2
        watchlistWindowState.position = WindowPosition(x = watchlistX, y = watchlistY)

        // 更新分时图窗口位置
        val timeShareY = currentPosition.y - timeShareDialogState.size.height - 4.dp
        val timeShareX = currentPosition.x + (mainDialogState.size.width - timeShareDialogState.size.width) / 2
        timeShareDialogState.position = WindowPosition(x = timeShareX, y = timeShareY)
    }

    // 监听自选股弹窗的可见性变化
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
            dialogState = mainDialogState,
            stockData = stockData,
            virtualWidth = virtualWidth,
            virtualHeight = virtualHeight,
            initialScale = initialScale,
            showContextMenu = showContextMenu,
            onShowHistory = { showHistoryPopup.value = !showHistoryPopup.value },
            onShowWatchlist = { showWatchlistPopup.value = !showWatchlistPopup.value },
            onShowTimeShare = { showTimeShareWindow.value = !showTimeShareWindow.value },
            onDoubleClick = {
                showWatchlistPopup.value = !showWatchlistPopup.value
                showTimeShareWindow.value = false
            },
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

    // 分时图弹窗
    if (showTimeShareWindow.value && stockData != null) {
        TimeShareScreen(
            code = stockData.code,
            dialogState = timeShareDialogState,
            onCloseRequest = { showTimeShareWindow.value = false }
        )
    }
}
