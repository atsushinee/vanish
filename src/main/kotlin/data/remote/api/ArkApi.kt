package data.remote.api

import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole
import com.volcengine.ark.runtime.service.ArkService
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import java.util.*
import java.util.concurrent.TimeUnit

class ArkApi {
    companion object {
        // 从环境变量中获取您的 API Key
        private val apiKey = "c61656e6-175f-4486-a85a-7b1fa0df8ecf"
        
        // 此为默认路径，您可根据业务所在地域进行配置
        private const val baseUrl = "https://ark.cn-beijing.volces.com/api/v3"
        
        private val connectionPool = ConnectionPool(5, 1, TimeUnit.MINUTES)
        private val dispatcher = Dispatcher()
        
        private val service = ArkService.builder()
            .dispatcher(dispatcher)
            .connectionPool(connectionPool)
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .build()

        @JvmStatic
        fun main(args: Array<String>) {
            println("ARK API 聊天机器人启动成功！")
            println("请输入您的问题，输入 'quit' 或 'exit' 退出对话。")
            
            val scanner = Scanner(System.`in`)
            val conversationHistory = mutableListOf<ChatMessage>()
            
            // 添加系统消息
            val systemMessage = ChatMessage.builder()
                .role(ChatMessageRole.SYSTEM)
                .content("你是一个有用的AI助手，专注于提供准确和有用的信息。")
                .build()
            conversationHistory.add(systemMessage)
            
            while (true) {
                print("\n用户: ")
                val userInput = scanner.nextLine().trim()
                
                when {
                    userInput.lowercase() == "quit" || userInput.lowercase() == "exit" -> {
                        println("再见！")
                        break
                    }
                    userInput.isEmpty() -> {
                        println("请输入有效的问题。")
                        continue
                    }
                    else -> {
                        // 添加用户消息到历史记录
                        val userMessage = ChatMessage.builder()
                            .role(ChatMessageRole.USER)
                            .content(userInput)
                            .build()
                        conversationHistory.add(userMessage)
                        
                        try {
                            // 发送请求并获取响应
                            val response = getChatResponse(conversationHistory)
                            
                            // 添加AI响应到历史记录
                            val aiMessage = ChatMessage.builder()
                                .role(ChatMessageRole.ASSISTANT)
                                .content(response)
                                .build()
                            conversationHistory.add(aiMessage)
                            
                            println("AI助手: $response")
                        } catch (e: Exception) {
                            println("发生错误: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            }
            
            service.shutdownExecutor()
        }

        private fun getChatResponse(messages: MutableList<ChatMessage>): String {
            val chatCompletionRequest = ChatCompletionRequest.builder()
                // 您需要替换为您自己的模型ID
                .model("deepseek-v3-2-251201")
                .messages(messages)
                .maxTokens(32768)
                .temperature(0.7)
                .topP(0.9)
                .build()

            val response = service.createChatCompletion(chatCompletionRequest)
            return response.choices.firstOrNull()?.message?.content.toString()
        }
    }
}
