package viewmodel

import data.model.StockData
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.logging.FileHandler
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import data.remote.getSinaBatchRealtimeData

// TODO: 将这些常量移至 AppConfig
private const val TARGET_STOCK = "sz002413"
private const val INDEX_CODE = "sh000001"

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
            val codes = listOf(TARGET_STOCK, INDEX_CODE).joinToString(",")
            val rawResponse = getSinaBatchRealtimeData(codes)
            val quotesMap = rawResponse.lines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        val code = line.substringAfter("var hq_str_").substringBefore("=")
                        val dataString = line.substringAfter('"').substringBeforeLast('"')

                        // 目的: 防御性编程，如果数据部分为空，则此条数据无效。
                        if (dataString.isBlank()) {
                            logger.warning("[数据解析警告]: 代码 $code 的行情数据为空。")
                            return@mapNotNull null // 返回null，mapNotNull会将其过滤掉
                        }

                        // 目的: 将数据字符串按逗号分割成多个字段。
                        val parts = dataString.split(',')
                        // 目的: 校验数据字段数量，防止因数据格式问题导致索引越界。
                        if (parts.size < 4) {
                            logger.warning("[数据解析警告]: 代码 $code 的行情数据字段不足: $dataString")
                            return@mapNotNull null
                        }

                        // 目的: 解析当前价格和昨日收盘价。
                        // 原理: 新浪接口中，parts[3] 是当前价，parts[2] 是昨日收盘价。使用 toDoubleOrNull() 进行安全转换。
                        val price = parts[3].toDoubleOrNull() ?: 0.0
                        val preClose = parts[2].toDoubleOrNull() ?: 0.0

                        // 目的: 过滤掉价格为0的无效数据，这通常表示股票停牌或数据异常。
                        if (price == 0.0 || preClose == 0.0) return@mapNotNull null

                        // 目的: 将解析结果构造成一个键值对。
                        // 原理: 键是股票代码，值是一个Pair，包含价格和昨收价，方便后续使用。
                        code to (price to preClose)
                    } catch (e: Exception) {
                        // 目的: 捕获单行解析中可能出现的任何异常，保证一个数据的错误不影响其他数据的处理。
                        logger.warning("[数据解析警告]: 解析行数据失败: '$line', 错误: ${e.message}")
                        null // 返回null，让mapNotNull过滤掉此错误条目
                    }
                }.toMap() // 将 (code, data) 对的列表转换为 Map<String, Pair<Double, Double>>

            // 目的: 从解析后的Map中安全地获取个股和指数的数据。
            // 原理: 使用代码作为键直接从Map中查找。
            val stockQuoteData = quotesMap[TARGET_STOCK]
            val indexQuoteData = quotesMap[INDEX_CODE]

            // 目的: 确保个股和指数的数据都成功获取到，才能进行后续计算。
            if (stockQuoteData != null && indexQuoteData != null) {
                // 目的: 从Pair中解构出价格和昨收价，使代码更具可读性。
                val (price, preClose) = stockQuoteData
                val (indexPrice, indexPreClose) = indexQuoteData

                // === 以下逻辑与原版本完全一致，以确保UI行为和数据计算规则不变 ===

                // 目的: 计算个股的涨跌幅百分比。
                // 原理: (当前价 / 昨收价 - 1) * 100。增加 preClose > 0 的判断避免除零错误。
                val changePct = if (preClose > 0) (price / preClose - 1) * 100 else 0.0
                // 目的: 计算个股的瞬时涨速，即与上一次刷新价格的变动百分比。
                // 原理: (当前价 - 上次价格) / 上次价格 * 100。
                val rise = if (lastPrice > 0) (price - lastPrice) / lastPrice * 100 else 0.0
                // 目的: 更新“上次价格”，为下一次计算涨速做准备。
                // 注意事项: 这个状态(lastPrice)是与 fetchRealtimeData 的调用频率相关的。
                lastPrice = price

                // 目的: 计算大盘指数的涨跌幅百分比。
                val indexPct = if (indexPreClose > 0) (indexPrice / indexPreClose - 1) * 100 else 0.0

                // 目的: 创建一个新的 StockData 对象，用于封装所有计算出的新数据。
                // 注意事项: 这是UI状态更新的源头。
                val newStockData = StockData(
                    code = TARGET_STOCK,
                    name = "", // 主监控窗口不展示名称，保持与原逻辑一致
                    price = price,
                    changePercent = changePct,
                    rise = rise,
                    indexPercent = indexPct,
                    timestamp = LocalDateTime.now()
                )
                // 目的: 更新UI状态，触发Compose UI的重组。
                // 原理: stockData 是一个 mutableStateOf 对象，对其 .value 的赋值会通知Compose框架。
                stockData.value = newStockData
                // 目的: 将新数据添加到历史记录中，用于绘制价格曲线图。
                history.add(newStockData)
                // 目的: 维持历史记录列表的固定大小，防止内存无限增长。
                // 原理: 如果列表大小超过500，则移除最早的一条数据。
                if (history.size > 500) {
                    history.removeAt(0)
                }
            } else {
                // 目的: 如果批量获取后，个股或指数任一数据缺失，则记录警告。
                logger.warning("[数据处理警告]: 批量获取行情数据失败或返回的数据不完整。")
            }
        } catch (e: Exception) {
            // 目的: 捕获整个数据获取和处理流程中的顶层异常，如网络请求失败。
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
                        val rise =
                            if (lastPriceForRise != null && lastPriceForRise > 0) (price - lastPriceForRise) / lastPriceForRise * 100 else 0.0
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
        LocalTime.now(ZoneId.of("Asia/Shanghai"))
        LocalTime.of(9, 25)
        LocalTime.of(11, 31)
        LocalTime.of(13, 0)
        LocalTime.of(15, 1)
//        return now in amStart..amEnd || now in pmStart..pmEnd
        return true
    }
}
