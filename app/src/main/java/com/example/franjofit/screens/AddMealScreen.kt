package com.example.franjofit.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.franjofit.ui.theme.DeepBlue
import com.example.franjofit.ui.theme.Orange
import com.example.franjofit.ui.theme.White

data class FoodSuggestion(
    val name: String,
    val kcal: Int,
    val portionLabel: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    mealKey: String,
    onBack: () -> Unit = {},
    onScan: () -> Unit = {},
    onAddFood: (FoodSuggestion) -> Unit = {}
) {
    val title = when (mealKey.lowercase()) {
        "desayuno" -> "Agregar desayuno"
        "almuerzo" -> "Agregar almuerzo"
        "cena"     -> "Agregar cena"
        else       -> "Agregar extras"
    }

    var query by remember { mutableStateOf("") }

    // Sugerencias mock (conéctalas luego a Firestore/API)
    val suggestions = remember {
        listOf(
            FoodSuggestion("Avena cocida", 150, "1 taza (240 ml)"),
            FoodSuggestion("Pan integral", 80, "1 rebanada (28 g)"),
            FoodSuggestion("Huevo cocido", 78, "1 unidad (50 g)"),
            FoodSuggestion("Yogur natural", 120, "1 pote (150 g)"),
            FoodSuggestion("Manzana", 95, "1 unidad (182 g)")
        )
    }
    val filtered = if (query.isBlank()) suggestions
    else suggestions.filter { it.name.contains(query, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepBlue)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("Buscar alimento...", color = White.copy(0.7f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = White.copy(0.10f),
                        unfocusedContainerColor = White.copy(0.08f),
                        disabledContainerColor = White.copy(0.08f),
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        focusedBorderColor = Orange,
                        unfocusedBorderColor = White.copy(0.25f),
                        cursorColor = Orange
                    ),
                    modifier = Modifier.weight(1f)
                )

                FilledTonalButton(
                    onClick = onScan,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = White.copy(0.12f),
                        contentColor = Orange
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text("Escanear")
                }
            }

            Text(
                "Opciones recomendadas",
                color = White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { food ->
                    SuggestionCard(
                        item = food,
                        onAdd = { onAddFood(food) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    item: FoodSuggestion,
    onAdd: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = White.copy(alpha = 0.15f))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.name, color = White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text("${item.kcal} kcal • ${item.portionLabel}", color = White.copy(0.85f))
            }
            FilledTonalButton(
                onClick = onAdd,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Orange,
                    contentColor = White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar")
                Spacer(Modifier.width(6.dp))
                Text("Agregar")
            }
        }
    }
}
