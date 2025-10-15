package com.example.franjofit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ColorScheme = darkColorScheme(
    primary      = Orange,
    onPrimary    = White,
    background   = DeepBlue,
    surface      = NightBlue,
    onSurface    = White,
    error        = Red
)

@Composable
fun FranJoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography  = MaterialTheme.typography,
        content     = content
    )
}
