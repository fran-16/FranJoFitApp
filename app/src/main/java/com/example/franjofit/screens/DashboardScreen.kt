package com.example.franjofit.screens
import kotlinx.coroutines.launch

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.franjofit.data.FoodRepository
import com.example.franjofit.data.GoalsRepository
import com.example.franjofit.data.DailyGoal
import com.example.franjofit.ui.theme.DeepBlue
import com.example.franjofit.ui.theme.Orange
import com.example.franjofit.ui.theme.White
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class MealType { DESAYUNO, ALMUERZO, CENA, EXTRAS }
data class MealItem(val name: String, val kcal: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onAddWeight: () -> Unit,
    onOpenProfile: () -> Unit = {},
    onOpenAddMeal: (String) -> Unit = {},
    onUpdateBaseGoal: (Int) -> Unit = {}


) {
    val uiState = viewModel.ui.collectAsState()
    var selectedIndex by remember { mutableStateOf(0) }

    var meals by remember { mutableStateOf<Map<String, List<Map<String, Any>>>>(emptyMap()) }


    var dailyGoal by remember { mutableStateOf(DailyGoal(baseGoal = uiState.value.baseGoal)) }
    val scope = rememberCoroutineScope()
    suspend fun reloadMealsAndGoal() {
        meals = FoodRepository.getMealsForToday()

        val consumed = meals.values.flatten().sumOf { (it["kcal"] as? Long ?: 0L).toInt() }

        val goal = GoalsRepository.getDailyGoalOrDefault(uiState.value.baseGoal)
        val base = goal.baseGoal

        GoalsRepository.setTotals(base, consumed)
        dailyGoal = DailyGoal(baseGoal = base, consumed = consumed)
    }


    LaunchedEffect(Unit) { runCatching { reloadMealsAndGoal() } }


    LaunchedEffect(selectedIndex) {
        if (selectedIndex == 1) runCatching { reloadMealsAndGoal() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (selectedIndex) {
                            1 -> "Seguimiento • Hoy"
                            2 -> "Progreso"
                            else -> "Hola ${uiState.value.username}"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Perfil de usuario")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = DeepBlue, tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = selectedIndex == 0,
                    onClick = { selectedIndex = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Principal") },
                    label = { Text("Principal") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Orange, selectedTextColor = Orange,
                        unselectedIconColor = White.copy(0.85f), unselectedTextColor = White.copy(0.85f),
                        indicatorColor = White.copy(0.10f)
                    )
                )
                NavigationBarItem(
                    selected = selectedIndex == 1,
                    onClick = { selectedIndex = 1 },
                    icon = { Icon(Icons.Filled.List, contentDescription = "Seguimiento") },
                    label = { Text("Seguimiento") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Orange, selectedTextColor = Orange,
                        unselectedIconColor = White.copy(0.85f), unselectedTextColor = White.copy(0.85f),
                        indicatorColor = White.copy(0.10f)
                    )
                )
                NavigationBarItem(
                    selected = selectedIndex == 2,
                    onClick = { selectedIndex = 2 },
                    icon = { Icon(Icons.Filled.Star, contentDescription = "Progreso") },
                    label = { Text("Progreso") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Orange, selectedTextColor = Orange,
                        unselectedIconColor = White.copy(0.85f), unselectedTextColor = White.copy(0.85f),
                        indicatorColor = White.copy(0.10f)
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepBlue)
                .padding(padding)
        ) {
            when (selectedIndex) {
                0 -> {
                    val remaining = (dailyGoal.baseGoal - dailyGoal.consumed).coerceAtLeast(0)
                    PrincipalContent(
                        baseGoal = dailyGoal.baseGoal,
                        food = dailyGoal.consumed,
                        exercise = uiState.value.exercise, // si luego lo sumas al cálculo, fácil
                        remaining = remaining,
                        steps = uiState.value.steps,
                        stepsGoal = uiState.value.stepsGoal,
                        exerciseMinutes = uiState.value.exerciseMinutes,
                        onAddWeight = onAddWeight,
                        onEditGoal = { newGoal ->
                            // ⚠️ NO usar LaunchedEffect aquí
                            scope.launch {
                                runCatching {
                                    GoalsRepository.setBaseGoal(newGoal)
                                    val consumed = meals.values.flatten()
                                        .sumOf { (it["kcal"] as? Long ?: 0L).toInt() }
                                    GoalsRepository.setTotals(newGoal, consumed)
                                    dailyGoal = DailyGoal(baseGoal = newGoal, consumed = consumed)
                                }
                            }
                            onUpdateBaseGoal(newGoal)
                        }
                    )
                }
                1 -> TrackingContent(
                    meals = meals,
                    onAddItem = { mealType -> onOpenAddMeal(mealType) }
                )
                2 -> ProgressPlaceholder()
            }
        }
    }
}

@Composable
private fun PrincipalContent(
    baseGoal: Int,
    food: Int,
    exercise: Int,
    remaining: Int,
    steps: Int,
    stepsGoal: Int,
    exerciseMinutes: Int,
    onAddWeight: () -> Unit,
    onEditGoal: (Int) -> Unit
) {
    Column(Modifier.padding(16.dp)) {

        CalorieGoalCard(
            baseGoal = baseGoal,
            remaining = remaining,
            onEditBaseGoal = onEditGoal
        )

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = White.copy(alpha = 0.15f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Pasos: $steps", color = White)
                    Text("Meta: $stepsGoal", color = White)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = White.copy(alpha = 0.15f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Ejercicio: $exercise cal", color = White)
                    Text("Tiempo: $exerciseMinutes min", color = White)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = White.copy(alpha = 0.15f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Peso (últimos 90 días)", color = White)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onAddWeight,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange)
                ) { Text("Agregar peso", color = White) }
            }
        }
    }
}



@Composable
private fun TrackingContent(
    meals: Map<String, List<Map<String, Any>>>,
    onAddItem: (String) -> Unit
) {
    val totalKcal = remember(meals) {
        meals.values.flatten().sumOf { (it["kcal"] as? Long ?: 0L).toInt() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DayHeaderCard(
                totalKcal = totalKcal,
                remaining = (2200 - totalKcal).coerceAtLeast(0) // si quieres 2200 fijo aquí
            )
        }

        listOf("desayuno", "almuerzo", "cena", "extras").forEach { type ->
            item {
                val list = meals[type].orEmpty()
                MealSectionCard(
                    title = type.replaceFirstChar { it.uppercase() },
                    items = list.map {
                        MealItem(
                            name = it["name"] as? String ?: "Sin nombre",
                            kcal = (it["kcal"] as? Long ?: 0L).toInt()
                        )
                    },
                    onAdd = { onAddItem(type) }
                )
            }
        }
    }
}



@Composable
private fun DayHeaderCard(totalKcal: Int, remaining: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Calorías del día", color = White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Consumidas", color = White.copy(0.9f), fontSize = 13.sp)
                    Text("$totalKcal kcal", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Restantes", color = White.copy(0.9f), fontSize = 13.sp)
                    Text("${remaining.coerceAtLeast(0)} kcal", color = Orange, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MealSectionCard(
    title: String,
    items: List<MealItem>,
    onAdd: () -> Unit
) {
    val total = items.sumOf { it.kcal }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(title, color = White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("$total kcal", color = White.copy(0.9f), fontSize = 13.sp)
                }
                Button(
                    onClick = onAdd,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange)
                ) { Text("Agregar", color = White) }
            }
            Spacer(Modifier.height(8.dp))
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(White.copy(alpha = 0.07f))
                        .padding(14.dp)
                ) { Text("Sin ítems aún", color = White.copy(0.75f)) }
            } else {
                items.forEachIndexed { index, item ->
                    MealRow(item)
                    if (index != items.lastIndex) Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = White.copy(alpha = 0.12f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MealRow(item: MealItem) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(item.name, color = White, fontSize = 15.sp)
        Text("${item.kcal} kcal", color = White.copy(0.9f), fontSize = 14.sp)
    }
}

@Composable
private fun ProgressPlaceholder() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Progreso", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Aquí falta que pongamos las gráficas (sprint 2).Amo a bb :3", color = White.copy(0.85f))
    }
}



