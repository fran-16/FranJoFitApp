package com.example.franjofit.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.franjofit.ui.theme.White

@Composable
fun TextButtonWhite(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = White,
        textAlign = TextAlign.Center,
        modifier = modifier.clickable { onClick() }
    )
}
