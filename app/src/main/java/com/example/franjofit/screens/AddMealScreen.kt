package com.example.franjofit.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.franjofit.data.FoodRepository
import com.example.franjofit.ui.theme.DeepBlue
import com.example.franjofit.ui.theme.Orange
import com.example.franjofit.ui.theme.White
import kotlinx.coroutines.launch
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    mealKey: String,
    onBack: () -> Unit = {},
    onScan: () -> Unit = {},

    // 🔥 Desde ScanFood
    scannedName: String? = null,
    scannedKcal: Int? = null,
    scannedPortion: String? = null
) {
    val title = when (mealKey.lowercase()) {
        "desayuno" -> "Agregar desayuno"
        "almuerzo" -> "Agregar almuerzo"
        "cena" -> "Agregar cena"
        else -> "Agregar extras"
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val snackbar = remember { SnackbarHostState() }
    var isSaving by remember { mutableStateOf(false) }


    var selectedItems by remember { mutableStateOf<List<FoodRepository.CatalogUiItem>>(emptyList()) }


    LaunchedEffect(scannedName, scannedKcal, scannedPortion) {
        if (scannedName != null) {
            selectedItems = selectedItems + FoodRepository.CatalogUiItem(
                name = scannedName,
                portionLabel = scannedPortion ?: "100 g",
                kcal = scannedKcal ?: 100,
                preview = FoodRepository.PortionPreview(
                    ig = 0,
                    grams = 100,
                    carbsG = 0.0,
                    proteinG = 0.0,
                    fiberG = 0.0,
                    kcal = scannedKcal ?: 100,
                    gl = 0.0,
                    portionLabel = "100g"
                )
            )
        }
    }

    var catalog by remember { mutableStateOf<List<FoodRepository.CatalogUiItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        loading = true
        try {
            catalog = FoodRepository.listCatalogForUi(context)
        } catch (e: Exception) {
            e.printStackTrace()
            catalog = emptyList()
        }
        loading = false
    }



    val filtered = remember(catalog, query) {
        if (query.isBlank()) catalog
        else catalog.filter { it.name.contains(query, ignoreCase = true) }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbar) },

        // 🔥 BOTÓN CONFIRMAR AL FINAL
        bottomBar = {
            if (selectedItems.isNotEmpty()) {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                isSaving = true
                                FoodRepository.saveMealItems(
                                    context = context,
                                    mealType = mealKey.lowercase(),
                                    items = selectedItems
                                )
                                snackbar.showSnackbar("Guardado correctamente")
                                onBack()
                            } catch (e: Exception) {
                                snackbar.showSnackbar("Error: ${e.message}")
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Confirmar $mealKey")
                }
            }
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

            // ======================================================
            // 🔥 LISTA DE ITEMS YA ELEGIDOS (TEMPORAL)
            // ======================================================
            if (selectedItems.isNotEmpty()) {
                Text(
                    "Seleccionados",
                    color = White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                selectedItems.forEachIndexed { index, food ->
                    SelectedItemCard(
                        item = food,
                        onRemove = {
                            selectedItems = selectedItems.toMutableList().apply {
                                removeAt(index)
                            }
                        }
                    )
                }

                Spacer(Modifier.height(14.dp))
            }


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    placeholder = { Text("Buscar alimento...", color = White.copy(0.7f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = White.copy(0.10f),
                        unfocusedContainerColor = White.copy(0.08f),
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        focusedBorderColor = Orange,
                        unfocusedBorderColor = White.copy(0.25f),
                        cursorColor = Orange,
                    ),
                    modifier = Modifier.weight(1f)
                )

                FilledTonalButton(
                    onClick = onScan,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = White.copy(0.12f),
                        contentColor = Orange
                    )
                ) {
                    Text("Escanear")
                }
            }

            Text(
                "Todos los alimentos",
                color = White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (loading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Orange)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.name }) { food ->
                        CatalogItemCard(
                            item = food,
                            enabled = !isSaving,
                            onAdd = {
                                selectedItems = selectedItems + food
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedItemCard(
    item: FoodRepository.CatalogUiItem,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = White.copy(0.20f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(item.name, color = White, fontWeight = FontWeight.SemiBold)
                Text("${item.kcal} kcal • ${item.portionLabel}", color = White.copy(0.8f))
            }
            TextButton(onClick = onRemove) {
                Text("Eliminar", color = Orange)
            }
        }
    }
}

@Composable
private fun CatalogItemCard(
    item: FoodRepository.CatalogUiItem,
    enabled: Boolean,
    onAdd: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rot by animateFloatAsState(if (expanded) 180f else 0f)

    Card(
        colors = CardDefaults.cardColors(containerColor = White.copy(0.15f)),
        modifier = Modifier.animateContentSize()
    ) {
        Column(Modifier.fillMaxWidth()) {

            // HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, color = White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("${item.kcal} kcal • ${item.portionLabel}", color = White.copy(0.8f))
                }
                Icon(Icons.Filled.ExpandMore, null, tint = White, modifier = Modifier.rotate(rot))
            }

            if (expanded) {
                Divider(color = White.copy(0.1f))
                MetricsGrid(item.preview)

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(
                        enabled = enabled,
                        onClick = onAdd,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Orange,
                            contentColor = White
                        )
                    ) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Agregar")
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricsGrid(p: FoodRepository.PortionPreview) {
    Column(Modifier.padding(14.dp)) {
        Text("Métricas de la porción", color = White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricChip("IG", p.ig.toString())
            MetricChip("Gramos", "${p.grams} g")
            MetricChip("GL", "%.1f".format(Locale.US, p.gl))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricChip("Carbs", "%.1f g".format(Locale.US, p.carbsG))
            MetricChip("Proteína", "%.1f g".format(Locale.US, p.proteinG))
            MetricChip("Fibra", "%.1f g".format(Locale.US, p.fiberG))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricChip("Kcal", "${p.kcal} kcal")
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    Surface(color = White.copy(0.10f), shape = MaterialTheme.shapes.small) {
        Column(
            Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(label, color = White.copy(0.7f), fontSize = 11.sp)
            Text(value, color = White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}
