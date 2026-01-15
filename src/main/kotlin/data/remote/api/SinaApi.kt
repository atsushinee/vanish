package data.remote.api

import data.model.SinaQuote
import data.remote.apiClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import util.toSinaSymbol
import java.nio.charset.Charset
import java.util.logging.Logger

private val logger = Logger.getLogger("SinaApiLog")
private const val SINA_HQ_URL_FORMAT = "http://hq.sinajs.cn/rn=%s&list=%s"

/**
 * 获取单只股票的新浪实时行情数据。
 * @param code 股票代码。
 * @return 解析后的 SinaQuote 对象，或在失败时返回 null。
 */
internal suspend fun getSinaRealtimeData(code: String): SinaQuote? {
    return try {
        val url = SINA_HQ_URL_FORMAT.format(System.currentTimeMillis(), toSinaSymbol(code))
        val response = apiClient.get(url) {
            header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36 Edg/119.0.0.0"
            )
            header("Referer", "https://finance.sina.com.cn/")
        }

        val responseText = response.bodyAsText(Charset.forName("GBK"))
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
        logger.severe("[新浪接口请求失败 - $code]: ${e.message}")
        null
    }
}

/**
 * 批量获取新浪实时行情数据。
 * @param codes 逗号分隔的股票代码字符串 (例如 "sh600000,sz000001")。
 * @return 从API获取的原始响应字符串，多行数据以 \n 分隔。
 */
suspend fun getSinaBatchRealtimeData(codes: String): String {
    if (codes.isBlank()) {
        logger.warning("[新浪批量接口警告]: 请求的股票代码列表为空。")
        return ""
    }
    return try {
        val url = "http://hq.sinajs.cn/rn=${System.currentTimeMillis()}&list=$codes"
        val response = apiClient.get(url) {
            header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36 Edg/119.0.0.0"
            )
            header("Referer", "https://finance.sina.com.cn/")
        }

        val responseText = response.bodyAsText(Charset.forName("GBK"))
        logger.info("[新浪接口原始响应 getSinaBatchRealtimeData]: $responseText")
        responseText
    } catch (e: Exception) {
        logger.severe("[新浪批量接口请求失败]: 请求代码 '$codes' 时发生错误: ${e.message}")
        ""
    }
}
