package com.example.franjofit.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

data class WeightEntry(
    val date: Long = 0L,
    val weight: Float = 0f
)

object UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()


    //Esta es la función para obtener el perfil del usuario
    suspend fun getUserProfileOrNull(): UserProfile? {
        //Se obtiene el uid del usuario logueado
        val uid = auth.currentUser?.uid ?: return null
        //Pedimos el documento users/uid
        val snap = db.collection("users").document(uid).get().await()
        if (!snap.exists()) return null

        return UserProfile(
            uid = uid,
            email = snap.getString("email") ?: auth.currentUser?.email.orEmpty(),
            displayName = snap.getString("displayName") ?: auth.currentUser?.displayName ?: "Usuario",
            birthDate = snap.getString("birthDate"),
            heightCm = (snap.getLong("heightCm") ?: 0L).takeIf { it != 0L }?.toInt(),
            currentWeightKg = snap.getDouble("currentWeightKg")?.toFloat(),
            sex = snap.getString("sex"),
            photoUrl = snap.getString("photoUrl"),
            createdAt = snap.getString("createdAt") ?: System.currentTimeMillis().toString()
        )
    }

    // ============================================================
    //  📌 SUBIR FOTO
    // ============================================================
    suspend fun uploadProfilePhoto(localUri: Uri): String {
        val uid = auth.currentUser?.uid ?: error("No hay usuario logueado")
        val ref = storage.reference.child("users/$uid/profile.jpg")
        ref.putFile(localUri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun savePhotoUrl(photoUrl: String, displayName: String? = null) {
        val uid = auth.currentUser?.uid ?: return

        val data = mutableMapOf<String, Any>("photoUrl" to photoUrl)
        if (!displayName.isNullOrBlank()) data["displayName"] = displayName
        db.collection("users").document(uid).set(data, SetOptions.merge()).await()

        val user = auth.currentUser
        if (user != null) {
            val updates = userProfileChangeRequest {
                photoUri = Uri.parse(photoUrl)
                if (!displayName.isNullOrBlank()) this.displayName = displayName
            }
            user.updateProfile(updates).await()
        }
    }

    // ============================================================
    //  📌 PESO — SISTEMA CORRECTO (ACTUAL + HISTÓRICO)
    // ============================================================

    /**
     * Guarda el peso actual en el perfil y crea un registro en el histórico.
     * Este es el MÉTODO OFICIAL para registrar peso.
     */
    suspend fun addWeightRecord(weightKg: Float) {
        val uid = auth.currentUser?.uid ?: return
        val ts = System.currentTimeMillis()

        // 1️⃣ Guardar peso actual
        db.collection("users")
            .document(uid)
            .set(mapOf("currentWeightKg" to weightKg), SetOptions.merge())
            .await()

        // 2️⃣ Guardar en histórico con ID = timestamp (más ordenado)
        val entry = WeightEntry(date = ts, weight = weightKg)

        db.collection("users")
            .document(uid)
            .collection("weights")
            .document(ts.toString())   // evita duplicados
            .set(entry)
            .await()
    }

    //Se obtiene el último peso registrado
    suspend fun getLatestWeight(): Float? {
        val uid = auth.currentUser?.uid ?: return null

        val snap = db.collection("users")
            .document(uid)
            .collection("weights")
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        val doc = snap.documents.firstOrNull() ?: return null
        return doc.getDouble("weight")?.toFloat()
    }

    /**
     * Obtiene el historial (últimos 90 días por defecto)
     */
    suspend fun getWeightHistory(limit90days: Boolean = true): List<WeightEntry> {
        val uid = auth.currentUser?.uid ?: return emptyList()

        val baseRef = db.collection("users")
            .document(uid)
            .collection("weights")

        val query =
            if (limit90days) {
                val minDate = System.currentTimeMillis() - 90L * 24L * 60L * 60L * 1000L
                baseRef.whereGreaterThan("date", minDate).orderBy("date")
            } else {
                baseRef.orderBy("date")
            }

        val snap = query.get().await()
        return snap.toObjects(WeightEntry::class.java)
    }
}
