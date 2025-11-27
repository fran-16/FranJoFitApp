package com.example.franjofit.data

data class MealItemDto(
    val mealType: String,
    val id: String,
    val name: String,
    val grams: Int,
    val ig: Int,
    val carbs_g: Double,
    val protein_g: Double,
    val fiber_g: Double,
    val kcal: Int,
    val gl: Double,
    val portion_text: String
)

//Resumen del día (goals)
data class DaySummaryDto(
    val baseGoal: Int,
    val consumed: Int,
    val remaining: Int,
    val smpCurrent: Int
)


data class SuggestionRequestDto(
    val summary: DaySummaryDto,
    val meals: List<MealItemDto>,
    val profile: String,
    val user_message: String? = null
)

data class SuggestionResponseDto(
    val suggestion: String
)


data class ChatMessageDto(
    val role: String,
    val content: String
)

data class ChatRequestDto(
    val summary: DaySummaryDto,
    val meals: List<MealItemDto>,
    val profile: String,
    val messages: List<ChatMessageDto>
)

data class ChatResponseDto(
    val reply: String
)

@Suppress("UNCHECKED_CAST")
fun Map<String, List<Map<String, Any>>>.toMealItemDtoList(): List<MealItemDto> {
    return flatMap { (mealType, items) ->
        items.map { raw ->
            MealItemDto(
                mealType = mealType,
                id = raw["id"] as? String ?: "",
                name = raw["name"] as? String ?: "",
                grams = (raw["grams"] as? Number ?: 0).toInt(),
                ig = (raw["ig"] as? Number ?: 0).toInt(),
                carbs_g = (raw["carbs_g"] as? Number ?: 0.0).toDouble(),
                protein_g = (raw["protein_g"] as? Number ?: 0.0).toDouble(),
                fiber_g = (raw["fiber_g"] as? Number ?: 0.0).toDouble(),
                kcal = (raw["kcal"] as? Number ?: 0).toInt(),
                gl = (raw["gl"] as? Number ?: 0.0).toDouble(),
                portion_text = raw["portion_text"] as? String ?: ""
            )
        }
    }
}
