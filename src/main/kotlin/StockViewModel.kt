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
import java.time.format.DateTimeFormatter

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
    val watchlistCodes = mutableStateListOf("sh000001", "sz002413", "sz002639", "sh603273")

    // 存储处理后的自选股实时数据，用于UI展示。
    val watchlistData = mutableStateListOf<StockData>()

    // 新增：用于UI显示的最后更新时间。
    // 使用 mutableStateOf，当它的值改变时，Compose UI会自动更新。
    val lastUpdateTime = mutableStateOf("")

    // 管理自选股监控协程的Job对象。
    private var watchlistJob: Job? = null

    // 存储每只自选股上一次查询时的价格，用于计算“涨速”。
    private val watchlistLastPrices = mutableMapOf<String, Double>()

    /**
     * 启动自选股的实时监控。
     */
    fun startWatchlistMonitor() {
        if (watchlistJob?.isActive == true) {
            logger.info("[自选股监控]: 监控任务已在运行，无需重复启动。")
            return
        }

        watchlistJob = viewModelScope.launch {
            logger.info("[自选股监控]: 监控任务已启动。")
            while (isActive) {
                if (isTradingTime()) {
                    fetchWatchlistData()
                }
                delay(2000L)
            }
        }
    }

    /**
     * 停止自选股的实时监控。
     */
    fun stopWatchlistMonitor() {
        watchlistJob?.cancel()
        watchlistJob = null
        watchlistData.clear()
        watchlistLastPrices.clear()
        lastUpdateTime.value = "" // 停止时清空更新时间
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

                val newStockData = StockData(
                    code = TARGET_STOCK,
                    name = "", // 主监控窗口暂时不需要名称
                    price = price,
                    changePercent = changePct,
                    rise = rise,
                    indexPercent = indexPct,
                    timestamp = LocalDateTime.now()
                )
                stockData.value = newStockData
                history.add(newStockData)
                if (history.size > 500) {
                    history.removeAt(0)
                }
            } else {
                logger.warning("[数据处理警告]: 获取新浪行情数据失败或返回数据不完整。")
            }
        } catch (e: Exception) {
            logger.severe("[数据处理异常]: 获取实时数据时发生未知异常: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 批量获取自选股列表的实时行情数据并更新UI状态。
     */
    private suspend fun fetchWatchlistData() {
        if (watchlistCodes.isEmpty()) return

        try {
            val codes = watchlistCodes.joinToString(",")
            val rawResponse = getSinaBatchRealtimeData(codes)

            val newStockDataMap = rawResponse.lines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        val code = line.substringAfter("var hq_str_").substringBefore("=")
                        val dataString = line.substringAfter('"').substringBeforeLast('"')

                        if (dataString.isBlank()) {
                            logger.warning("[自选股数据警告]: 代码 $code 的行情数据为空，可能为无效代码。")
                            return@mapNotNull null
                        }

                        val parts = dataString.split(',')
                        // 新浪接口返回的数据字段中，第一个是股票名称
                        if (parts.size < 4) {
                            logger.warning("[自选股数据警告]: 代码 $code 的行情数据字段不足: $dataString")
                            return@mapNotNull null
                        }

                        val name = parts[0] // 解析股票名称
                        val price = parts[3].toDoubleOrNull() ?: 0.0
                        val preClose = parts[2].toDoubleOrNull() ?: 0.0

                        if (price == 0.0 || preClose == 0.0) return@mapNotNull null

                        val changePct = (price / preClose - 1) * 100

                        val lastPriceForRise = watchlistLastPrices[code]
                        val rise = if (lastPriceForRise != null && lastPriceForRise > 0) (price - lastPriceForRise) / lastPriceForRise * 100 else 0.0
                        watchlistLastPrices[code] = price

                        StockData(
                            code = code,
                            name = name, // 传入解析出的名称
                            price = price,
                            changePercent = changePct,
                            rise = rise,
                            indexPercent = 0.0,
                            timestamp = LocalDateTime.now()
                        )
                    } catch (e: Exception) {
                        logger.warning("[自选股数据警告]: 解析行数据失败: '$line', 错误: ${e.message}")
                        null
                    }
                }.associateBy { it.code }

            val updatedList = watchlistCodes.mapNotNull { code -> newStockDataMap[code] }

            if (watchlistData != updatedList) {
                watchlistData.clear()
                watchlistData.addAll(updatedList)
                // 数据更新成功后，更新时间戳
                // 定义一个更友好的时间格式
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                // 将当前时间格式化为字符串，并更新到UI状态中
                lastUpdateTime.value = "更新时间: ${LocalDateTime.now().format(formatter)}"
            }

        } catch (e: Exception) {
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
//        return now in amStart..amEnd || now in pmStart..pmEnd
        return true
    }
}
