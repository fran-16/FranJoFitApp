package com.example.franjofit.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import com.example.franjofit.ui.components.GradientBackground
import com.example.franjofit.ui.components.OutlineInput
import com.example.franjofit.ui.components.PrimaryButton
import com.example.franjofit.ui.components.Spacing
import com.example.franjofit.ui.theme.White
import com.example.franjofit.ui.theme.White80
import com.google.firebase.auth.FirebaseAuth
import com.example.franjofit.ui.components.GoogleButton

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

    GradientBackground {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Iniciar sesión",
                color = White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(Spacing.xl))

            OutlineInput(
                value = email,
                onValueChange = { new -> email = new },
                placeholder = "Dirección de email",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.md))

            OutlineInput(
                value = password,
                onValueChange = { new -> password = new },
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
                                    // Change this to navigate to DashboardScreen instead of ProfileScreen
                                    onLoggedIn() // Make sure this navigates to DashboardScreen
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
                onClick = { /* TODO: tu flujo de sign-in con Google */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

        }
    }
}
