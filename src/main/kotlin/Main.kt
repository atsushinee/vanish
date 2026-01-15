import androidx.compose.ui.window.application

/**
 * 应用程序的入口点。
 * 使用 `application` 构建器来启动整个 Compose for Desktop 应用。
 */
fun main() = application(
    // 核心修正：明确设置 exitProcessOnExit 为 false。
    // 目的：从根本上解决因主对话框隐藏而导致整个应用退出的问题。
    // 原理：Compose Desktop 的 application 块默认会在最后一个窗口关闭时退出应用。
    //      通过将此参数设为 false，我们将应用的生命周期完全交由我们自己的逻辑（即托盘菜单的退出按钮）来控制。
    //      这样，即便是所有 Dialog 都被隐藏，只要托盘图标还在，应用进程就会继续运行。
    exitProcessOnExit = false
) {
    // 调用主 Composable `App`，并传入退出应用的回调。
    // `application` 上下文提供了 `exitApplication` 函数，
    // 我们可以将其作为 lambda 传递给 App Composable，
    // 从而将应用生命周期管理与UI逻辑解耦。
    App(onExit = ::exitApplication)
}
