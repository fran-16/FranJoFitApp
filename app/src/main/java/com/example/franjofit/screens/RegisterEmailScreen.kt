package com.example.franjofit.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.franjofit.ui.components.OutlineInput
import com.example.franjofit.ui.components.PrimaryButton
import com.example.franjofit.ui.theme.White
import com.example.franjofit.ui.components.Spacing
import com.example.franjofit.data.UserProfile
import com.example.franjofit.ui.components.GradientBackground
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RegisterEmailScreen(
    displayName: String,
    navController: NavController
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance() // Para registrar con Firebase Auth
    val db = FirebaseFirestore.getInstance() // Para guardar el perfil en Firestore

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Registra tu correo y contraseña para $displayName",
                color = White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(Spacing.xl))

            OutlineInput(
                value = email,
                onValueChange = { email = it },
                placeholder = "Correo electrónico",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Spacing.md))


            OutlineInput(
                value = password,
                onValueChange = { password = it },
                placeholder = "Contraseña",
                isPassword = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Spacing.md))


            OutlineInput(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = "Confirmar contraseña",
                isPassword = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Spacing.xl))

            PrimaryButton(
                text = "Registrar",
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty() && password == confirmPassword) {
                        // Crear usuario en Firebase Authentication
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // Crear el perfil del usuario en Firestore
                                    val user = auth.currentUser
                                    val userProfile = UserProfile(
                                        uid = user?.uid ?: "",
                                        email = email,
                                        displayName = displayName,
                                        birthDate = "", // Se puede dejar vacío o añadir
                                        sex = "", // Se puede dejar vacío
                                        heightCm = null, // Se puede dejar vacío
                                        currentWeightKg = null // Se puede dejar vacío
                                    )
                                    db.collection("users")
                                        .document(user?.uid ?: "")
                                        .set(userProfile)
                                        .addOnSuccessListener {
                                            // After successful registration, navigate to the Dashboard screen
                                            navController.navigate(Routes.Dashboard) {
                                                popUpTo(Routes.RegisterEmail) { inclusive = true }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("RegisterEmail", "Error al guardar el perfil: ${e.message}")
                                        }
                                } else {
                                    Log.e("RegisterEmail", "Error al crear el usuario: ${task.exception?.message}")
                                }
                            }
                    } else {
                        Log.w("RegisterEmail", "Las contraseñas no coinciden o los campos están vacíos")
                    }
                }
            )
        }
    }
}
