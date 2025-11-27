package com.example.franjofit.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.franjofit.ui.theme.Orange
import com.example.franjofit.ui.theme.PrimaryBlue
import com.example.franjofit.ui.theme.White

@Composable
fun TitleLogo(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(
            text = "META",
            color = White,
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "FRANJO",
            color = Color(0xFF0077C8),
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
