import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.*
import java.time.LocalTime
import java.time.ZoneId
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

    val stockData = mutableStateOf<StockData?>(null)
    private var lastPrice = 0.0
    private val viewModelScope = CoroutineScope(Dispatchers.IO)

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

                stockData.value = StockData(price, changePct, rise, indexPct)
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
}
