/**
 * 新浪行情接口返回的原始数据模型。
 * @param price 当前价格
 * @param preClose 昨日收盘价
 */
internal data class SinaQuote(val price: Double, val preClose: Double)

/**
 * 用于UI显示的股票数据模型。
 * @param price 当前价格
 * @param changePercent 涨跌幅
 * @param rise 瞬时涨跌幅 (与上次刷新相比)
 * @param indexPercent 大盘指数涨跌幅
 */
data class StockData(val price: Double, val changePercent: Double, val rise: Double, val indexPercent: Double)
