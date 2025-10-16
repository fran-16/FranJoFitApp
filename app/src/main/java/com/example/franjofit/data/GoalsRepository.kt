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
    val remaining: Int = baseGoal - consumed
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

    suspend fun getDailyGoalOrDefault(defaultGoal: Int): DailyGoal {
        val snap = goalDoc().get().await()
        return if (snap.exists()) {
            val base = (snap.getLong("baseGoal") ?: defaultGoal.toLong()).toInt()
            val consumed = (snap.getLong("consumed") ?: 0L).toInt()
            DailyGoal(base, consumed, (base - consumed).coerceAtLeast(0))
        } else {
            // si no existe, créalo con el default
            val dg = DailyGoal(baseGoal = defaultGoal, consumed = 0)
            goalDoc().set(
                mapOf(
                    "baseGoal" to dg.baseGoal,
                    "consumed" to dg.consumed,
                    "remaining" to dg.remaining
                ),
                SetOptions.merge()
            ).await()
            dg
        }
    }

    suspend fun setBaseGoal(newGoal: Int) {

        val current = getDailyGoalOrDefault(newGoal)
        val updated = current.copy(baseGoal = newGoal,
            remaining = (newGoal - current.consumed).coerceAtLeast(0))
        goalDoc().set(
            mapOf(
                "baseGoal" to updated.baseGoal,
                "consumed" to updated.consumed,
                "remaining" to updated.remaining
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun setTotals(baseGoal: Int, consumed: Int) {
        val remaining = (baseGoal - consumed).coerceAtLeast(0)
        goalDoc().set(
            mapOf(
                "baseGoal" to baseGoal,
                "consumed" to consumed,
                "remaining" to remaining
            ),
            SetOptions.merge()
        ).await()
    }
}
