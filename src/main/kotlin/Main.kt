import androidx.compose.ui.window.application

/**
 * 应用程序的入口点。
 * 使用 `application` 构建器来启动整个 Compose for Desktop 应用。
 */
fun main() = application {
    // 调用重构后的主 Composable `App`，并传入退出应用的回调。
    // `application` 上下文提供了 `exitApplication` 函数，
    // 我们可以将其作为 lambda 传递给 App Composable，
    // 从而将应用生命周期管理与UI逻辑解耦。
    App(onExit = ::exitApplication)
}
