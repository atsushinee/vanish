import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import getSinaBatchRealtimeData
import kotlinx.coroutines.*
import java.time.LocalTime
import java.time.ZoneId
import java.util.logging.FileHandler
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import java.time.LocalDateTime

class StockViewModel {

    // 日志记录器
    private val logger = Logger.getLogger("StockMonitorLog").apply {
        useParentHandlers = false
        val fileHandler = FileHandler("stock_monitor_log_%g.log", 1024 * 1024, 3, true)
        fileHandler.formatter = SimpleFormatter()
        addHandler(fileHandler)
    }

    // 当前股票数据
    val stockData = mutableStateOf<StockData?>(null)
    // 历史股票数据列表
    val history = mutableStateListOf<StockData>()
    private var lastPrice = 0.0
    private val viewModelScope = CoroutineScope(Dispatchers.IO)

    // ================= 自选股监控模块 =================

    // 用于存储用户配置的自选股代码列表。
    // UI层可以对这个列表进行增删改，从而动态调整监控对象。
    // 这里使用 Compose 的 `mutableStateListOf`，以便UI能够自动响应代码列表的变化。
    val watchlistCodes = mutableStateListOf("sh000001", "sz002413", "sz002639", "sh603273")

    // 存储处理后的自选股实时数据，用于UI展示。
    // UI层应观察此列表的变化来刷新自选股面板。
    val watchlistData = mutableStateListOf<StockData>()

    // 管理自选股监控协程的Job对象。
    // 通过持有该Job的引用，我们可以方便地从外部（如UI事件）启动或取消后台的轮询任务。
    private var watchlistJob: Job? = null

    // 存储每只自选股上一次查询时的价格，用于计算“涨速”。
    // Key为股票代码(e.g., "sh600000")，Value为对应的上一次价格。
    // 这是一个关键的数据结构，用于实现“涨速”这一动态指标。
    private val watchlistLastPrices = mutableMapOf<String, Double>()

    /**
     * 启动自选股的实时监控。
     * 此方法应由UI层在自选股监控窗口/面板显示时调用。
     * 它会启动一个后台协程，在交易时段内以固定频率（例如2秒）刷新自选股数据。
     * 这种设计将数据获取的生命周期与UI组件的可见性绑定，避免了在UI不显示时进行不必要的网络请求和计算，从而优化了资源使用。
     */
    fun startWatchlistMonitor() {
        // 防止重复启动：检查`watchlistJob`是否已存在且处于活动状态。
        // 这是保证同一时间只有一个监控循环在运行的关键。
        if (watchlistJob?.isActive == true) {
            logger.info("[自选股监控]: 监控任务已在运行，无需重复启动。")
            return
        }

        // 在IO优化的协程调度器上启动一个新的后台任务。
        // `Dispatchers.IO` 适合执行网络请求等阻塞式I/O操作。
        watchlistJob = viewModelScope.launch {
            logger.info("[自选股监控]: 监控任务已启动。")
            // `while(isActive)` 是协程中实现可取消循环的标准模式。
            // 当外部调用 `watchlistJob.cancel()` 时，`isActive` 会变为 `false`，循环将在下一次检查时优雅地退出。
            while (isActive) {
                // 仅在A股交易时段执行刷新，减少非交易时间的CPU和网络资源消耗。
                if (isTradingTime()) {
                    fetchWatchlistData()
                }
                // 每次循环后暂停2秒。这个延迟控制了数据刷新的频率。
                // 2秒是一个比较折中的值，既能提供较好的实时性，又不会对API服务器造成过大压力。
                delay(2000L)
            }
        }
    }

    /**
     * 停止自选股的实时监控。
     * 此方法应由UI层在自选股监控窗口/面板关闭或隐藏时调用。
     * 它会取消正在运行的监控协程，并清理相关状态，为下一次启动做准备。
     */
    fun stopWatchlistMonitor() {
        // 调用 `cancel()` 来请求取消 `startWatchlistMonitor` 中启动的协程。
        watchlistJob?.cancel()
        // 将job引用设为null，表明当前没有活动的监控任务，并允许垃圾回收。
        watchlistJob = null
        // 清空UI数据列表和价格记录，确保下次启动时是干净的状态。
        watchlistData.clear()
        watchlistLastPrices.clear()
        logger.info("[自选股监控]: 监控任务已停止并清理状态。")
    }

