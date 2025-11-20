package com.example.franjofit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.franjofit.ui.theme.PrimarySoft
import com.example.franjofit.ui.theme.White

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(100.dp) // pill

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(PrimarySoft)               // ⭐ Fondo CELESTE
            .border(2.dp, PrimarySoft, shape),     // ⭐ Borde CELESTE
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,     // No sobrescribas el fondo
            contentColor = White                    // Texto blanco
        )
    ) {
        Text(text)
    }
}
