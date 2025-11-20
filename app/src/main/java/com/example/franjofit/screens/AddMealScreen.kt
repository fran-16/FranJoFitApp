package com.example.franjofit.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.franjofit.reminders.SmpReminderManager
import com.example.franjofit.ui.theme.CardBorderSoft
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    mealKey: String,
    onBack: () -> Unit = {},
    onScan: () -> Unit = {},

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

    // Si viene algo escaneado con la cámara/IA, se agrega a la lista
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

    // Cargar catálogo
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
                title = {
                    Text(
                        title,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbar) },
        bottomBar = {
            if (selectedItems.isNotEmpty()) {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                isSaving = true

                                // 1️⃣ Guardar la comida en Firebase
                                FoodRepository.saveMealItems(
                                    context = context,
                                    mealType = mealKey.lowercase(),
                                    items = selectedItems
                                )
                                snackbar.showSnackbar("Guardado correctamente")

                                // 2️⃣ Programar notificación a 1 minuto
                                SmpReminderManager.schedulePostprandialReminder(
                                    context = context,
                                    minutes = 1L   // 👈 aquí 1 minuto
                                )

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
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Confirmar $mealKey")
                }
            }
        }

    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (selectedItems.isNotEmpty()) {
                Text(
                    "Seleccionados",
                    color = MaterialTheme.colorScheme.onSurface,
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
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    placeholder = {
                        Text(
                            "Buscar alimento...",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                )

                FilledTonalButton(
                    onClick = onScan,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Escanear")
                }
            }

            Text(
                "Todos los alimentos",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (loading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = CardBorderSoft,
                shape = MaterialTheme.shapes.medium
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium
    ) {

        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    item.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${item.kcal} kcal • ${item.portionLabel}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            TextButton(onClick = onRemove) {
                Text(
                    "Eliminar",
                    color = MaterialTheme.colorScheme.error
                )
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
    val rot by animateFloatAsState(if (expanded) 180f else 0f, label = "expand")

    Card(
        modifier = Modifier
            .animateContentSize()
            .border(
                1.dp,
                CardBorderSoft,
                MaterialTheme.shapes.medium
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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
                    Text(
                        item.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${item.kcal} kcal • ${item.portionLabel}",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(rot)
                )
            }

            if (expanded) {
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
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
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
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
        Text(
            "Métricas de la porción",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
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
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
            Text(
                value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
