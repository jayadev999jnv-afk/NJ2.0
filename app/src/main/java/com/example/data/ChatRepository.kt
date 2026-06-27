package com.example.data

import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao, private val geminiApiService: GeminiApiService) {
    val allMessages: Flow<List<ChatMessage>> = chatDao.getAllMessages()

    suspend fun sendMessage(text: String, apiKey: String, systemPrompt: String): String {
        // Save user message
        chatDao.insertMessage(ChatMessage(role = "user", text = text))

        // Construct Gemini request
        val request = GenerateContentRequest(
            contents = listOf(Content(role = "user", parts = listOf(Part(text = text)))),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        return try {
            val response = geminiApiService.generateContent(apiKey, request)
            val reply = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "I am sorry, Sir. I couldn't process that."
            chatDao.insertMessage(ChatMessage(role = "model", text = reply))
            reply
        } catch (e: Exception) {
            val errorMsg = "Error: ${e.message}"
            chatDao.insertMessage(ChatMessage(role = "model", text = "I'm having some trouble connecting, Sir. Please check your internet."))
            errorMsg
        }
    }

    suspend fun clearHistory() {
        chatDao.clearHistory()
    }
}
