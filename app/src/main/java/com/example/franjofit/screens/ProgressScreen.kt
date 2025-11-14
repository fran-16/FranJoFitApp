package com.example.franjofit.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.franjofit.data.UserRepository
import com.example.franjofit.data.WeightEntry
import com.example.franjofit.ui.theme.DeepBlue
import com.example.franjofit.ui.theme.White
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProgressScreen() {
    var weights by remember { mutableStateOf<List<WeightEntry>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            weights = UserRepository.getWeightHistory()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(DeepBlue)
            .padding(16.dp)
    ) {
        Text(
            "Progreso",
            color = White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        if (weights.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Sin datos todavía", color = White.copy(0.7f))
            }
        } else {
            WeightChartCard(weights)
        }
    }
}

@Composable
fun WeightChartCard(weights: List<WeightEntry>) {

    val minW = weights.minOf { it.weight }
    val maxW = weights.maxOf { it.weight }
    val lastW = weights.last().weight
    val diff = lastW - weights.first().weight

    val dateFormatter = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }
    val firstDate = dateFormatter.format(Date(weights.first().date))
    val lastDate = dateFormatter.format(Date(weights.last().date))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White.copy(alpha = 0.12f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Peso (últimos 90 días)",
                color = White,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(6.dp))

            // Resumen de números en texto (en vez de eje Y)
            Text(
                text = "Actual: ${"%.1f".format(lastW)} kg   •   Mín: ${"%.1f".format(minW)}   •   Máx: ${"%.1f".format(maxW)}",
                color = White.copy(0.85f),
                fontSize = 13.sp
            )

            if (diff != 0f) {
                val arrow = if (diff < 0) "⬇" else "⬆"
                val color = if (diff < 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$arrow Cambio total: ${"%.1f".format(kotlin.math.abs(diff))} kg",
                    color = color,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            // Gráfica
            WeightLineChart(weights)

            Spacer(Modifier.height(12.dp))

            // “Eje X” como textos abajo del gráfico
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(firstDate, color = White.copy(0.8f), fontSize = 12.sp)
                Text(lastDate, color = White.copy(0.8f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun WeightLineChart(weights: List<WeightEntry>) {

    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1200),
        label = "chartAlpha"
    )

    val minW = weights.minOf { it.weight }
    val maxW = weights.maxOf { it.weight }
    val range = (maxW - minW).takeIf { it != 0f } ?: 1f // evita división por cero

    val points = weights.map { it.date to it.weight }

    Box(
        modifier = Modifier
            .height(220.dp)
            .fillMaxWidth()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {

            val padding = 32f
            val width = size.width - padding * 2
            val height = size.height - padding * 2

            fun x(i: Int): Float =
                padding + if (points.size == 1) width / 2f else (i.toFloat() / (points.size - 1)) * width

            fun y(value: Float): Float =
                padding + (1f - (value - minW) / range) * height

            // -----------------------------
            // Curva suavizada (Bézier)
            // -----------------------------
            val path = Path()
            val firstY = y(points.first().second)
            path.moveTo(x(0), firstY)

            if (points.size == 1) {
                path.lineTo(x(0) + 0.1f, firstY)
            } else {
                for (i in 1 until points.size) {
                    val x1 = x(i - 1)
                    val y1 = y(points[i - 1].second)

                    val x2 = x(i)
                    val y2 = y(points[i].second)

                    val midX = (x1 + x2) / 2f

                    path.cubicTo(
                        midX, y1,
                        midX, y2,
                        x2, y2
                    )
                }
            }

            // Sombra bajo la curva
            val shadowPath = Path().apply {
                addPath(path)
                lineTo(x(points.lastIndex), height + padding)
                lineTo(padding, height + padding)
                close()
            }

            drawPath(
                path = shadowPath,
                color = Color(0xFFFF9300).copy(alpha = 0.18f * animatedAlpha)
            )

            // Línea principal
            drawPath(
                path = path,
                color = Color(0xFFFF9300).copy(alpha = animatedAlpha),
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )

            // Puntos
            points.forEachIndexed { i, (_, w) ->
                drawCircle(
                    color = Color.White.copy(alpha = animatedAlpha),
                    radius = 6f,
                    center = androidx.compose.ui.geometry.Offset(
                        x(i),
                        y(w)
                    )
                )
            }
        }
    }
}
