import java.io.File
import java.util.*

// AppConfig 单例对象，用于管理应用程序的配置
object AppConfig {
    // 配置文件路径
    private val configFile = File("config.properties")
    // Properties 对象，用于存储键值对
    private val properties = Properties()

    // 初始化块，在对象创建时执行
    init {
        // 如果配置文件存在，则加载它
        if (configFile.exists()) {
            // 使用文件输入流读取配置文件
            configFile.inputStream().use { properties.load(it) }
        }
    }

    // 获取属性值，如果不存在则返回默认值
    fun getProperty(key: String, defaultValue: String): String {
        return properties.getProperty(key, defaultValue)
    }

    // 设置属性值
    fun setProperty(key: String, value: String) {
        properties.setProperty(key, value)
    }

    // 保存配置到文件
    fun save() {
        // 使用文件输出流将配置写入文件
        configFile.outputStream().use { properties.store(it, null) }
    }
}
