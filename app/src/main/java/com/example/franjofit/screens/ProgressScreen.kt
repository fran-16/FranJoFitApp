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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.franjofit.data.UserRepository
import com.example.franjofit.data.WeightEntry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

val CardBorderSoft = Color(0xFFD3E4FF)
val TextDark = Color(0xFF0D1B2A)

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
            .background(CardBorderSoft)
            .padding(16.dp)
    ) {
        Text(
            "Progreso",
            color = TextDark,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        if (weights.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().padding(top = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Sin datos todavía", color = TextDark.copy(0.7f))
            }
        } else {
            WeightChartCard(weights)
            Spacer(Modifier.height(20.dp))

            MacroChartCard(
                carbs = 180f,
                protein = 70f,
                fiber = 25f
            )
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
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.6f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(16.dp)) {

            Text("Peso (últimos 90 días)", color = TextDark, fontWeight = FontWeight.SemiBold)

            Spacer(Modifier.height(6.dp))

            Text(
                "Actual: ${"%.1f".format(lastW)} kg   •   Mín: ${"%.1f".format(minW)}   •   Máx: ${"%.1f".format(maxW)}",
                color = TextDark.copy(0.75f),
                fontSize = 13.sp
            )

            if (diff != 0f) {
                val arrow = if (diff < 0) "⬇" else "⬆"
                val color = if (diff < 0) Color(0xFF4CAF50) else Color(0xFFF44336)

                Spacer(Modifier.height(4.dp))
                Text(
                    "$arrow Cambio total: ${"%.1f".format(kotlin.math.abs(diff))} kg",
                    color = color,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(10.dp))

            WeightLineChart(weights)

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(firstDate, color = TextDark.copy(0.8f), fontSize = 12.sp)
                Text(lastDate, color = TextDark.copy(0.8f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun WeightLineChart(weights: List<WeightEntry>) {

    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1200),
        label = "chartAlpha"
    )

    val minW = weights.minOf { it.weight }
    val maxW = weights.maxOf { it.weight }
    val range = (maxW - minW).takeIf { it != 0f } ?: 1f

    val points = weights.map { it.date to it.weight }

    Box(
        Modifier.height(220.dp).fillMaxWidth()
    ) {

        Canvas(Modifier.fillMaxSize()) {

            val pad = 32f
            val w = size.width - pad * 2
            val h = size.height - pad * 2

            fun x(i: Int) =
                pad + if (points.size == 1) w / 2f else (i.toFloat() / (points.size - 1)) * w

            fun y(v: Float) = pad + (1f - (v - minW) / range) * h

            val path = Path()
            val startY = y(points.first().second)
            path.moveTo(x(0), startY)

            if (points.size > 1) {
                for (i in 1 until points.size) {
                    val x1 = x(i - 1)
                    val y1 = y(points[i - 1].second)
                    val x2 = x(i)
                    val y2 = y(points[i].second)
                    val m = (x1 + x2) / 2f
                    path.cubicTo(m, y1, m, y2, x2, y2)
                }
            } else {
                path.lineTo(x(0) + 0.1f, startY)
            }

            val shadow = Path().apply {
                addPath(path)
                lineTo(x(points.lastIndex), h + pad)
                lineTo(pad, h + pad)
                close()
            }

            drawPath(
                path = shadow,
                color = Color(0xFFFF9300).copy(alpha = 0.20f * alpha)
            )

            drawPath(
                path = path,
                color = Color(0xFFFF9300).copy(alpha = alpha),
                style = Stroke(6f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun MacroChartCard(carbs: Float, protein: Float, fiber: Float) {

    val total = (carbs + protein + fiber).takeIf { it > 0 } ?: 1f
    val carbRatio = carbs / total
    val protRatio = protein / total
    val fibRatio = fiber / total

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.6f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(16.dp)) {

            Text("Macronutrientes del día", color = TextDark, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .background(Color(0xFFE9F0FF), MaterialTheme.shapes.small)
            ) {
                Row(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.weight(carbRatio).fillMaxHeight()
                            .background(Color(0xFF4FC3F7))
                    )
                    Box(
                        Modifier.weight(protRatio).fillMaxHeight()
                            .background(Color(0xFF81C784))
                    )
                    Box(
                        Modifier.weight(fibRatio).fillMaxHeight()
                            .background(Color(0xFFFFF176))
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            MacroLegend("Carbohidratos", carbs, Color(0xFF4FC3F7))
            MacroLegend("Proteína", protein, Color(0xFF81C784))
            MacroLegend("Fibra", fiber, Color(0xFFFFF176))
        }
    }
}

@Composable
fun MacroLegend(label: String, grams: Float, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(10.dp).background(color, MaterialTheme.shapes.small)
            )
            Spacer(Modifier.width(8.dp))
            Text(label, color = TextDark.copy(0.9f), fontSize = 13.sp)
        }
        Text("${grams.toInt()} g", color = TextDark, fontSize = 13.sp)
    }
}
