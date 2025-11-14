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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.franjofit.components.story.StoryItem
import com.example.franjofit.ui.theme.CardBackground
import com.example.franjofit.ui.theme.CardBorderSoft

@Composable
fun StoriesRow(
    items: List<StoryItem>,          // historias
    onClickStory: (Int) -> Unit,     // índice de la historia
    onSeeMore: () -> Unit
) {
    Column {
        // 👇 Ya NO dibujamos aquí "Historias y salud"
        // ese título lo pone la tarjeta padre en PrincipalContent

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
        colors = CardDefaults.cardColors(
            containerColor = CardBackground      // 💙 mismo fondo que las demás tarjetas
        ),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                listOf(CardBorderSoft, CardBorderSoft)
            )
        )
    ) {
        Column {
            // Imagen arriba
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(CardBackground),
                contentAlignment = Alignment.Center
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        "Imagen",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,   // 👈 texto oscuro, visible
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                listOf(CardBorderSoft, CardBorderSoft)
            )
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Ver más",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
