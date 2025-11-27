package com.example.franjofit.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.franjofit.data.AiRepository
import com.example.franjofit.data.ChatMessageDto
import kotlinx.coroutines.launch

data class BotMessage(
    val id: Long = System.currentTimeMillis(),
    val fromUser: Boolean,
    val text: String
)

data class SmpBotUiState(
    val messages: List<BotMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class SmpBotViewModel : ViewModel() {

    private val _ui = androidx.compose.runtime.mutableStateOf(
        SmpBotUiState(
            messages = listOf(
                BotMessage(
                    fromUser = false,
                    text = "Hola, soy el SMP Bot MetaFranjo 🤖. Puedo ayudarte con tus comidas y tu SMP del día. ¿Qué te gustaría saber?"
                )
            )
        )
    )
    val ui = _ui

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || ui.value.isLoading) return

        //Añadimos mensaje del usuario
        val newUserMsg = BotMessage(fromUser = true, text = trimmed)
        val currentList = ui.value.messages + newUserMsg

        _ui.value = ui.value.copy(
            messages = currentList,
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                //Convertir historial a DTOs para el backend
                val dtoHistory = currentList.map {
                    ChatMessageDto(
                        role = if (it.fromUser) "user" else "assistant",
                        content = it.text
                    )
                }

                //Llama al backend
                val reply = AiRepository.chatWithBot(dtoHistory)

                //Añade respuesta del bot
                val botMsg = BotMessage(fromUser = false, text = reply)

                _ui.value = ui.value.copy(
                    messages = ui.value.messages + botMsg,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _ui.value = ui.value.copy(
                    isLoading = false,
                    error = "Error al hablar con el bot. Intenta otra vez."
                )
            }
        }
    }
}
