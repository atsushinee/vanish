package data.model

/**
 * 新浪行情接口返回的原始数据模型。
 * @param price 当前价格
 * @param preClose 昨日收盘价
 */
internal data class SinaQuote(val price: Double, val preClose: Double)
