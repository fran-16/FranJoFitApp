package com.example.franjofit.ui.components

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun OutlineInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = Color(0xFF6E6E6E) // ⭐ Gris oscuro para placeholder
            )
        },
        singleLine = true,
        modifier = modifier,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        colors = TextFieldDefaults.colors(
            // ⭐ Texto negro
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,

            // ⭐ Cursor negro
            cursorColor = Color.Black,

            // ⭐ Fondo del campo (transparente)
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,

            // ⭐ Líneas / bordes negros suaves
            focusedIndicatorColor = Color.Black,
            unfocusedIndicatorColor = Color.Gray,
        )
    )
}
