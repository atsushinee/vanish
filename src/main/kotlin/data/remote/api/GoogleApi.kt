package data.remote.api

import com.google.genai.Client
import com.google.genai.ResponseStream
import com.google.genai.types.*
import config.AppConfig
import java.util.*

fun main() {
    // 创建客户端（保持原有配置）
    val client = Client.builder()
        .apiKey(AppConfig.geminiApiKey)
        .clientOptions(
            ClientOptions.builder()
                .proxyOptions(
                    ProxyOptions.builder()
                        .host("127.0.0.1")
                        .port(10808)
                        .build()
                )
                .build()
        )
        .httpOptions(
            HttpOptions.builder()
                .apiVersion("v1")
                .build()
        )
        .build()

    println("=== Gemini 对话助手 (流式输出) ===")
    println("输入 'quit' 或 'exit' 退出对话\n")

    val scanner = Scanner(System.`in`)
    val conversationHistory = mutableListOf<Content>() // 存储对话历史

    while (true) {
        print("你: ")
        val userInput = scanner.nextLine().trim()

        if (userInput.lowercase() in listOf("quit", "exit", "退出")) {
            println("再见！")
            break
        }

        if (userInput.isEmpty()) continue

        // 将用户输入添加到对话历史
        val userContent = Content.fromParts(Part.fromText(userInput))
        conversationHistory.add(userContent)

        print("Gemini: ")

        val config = GenerateContentConfig.builder()
            .build()

        // 使用流式接口获取响应
        val responseStream: ResponseStream<GenerateContentResponse> = client.models.generateContentStream(
            "gemini-2.5-flash-lite",
            userContent, // 当前输入
            config      // 包含历史的配置
        )

        var fullResponse = "" // 收集完整响应以添加到历史

        responseStream.use { stream ->
            for (chunk in stream) {
                val text = chunk.text()
                text?.let {
                    if (it.isNotEmpty()) {
                        print(text)           // 实时输出
                        fullResponse += text  // 累积完整文本
                    }
                }
            }
        }

        // 将模型回复也添加到对话历史
        if (fullResponse.isNotEmpty()) {
            val aiContent = Content.fromParts(Part.fromText(fullResponse))
            conversationHistory.add(aiContent)
        }

        println("\n") // 新行分隔
    }

    scanner.close()
}
