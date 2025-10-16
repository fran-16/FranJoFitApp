package com.example.franjofit.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.franjofit.ui.theme.DeepBlue
import com.example.franjofit.ui.theme.Orange
import com.example.franjofit.ui.theme.White

enum class MealType { DESAYUNO, ALMUERZO, CENA, EXTRAS }
data class MealItem(val name: String, val kcal: Int)
data class TrackingUi(
    val dayLabel: String = "Hoy",
    val meals: Map<MealType, List<MealItem>> = mapOf(
        MealType.DESAYUNO to listOf(MealItem("Avena con leche", 220), MealItem("Banana", 90)),
        MealType.ALMUERZO to listOf(MealItem("Pollo + ensalada", 430)),
        MealType.CENA to emptyList(),
        MealType.EXTRAS to listOf(MealItem("Yogur", 110))
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onAddWeight: () -> Unit,
    onOpenProfile: () -> Unit = {},
    onOpenAddMeal: (String) -> Unit = {}
) {
    val uiState = viewModel.ui.collectAsState()

    var selectedIndex by remember { mutableStateOf(0) }

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
                0 -> PrincipalContent(
                    baseGoal = uiState.value.baseGoal,
                    food = uiState.value.food,
                    exercise = uiState.value.exercise,
                    remaining = uiState.value.remaining,
                    steps = uiState.value.steps,
                    stepsGoal = uiState.value.stepsGoal,
                    exerciseMinutes = uiState.value.exerciseMinutes,
                    onAddWeight = onAddWeight
                )
                1 -> TrackingContent(
                    ui = TrackingUi(),
                    onAddItem = { mealType -> onOpenAddMeal(mealType.name.lowercase()) }
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
    onAddWeight: () -> Unit
) {
    Column(Modifier.padding(16.dp)) {
        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = White.copy(alpha = 0.15f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Calorías", color = White)
                Spacer(Modifier.height(8.dp))
                Text("Objetivo base: $baseGoal", color = White)
                Text("Alimentos: $food", color = White)
                Text("Ejercicio: $exercise", color = White)
                Text("Restantes: $remaining", color = White)
            }
        }

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
    ui: TrackingUi,
    onAddItem: (MealType) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DayHeaderCard(
                totalKcal = ui.meals.values.flatten().sumOf { it.kcal },
                remaining = 2200 - ui.meals.values.flatten().sumOf { it.kcal }
            )
        }
        item { MealSectionCard("Desayuno", ui.meals[MealType.DESAYUNO].orEmpty()) { onAddItem(MealType.DESAYUNO) } }
        item { MealSectionCard("Almuerzo", ui.meals[MealType.ALMUERZO].orEmpty()) { onAddItem(MealType.ALMUERZO) } }
        item { MealSectionCard("Cena",     ui.meals[MealType.CENA].orEmpty())     { onAddItem(MealType.CENA) } }
        item { MealSectionCard("Extras",   ui.meals[MealType.EXTRAS].orEmpty())   { onAddItem(MealType.EXTRAS) } }
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
        Text("Aquí falta que pongamos las gráficas(sprint2), Amo a bb :3 ", color = White.copy(0.85f))
    }
}
