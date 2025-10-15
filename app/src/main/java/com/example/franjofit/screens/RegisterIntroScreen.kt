package com.example.franjofit.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.franjofit.ui.components.*
import com.example.franjofit.ui.theme.White

@Composable
fun RegisterIntroScreen(onContinue: () -> Unit, onGoogle: () -> Unit) {
    GradientBackground {
        Column(
            Modifier.fillMaxSize().padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(Spacing.xl))
            Text("¡Te damos la bienvenida a “FranJo Fit J”!", color = White, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Spacing.md))
            Text("Queremos conocerte un poco mejor antes, ayúdanos a personalizar tu app de acuerdo a tus objetivos.", color = White.copy(.8f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(Spacing.xl))
            PrimaryButton("Continuar", onContinue)
            Spacer(Modifier.height(Spacing.md))
            GoogleButton(onClick = onGoogle)
        }
    }
}
