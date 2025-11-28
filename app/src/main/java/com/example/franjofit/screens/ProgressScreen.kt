package com.example.franjofit.screens

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.franjofit.data.FoodRepository
import com.example.franjofit.data.UserRepository
import com.example.franjofit.data.WeightEntry
import com.github.tehras.charts.line.LineChart
import com.github.tehras.charts.line.LineChartData
import com.github.tehras.charts.line.renderer.line.SolidLineDrawer
import com.github.tehras.charts.line.renderer.point.FilledCircularPointDrawer
import com.github.tehras.charts.line.renderer.xaxis.SimpleXAxisDrawer
import com.github.tehras.charts.line.renderer.yaxis.SimpleYAxisDrawer
import com.github.tehras.charts.piechart.animation.simpleChartAnimation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

val CardBorderSoft = Color(0xFFD3E4FF)
val TextDark = Color(0xFF0D1B2A)

data class SmpDay(
    val day: Int,   // día del mes (1..31)
    val score: Float // SMP (0..100)
)

data class PatientInfo(
    val name: String? = null,
    val email: String? = null,
    val sex: String? = null,
    val birthDate: String? = null,
    val heightCm: Double? = null,
    val currentWeightKg: Double? = null
)


data class MonthlyMacroAverages(
    val avgCarbs: Float,
    val avgProtein: Float,
    val avgFiber: Float
)

