package com.example.franjofit.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.franjofit.ui.components.OutlineInput
import com.example.franjofit.ui.components.PrimaryButton
import com.example.franjofit.ui.theme.White
import com.example.franjofit.ui.components.Spacing
import com.example.franjofit.data.UserProfile
import com.example.franjofit.ui.components.GradientBackground

@Composable

fun RegisterUsernameScreen(onNext: (String) -> Unit) {
    var username by remember { mutableStateOf("") }
    val context = LocalContext.current

    GradientBackground {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.xl, vertical = Spacing.xl)
        ) {
            Text(
                text = "¿Cómo te gustaría que te llamemos?",
                color = White,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(Spacing.lg))

            OutlineInput(
                value = username,
                onValueChange = { username = it },
                placeholder = "Introduce tu nombre de usuario",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.weight(1f))
            PrimaryButton(
                text = "Continuar",
                onClick = {
                    if (username.isNotBlank()) {
                        onNext(username.trim())
                    } else {
                        Toast.makeText(context, "El nombre de usuario no puede estar vacío", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}
