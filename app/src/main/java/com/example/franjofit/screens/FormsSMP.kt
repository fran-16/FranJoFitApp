package com.example.franjofit.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.franjofit.data.GoalsRepository
import kotlinx.coroutines.launch

data class SymptomOption(
    val id: String,
    val label: String,
    val description: String,
    val scoreDelta: Int   // cuánto suma (+) o resta (−) al SMP
)

/**
 * Entry point del formulario SMP:
 * - Lee el SMP actual de Firestore (smpCurrent o 100 por defecto)
 * - Renderiza FormsSMPScreen con ese baseScore
 * - Al guardar, actualiza smpCurrent en Firestore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormsSmpScreenEntry(
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var baseScore by remember { mutableStateOf(100) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            baseScore = GoalsRepository.getTodaySmpCurrentOrDefault(100)
        } catch (e: Exception) {
            e.printStackTrace()
            baseScore = 100
        } finally {
            loading = false
        }
    }

    if (loading) {
        //Pantalla de carga sencilla mientras se obtiene el SMP base
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Síntomas postprandiales") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver"
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    } else {
        FormsSMPScreen(
            baseScore = baseScore,
            onSubmit = { newScore ->
                scope.launch {
                    try {
                        //Se guarda el nuevo smp luego de los sintomas
                        GoalsRepository.updateTodaySmpCurrent(newScore)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
            onBack = onBack
        )
    }
}

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormsSMPScreen(
    baseScore: Int,
    onSubmit: (newScore: Int) -> Unit,
    onBack: () -> Unit = {}
) {
    //Lista de síntomas con deltas
    val symptoms = listOf(
        SymptomOption(
            id = "energia_buena",
            label = "Buena energía, te sientes normal o incluso mejor",
            description = "Sensación general de bienestar, sin pesadez.",
            scoreDelta = +6
        ),
        SymptomOption(
            id = "concentracion_buena",
            label = "Buena concentración / sin niebla mental",
            description = "Puedes seguir trabajando/estudiando sin problema.",
            scoreDelta = +3
        ),
        SymptomOption(
            id = "saciado_3h",
            label = "Te mantuviste saciado ~3 horas sin hambre intensa",
            description = "No necesitaste picar nada a la hora o a las 2 horas.",
            scoreDelta = +5
        ),
        SymptomOption(
            id = "somnolencia_leve",
            label = "Ligera somnolencia o pesadez, pero manejable",
            description = "Algo de sueño o bajón, pero no te tumba.",
            scoreDelta = -4
        ),
        SymptomOption(
            id = "antojos_leves",
            label = "Antojos leves de dulce / pan, pero los controlas",
            description = "Te provoca algo, pero no se vuelve urgente.",
            scoreDelta = -3
        ),
        SymptomOption(
            id = "malestar_digestivo_leve",
            label = "Ligero malestar digestivo (inflamación, gases leves)",
            description = "Sensación molesta pero no incapacitante.",
            scoreDelta = -4
        ),
        SymptomOption(
            id = "sueno_intenso",
            label = "Sueño intenso / bostezo constante después de comer",
            description = "Necesidad fuerte de acostarte o dejar la actividad.",
            scoreDelta = -8
        ),
        SymptomOption(
            id = "hambre_fuerte",
            label = "Hambre fuerte antes de 2 horas de la comida",
            description = "Necesitas comer de nuevo muy pronto.",
            scoreDelta = -7
        ),
        SymptomOption(
            id = "mareos_niebla",
            label = "Mareos, niebla mental o sensación rara en la cabeza",
            description = "Dificultad para concentrarte, aturdimiento.",
            scoreDelta = -10
        ),
        SymptomOption(
            id = "palpitaciones_ansiedad",
            label = "Palpitaciones, ansiedad o nerviosismo marcado",
            description = "Te sientes acelerado o inquieto sin razón clara.",
            scoreDelta = -10
        ),
        SymptomOption(
            id = "malestar_digestivo_fuerte",
            label = "Malestar digestivo fuerte / náuseas / diarrea",
            description = "Síntomas intensos que afectan tus actividades.",
            scoreDelta = -12
        )
    )

    val selected = remember { mutableStateMapOf<String, Boolean>() }

    // Cálculo en vivo del SMP estimado con síntomas
    val totalDelta by derivedStateOf {
        symptoms.sumOf { s -> if (selected[s.id] == true) s.scoreDelta else 0 }
    }
    val estimatedScore by derivedStateOf {
        (baseScore + totalDelta).coerceIn(0, 100)
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Síntomas postprandiales") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Text(
                    text = "Marca lo que hayas sentido aprox. 90 minutos después de esta comida.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "SMP base (antes de síntomas): $baseScore",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF8B0000)
                )

                Text(
                    text = "SMP estimado con síntomas: $estimatedScore",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFB71C1C)
                )

                Spacer(Modifier.height(8.dp))

                symptoms.forEach { symptom ->
                    val checked = selected[symptom.id] ?: false
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    selected[symptom.id] = isChecked
                                }
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = symptom.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = symptom.description,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Impacto en SMP: ${if (symptom.scoreDelta > 0) "+" else ""}${symptom.scoreDelta}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (symptom.scoreDelta >= 0) Color(0xFF2E7D32) else Color(
                                        0xFFB71C1C
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onSubmit(estimatedScore)
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Guardar y actualizar SMP")
            }
        }
    }
}
