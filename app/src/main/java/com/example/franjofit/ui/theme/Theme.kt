package com.example.franjofit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// ⭐ TEMA CLARO: blanco + celeste
private val ColorScheme = lightColorScheme(
    primary    = PrimarySoft,      // azul principal (botones, etc.)
    onPrimary  = White,
    background = ScreenBackground, // fondo de pantallas
    surface    = White,            // tarjetas blancas
    onSurface  = DeepBlue,         // texto en tarjetas
    error      = Red
)

@Composable
fun FranJoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        content     = content
    )
}
