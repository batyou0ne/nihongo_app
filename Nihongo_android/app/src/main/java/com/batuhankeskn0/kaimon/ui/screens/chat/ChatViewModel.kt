package com.batuhankeskn0.kaimon.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batuhankeskn0.kaimon.data.model.ChatMessage
import com.batuhankeskn0.kaimon.data.service.AIService
import com.batuhankeskn0.kaimon.data.service.FeatureGate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Bot mesajının Japonca gövdesi ve (varsa) parantez içindeki Türkçe çevirisi.
 */
data class ParsedBotMessage(
    val japanese: String,
    val translation: String?
)

/**
 * AI Dil Partneri ekran durumu.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            text = ChatViewModel.WELCOME_MESSAGE_TEXT,
            isUser = false
        )
    ),
    val isTyping: Boolean = false,
    val lastFailedText: String? = null
)

/**
 * AI Dil Partneri Sohbet ViewModel'i (`ViewModels/ChatViewModel.swift` karşılığı).
 */
@HiltViewModel
class ChatViewModel(
    private val aiService: AIService = AIService.shared,
    private val featureGate: FeatureGate = FeatureGate.shared,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    @Inject
    constructor(
        aiService: AIService,
        featureGate: FeatureGate
    ) : this(
        aiService = aiService,
        featureGate = featureGate,
        dispatcher = Dispatchers.Main
    )

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val messages: List<ChatMessage>
        get() = _uiState.value.messages

    val isTyping: Boolean
        get() = _uiState.value.isTyping

    val lastFailedText: String?
        get() = _uiState.value.lastFailedText

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val userMsg = ChatMessage(text = trimmed, isUser = true)
        _uiState.update { state ->
            state.copy(messages = state.messages + userMsg)
        }

        executeAIMessage(userText = trimmed)
    }

    fun retryLastMessage() {
        val failedText = _uiState.value.lastFailedText ?: return
        _uiState.update { state ->
            val cleaned = if (state.messages.lastOrNull()?.isError == true) {
                state.messages.dropLast(1)
            } else {
                state.messages
            }
            state.copy(messages = cleaned)
        }
        executeAIMessage(userText = failedText)
    }

    private fun executeAIMessage(userText: String) {
        _uiState.update { state ->
            state.copy(
                lastFailedText = null,
                isTyping = true
            )
        }

        viewModelScope.launch(dispatcher) {
            try {
                val aiResponse = aiService.sendMessage(userText)
                featureGate.recordAIMessage()
                val botMessage = ChatMessage(text = aiResponse, isUser = false)
                _uiState.update { state ->
                    state.copy(
                        isTyping = false,
                        lastFailedText = null,
                        messages = state.messages + botMessage
                    )
                }
            } catch (error: Throwable) {
                val errorText = error.message ?: "Sunucuya ulaşılamadı. Lütfen tekrar deneyin."
                val errorMessage = ChatMessage(
                    text = errorText,
                    isUser = false,
                    isError = true
                )
                _uiState.update { state ->
                    state.copy(
                        isTyping = false,
                        lastFailedText = userText,
                        messages = state.messages + errorMessage
                    )
                }
            }
        }
    }

    fun clearMessages() {
        aiService.resetConversation()
        _uiState.value = ChatUiState(
            messages = listOf(
                ChatMessage(
                    text = WELCOME_MESSAGE_TEXT,
                    isUser = false
                )
            ),
            isTyping = false,
            lastFailedText = null
        )
    }

    companion object {
        const val WELCOME_MESSAGE_TEXT =
            "こんにちは！今日はどんな練習をしましょうか？ (Merhaba! Bugün ne tür bir pratik yapalım?)"

        private val BOT_MESSAGE_REGEX =
            Regex("^([\\s\\S]*?)[\\s\\n]*\\(([^()]+)\\)[\\s\\n]*$")

        /**
         * iOS `BotBubble.parseMessage` ile birebir aynı şekilde Japonca metni ve
         * sonda parantez içindeki Türkçe çeviriyi ayrıştırır.
         */
        fun parseBotMessage(text: String): ParsedBotMessage {
            val match = BOT_MESSAGE_REGEX.matchEntire(text)
            if (match != null && match.groupValues.size >= 3) {
                val jp = match.groupValues[1].trim()
                val tr = match.groupValues[2].trim()
                if (jp.isNotEmpty() && tr.isNotEmpty()) {
                    return ParsedBotMessage(japanese = jp, translation = tr)
                }
            }
            return ParsedBotMessage(japanese = text, translation = null)
        }
    }
}
