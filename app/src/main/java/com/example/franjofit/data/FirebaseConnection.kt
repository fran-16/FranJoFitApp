package com.example.franjofit.data

import com.google.firebase.auth.FirebaseAuth

object FirebaseConnection {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }

    fun isFirebaseConnected(): Boolean {
        return auth != null
    }
}
