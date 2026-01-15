package data.model

import java.time.LocalTime

/**
 * 定义分时图上的一个数据点。
 * 这是一个纯粹的数据模型，因此独立存放在 model 包中。
 */
data class TimeSharePoint(val time: LocalTime, val price: Float)
