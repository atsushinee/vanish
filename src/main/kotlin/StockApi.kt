import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.nio.charset.Charset
import java.util.logging.Logger

// ================= 1. 核心配置 =================
private const val SINA_HQ_URL_FORMAT = "http://hq.sinajs.cn/rn=%s&list=%s"
//internal const val TARGET_STOCK = "002639.SZ"
internal const val TARGET_STOCK = "002413.SZ"
internal const val INDEX_CODE = "sh000001"

private val client = HttpClient(CIO)
private val logger = Logger.getLogger("StockApiLog")

// ================= 3. API 请求封装 (新浪接口) =================

internal suspend fun getSinaRealtimeData(code: String): SinaQuote? {
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
