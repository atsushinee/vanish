package viewmodel

import data.model.TimeSharePoint
import data.remote.getTimeShare
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.logging.Logger

/**
 * 负责处理分时图业务逻辑和状态管理。
 * 从 UI 层分离出来，遵循 MVVM 模式。
 */
class TimeShareViewModel(private val code: String) {
    // 使用StateFlow来管理UI状态，确保线程安全和Compose的响应式更新
    private val _uiState = MutableStateFlow<TimeShareUiState>(TimeShareUiState.Loading)
    val uiState: StateFlow<TimeShareUiState> = _uiState

    // 日志记录器
    private val logger = Logger.getLogger("TimeShareViewModel")

    init {
        // ViewModel初始化时，立即在IO线程上加载数据
        loadTimeShareData()
    }

    private fun loadTimeShareData() {
        // 使用ViewModel的协程作用域，确保协程在ViewModel销毁时自动取消
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 调用API获取分时数据
                val response = getTimeShare(code)
                val dataContainer = response?.data
                if (dataContainer == null || dataContainer.trends.isEmpty()) {
                    _uiState.value = TimeShareUiState.NoData
                    logger.warning("股票[$code]的原始分时数据列表(trends)为空或数据容器(data)为null")
                } else {
                    val points = parseTrends(dataContainer.trends)
                    if (points.isEmpty()) {
                        _uiState.value = TimeShareUiState.NoData
                        logger.warning("解析后的股票[$code]分时数据点列表为空，可能所有数据点格式都有问题。")
                    } else {
                        _uiState.value = TimeShareUiState.Success(points, dataContainer.preClosePrice)
                        logger.info("成功加载并解析了股票[$code]的分时数据，共${points.size}个点，昨收价: ${dataContainer.preClosePrice}")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = TimeShareUiState.Error("数据加载失败: ${e.message}")
                logger.severe("加载股票[$code]的分时数据时发生错误: ${e.message}")
            }
        }
    }

    private fun parseTrends(trends: List<String>): List<TimeSharePoint> {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        return trends.mapNotNull { trendString ->
            val parts = trendString.split(',')
            try {
                val timeStr = parts[0].split(' ')[1]
                val time = LocalTime.parse(timeStr, formatter)
                val price = parts[1].toFloat()
                TimeSharePoint(time, price)
            } catch (e: Exception) {
                logger.warning("解析分时数据点失败: '$trendString', 错误: ${e.message}")
                null
            }
        }
    }
}

/**
 * 定义分时图UI状态的密封类。
 * 与 ViewModel 紧密相关，因此放在同一个文件中。
 */
sealed class TimeShareUiState {
    object Loading : TimeShareUiState()
    object NoData : TimeShareUiState()
    data class Success(val points: List<TimeSharePoint>, val preClosePrice: Float) : TimeShareUiState()
    data class Error(val message: String) : TimeShareUiState()
}
