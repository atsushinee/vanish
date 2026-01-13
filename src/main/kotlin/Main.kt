import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import java.io.File
import java.util.*
import kotlin.system.exitProcess

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

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
fun main() = application {
    val virtualWidth = 150.dp
    val virtualHeight = 30.dp
    val initialScale = 0.8f

    // 从配置中加载窗口位置，如果不存在则使用默认值
    val initialX = AppConfig.getProperty("window.x", "0").toFloat()
    val initialY = AppConfig.getProperty("window.y", "0").toFloat()

    val windowState = rememberWindowState(
        size = DpSize(virtualWidth * initialScale, virtualHeight * initialScale),
        // 使用加载的或默认的位置
        position = WindowPosition(initialX.dp, initialY.dp)
    )
    var isHovered by remember { mutableStateOf(false) }
    val targetAlpha = if (isHovered) 0.6f else 0f
    val animatedAlpha by animateFloatAsState(targetValue = targetAlpha)
    var showMenu by remember { mutableStateOf(false) }

    val stockViewModel = remember { StockViewModel() }

    Window(
        // 在关闭请求时保存窗口位置并退出
        onCloseRequest = {
            // 保存窗口的 x 和 y 坐标
            AppConfig.setProperty("window.x", windowState.position.x.value.toString())
            AppConfig.setProperty("window.y", windowState.position.y.value.toString())
            // 保存配置
            AppConfig.save()
            // 退出应用程序
            exitApplication()
        },
        state = windowState,
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        title = "Vanish",
        resizable = false
    ) {
        LaunchedEffect(Unit) {
            window.background = java.awt.Color(0, 0, 0, 0)
        }

        WindowDraggableArea {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Enter -> isHovered = true
                                    PointerEventType.Exit -> isHovered = false
                                    PointerEventType.Press -> {
                                        if (event.buttons.isSecondaryPressed) {
                                            showMenu = true
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = animatedAlpha))
            ) {
                StockInfo(stockViewModel.stockData.value)

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(onClick = {
                        stockViewModel.refresh()
                        showMenu = false
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("刷新")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StockInfo(stockData: StockData?) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stockData?.price?.let { "%.2f".format(it) } ?: "--.--",
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = stockData?.changePercent?.let { "%.2f%%".format(it) } ?: "--.--%",
            color = when {
                stockData == null -> Color.White
                stockData.changePercent >= 0 -> Color(0xFFd81e06)
                else -> Color(0xFF1aad19)
            },
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )
//        Text(
//            text = stockData?.rise?.let { "%.2f%%".format(it) } ?: "--.--%",
//            color = when {
//                stockData == null -> Color.White
//                stockData.rise > 0.1 -> Color(0xFFd81e06)
//                stockData.rise < -0.1 -> Color(0xFF1aad19)
//                else -> Color.White
//            },
//            fontSize = 8.sp,
//            fontFamily = FontFamily.Monospace
//        )
        Text(
            text = stockData?.indexPercent?.let { "%.2f%%".format(it) } ?: "--.--%",
            color = when {
                stockData == null -> Color.White
                stockData.indexPercent >= 0 -> Color(0xFFd81e06)
                else -> Color(0xFF1aad19)
            },
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Preview
@Composable
fun PreviewStockInfo() {
    MaterialTheme {
        Box(Modifier.background(Color.Black).size(240.dp * 0.8f, 40.dp * 0.8f)) {
            StockInfo(
                stockData = StockData(price = 12.34, changePercent = 1.23, rise = 0.5, indexPercent = -0.25)
            )
        }
    }
}
