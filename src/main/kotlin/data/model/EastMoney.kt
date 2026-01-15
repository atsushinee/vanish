package data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 东方财富分时数据API响应的顶层结构。
 * 将其放在独立文件中，有助于避免kotlinx.serialization插件的潜在问题。
 */
@Serializable
data class EastMoneyTimeShareResponse(
    val data: TimeShareDataContainer? = null
)

/**
 * 定义分时图数据的容器，对应JSON中的 "data" 对象。
 */
@Serializable
data class TimeShareDataContainer(
    val code: String? = null,
    // 使用 @SerialName 注解来映射JSON中的 "preClose" 字段到 Kotlin 的 preClosePrice 属性
    @SerialName("preClose")
    val preClosePrice: Float = 0.0f,
    val trends: List<String> = emptyList()
)
