package com.example.franjofit.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

object FoodRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun addMealItem(mealType: String, name: String, kcal: Int, portion: String) {
        val uid = auth.currentUser?.uid ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val mealData = mapOf(
            "name" to name,
            "kcal" to kcal,
            "portion" to portion
        )

        val mealDoc = db.collection("users")
            .document(uid)
            .collection("meals")
            .document(today)

        val snapshot = mealDoc.get().await()
        val existingMeals = snapshot.get(mealType) as? List<Map<String, Any>> ?: emptyList()
        val updatedMeals = existingMeals + mealData

        mealDoc.set(mapOf(mealType to updatedMeals), SetOptions.merge()).await()
    }

    suspend fun getMealsForToday(): Map<String, List<Map<String, Any>>> {
        val uid = auth.currentUser?.uid ?: return emptyMap()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val doc = db.collection("users")
            .document(uid)
            .collection("meals")
            .document(today)
            .get()
            .await()

        return doc.data?.mapValues { (_, value) ->
            value as? List<Map<String, Any>> ?: emptyList()
        } ?: emptyMap()
    }
}
