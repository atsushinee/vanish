package data.remote

import data.model.SinaQuote
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.nio.charset.Charset
import java.util.logging.Logger

// ================= 1. 核心配置 =================
private const val SINA_HQ_URL_FORMAT = "http://hq.sinajs.cn/rn=%s&list=%s"

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

/**
 * 批量获取新浪实时行情数据。
 * @param codes 逗号分隔的股票代码字符串 (例如 "sh600000,sz000001")。
 * @return 从API获取的原始响应字符串，多行数据以 \n 分隔。
 */
suspend fun getSinaBatchRealtimeData(codes: String): String {
    // 如果传入的代码字符串为空，则直接返回空字符串，避免无效的网络请求。
    if (codes.isBlank()) {
        // 记录一次警告日志，便于追踪可能的前端逻辑问题。
        logger.warning("[新浪批量接口警告]: 请求的股票代码列表为空。")
        // 返回空字符串，调用方已准备好处理此情况。
        return ""
    }
    return try {
        // 构建批量查询的URL，格式比单个查询更简洁。
        val url = "http://hq.sinajs.cn/rn=${System.currentTimeMillis()}&list=$codes"
        // 使用共享的Ktor客户端实例发起GET请求。
        val response = client.get(url) {
            // 设置与浏览器行为一致的User-Agent头，模拟普通用户访问，提高接口可用性。
            header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36 Edg/119.0.0.0"
            )
            // 设置Referer头，这是新浪行情接口的一个常见要求，表明请求来源。
            header("Referer", "https://finance.sina.com.cn/")
        }

        // 将响应体以GBK编码解析为字符串，这是新浪接口的标准编码。
        val responseText = response.bodyAsText(Charset.forName("GBK"))
        // 成功获取后，直接返回原始文本，交由ViewModel进行解析。
        logger.info("[新浪接口原始响应 getSinaBatchRealtimeData]: $responseText")
        responseText
    } catch (e: Exception) {
        // 在捕获到任何网络或请求相关的异常时，记录一条严重的错误日志。
        logger.severe("[新浪批量接口请求失败]: 请求代码 '$codes' 时发生错误: ${e.message}")
        // 在失败时返回空字符串，确保函数总有返回值，防止调用方出现空指针异常。
        ""
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