    init {
        viewModelScope.launch {
            startRealtimeMonitor()
        }
    }

    // ================= 2. 实时监控模块 =================

    private suspend fun startRealtimeMonitor() {
        while (viewModelScope.isActive) {
            if (isTradingTime()) {
                fetchRealtimeData()
            }
            delay(2000L)
        }
    }

    private suspend fun fetchRealtimeData() {
        try {
            val stockQuoteDeferred = viewModelScope.async { getSinaRealtimeData(TARGET_STOCK) }
            val indexQuoteDeferred = viewModelScope.async { getSinaRealtimeData(INDEX_CODE) }

            val stockQuote = stockQuoteDeferred.await()
            val indexQuote = indexQuoteDeferred.await()

            if (stockQuote != null && indexQuote != null) {
                val price = stockQuote.price
                val preClose = stockQuote.preClose
                val changePct = if (preClose > 0) (price / preClose - 1) * 100 else 0.0
                val rise = if (lastPrice > 0) (price - lastPrice) / lastPrice * 100 else 0.0
                lastPrice = price

                val indexPct = if (indexQuote.preClose > 0) (indexQuote.price / indexQuote.preClose - 1) * 100 else 0.0

                // 创建新的 StockData 实例
                val newStockData = StockData(
                    code = TARGET_STOCK,
                    price = price,
                    changePercent = changePct,
                    rise = rise,
                    indexPercent = indexPct,
                    timestamp = LocalDateTime.now()
                )
                // 更新当前数据
                stockData.value = newStockData
                // 将新数据添加到历史记录中
                history.add(newStockData)
                // 保持历史记录列表的大小不超过500，移除最旧的条目
                if (history.size > 500) {
                    history.removeAt(0)
                }
            } else {
                // 添加中文日志：数据获取或解析失败
                logger.warning("[数据处理警告]: 获取新浪行情数据失败或返回数据不完整。")
            }
        } catch (e: Exception) {
            // 添加中文日志：未知异常
            logger.severe("[数据处理异常]: 获取实时数据时发生未知异常: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 批量获取自选股列表的实时行情数据并更新UI状态。
     * 该方法利用新浪财经的批量查询接口，通过一次网络请求获取所有自选股的数据，相比于逐个请求，极大地提高了效率和性能。
     * 它负责解析返回的特殊JavaScript格式数据，计算涨跌幅和涨速，并以线程安全的方式更新 `watchlistData` 状态列表。
     */
    private suspend fun fetchWatchlistData() {
        // 如果自选列表为空，则无需执行任何网络操作，直接返回以节省资源。
        if (watchlistCodes.isEmpty()) return

        try {
            // 将自选股代码列表合并成一个逗号分隔的字符串，用于API请求。
            val codes = watchlistCodes.joinToString(",")
            // 调用迁移至 StockApi.kt 的真实批量获取函数。
            // 此处会自动使用我们新添加的 `getSinaBatchRealtimeData` 函数。
            val rawResponse = getSinaBatchRealtimeData(codes)

            // 使用Kotlin集合操作流对返回的多行字符串进行高效处理。
            val newStockDataMap = rawResponse.lines() // 1. 按行分割
                .filter { it.isNotBlank() } // 2. 过滤掉可能存在的空行
                .mapNotNull { line -> // 3. 对每一行进行解析和转换，`mapNotNull`会自动过滤掉解析失败返回null的结果。
                    try {
                        // 从 "var hq_str_" 之后、"=" 之前提取股票代码。
                        val code = line.substringAfter("var hq_str_").substringBefore("=")
                        // 提取双引号之间的核心数据字符串。
                        val dataString = line.substringAfter('"').substringBeforeLast('"')

                        // 如果数据字符串为空，说明该代码可能无效，记录警告并跳过。
                        if (dataString.isBlank()) {
                            logger.warning("[自选股数据警告]: 代码 $code 的行情数据为空，可能为无效代码。")
                            return@mapNotNull null
                        }

                        // 按逗号分割数据字段。
                        val parts = dataString.split(',')
                        // 防御性编程：确保至少有足够的字段用于计算。
                        if (parts.size < 4) {
                            logger.warning("[自选股数据警告]: 代码 $code 的行情数据字段不足: $dataString")
                            return@mapNotNull null
                        }

                        // 解析字段：字段3是当前价，字段2是昨日收盘价。使用 `toDoubleOrNull` 避免因格式错误导致程序崩溃。
                        val price = parts[3].toDoubleOrNull() ?: 0.0
                        val preClose = parts[2].toDoubleOrNull() ?: 0.0

                        // 如果价格或昨收价为0，则无法计算有意义的涨跌幅，视为无效数据点并跳过。
                        if (price == 0.0 || preClose == 0.0) return@mapNotNull null

                        // 计算涨跌幅百分比: (现价 / 昨收 - 1) * 100
                        val changePct = (price / preClose - 1) * 100

                        // 计算涨速：(当前价格 - 上次价格) / 上次价格 * 100
                        val lastPriceForRise = watchlistLastPrices[code]
                        // 只有当上次价格有效时才计算涨速，否则为0。
                        val rise = if (lastPriceForRise != null && lastPriceForRise > 0) (price - lastPriceForRise) / lastPriceForRise * 100 else 0.0
                        // 更新map中该股票的价格，为下一次计算涨速做准备。
                        watchlistLastPrices[code] = price

                        // 创建 StockData 对象。
                        StockData(
                            code = code,
                            price = price,
                            changePercent = changePct,
                            rise = rise,
                            indexPercent = 0.0, // 自选股列表不与特定指数关联
                            timestamp = LocalDateTime.now()
                        )
                    } catch (e: Exception) {
                        // 捕获单行解析时可能出现的异常，记录错误并允许循环继续处理其他行。
                        logger.warning("[自选股数据警告]: 解析行数据失败: '$line', 错误: ${e.message}")
                        null // 返回null，让 `mapNotNull` 过滤掉此条目
                    }
                }.associateBy { it.code } // 4. 将列表转换为以股票代码为键的Map，优化后续查找效率。

            // 核心修正：移除了 withContext(Dispatchers.Main) 包装。
            // 原理：在 Compose for Desktop 中，UI 状态对象（如 mutableStateListOf）是线程安全的。它们内部使用快照（Snapshot）系统，
            // 允许从任何线程（如此处的 Dispatchers.IO）直接修改。Compose 框架会自动、安全地将这些变更同步到 UI 线程并触发重绘。
            // 优点：这样可以避免在非 Android 环境下因缺少 Main 调度器而导致的崩溃，同时也简化了代码，并减少了不必要的线程切换开销。

            // 根据用户配置的 `watchlistCodes` 顺序，构建最终的UI列表。
            val updatedList = watchlistCodes.mapNotNull { code -> newStockDataMap[code] }

            // 仅在数据实际发生变化时才更新状态，这是Compose性能优化的最佳实践，可避免不必要的UI重组。
            // 注意：此比较和更新操作现在直接在 IO 线程上执行，但由于 Compose 状态的线程安全性，这是完全安全的。
            if (watchlistData != updatedList) {
                watchlistData.clear()
                watchlistData.addAll(updatedList)
            }

        } catch (e: Exception) {
            // 捕获在数据获取和处理过程中发生的任何顶层异常（如网络连接问题）。
            logger.severe("[自选股数据异常]: 获取自选股实时数据时发生未知异常: ${e.message}")
            e.printStackTrace()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            fetchRealtimeData()
        }
    }

    private fun isTradingTime(): Boolean {
        val now = LocalTime.now(ZoneId.of("Asia/Shanghai"))
        val amStart = LocalTime.of(9, 25)
        val amEnd = LocalTime.of(11, 31)
        val pmStart = LocalTime.of(13, 0)
        val pmEnd = LocalTime.of(15, 1)
        return now in amStart..amEnd || now in pmStart..pmEnd
//        return true
    }
}
