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
    mealKey: String,                 // "desayuno" | "almuerzo" | "cena" | "extras"
    onBack: () -> Unit = {},
    onScan: () -> Unit = {},

    // 🔥 Estos vienen desde NAV (resultado del scan)
    scannedName: String? = null,
    scannedKcal: Int? = null,
    scannedPortion: String? = null
) {
    val title = when (mealKey.lowercase()) {
        "desayuno" -> "Agregar desayuno"
        "almuerzo" -> "Agregar almuerzo"
        "cena"     -> "Agregar cena"
        else       -> "Agregar extras"
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var isSaving by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    // ======================
    // Cargar Catálogo
    // ======================
    var catalog by remember { mutableStateOf<List<FoodRepository.CatalogUiItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        loading = true
        runCatching { FoodRepository.listCatalogForUi(context) }
            .onSuccess { catalog = it }
        loading = false
    }

    // ======================
    // 🔥 TRATAMIENTO DE SCAN (Highlight)
    // ======================
    val highlightItem = remember(scannedName, catalog) {
        catalog.firstOrNull {
            it.name.equals(scannedName ?: "", ignoreCase = true)
        }
    }

    // ======================
    // Filtro de búsqueda
    // ======================
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
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbar) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepBlue)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ======================
            // Barra de búsqueda + scan
            // ======================
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
                    enabled = !isSaving,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = White.copy(0.12f),
                        contentColor = Orange
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) { Text("Escanear") }
            }

            Text(
                "Todos los alimentos",
                color = White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            // ======================
            // 🔥 ITEM DESTACADO DEL ESCANEO
            // ======================
            if (highlightItem != null) {
                CatalogItemCard(
                    item = FoodRepository.CatalogUiItem(
                        name = highlightItem.name,
                        portionLabel = scannedPortion ?: highlightItem.portionLabel,
                        kcal = scannedKcal ?: highlightItem.kcal,
                        preview = highlightItem.preview
                    ),
                    enabled = !isSaving,
                    onAdd = {
                        scope.launch {
                            try {
                                isSaving = true
                                FoodRepository.addMealItemAuto(
                                    context = context,
                                    mealType = mealKey.lowercase(),
                                    displayName = highlightItem.name
                                )
                                snackbar.showSnackbar("${highlightItem.name} agregado a $mealKey")
                                onBack()
                            } catch (e: Exception) {
                                snackbar.showSnackbar("Error: ${e.message ?: "no se pudo guardar"}")
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                                scope.launch {
                                    try {
                                        isSaving = true
                                        FoodRepository.addMealItemAuto(
                                            context = context,
                                            mealType = mealKey.lowercase(),
                                            displayName = food.name
                                        )
                                        snackbar.showSnackbar("${food.name} agregado a $mealKey")
                                        onBack()
                                    } catch (e: Exception) {
                                        snackbar.showSnackbar("Error: ${e.message ?: "no se pudo guardar"}")
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ========================================================================
// TARJETAS E ITEM UI
// ========================================================================

@Composable
private fun CatalogItemCard(
    item: FoodRepository.CatalogUiItem,
    enabled: Boolean,
    onAdd: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rot by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    Card(
        colors = CardDefaults.cardColors(containerColor = White.copy(alpha = 0.15f)),
        modifier = Modifier.animateContentSize()
    ) {
        Column(Modifier.fillMaxWidth()) {
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
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${item.kcal} kcal • ${item.portionLabel}",
                        color = White.copy(0.85f)
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = "Ver detalles",
                    tint = White,
                    modifier = Modifier.rotate(rot)
                )
            }

            if (expanded) {
                Divider(color = White.copy(0.1f))
                MetricsGrid(item.preview)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(
                        onClick = onAdd,
                        enabled = enabled,
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
    }
}

@Composable
private fun MetricsGrid(p: FoodRepository.PortionPreview) {
    Column(Modifier.padding(all = 14.dp)) {
        Text(
            text = "Métricas de la porción",
            color = White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricChip(label = "IG", value = p.ig.toString())
            MetricChip(label = "Gramos", value = "${p.grams} g")
            MetricChip(label = "GL", value = String.format(Locale.US, "%.1f", p.gl))
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricChip(label = "Carbohidratos", value = String.format(Locale.US, "%.1f g", p.carbsG))
            MetricChip(label = "Proteína", value = String.format(Locale.US, "%.1f g", p.proteinG))
            MetricChip(label = "Fibra", value = String.format(Locale.US, "%.1f g", p.fiberG))
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricChip(label = "Energía", value = "${p.kcal} kcal")
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    Surface(color = White.copy(0.10f), shape = MaterialTheme.shapes.small) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(label, color = White.copy(0.75f), fontSize = 11.sp)
            Text(value, color = White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}
