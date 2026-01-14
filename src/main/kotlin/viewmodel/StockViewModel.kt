package viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import config.AppConfig
import data.model.StockData
import data.remote.getSinaBatchRealtimeData
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.logging.FileHandler
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

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
    val watchlistCodes = mutableStateListOf<String>()

    // 存储处理后的自选股实时数据，用于UI展示。
    val watchlistData = mutableStateListOf<StockData>()

    // 新增：用于UI显示的最后更新时间。
    val lastUpdateTime = mutableStateOf("")

    // 管理自选股监控协程的Job对象。
    private var watchlistJob: Job? = null

    // 存储每只自选股上一次查询时的价格，用于计算“涨速”。
    private val watchlistLastPrices = mutableMapOf<String, Double>()

    /**
     * 启动自选股的实时监控。
     * 优化了轮询逻辑，区分交易与非交易时段。
     */
    fun startWatchlistMonitor() {
        // 防止重复启动监控任务
        if (watchlistJob?.isActive == true) {
            logger.info("[自选股监控]: 监控任务已在运行，无需重复启动。")
            return
        }

        watchlistJob = viewModelScope.launch {
            logger.info("[自选股监控]: 监控任务已启动。")
            var hasFetchedOnceOutOfHours = false

            while (isActive) {
                val inTradingTime = isTradingTime()
                if (inTradingTime || !hasFetchedOnceOutOfHours) {
                    fetchWatchlistData()
                    if (!inTradingTime) hasFetchedOnceOutOfHours = true
                }
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
        // 目的: 从 AppConfig 的类型安全属性中加载自选股列表。
        // 原理: 直接访问 AppConfig.watchlist，它返回一个 List<String>，无需关心序列化和默认值。
        watchlistCodes.addAll(AppConfig.watchlist)

        viewModelScope.launch {
            startRealtimeMonitor()
        }
    }

    // ================= 2. 实时监控模块 =================

    private suspend fun startRealtimeMonitor() {
        var hasFetchedOnceOutOfHours = false

        while (viewModelScope.isActive) {
            val inTradingTime = isTradingTime()
            if (inTradingTime || !hasFetchedOnceOutOfHours) {
                fetchRealtimeData()
                if (!inTradingTime) hasFetchedOnceOutOfHours = true
            }
            delay(2000L)
        }
    }

    private suspend fun fetchRealtimeData() {
        try {
            // 目的: 通过 AppConfig 的类型安全属性获取目标股票和指数代码。
            // 原理: 直接访问 AppConfig.targetStock 和 AppConfig.indexCode，如同访问普通变量。
            val targetStock = AppConfig.targetStock
            val indexCode = AppConfig.indexCode

            val codes = listOf(targetStock, indexCode).joinToString(",")
            val rawResponse = getSinaBatchRealtimeData(codes)
            val quotesMap = rawResponse.lines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        val code = line.substringAfter("var hq_str_").substringBefore("=")
                        val dataString = line.substringAfter('"').substringBeforeLast('"')

                        if (dataString.isBlank()) {
                            logger.warning("[数据解析警告]: 代码 $code 的行情数据为空。")
                            return@mapNotNull null
                        }

                        val parts = dataString.split(',')
                        if (parts.size < 4) {
                            logger.warning("[数据解析警告]: 代码 $code 的行情数据字段不足: $dataString")
                            return@mapNotNull null
                        }

                        val price = parts[3].toDoubleOrNull() ?: 0.0
                        val preClose = parts[2].toDoubleOrNull() ?: 0.0

                        if (price == 0.0 || preClose == 0.0) return@mapNotNull null

                        code to (price to preClose)
                    } catch (e: Exception) {
                        logger.warning("[数据解析警告]: 解析行数据失败: '$line', 错误: ${e.message}")
                        null
                    }
                }.toMap()

            val stockQuoteData = quotesMap[targetStock]
            val indexQuoteData = quotesMap[indexCode]

            if (stockQuoteData != null && indexQuoteData != null) {
                val (price, preClose) = stockQuoteData
                val (indexPrice, indexPreClose) = indexQuoteData

                val changePct = if (preClose > 0) (price / preClose - 1) * 100 else 0.0
                val rise = if (lastPrice > 0) (price - lastPrice) / lastPrice * 100 else 0.0
                lastPrice = price

                val indexPct = if (indexPreClose > 0) (indexPrice / indexPreClose - 1) * 100 else 0.0

                val newStockData = StockData(
                    code = targetStock,
                    name = "",
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
                logger.warning("[数据处理警告]: 批量获取行情数据失败或返回的数据不完整。")
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
                        if (parts.size < 4) {
                            logger.warning("[自选股数据警告]: 代码 $code 的行情数据字段不足: $dataString")
                            return@mapNotNull null
                        }

                        val name = parts[0]
                        val price = parts[3].toDoubleOrNull() ?: 0.0
                        val preClose = parts[2].toDoubleOrNull() ?: 0.0

                        if (price == 0.0 || preClose == 0.0) return@mapNotNull null

                        val changePct = (price / preClose - 1) * 100

                        val lastPriceForRise = watchlistLastPrices[code]
                        val rise =
                            if (lastPriceForRise != null && lastPriceForRise > 0) (price - lastPriceForRise) / lastPriceForRise * 100 else 0.0
                        watchlistLastPrices[code] = price

                        StockData(
                            code = code,
                            name = name,
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
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                lastUpdateTime.value = "${LocalDateTime.now().format(formatter)}"
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
        return now in amStart..amEnd || now in pmStart..pmEnd
    }

    /**
     * 添加一只新的自选股到列表，并持久化保存。
     * @param code 股票代码
     */
    fun addWatchlistCode(code: String) {
        if (code.isNotBlank() && !watchlistCodes.contains(code)) {
            watchlistCodes.add(code)
            // 目的: 通过 AppConfig 的类型安全属性更新自选股列表。
            // 原理: 直接将 ViewModel 中的列表赋值给 AppConfig.watchlist，其 setter 会处理序列化和持久化。
            AppConfig.watchlist = watchlistCodes.toList()
            AppConfig.save()
            logger.info("[自选股配置]: 添加新自选股 $code 并已保存。")
        }
    }

    /**
     * 从列表中移除一只自选股，并持久化保存。
     * @param code 股票代码
     */
    fun removeWatchlistCode(code: String) {
        if (watchlistCodes.remove(code)) {
            // 目的: 同样通过类型安全的属性来更新配置。
            // 原理: 将修改后的列表重新赋值给 AppConfig.watchlist。
            AppConfig.watchlist = watchlistCodes.toList()
            AppConfig.save()
            logger.info("[自选股配置]: 移除自选股 $code 并已保存。")
        }
    }
}
