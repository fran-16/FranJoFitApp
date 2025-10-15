package com.example.franjofit.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.franjofit.R

@Composable
fun GoogleButton(
    text: String = "Continuar con Google",
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(28.dp)

    Surface(
        onClick = onClick,
        shape = shape,
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        color = Color(0xFFF2F2F2),
        modifier = modifier
            .height(50.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFFF2F2F2))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.google_imagen),
                    contentDescription = "Google",
                    modifier = Modifier
                        .size(22.dp)
                )

                Spacer(Modifier.width(12.dp))

                Text(
                    text = text,
                    color = Color(0xFF3C4043),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
