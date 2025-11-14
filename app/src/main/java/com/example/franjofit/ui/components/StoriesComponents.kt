package com.example.franjofit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.franjofit.components.story.StoryItem
import com.example.franjofit.ui.theme.Orange
import com.example.franjofit.ui.theme.White

@Composable
fun StoriesRow(
    items: List<StoryItem>,          // 👈 AHORA RECIBE StoryItem
    onClickStory: (Int) -> Unit,     // índice de la historia
    onSeeMore: () -> Unit
) {
    Column {
        Text(
            "Historias y salud",
            color = White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.width(4.dp))

            items.forEachIndexed { index, story ->
                StoryCard(
                    title = story.title,
                    imageUrl = story.imageUrl,
                    onClick = { onClickStory(index) }
                )
            }

            MoreStoriesButton(onClick = onSeeMore)

            Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
fun StoryCard(
    title: String,
    imageUrl: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .height(190.dp)
            .clip(RoundedCornerShape(16.dp))
            .padding(bottom = 2.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = White.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // 🔥 IMAGEN REAL ARRIBA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("Imagen", color = White.copy(0.6f))
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                title,
                color = White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
    }
}

@Composable
fun MoreStoriesButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(190.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Orange.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Ver más", color = Orange, fontWeight = FontWeight.Bold)
        }
    }
}
