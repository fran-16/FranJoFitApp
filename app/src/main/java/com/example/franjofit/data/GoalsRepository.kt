package com.example.franjofit.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class DailyGoal(
    val baseGoal: Int = 2000,
    val consumed: Int = 0,
    val remaining: Int = baseGoal - consumed,
    val smpCurrent: Int? = null   // único valor SMP vigente
)

object GoalsRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun todayId(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun goalDoc() = db.collection("users")
        .document(auth.currentUser?.uid ?: "_")
        .collection("goals")
        .document(todayId())

    // ==========================
    // METAS KCAL
    // ==========================
    suspend fun getDailyGoalOrDefault(defaultGoal: Int): DailyGoal {
        val snap = goalDoc().get().await()
        return if (snap.exists()) {
            val base = (snap.getLong("baseGoal") ?: defaultGoal.toLong()).toInt()
            val consumed = (snap.getLong("consumed") ?: 0L).toInt()
            val remaining = (snap.getLong("remaining")
                ?: (base - consumed).coerceAtLeast(0).toLong()).toInt()

            val smpCurrent = snap.getLong("smpCurrent")?.toInt()

            DailyGoal(
                baseGoal = base,
                consumed = consumed,
                remaining = remaining,
                smpCurrent = smpCurrent
            )
        } else {
            val dg = DailyGoal(baseGoal = defaultGoal, consumed = 0)
            val map = mutableMapOf<String, Any>(
                "baseGoal" to dg.baseGoal,
                "consumed" to dg.consumed,
                "remaining" to dg.remaining
            )
            goalDoc().set(map, SetOptions.merge()).await()
            dg
        }
    }

    suspend fun setBaseGoal(newGoal: Int) {
        val current = getDailyGoalOrDefault(newGoal)
        val updatedRemaining = (newGoal - current.consumed).coerceAtLeast(0)
        val update = mapOf(
            "baseGoal" to newGoal,
            "consumed" to current.consumed,
            "remaining" to updatedRemaining
        )
        goalDoc().set(update, SetOptions.merge()).await()
    }

    suspend fun setTotals(baseGoal: Int, consumed: Int) {
        val remaining = (baseGoal - consumed).coerceAtLeast(0)
        val update = mapOf(
            "baseGoal" to baseGoal,
            "consumed" to consumed,
            "remaining" to remaining
        )
        goalDoc().set(update, SetOptions.merge()).await()
    }

    // ==========================
    // SMP ACTUAL
    // ==========================

    /**
     * Devuelve el SMP “vigente” para hoy:
     * - smpCurrent si existe
     * - si no, [defaultScore] (normalmente 100).
     */
    suspend fun getTodaySmpCurrentOrDefault(defaultScore: Int = 100): Int {
        val snap = goalDoc().get().await()
        if (!snap.exists()) return defaultScore
        val smpCurrent = snap.getLong("smpCurrent")?.toInt()
        return smpCurrent ?: defaultScore
    }

    suspend fun getTodaySmpCurrent(defaultScore: Int = 100): Int {
        return getTodaySmpCurrentOrDefault(defaultScore)
    }

    /**
     * Setea el SMP actual de hoy.
     */
    suspend fun updateTodaySmpCurrent(newScore: Int) {
        val update = mapOf("smpCurrent" to newScore)
        goalDoc().set(update, SetOptions.merge()).await()
    }

    // ==========================
    // SMP calculado desde comidas
    // (para usar al GUARDAR una comida)
    // ==========================

    private const val DEFAULT_IG = 55.0
    private const val MAX_FIBER_BONUS = 10.0
    private const val MAX_PROT_BONUS  = 10.0
    private const val GL_COEF = 1.5
    private const val IG_COEF = 0.5
    private const val KCAL_SOFT_CAP = 650.0
    private const val KCAL_PEN_STEP = 50.0
    private const val KCAL_PEN_PER_STEP = 1.0
    private const val KCAL_PEN_MAX = 12.0

    private data class MealMetrics(
        val igPlate: Double,
        val glTotal: Double,
        val carbs: Double,
        val protein: Double,
        val fiber: Double,
        val kcal: Double
    )

    private data class MealSmp(
        val score: Int
    )

    private fun Map<String, Any>.num(key: String): Double {
        val v = this[key]
        return when (v) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    private fun computeMealMetrics(items: List<Map<String, Any>>): MealMetrics {
        var carbs = 0.0
        var prot  = 0.0
        var fiber = 0.0
        var kcal  = 0.0
        var glTot = 0.0
        var igWeightedNum = 0.0

        items.forEach { m ->
            val ig = m.num("ig").takeIf { it > 0 } ?: DEFAULT_IG
            val gl = m.num("gl")

            val c  = m.num("carbs_g")
            val p  = m.num("protein_g")
            val f  = m.num("fiber_g")
            val k  = m.num("kcal")

            carbs += c
            prot  += p
            fiber += f
            kcal  += k
            glTot += gl
            igWeightedNum += ig * c
        }

        val igPlate = if (carbs > 0.0) igWeightedNum / carbs else DEFAULT_IG
        return MealMetrics(igPlate, glTot, carbs, prot, fiber, kcal)
    }

    private fun smpForMeal(metrics: MealMetrics): MealSmp {

        val penGL = metrics.glTotal * GL_COEF
        val penIG = metrics.igPlate * IG_COEF

        val fiberBonus = (metrics.fiber.coerceAtMost(10.0) / 10.0) * MAX_FIBER_BONUS
        val protBonus  = (metrics.protein.coerceAtMost(25.0) / 25.0) * MAX_PROT_BONUS

        val kcalPen = if (metrics.kcal > KCAL_SOFT_CAP) {
            val steps = ((metrics.kcal - KCAL_SOFT_CAP) / KCAL_PEN_STEP)
            (steps * KCAL_PEN_PER_STEP).coerceAtMost(KCAL_PEN_MAX)
        } else 0.0

        var score = 100.0 - penGL - penIG + fiberBonus + protBonus - kcalPen
        score = score.coerceIn(0.0, 100.0)
        return MealSmp(score.toInt())
    }

    private fun calculateDailySmpPredicted(
        meals: Map<String, List<Map<String, Any>>>
    ): Int {
        if (meals.isEmpty()) return 100
        var totalKcal = 0.0
        var weighted = 0.0
        meals.values.forEach { list ->
            if (list.isEmpty()) return@forEach
            val metrics = computeMealMetrics(list)
            val smp = smpForMeal(metrics)
            weighted += smp.score * metrics.kcal
            totalKcal += metrics.kcal
        }
        return if (totalKcal > 0) (weighted / totalKcal).toInt() else 100
    }

    /**
     * La idea es llamar a esto DESPUÉS de guardar una comida:
     * - Lee las comidas de hoy
     * - Calcula un SMP predicho del día
     * - Lo guarda en `smpCurrent`
     * - Devuelve el valor por si lo quieres mostrar en UI.
     */
    suspend fun recalcTodaySmpFromMeals(): Int {
        val meals = FoodRepository.getMealsForToday()
        val newScore = calculateDailySmpPredicted(meals)
        updateTodaySmpCurrent(newScore)
        return newScore
    }
}
