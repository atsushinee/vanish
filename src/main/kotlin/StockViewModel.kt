import androidx.compose.runtime.mutableStateOf
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import java.nio.charset.Charset
import java.time.LocalTime
import java.time.ZoneId
import java.util.logging.FileHandler
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

// ================= 1. 核心配置 =================
private const val SINA_HQ_URL_FORMAT = "http://hq.sinajs.cn/rn=%s&list=%s"
private const val TARGET_STOCK = "002639.SZ"
private const val INDEX_CODE = "sh000001"

class StockViewModel {

    private val client = HttpClient(CIO)

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

    // ================= 3. API 请求封装 (新浪接口) =================

    private suspend fun getSinaRealtimeData(code: String): SinaQuote? {
        return try {
            val url = SINA_HQ_URL_FORMAT.format(System.currentTimeMillis(), toSinaSymbol(code))
            val response = client.get(url) {
                header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36 Edg/119.0.0.0"
                )
                header("Referer", "https://finance.sina.com.cn/")
            }

            val responseText = response.bodyAsText(Charset.forName("GBK"))
            // 核心修正：添加中文日志，记录接口返回的原始信息
            // 目的：便于调试和分析接口返回的具体内容，快速定位问题。
            logger.info("[新浪接口原始响应 - $code]: $responseText")

            val dataPart = responseText.substringAfter('"').substringBefore('"')
            if (dataPart.isBlank()) {
                logger.warning("[数据解析警告 - $code]: 接口返回的数据部分为空。")
                return null
            }

            val parts = dataPart.split(',')
            if (parts.size < 4) {
                logger.warning("[数据解析警告 - $code]: 数据列数量不足，无法解析。")
                return null
            }

            val price = parts[3].toDoubleOrNull()
            val preClose = parts[2].toDoubleOrNull()

            if (price != null && preClose != null) {
                SinaQuote(price, preClose)
            } else {
                logger.warning("[数据解析警告 - $code]: 价格或昨收价解析为null。")
                null
            }
        } catch (e: Exception) {
            // 核心修正：添加中文日志，记录接口请求失败的错误
            logger.severe("[新浪接口请求失败 - $code]: ${e.message}")
            null
        }
    }

    private fun toSinaSymbol(code: String): String {
        return if (code.contains('.')) {
            val parts = code.split('.')
            parts[1].lowercase() + parts[0]
        } else {
            code
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

// ================= 4. 数据类 =================

private data class SinaQuote(val price: Double, val preClose: Double)

data class StockData(val price: Double, val changePercent: Double, val rise: Double, val indexPercent: Double)
