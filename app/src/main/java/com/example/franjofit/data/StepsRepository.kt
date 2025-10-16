package com.example.franjofit.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StepsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun upsertToday(
        steps: Int,
        goal: Int = 10000,
        onComplete: (Exception?) -> Unit = {}
    ) {
        val uid = auth.currentUser?.uid
            ?: return onComplete(IllegalStateException("No user"))

        val today = sdf.format(Date())
        val data = mapOf(
            "date" to today,
            "steps" to steps,
            "goal" to goal,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("users").document(uid)
            .collection("dailyMetrics").document(today)
            .set(data, SetOptions.merge())
            .addOnCompleteListener { onComplete(it.exception) }
    }
}
