package com.example.franjofit.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.franjofit.data.UserRepository
import com.example.franjofit.data.WeightEntry
import com.example.franjofit.data.FoodRepository
import com.github.tehras.charts.bar.renderer.label.SimpleValueDrawer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// Tehras Charts
// Tehras Charts
// Tehras Charts

import com.github.tehras.charts.line.LineChart
import com.github.tehras.charts.line.LineChartData
import com.github.tehras.charts.line.renderer.line.SolidLineDrawer
import com.github.tehras.charts.line.renderer.point.FilledCircularPointDrawer
import com.github.tehras.charts.line.renderer.xaxis.SimpleXAxisDrawer
import com.github.tehras.charts.line.renderer.yaxis.SimpleYAxisDrawer
import com.github.tehras.charts.piechart.animation.simpleChartAnimation

val CardBorderSoft = Color(0xFFD3E4FF)
val TextDark = Color(0xFF0D1B2A)

// SMP de un día (para el gráfico mensual)
data class SmpDay(
    val day: Int,     // día del mes (1..31)
    val score: Float  // SMP (0..100)
)

@Composable
fun ProgressScreen() {

    var weights by remember { mutableStateOf<List<WeightEntry>>(emptyList()) }
    val scroll = rememberScrollState()

    // Macros del día
    var dailyCarbs by remember { mutableStateOf(0f) }
    var dailyProtein by remember { mutableStateOf(0f) }
    var dailyFiber by remember { mutableStateOf(0f) }

    // SMP historial por mes
    var smpDays by remember { mutableStateOf<List<SmpDay>>(emptyList()) }

    // Mes actual
    val now = remember { Calendar.getInstance() }
    var currentYear by remember { mutableStateOf(now.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableStateOf(now.get(Calendar.MONTH)) } // 0..11

    // Peso + macros (no dependen del mes)
    LaunchedEffect(Unit) {
        weights = UserRepository.getWeightHistory()

        val meals = FoodRepository.getMealsForToday()
        val allItems = meals.values.flatten()

        fun sumKey(key: String): Float =
            allItems.sumOf { item ->
                when (val v = item[key]) {
                    is Number -> v.toDouble()
                    else -> 0.0
                }
            }.toFloat()

        dailyCarbs = sumKey("carbs_g")
        dailyProtein = sumKey("protein_g")
        dailyFiber = sumKey("fiber_g")
    }

    // Cada vez que cambia el mes/año, leemos los smpCurrent de ese mes
    LaunchedEffect(currentYear, currentMonth) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid
        if (uid == null) {
            smpDays = emptyList()
            return@LaunchedEffect
        }

        val db = FirebaseFirestore.getInstance()
        val col = db.collection("users")
            .document(uid)
            .collection("goals")

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startId = sdf.format(cal.time)

        val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, lastDay)
        val endId = sdf.format(cal.time)

        try {
            val snaps = col
                .orderBy(FieldPath.documentId())
                .startAt(startId)
                .endAt(endId)
                .get()
                .await()

            smpDays = snaps.documents.mapNotNull { doc ->
                val id = doc.id // "yyyy-MM-dd"
                val parts = id.split("-")
                val day = parts.getOrNull(2)?.toIntOrNull() ?: return@mapNotNull null
                val smp = (doc.getLong("smpCurrent") ?: 100L).toFloat()
                SmpDay(day = day, score = smp)
            }.sortedBy { it.day }
        } catch (e: Exception) {
            e.printStackTrace()
            smpDays = emptyList()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(CardBorderSoft)
            .verticalScroll(scroll)
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
                Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Sin datos todavía", color = TextDark.copy(0.7f))
            }
        } else {
            // Peso
            WeightChartCard(weights)
            Spacer(Modifier.height(20.dp))

            // Calorías últimos 7 días (de momento mock)
            DailyCaloriesCard(
                calories = listOf(1800f, 1950f, 2100f, 1600f, 2000f, 1900f, 2200f)
            )
            Spacer(Modifier.height(20.dp))

            // Macros del día
            MacroChartCard(
                carbs = dailyCarbs,
                protein = dailyProtein,
                fiber = dailyFiber
            )
            Spacer(Modifier.height(20.dp))

            // 🔹 Nuevo: gráfico mensual del SMP
            SmpMonthlyCard(
                smpDays = smpDays,
                year = currentYear,
                month = currentMonth,
                onPrevMonth = {
                    if (currentMonth == 0) {
                        currentMonth = 11
                        currentYear -= 1
                    } else {
                        currentMonth -= 1
                    }
                },
                onNextMonth = {
                    if (currentMonth == 11) {
                        currentMonth = 0
                        currentYear += 1
                    } else {
                        currentMonth += 1
                    }
                }
            )

            Spacer(Modifier.height(40.dp))
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
        Modifier
            .height(220.dp)
            .fillMaxWidth()
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
fun DailyCaloriesCard(calories: List<Float>) {

    val labels = listOf("L", "M", "X", "J", "V", "S", "D")
    val max = calories.maxOrNull()?.takeIf { it > 0f } ?: 1f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.6f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(16.dp)) {

            Text("Calorías últimos 7 días", color = TextDark, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val barWidth = size.width / (calories.size * 2f)
                    val bottom = size.height - 24f

                    calories.forEachIndexed { index, value ->
                        val ratio = value / max
                        val barHeight = ratio * (bottom - 8f)
                        val xCenter = (index * 2 + 1) * barWidth
                        val top = bottom - barHeight

                        drawRoundRect(
                            color = Color(0xFF4FC3F7),
                            topLeft = Offset(xCenter - barWidth / 2f, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                calories.forEachIndexed { index, value ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(labels.getOrElse(index) { "" }, color = TextDark, fontSize = 11.sp)
                        Text("${value.toInt()} kcal", color = TextDark.copy(0.7f), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MacroChartCard(carbs: Float, protein: Float, fiber: Float) {

    val sum = carbs + protein + fiber

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.6f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(16.dp)) {

            Text("Macronutrientes del día", color = TextDark, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            if (sum <= 0f) {
                Text(
                    "Aún no has registrado comidas hoy.",
                    color = TextDark.copy(0.7f),
                    fontSize = 13.sp
                )
            } else {
                val total = sum
                val carbRatio = carbs / total
                val protRatio = protein / total
                val fibRatio = fiber / total

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .background(Color(0xFFE9F0FF), MaterialTheme.shapes.small)
                ) {
                    Row(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .weight(carbRatio)
                                .fillMaxHeight()
                                .background(Color(0xFF4FC3F7))
                        )
                        Box(
                            Modifier
                                .weight(protRatio)
                                .fillMaxHeight()
                                .background(Color(0xFF81C784))
                        )
                        Box(
                            Modifier
                                .weight(fibRatio)
                                .fillMaxHeight()
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
}

@Composable
fun MacroLegend(label: String, grams: Float, color: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(color, MaterialTheme.shapes.small)
            )
            Spacer(Modifier.width(8.dp))
            Text(label, color = TextDark.copy(0.9f), fontSize = 13.sp)
        }
        Text("${grams.toInt()} g", color = TextDark, fontSize = 13.sp)
    }
}
@Composable
fun SmpMonthlyCard(
    smpDays: List<SmpDay>,
    year: Int,
    month: Int, // 0..11
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthName = remember(year, month) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
        }
        SimpleDateFormat("MMMM yyyy", Locale("es", "ES")).format(cal.time)
            .replaceFirstChar { it.uppercase() }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.6f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "SMP por día",
                    color = TextDark,
                    fontWeight = FontWeight.SemiBold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrevMonth) {
                        Text("<", color = TextDark)
                    }
                    Text(
                        text = monthName,
                        color = TextDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = onNextMonth) {
                        Text(">", color = TextDark)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (smpDays.isEmpty()) {
                Text(
                    "Sin registros de SMP para este mes.",
                    color = TextDark.copy(0.7f),
                    fontSize = 13.sp
                )
            } else {
                val sorted = smpDays.sortedBy { it.day }

                // 👉 AQUÍ VA EL lineDrawer (obligatorio en 0.2.2-alpha)
                val lineData = listOf(
                    LineChartData(
                        points = sorted.map { day ->
                            LineChartData.Point(
                                day.score.toFloat(),      // value
                                day.day.toString()        // label para el eje X
                            )
                        },
                        lineDrawer = SolidLineDrawer()
                    )
                )

                LineChart(
                    linesChartData = lineData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    animation = simpleChartAnimation(),
                    pointDrawer = FilledCircularPointDrawer(),
                    xAxisDrawer = SimpleXAxisDrawer(),
                    yAxisDrawer = SimpleYAxisDrawer(),
                    horizontalOffset = 5f
                )

                Spacer(Modifier.height(4.dp))
                Text(
                    "Valores entre 0 y 100 (SMP del día).",
                    color = TextDark.copy(0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
