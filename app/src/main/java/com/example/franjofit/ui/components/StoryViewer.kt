package com.example.franjofit.components.story

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.franjofit.ui.theme.DeepBlue
import com.example.franjofit.ui.theme.White

data class StoryItem(
    val title: String,
    val imageUrl: String,   // pon un link o luego un drawable
    val body: String
)

@Composable
fun StoryViewerModal(
    visible: Boolean,
    items: List<StoryItem>,
    startIndex: Int,
    onClose: () -> Unit
) {
    if (!visible) return

    var index by remember { mutableStateOf(startIndex) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = DeepBlue),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.80f)

        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {

                // ======== Imagen ========
                AsyncImage(
                    model = items[index].imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Gray)
                )

                Spacer(Modifier.height(12.dp))

                // ======== Título ========
                Text(
                    items[index].title,
                    fontSize = 20.sp,
                    color = White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(10.dp))

                // ======== Contenido ========
                Text(
                    items[index].body,
                    color = White.copy(0.9f),
                    fontSize = 15.sp,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(end = 4.dp)
                        .verticalScroll(rememberScrollState())
                )

                Spacer(Modifier.height(14.dp))

                // ======== Navegación ========
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { if (index > 0) index-- },
                        enabled = index > 0
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Anterior", tint = White)
                    }

                    TextButton(onClick = onClose) {
                        Text("Cerrar", color = White)
                    }

                    IconButton(
                        onClick = { if (index < items.lastIndex) index++ },
                        enabled = index < items.lastIndex
                    ) {
                        Icon(Icons.Filled.ArrowForward, contentDescription = "Siguiente", tint = White)
                    }
                }
            }
        }
    }
}
