package com.example.franjofit.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.franjofit.ui.components.GradientBackground
import com.example.franjofit.ui.components.GoogleButton
import com.example.franjofit.ui.components.OutlineInput
import com.example.franjofit.ui.components.PrimaryButton
import com.example.franjofit.ui.components.Spacing
import com.example.franjofit.ui.theme.White
import com.example.franjofit.ui.theme.White80
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.example.franjofit.data.AuthRepository
import com.example.franjofit.R

@Composable
fun LoginScreen(
    onForgot: () -> Unit,
    onGoogle: () -> Unit,
    onLoggedIn: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val activity = context as Activity
    val repo = remember { AuthRepository }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleClient = remember { GoogleSignIn.getClient(activity, gso) }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential)
                .addOnSuccessListener {
                    val uid = auth.currentUser?.uid ?: ""
                    if (uid.isBlank()) {
                        errorMessage = "No se pudo obtener el usuario"
                        return@addOnSuccessListener
                    }
                    repo.isUserRegistered(
                        uid = uid,
                        onResult = { exists ->
                            if (exists) {
                                onLoggedIn()
                            } else {
                                try {
                                    googleClient.signOut()
                                } catch (_: Exception) {}
                                repo.signOut()
                                errorMessage = "Tu cuenta de Google no está registrada. Regístrate primero."
                            }
                        },
                        onError = { e ->
                            Log.e("LoginGoogle", "Error consultando Firestore", e)
                            errorMessage = "Error verificando registro: ${e.message}"
                        }
                    )
                }
                .addOnFailureListener { e ->
                    Log.e("LoginGoogle", "signInWithCredential falló", e)
                    errorMessage = e.message ?: "No se pudo iniciar sesión con Google"
                }
        } catch (e: ApiException) {
            errorMessage = "Inicio con Google cancelado o fallido (${e.statusCode})"
        } catch (e: Exception) {
            errorMessage = e.message ?: "Error al iniciar con Google"
        }
    }

    GradientBackground {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Text("Iniciar sesión", color = White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(Spacing.xl))

            OutlineInput(
                value = email,
                onValueChange = { email = it },
                placeholder = "Dirección de email",
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

            Spacer(Modifier.height(Spacing.xl))

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = White80,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Spacing.md))
            }

            PrimaryButton(
                text = "Iniciar sesión",
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    onLoggedIn()
                                } else {
                                    errorMessage = task.exception?.message ?: "Error desconocido"
                                }
                            }
                    } else {
                        errorMessage = "Por favor, ingrese ambos campos"
                    }
                }
            )

            Spacer(Modifier.height(Spacing.md))

            Text(
                text = "¿Olvidaste tu contraseña?",
                color = White80,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onForgot() }
            )

            Spacer(Modifier.height(Spacing.xl))

            GoogleButton(
                text = "Continuar con Google",
                onClick = {
                    errorMessage = ""
                    googleClient.signOut().addOnCompleteListener {
                        googleLauncher.launch(googleClient.signInIntent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
        }
    }
}
