package data.remote.api

import data.model.EastMoneyTimeShareResponse
import data.remote.apiClient
import data.remote.jsonParser
import io.ktor.client.request.*
import io.ktor.client.statement.*
import util.toEastMoneySymbol
import java.util.logging.Logger

private val logger = Logger.getLogger("EastMoneyApiLog")

/**
 * 获取东方财富分时图数据。
 * @param code 股票代码 (例如 "600000.SH")。
 * @return 解析后的分时图数据模型，或在失败时返回 null。
 */
suspend fun getTimeShare(code: String): EastMoneyTimeShareResponse? {
    logger.info("准备获取东方财富分时数据，代码: $code")
    return try {
        val secid = toEastMoneySymbol(code)
        val url =
            "http://push2his.eastmoney.com/api/qt/stock/trends2/get?fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&fields2=f51,f52,f53,f54,f55,f56,f57,f58&secid=$secid&ndays=1"

        val response = apiClient.get(url) {
            header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
            header("Referer", "https://quote.eastmoney.com/")
        }

        val responseText = response.bodyAsText()
        logger.info("[东方财富分时接口原始响应 -${url} $code]:\n $responseText")

        jsonParser.decodeFromString<EastMoneyTimeShareResponse>(responseText)
    } catch (e: Exception) {
        logger.severe("[东方财富分时接口请求失败 - $code]: ${e.message}")
        null
    }
}
