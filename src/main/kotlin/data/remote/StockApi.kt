package data.remote

import data.model.EastMoneyTimeShareResponse
import data.model.SinaQuote
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import java.nio.charset.Charset
import java.util.logging.Logger

// ================= 1. 核心配置 =================
private const val SINA_HQ_URL_FORMAT = "http://hq.sinajs.cn/rn=%s&list=%s"

private val client = HttpClient(CIO)
private val logger = Logger.getLogger("StockApiLog")

// 新增Json解析器，配置为忽略JSON中存在但数据类中没有的字段
private val json = Json { ignoreUnknownKeys = true }


// ================= 2. API 请求封装 (新浪接口) =================

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

// ================= 3. API 请求封装 (东方财富接口) =================

/**
 * 获取东方财富分时图数据。
 * @param code 股票代码 (例如 "600000.SH")。
 * @return 解析后的分时图数据模型，或在失败时返回 null。
 */
suspend fun getTimeShare(code: String): EastMoneyTimeShareResponse? {
    // 增加日志记录，跟踪请求的股票代码
    logger.info("准备获取东方财富分时数据，代码: $code")
    return try {
        // 将内部代码（如 600000.SH）转换为东方财富API要求的格式（如 1.600000）
        val secid = toEastMoneySymbol(code)
        // 构建请求URL，包含所有必需的字段参数
        val url =
            "http://push2his.eastmoney.com/api/qt/stock/trends2/get?fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&fields2=f51,f52,f53,f54,f55,f56,f57,f58&secid=$secid&ndays=1"

        // 发起异步GET请求
        val response = client.get(url) {
            // 设置User-Agent，模拟浏览器访问，这是反爬虫策略的要求
            header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
            // 设置Referer，表明请求来源，同样是反爬虫的关键
            header("Referer", "https://quote.eastmoney.com/")
        }

        // 获取响应文本
        val responseText = response.bodyAsText()
        // 记录原始响应，便于调试
        logger.info("[东方财富分时接口原始响应 -${url} $code]:\n $responseText")

        // 使用kotlinx.serialization库将JSON字符串解码为数据类对象
        // 这种方式类型安全，且能自动处理JSON到Kotlin对象的映射
        json.decodeFromString<EastMoneyTimeShareResponse>(responseText)
    } catch (e: Exception) {
        // 记录详细的错误日志，包括请求代码和异常信息，便于问题排查
        logger.severe("[东方财富分时接口请求失败 - $code]: ${e.message}")
        // 发生异常时返回null，让调用方可以处理错误情况
        null
    }
}


// ================= 4. 代码转换工具 =================

/**
 * 将多种格式的股票代码统一转换为东方财富API要求的格式。
 * @param code 股票代码，支持 "sh600000", "sz000001", "600000.SH", "000001.SZ", "600000" 等格式。
 * @return 东方财富API格式的代码 (例如 "1.600000" 或 "0.000001")。
 */
private fun toEastMoneySymbol(code: String): String {
    // 核心修正：增加对 "sh" 和 "sz" 前缀的处理，使其成为最优先的判断逻辑。
    // 原理：通过检查字符串前缀，可以快速、准确地识别新格式，避免与旧的纯数字代码规则冲突。
    // 注意：使用 .startsWith() 方法比正则表达式更高效，且代码可读性更好。
    val lowerCaseCode = code.lowercase()
    if (lowerCaseCode.startsWith("sh")) {
        // 如果是 "sh" 开头，移除前缀并格式化为 "1.代码"
        return "1.${lowerCaseCode.removePrefix("sh")}"
    }
    if (lowerCaseCode.startsWith("sz")) {
        // 如果是 "sz" 开头，移除前缀并格式化为 "0.代码"
        return "0.${lowerCaseCode.removePrefix("sz")}"
    }

    // 保留原有逻辑：处理 "600000.SH" 这种带"."的格式
    if (code.contains('.')) {
        val parts = code.split('.')
        val market = parts[1].uppercase()
        val stockCode = parts[0]
        // 根据市场标识，添加东方财富特定的前缀：1代表上海，0代表深圳
        return when (market) {
            "SH" -> "1.$stockCode"
            "SZ" -> "0.$stockCode"
            // 如果市场未知，则返回原始代码，尽力而为
            else -> code
        }
    }

    // 保留原有逻辑：处理纯数字代码，根据首位数字猜测市场
    return when {
        code.startsWith("6") -> "1.$code" // 上证
        code.startsWith("0") || code.startsWith("3") -> "0.$code" // 深证
        // 其他情况，无法判断，返回原样
        else -> code
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
