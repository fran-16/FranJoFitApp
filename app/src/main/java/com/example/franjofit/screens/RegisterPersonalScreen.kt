package com.example.franjofit.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun RegisterPersonalScreen(
    username: String,
    onNext: (UserProfile) -> Unit
) {
    var birth by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }

    GradientBackground {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Hola $username, queremos conocerte un poco mejor.",
                color = White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(Spacing.md))

            OutlineInput(
                value = birth,
                onValueChange = { birth = it },
                placeholder = "Fecha de nacimiento (ej. 16 abril 2002)",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.md))

            OutlineInput(
                value = sex,
                onValueChange = { sex = it },
                placeholder = "Sexo (ej. Mujer / Hombre)",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.md))

            OutlineInput(
                value = height,
                onValueChange = { height = it },
                placeholder = "Estatura (ej. 1.68)",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.md))

            OutlineInput(
                value = weight,
                onValueChange = { weight = it },
                placeholder = "Peso (ej. 58 kg)",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.weight(1f))

            PrimaryButton(
                text = "Continuar",
                onClick = {
                    val userProfile = UserProfile(
                        uid = username,
                        email = "",
                        displayName = username,
                        birthDate = birth,
                        sex = sex,
                        heightCm = height.toIntOrNull(),
                        currentWeightKg = weight.toFloatOrNull()
                    )
                    onNext(userProfile)
                }
            )
        }
    }
}
