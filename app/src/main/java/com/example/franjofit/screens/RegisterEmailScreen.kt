package com.example.franjofit.screens

import android.util.Log
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.franjofit.ui.components.GoogleButton
import com.example.franjofit.ui.theme.White
import com.example.franjofit.ui.components.Spacing
import com.example.franjofit.data.UserProfile
import com.example.franjofit.ui.components.GradientBackground
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun RegisterEmailScreen(
    displayName: String,
    navController: NavController
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()


    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as Activity
    val clientId = remember { context.getString(com.example.franjofit.R.string.default_web_client_id) }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .build()
    }
    val googleClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(Exception::class.java)
                ?: throw Exception("No se pudo obtener la cuenta de Google")

            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    if (user == null) {
                        error = "Auth succeeded but user null"
                        return@addOnSuccessListener
                    }

                    val uid = user.uid
                    val usersRef = db.collection("users").document(uid)
                    usersRef.get()
                        .addOnSuccessListener { doc ->
                            if (!doc.exists()) {
                                // crear perfil con displayName de pasos previos + datos de Google
                                val profile = UserProfile(
                                    uid = uid,
                                    email = user.email ?: "",
                                    displayName = if (displayName.isNotBlank()) displayName else (user.displayName ?: ""),
                                    birthDate = "",
                                    sex = "",
                                    heightCm = null,
                                    currentWeightKg = null
                                )
                                usersRef.set(profile)
                                    .addOnSuccessListener { goToDashboard(navController) }
                                    .addOnFailureListener { e ->
                                        error = e.message
                                        Log.e("RegisterGoogle", "Error creando perfil", e)
                                    }
                            } else {
                                // ya existe => continuar
                                goToDashboard(navController)
                            }
                        }
                        .addOnFailureListener { e ->
                            error = e.message
                            Log.e("RegisterGoogle", "Error leyendo perfil", e)
                        }
                }
                .addOnFailureListener { e ->
                    error = e.message ?: "No se pudo registrar con Google"
                    Log.e("RegisterGoogle", "signInWithCredential failed", e)
                }
        } catch (e: Exception) {
            error = e.message ?: "Error al registrar con Google"
            Log.e("RegisterGoogle", "Google launcher error", e)
        }
    }

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
                    error = null
                    if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                        error = "Completa todos los campos"
                        return@PrimaryButton
                    }
                    if (password != confirmPassword) {
                        error = "Las contraseñas no coinciden"
                        return@PrimaryButton
                    }

                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                val profile = UserProfile(
                                    uid = user?.uid ?: "",
                                    email = email,
                                    displayName = displayName,
                                    birthDate = "",
                                    sex = "",
                                    heightCm = null,
                                    currentWeightKg = null
                                )
                                db.collection("users")
                                    .document(user?.uid ?: "")
                                    .set(profile)
                                    .addOnSuccessListener { goToDashboard(navController) }
                                    .addOnFailureListener { e ->
                                        error = e.message
                                        Log.e("RegisterEmail", "Error guardando perfil", e)
                                    }
                            } else {
                                error = task.exception?.message ?: "Error desconocido"
                                Log.e("RegisterEmail", "Error creando usuario", task.exception)
                            }
                        }
                }
            )

            Spacer(Modifier.height(Spacing.lg))

            GoogleButton(
                text = "Registrar con Google",
                onClick = {
                    error = null
                    // para forzar selector de cuenta
                    googleClient.signOut().addOnCompleteListener {
                        googleLauncher.launch(googleClient.signInIntent)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Spacer(Modifier.height(Spacing.md))
                Text(text = error ?: "", color = White)
            }
        }
    }
}

private fun goToDashboard(navController: NavController) {
    navController.navigate(Routes.Dashboard) {
        popUpTo(Routes.RegisterEmail) { inclusive = true }
    }
}
