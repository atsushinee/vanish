package data.model

import java.time.LocalDateTime

/**
 * 用于UI显示的股票数据模型。
 * @param code 股票代码
 * @param name 股票名称
 * @param price 当前价格
 * @param changePercent 涨跌幅
 * @param rise 瞬时涨跌幅 (与上次刷新相比)
 * @param indexPercent 大盘指数涨跌幅
 * @param timestamp 数据获取时间戳
 */
data class StockData(
    val code: String,
    val name: String, // 新增：股票名称字段
    val price: Double,
    val changePercent: Double,
    val rise: Double,
    val indexPercent: Double,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
