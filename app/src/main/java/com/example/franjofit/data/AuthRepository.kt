package com.example.franjofit.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = Firebase.firestore // Firestore


    fun registerWithEmail(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {

                        saveUserEmail(user.uid, email, onSuccess, onError)
                    } else {
                        onError(Exception("Error al obtener el usuario después del registro"))
                    }
                } else {
                    onError(task.exception ?: Exception("Error desconocido al registrar el usuario"))
                }
            }
    }

    private fun saveUserEmail(
        userId: String,
        email: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {

        db.collection("users").document(userId).set(mapOf("email" to email))
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }
}