@Composable
private fun CalorieGoalCard(
    baseGoal: Int,
    remaining: Int,
    onEditBaseGoal: (Int) -> Unit
) {
    val progress = (baseGoal.takeIf { it > 0 }?.let { 1f - (remaining.toFloat() / it) } ?: 0f)
        .coerceIn(0f, 1f)

    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White.copy(alpha = 0.15f)),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(130.dp),
                contentAlignment = Alignment.Center
            ) {
                CalorieLavaRing(progress = progress, bgAlpha = 0.2f)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$remaining",
                        color = White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "kcal\nrestantes",
                        color = White.copy(0.85f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text("Calorías", color = White)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Objetivo base:", color = White.copy(0.9f))
                    Text(
                        "$baseGoal kcal",
                        color = Orange,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable { showDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("Consumido: ${(progress * baseGoal).toInt()} kcal", color = White.copy(0.9f))
                Text("Restantes: $remaining kcal", color = White.copy(0.9f))
            }
        }
    }

    if (showDialog) {
        EditBaseGoalDialog(
            current = baseGoal,
            onDismiss = { showDialog = false },
            onSave = { newGoal ->
                showDialog = false
                onEditBaseGoal(newGoal)
            }
        )
    }
}

@Composable
private fun CalorieLavaRing(
    progress: Float,
    bgAlpha: Float = 0.15f
) {
    val infinite = rememberInfiniteTransition()
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val wavePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(600)
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = 16.dp.toPx()
        val sizeArc = Size(size.minDimension - stroke, size.minDimension - stroke)
        val topLeft = Offset(
            (this.size.width - sizeArc.width) / 2,
            (this.size.height - sizeArc.height) / 2
        )


        drawArc(
            color = White.copy(bgAlpha),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = sizeArc,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )


        val brush = Brush.sweepGradient(
            colors = listOf(
                Orange.copy(alpha = 0.2f),
                Orange,
                Orange.copy(alpha = 0.6f),
                Orange
            )
        )
        drawArc(
            brush = brush,
            startAngle = -90f + rotation,
            sweepAngle = animatedProgress * 360f,
            useCenter = false,
            topLeft = topLeft,
            size = sizeArc,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )


        if (animatedProgress > 0.05f) {
            val r = sizeArc.width / 2
            val cx = topLeft.x + r
            val cy = topLeft.y + r
            val angle = (-90f + animatedProgress * 360f) * (PI / 180).toFloat()
            val edgeX = cx + r * cos(angle)
            val edgeY = cy + r * sin(angle)

            val path = Path().apply {
                moveTo(edgeX, edgeY)
                val wobble = 8.dp.toPx() * sin(wavePhase)
                relativeQuadraticBezierTo(-wobble, -wobble, -wobble * 2, wobble)
            }
            drawPath(
                path = path,
                brush = Brush.linearGradient(listOf(Orange, Orange.copy(0.4f))),
                style = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun EditBaseGoalDialog(
    current: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var text by remember { mutableStateOf(current.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar objetivo base") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { new ->
                    if (new.all { it.isDigit() } && new.length <= 5) text = new
                },
                singleLine = true,
                placeholder = { Text("kcal") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val v = text.toIntOrNull()
                    if (v != null && v > 0) onSave(v) else onDismiss()
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
