package com.example.franjofit.data

data class UserProfile(
    var uid: String,
    var email: String,
    var displayName: String,
    var birthDate: String? = null,
    var heightCm: Int? = null,
    var currentWeightKg: Float? = null,
    var sex: String? = null,
    val createdAt: String = System.currentTimeMillis().toString()
)
