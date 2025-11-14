package com.example.franjofit.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.franjofit.ui.components.*
import com.example.franjofit.ui.theme.White

@Composable
fun RegisterGoalsScreen(onContinue: () -> Unit) {
    var g1 by remember { mutableStateOf(false) }
    var g2 by remember { mutableStateOf(false) }
    var g3 by remember { mutableStateOf(false) }
    var g4 by remember { mutableStateOf(false) }
    var g5 by remember { mutableStateOf(false) }

    GradientBackground {
        Column(
            Modifier.fillMaxSize().padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(Spacing.xl))
            Text(
                "¿Cuáles son tus objetivos principales, así podremos adaptar tu plan.",
                color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Spacing.xl))

            CheckItem("Prevenir o revertir prediabetes", g1) { g1 = it }
            CheckItem("Mejorar sensibilidad a la insulina", g2) { g2 = it }
            CheckItem("Reducir grasa visceral (abdomen)", g3) { g3 = it }
            CheckItem("Estabilizar glucosa post-comida", g4) { g4 = it }
            CheckItem("Aumentar energía y reducir fatiga", g5) { g5 = it }

            Spacer(Modifier.height(Spacing.xl))
            PrimaryButton("Continuar", onContinue)
        }
    }
}
