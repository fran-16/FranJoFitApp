package com.example.franjofit.data

import com.example.franjofit.data.FoodRepository
import com.example.franjofit.data.GoalsRepository
import com.example.franjofit.data.UserRepository

object AiRepository {

    suspend fun chatWithBot(
        historyMessages: List<ChatMessageDto>
    ): String {

        val mealsRaw = FoodRepository.getMealsForToday()
        val mealsDto = mealsRaw.toMealItemDtoList()

        val daily = GoalsRepository.getDailyGoalOrDefault(defaultGoal = 2200)
        val smpCurrent = daily.smpCurrent ?: GoalsRepository.getTodaySmpCurrentOrDefault(100)

        val summary = DaySummaryDto(
            baseGoal = daily.baseGoal,
            consumed = daily.consumed,
            remaining = daily.remaining,
            smpCurrent = smpCurrent
        )

        val profile = run {
            val u = UserRepository.getUserProfileOrNull()
            buildString {
                append("Nombre: ${u?.displayName ?: "Usuario"}; ")
                append("Sexo: ${u?.sex ?: "N/A"}; ")
                append("Altura: ${u?.heightCm ?: 0} cm; ")
                append("Peso actual: ${u?.currentWeightKg ?: 0f} kg; ")
                append("Edad: desconocida (no registrada en el sistema).")
            }
        }

        val request = ChatRequestDto(
            summary = summary,
            meals = mealsDto,
            profile = profile,
            messages = historyMessages
        )

        val response = AiService.api.chat(request)
        return response.reply
    }
}
