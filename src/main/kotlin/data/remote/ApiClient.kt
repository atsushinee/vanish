package data.remote

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.serialization.json.Json

/**
 * 提供整个应用共享的网络请求客户端和JSON解析器实例。
 * 目的：避免重复创建 HttpClient 和 Json 对象，节省资源，并为所有API请求提供统一的配置入口。
 */

// 设为 internal，使其在模块内部可见，但对外部模块隐藏。
internal val apiClient = HttpClient(CIO)

// 配置为忽略JSON中存在但数据类中没有的字段，增加解析的灵活性。
internal val jsonParser = Json { ignoreUnknownKeys = true }
