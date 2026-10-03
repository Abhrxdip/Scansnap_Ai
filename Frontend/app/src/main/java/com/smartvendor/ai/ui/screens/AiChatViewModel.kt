package com.smartvendor.ai.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvendor.ai.network.ApiClient
import com.smartvendor.ai.network.models.AiChatRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean
)

data class AiChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            text = "👋 Hello! I'm your ScanSnap Store Assistant.\n\nYou can ask me:\n• \"Is Maggi available?\"\n• \"What is the price of Amul milk?\"\n• \"Where else can I find Maggi?\"",
            isUser = false
        )
    ),
    val isLoading: Boolean = false,
    val error: String? = null
)

class AiChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()
    
    private var lastSentMessage: String? = null

    fun sendMessage(text: String) {
        val userMessage = ChatMessage(text = text, isUser = true)
        _uiState.update { it.copy(
            messages = it.messages + userMessage,
            isLoading = true,
            error = null
        ) }
        lastSentMessage = text
        fetchResponse(text)
    }
    
    fun retryLastMessage() {
        lastSentMessage?.let { text ->
            _uiState.update { it.copy(isLoading = true, error = null) }
            fetchResponse(text)
        }
    }

    private fun fetchResponse(message: String) {
        viewModelScope.launch {
            try {
                val apiResponse = ApiClient.apiService.sendAiChat(AiChatRequest(message = message))
                if (apiResponse.isSuccessful) {
                    val body = apiResponse.body()
                    if (body != null && body.success) {
                        val aiMessage = ChatMessage(text = body.response ?: "No response received", isUser = false)
                        _uiState.update { it.copy(
                            messages = it.messages + aiMessage,
                            isLoading = false
                        ) }
                    } else {
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = body?.error ?: "Failed to get AI response"
                        ) }
                    }
                } else {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Server error (${apiResponse.code()}): ${apiResponse.message()}"
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Network error connecting to backend"
                ) }
            }
        }
    }
}

