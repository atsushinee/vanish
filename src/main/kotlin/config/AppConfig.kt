package config

import java.io.File
import java.util.*

/**
 * AppConfig 单例对象，用于以类型安全的方式管理整个应用程序的配置。
 *
 * 设计原则:
 * 1.  **完全封装**: 外部模块仅通过 Kotlin 属性（如 `AppConfig.targetStock`）访问配置，无需关心底层的存储键或实现细节。
 * 2.  **类型安全**: 每个配置项都有明确的类型（如 `List<String>`, `Float`），在访问时自动完成类型转换和序列化。
 * 3.  **高内聚**: 所有与配置相关的逻辑，包括键名、默认值、序列化/反序列化，都内聚在此对象内部。
 * 4.  **结构化键名**: 内部使用带语义前缀的私有键名（如 "stock.target"），便于管理和调试。
 */
object AppConfig {
    // 配置文件路径
    private val configFile = File("config.properties")

    // Properties 对象，用于存储键值对
    private val properties = Properties()

    //region 私有键常量 (Private Key Constants)
    // 股票/数据相关配置
    private const val KEY_STOCK_WATCHLIST = "stock.watchlist"
    private const val KEY_STOCK_TARGET = "stock.target"
    private const val KEY_STOCK_INDEX = "stock.index"

    // UI 相关配置
    private const val KEY_UI_WINDOW_X = "ui.window.x"
    private const val KEY_UI_WINDOW_Y = "ui.window.y"
    //endregion

    // AI 相关配置
    private const val KEY_AI_GEMINI_APIKEY = "ai.gemini.apiKey"


    //region 默认值常量 (Default Value Constants)
    private const val DEFAULT_STOCK_TARGET = "sz002413"
    private const val DEFAULT_STOCK_INDEX = "sh000001"
    private val DEFAULT_STOCK_WATCHLIST = listOf("sh000001", "sz002413", "sz002639", "sh603273")
    private const val DEFAULT_UI_WINDOW_POS = 0f
    //endregion

    // 初始化块，在对象创建时从文件加载配置
    init {
        if (configFile.exists()) {
            configFile.inputStream().use { properties.load(it) }
        }
    }

    //region 公开的配置属性 (Public Configuration Properties)

    /**
     * 目标监控的股票代码。
     * getter/setter 内部处理与 properties 的交互和默认值。
     */
    var targetStock: String
        // 目的: 获取目标股票代码，如果未配置，则返回默认值。
        // 原理: 调用内部的 privateGetProperty，使用带前缀的私有键。
        get() = privateGetProperty(KEY_STOCK_TARGET, DEFAULT_STOCK_TARGET)
        // 目的: 设置新的目标股票代码。
        // 原理: 调用内部的 privateSetProperty，确保持久化时使用正确的键。
        set(value) = privateSetProperty(KEY_STOCK_TARGET, value)

    /**
     * 大盘指数代码。
     */
    var indexCode: String
        get() = privateGetProperty(KEY_STOCK_INDEX, DEFAULT_STOCK_INDEX)
        set(value) = privateSetProperty(KEY_STOCK_INDEX, value)

    /**
     * 自选股列表。
     * getter/setter 内部处理 List<String> 与逗号分隔字符串之间的序列化/反序列化。
     */
    var watchlist: List<String>
        // 目的: 获取自选股列表。
        // 原理: 从配置文件读取逗号分隔的字符串，然后分割成 List<String>。如果配置为空，则返回默认列表。
        get() {
            val storedValue = privateGetProperty(KEY_STOCK_WATCHLIST, "")
            return if (storedValue.isNotBlank()) {
                storedValue.split(',').filter { it.isNotBlank() }
            } else {
                DEFAULT_STOCK_WATCHLIST
            }
        }
        // 目的: 保存自选股列表。
        // 原理: 将 List<String> 通过逗号连接成一个字符串，然后存入 properties。
        set(value) {
            privateSetProperty(KEY_STOCK_WATCHLIST, value.joinToString(","))
        }

    /**
     * 窗口的 X 坐标。
     * getter/setter 内部处理 Float 与 String 之间的转换。
     */
    var windowX: Float
        get() = privateGetProperty(KEY_UI_WINDOW_X, DEFAULT_UI_WINDOW_POS.toString()).toFloatOrNull()
            ?: DEFAULT_UI_WINDOW_POS
        set(value) = privateSetProperty(KEY_UI_WINDOW_X, value.toString())

    /**
     * 窗口的 Y 坐标。
     */
    var windowY: Float
        get() = privateGetProperty(KEY_UI_WINDOW_Y, DEFAULT_UI_WINDOW_POS.toString()).toFloatOrNull()
            ?: DEFAULT_UI_WINDOW_POS
        set(value) = privateSetProperty(KEY_UI_WINDOW_Y, value.toString())


    var geminiApiKey: String
        get() = privateGetProperty(KEY_AI_GEMINI_APIKEY, "")
        set(value) = privateSetProperty(KEY_AI_GEMINI_APIKEY, value)
    //endregion

    //region 私有持久化方法 (Private Persistence Methods)

    // 将 getProperty 变为私有，强制外部通过属性访问
    private fun privateGetProperty(key: String, defaultValue: String): String {
        return properties.getProperty(key, defaultValue)
    }

    // 将 setProperty 变为私有，强制外部通过属性访问
    private fun privateSetProperty(key: String, value: String) {
        properties.setProperty(key, value)
    }

    /**
     * 将当前所有配置保存到 config.properties 文件。
     * 注意: 此方法应在应用退出或关键配置变更后调用。
     */
    fun save() {
        configFile.outputStream().use { properties.store(it, "Vanish App Configuration") }
    }
    //endregion
}