@Composable
fun ProgressScreen() {

    val context = LocalContext.current

    var weights by remember { mutableStateOf<List<WeightEntry>>(emptyList()) }
    val scroll = rememberScrollState()

    var dailyCarbs by remember { mutableStateOf(0f) }
    var dailyProtein by remember { mutableStateOf(0f) }
    var dailyFiber by remember { mutableStateOf(0f) }

    var smpDays by remember { mutableStateOf<List<SmpDay>>(emptyList()) }

    var monthlyMacroAvg by remember { mutableStateOf<MonthlyMacroAverages?>(null) }

    var patientInfo by remember { mutableStateOf<PatientInfo?>(null) }


    val now = remember { Calendar.getInstance() }
    var currentYear by remember { mutableStateOf(now.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableStateOf(now.get(Calendar.MONTH)) } // 0..11

    val currentMonthName = remember(currentYear, currentMonth) {
        monthNameSpanish(currentYear, currentMonth)
    }


    LaunchedEffect(Unit) {
        weights = UserRepository.getWeightHistory()

        val mealsToday = FoodRepository.getMealsForToday()
        val allItems = mealsToday.values.flatten()

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


        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                val userDoc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .get()
                    .await()

                if (userDoc.exists()) {
                    val hLong = userDoc.getLong("heightCm")?.toDouble()
                    val hDouble = userDoc.getDouble("heightCm")
                    val height = hDouble ?: hLong

                    val wLong = userDoc.getLong("currentWeightKg")?.toDouble()
                    val wDouble = userDoc.getDouble("currentWeightKg")
                    val currentW = wDouble ?: wLong

                    patientInfo = PatientInfo(
                        name = userDoc.getString("displayName"),
                        email = userDoc.getString("email"),
                        sex = userDoc.getString("sex"),
                        birthDate = userDoc.getString("birthDate"),
                        heightCm = height,
                        currentWeightKg = currentW
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(currentYear, currentMonth) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid
        if (uid == null) {
            smpDays = emptyList()
            monthlyMacroAvg = null
            return@LaunchedEffect
        }

        val db = FirebaseFirestore.getInstance()

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
            val goalsCol = db.collection("users")
                .document(uid)
                .collection("goals")

            val snapsGoals = goalsCol
                .orderBy(FieldPath.documentId())
                .startAt(startId)
                .endAt(endId)
                .get()
                .await()

            smpDays = snapsGoals.documents.mapNotNull { doc ->
                val id = doc.id // "yyyy-MM-dd"
                val parts = id.split("-")
                val day = parts.getOrNull(2)?.toIntOrNull() ?: return@mapNotNull null
                val smp = (doc.getLong("smpCurrent") ?: 100L).toFloat()
                SmpDay(day = day, score = smp)
            }.sortedBy { it.day }

            val mealsCol = db.collection("users")
                .document(uid)
                .collection("meals")

            val snapsMeals = mealsCol
                .orderBy(FieldPath.documentId())
                .startAt(startId)
                .endAt(endId)
                .get()
                .await()

            var totalCarbs = 0f
            var totalProtein = 0f
            var totalFiber = 0f
            var daysWithData = 0

            val mealTypes = listOf("desayuno", "almuerzo", "cena", "extras")

            fun numFromAny(v: Any?): Float =
                when (v) {
                    is Number -> v.toFloat()
                    is String -> v.toFloatOrNull() ?: 0f
                    else -> 0f
                }

            for (doc in snapsMeals.documents) {
                var dayCarbs = 0f
                var dayProtein = 0f
                var dayFiber = 0f

                for (type in mealTypes) {
                    val arr = doc.get(type) as? List<*> ?: emptyList<Any>()
                    for (rawItem in arr) {
                        val item = rawItem as? Map<*, *> ?: continue
                        dayCarbs += numFromAny(item["carbs_g"])
                        dayProtein += numFromAny(item["protein_g"])
                        dayFiber += numFromAny(item["fiber_g"])
                    }
                }

                if (dayCarbs > 0f || dayProtein > 0f || dayFiber > 0f) {
                    totalCarbs += dayCarbs
                    totalProtein += dayProtein
                    totalFiber += dayFiber
                    daysWithData++
                }
            }

            monthlyMacroAvg = if (daysWithData > 0) {
                MonthlyMacroAverages(
                    avgCarbs = totalCarbs / daysWithData,
                    avgProtein = totalProtein / daysWithData,
                    avgFiber = totalFiber / daysWithData
                )
            } else {
                null
            }

        } catch (e: Exception) {
            e.printStackTrace()
            smpDays = emptyList()
            monthlyMacroAvg = null
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

        Spacer(Modifier.height(8.dp))

        // Botón: genera PDF y abre WhatsApp
        OutlinedButton(
            onClick = {
                val uri = generateMonthlyReportPdf(
                    context = context,
                    year = currentYear,
                    month = currentMonth,
                    smpDays = smpDays,
                    weights = weights,
                    monthlyMacroAvg = monthlyMacroAvg,
                    patientInfo = patientInfo
                )
                if (uri != null) {
                    sharePdfToWhatsapp(context, uri)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Descargar y enviar reporte PDF ($currentMonthName)")
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = "El reporte usa los SMP diarios del mes, las comidas registradas para calcular carbohidratos, proteína y fibra promedio diarios, y la evolución de peso.",
            color = TextDark.copy(0.65f),
            fontSize = 11.sp
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

            WeightChartCard(weights)
            Spacer(Modifier.height(20.dp))

            DailyCaloriesCard(
                calories = listOf(1800f, 1950f, 2100f, 1600f, 2000f, 1900f, 2200f)
            )
            Spacer(Modifier.height(20.dp))

            MacroChartCard(
                carbs = dailyCarbs,
                protein = dailyProtein,
                fiber = dailyFiber
            )
            Spacer(Modifier.height(20.dp))
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
        monthNameSpanish(year, month)
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

                val lineData = listOf(
                    LineChartData(
                        points = sorted.map { day ->
                            LineChartData.Point(
                                day.score.toFloat(),
                                day.day.toString()
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


private fun monthNameSpanish(year: Int, month: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
    }
    return SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
        .format(cal.time)
        .replaceFirstChar { it.uppercase() }
}

private fun generateMonthlyReportPdf(
    context: Context,
    year: Int,
    month: Int,               // 0..11
    smpDays: List<SmpDay>,
    weights: List<WeightEntry>,
    monthlyMacroAvg: MonthlyMacroAverages?,
    patientInfo: PatientInfo?
): Uri? {
    if (smpDays.isEmpty() && weights.isEmpty() && monthlyMacroAvg == null) {
        Toast.makeText(
            context,
            "No hay datos suficientes para este mes.",
            Toast.LENGTH_LONG
        ).show()
        return null
    }

    val monthName = monthNameSpanish(year, month)

    val name = patientInfo?.name ?: "Sin nombre registrado"
    val email = patientInfo?.email ?: "Sin correo registrado"
    val sexStr = patientInfo?.sex?.takeIf { it.isNotBlank() } ?: "No especificado"
    val birth = patientInfo?.birthDate?.takeIf { it.isNotBlank() } ?: "No registrado"
    val heightStr = patientInfo?.heightCm?.let { "${"%.0f".format(it)} cm" } ?: "No registrado"
    val currentWeightStr = patientInfo?.currentWeightKg?.let { "${"%.1f".format(it)} kg" } ?: "No registrado"


    val smpAvg = smpDays.takeIf { it.isNotEmpty() }?.map { it.score }?.average()
    val smpMin = smpDays.minByOrNull { it.score }?.score
    val smpMax = smpDays.maxByOrNull { it.score }?.score


    val cal = Calendar.getInstance()
    val monthlyWeights = weights.filter { w ->
        cal.timeInMillis = w.date
        cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
    }

    val weightStart = monthlyWeights.firstOrNull()?.weight
    val weightEnd = monthlyWeights.lastOrNull()?.weight
    val weightMin = monthlyWeights.minByOrNull { it.weight }?.weight
    val weightMax = monthlyWeights.maxByOrNull { it.weight }?.weight
    val weightDiff = if (weightStart != null && weightEnd != null) weightEnd - weightStart else null


    val pdf = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdf.startPage(pageInfo)
    val canvas = page.canvas
    val paint = Paint().apply {
        isAntiAlias = true
        textSize = 12f
    }

    var y = 60f
    val marginX = 40f
    val maxWidth = pageInfo.pageWidth - marginX * 2

    fun drawLine(text: String, bold: Boolean = false, size: Float = 12f, extraSpace: Float = 6f) {
        paint.textSize = size
        paint.isFakeBoldText = bold

        val words = text.split(" ")
        var current = ""
        for (w in words) {
            val test = if (current.isEmpty()) w else "$current $w"
            if (paint.measureText(test) > maxWidth) {
                canvas.drawText(current, marginX, y, paint)
                y += size + extraSpace
                current = w
            } else {
                current = test
            }
        }
        if (current.isNotEmpty()) {
            canvas.drawText(current, marginX, y, paint)
            y += size + extraSpace
        }
    }

    drawLine("Reporte metabólico mensual", bold = true, size = 18f, extraSpace = 10f)
    drawLine("Mes: $monthName", bold = true, size = 14f, extraSpace = 12f)

    drawLine("Datos del paciente", bold = true, size = 14f, extraSpace = 8f)
    drawLine("Nombre: $name")
    drawLine("Email: $email")
    drawLine("Sexo: $sexStr   •   Fecha de nacimiento: $birth")
    drawLine("Altura: $heightStr   •   Peso actual (perfil): $currentWeightStr")
    y += 10f


    drawLine("1. Score metabólico postprandial (SMP)", bold = true, size = 14f, extraSpace = 8f)
    if (smpAvg != null && smpMin != null && smpMax != null) {
        drawLine("Promedio del SMP diario del mes: ${smpAvg.toInt()} (escala 0–100).")
        drawLine("Mínimo registrado: ${smpMin.toInt()}.")
        drawLine("Máximo registrado: ${smpMax.toInt()}.")
    } else {
        drawLine("No hay registros de SMP para este mes.")
    }
    y += 8f


    drawLine("2. Evolución del peso en el mes", bold = true, size = 14f, extraSpace = 8f)
    if (monthlyWeights.isNotEmpty() && weightStart != null && weightEnd != null &&
        weightMin != null && weightMax != null
    ) {
        drawLine("Peso al inicio del mes: ${"%.1f".format(weightStart)} kg.")
        drawLine("Peso al final del mes: ${"%.1f".format(weightEnd)} kg.")
        drawLine("Peso mínimo mensual: ${"%.1f".format(weightMin)} kg.")
        drawLine("Peso máximo mensual: ${"%.1f".format(weightMax)} kg.")
        weightDiff?.let {
            val sign = if (it < 0) "disminución" else "aumento"
            drawLine("Cambio total: ${"%.1f".format(kotlin.math.abs(it))} kg de $sign en el período.")
        }
    } else {
        drawLine("No hay registros de peso para este mes.")
    }
    y += 8f


    drawLine("3. Macronutrientes del mes", bold = true, size = 14f, extraSpace = 8f)
    if (monthlyMacroAvg == null) {
        drawLine("No hay registros de comidas para este mes.")
    } else {
        drawLine(
            "Carbohidratos promedio diario (según registros de comidas): " +
                    "${monthlyMacroAvg.avgCarbs.toInt()} g/día."
        )
        drawLine(
            "Proteína promedio diaria: ${monthlyMacroAvg.avgProtein.toInt()} g/día."
        )
        drawLine(
            "Fibra promedio diaria: ${monthlyMacroAvg.avgFiber.toInt()} g/día."
        )
    }
    y += 8f


    drawLine("4. Comentario automático", bold = true, size = 14f, extraSpace = 8f)
    drawLine(
        "Este documento resume el comportamiento del score metabólico postprandial, " +
                "la evolución del peso corporal y la ingesta de macronutrientes del paciente " +
                "durante el período indicado. Los valores deben interpretarse siempre dentro " +
                "del contexto clínico individual (antecedentes, medicación, resultados de " +
                "laboratorio y otros estudios complementarios)."
    )

    pdf.finishPage(page)


    val fileName = "Reporte_SMP_${year}_${month + 1}.pdf"

    return try {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            resolver.openOutputStream(uri)?.use { output ->
                pdf.writeTo(output)
            }
            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            Toast.makeText(
                context,
                "Reporte guardado en Descargas como $fileName",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                context,
                "No se pudo crear el archivo de reporte.",
                Toast.LENGTH_LONG
            ).show()
        }
        uri
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(
            context,
            "Error al generar el PDF.",
            Toast.LENGTH_LONG
        ).show()
        null
    } finally {
        pdf.close()
    }
}

private fun sharePdfToWhatsapp(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        setPackage("com.whatsapp")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(
            context,
            "No se encontró WhatsApp instalado.",
            Toast.LENGTH_LONG
        ).show()
    }
}
