package com.example.franjofit.screens

import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.franjofit.components.story.StoryItem
import com.example.franjofit.components.story.StoryViewerModal
import com.example.franjofit.data.FoodRepository
import com.example.franjofit.data.GoalsRepository
import com.example.franjofit.data.DailyGoal
import com.example.franjofit.data.UserRepository
import com.example.franjofit.ui.theme.DeepBlue
import com.example.franjofit.ui.theme.Orange
import com.example.franjofit.ui.theme.White
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import com.example.franjofit.ui.components.StoriesRow


// =========================
// NUEVO: SMP (Score) + Semáforo
// =========================
enum class SmpColor { GREEN, AMBER, RED }

fun smpColorFrom(score: Int): SmpColor = when {
    score >= 80 -> SmpColor.GREEN
    score >= 60 -> SmpColor.AMBER
    else        -> SmpColor.RED
}

/** Semáforo vertical con 3 luces. */
@Composable
fun TrafficLight(
    score: Int,
    modifier: Modifier = Modifier
) {
    val active = smpColorFrom(score)
    val redOn   = Color(0xFFD32F2F)
    val amberOn = Color(0xFFFFA000)
    val greenOn = Color(0xFF2E7D32)
    val off     = Color(0xFF2B2B2B)
    val housing = Color(0xFF1C1C1C)

    @Composable
    fun Bulb(on: Boolean, onColor: Color, contentDesc: String) {
        val glowAlpha by animateFloatAsState(targetValue = if (on) 0.35f else 0f, label = "glow")
        Box(
            modifier = Modifier
                .size(72.dp)
                .padding(vertical = 6.dp)
                .semantics { this.contentDescription = contentDesc }
                .drawBehind {
                    if (on && glowAlpha > 0f) {
                        drawCircle(
                            color = onColor.copy(alpha = glowAlpha),
                            radius = size.minDimension / 1.2f
                        )
                    }
                }
                .clip(CircleShape)
                .background(
                    brush = if (on) Brush.radialGradient(
                        colors = listOf(
                            onColor,
                            onColor.copy(alpha = 0.85f),
                            onColor.copy(alpha = 0.45f)
                        )
                    ) else Brush.radialGradient(colors = listOf(off, off))
                )
                .border(2.dp, Color.Black.copy(alpha = 0.6f), CircleShape)
        )
    }

    Box(
        modifier = modifier
            .width(96.dp)
            .wrapContentHeight()
            .background(housing, shape = RoundedCornerShape(16.dp))
            .border(2.dp, Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Bulb(on = active == SmpColor.RED,   onColor = redOn,   contentDesc = "Luz roja")
            Bulb(on = active == SmpColor.AMBER, onColor = amberOn, contentDesc = "Luz ámbar")
            Bulb(on = active == SmpColor.GREEN, onColor = greenOn, contentDesc = "Luz verde")
        }
    }
}

/** Tarjeta SMP + semáforo. */
@Composable
fun SmpSummaryCard(
    score: Int,
    pendingCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White.copy(alpha = 0.15f)),
        shape = MaterialTheme.shapes.large
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            TrafficLight(score = score)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("SMP del día: $score", color = White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = when (smpColorFrom(score)) {
                        SmpColor.GREEN -> "Verde: vas muy bien, mantén fibra/proteína."
                        SmpColor.AMBER -> "Ámbar: OK, pero ajusta porciones o camina 10–15’."
                        SmpColor.RED   -> "Rojo: alta carga glucémica hoy, prueba swaps."
                    },
                    color = White.copy(0.9f),
                    fontSize = 13.sp
                )
                if (pendingCount > 0) {
                    Spacer(Modifier.height(10.dp))
                    AssistChip(
                        onClick = { /* TODO */ },
                        label = { Text("📩 $pendingCount pendiente${if (pendingCount>1) "s" else ""} a 90’") },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = White,
                            containerColor = White.copy(alpha = 0.10f)
                        )
                    )
                }
            }
        }
    }
}

// =========================
// Tu código (con avatar mini en AppBar)
// =========================
enum class MealType { DESAYUNO, ALMUERZO, CENA, EXTRAS }
data class MealItem(val name: String, val kcal: Int)
@Composable
fun SmpAssistantBot(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val pulse by rememberInfiniteTransition().animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = modifier
            .size(90.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ⭐ Círculo blanco con sombra
        Box(
            modifier = Modifier
                .size(70.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, Color(0xFFE0E0E0), CircleShape)
                .shadow(12.dp, CircleShape)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {

            // ⭐ Robot 3D super simple (ojos + brillo)
            Canvas(modifier = Modifier.fillMaxSize()) {

                // Cabeza ovalada
                drawRoundRect(
                    color = Color(0xFF1E1E1E),
                    size = size * 0.9f,
                    cornerRadius = CornerRadius(40f, 40f),
                    topLeft = Offset(size.width * 0.05f, size.height * 0.12f)
                )

                // Ojos
                drawCircle(
                    color = Color.Cyan,
                    radius = size.width * 0.12f,
                    center = Offset(size.width * 0.35f, size.height * 0.48f)
                )
                drawCircle(
                    color = Color.Cyan,
                    radius = size.width * 0.12f,
                    center = Offset(size.width * 0.65f, size.height * 0.48f)
                )

                // Reflejo tipo 3D
                drawCircle(
                    color = Color.White.copy(alpha = 0.22f),
                    radius = size.width * 0.35f,
                    center = Offset(size.width * 0.52f, size.height * 0.30f)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            "SMP Bot",
            color = Color.White,
            fontSize = 12.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onAddWeight: () -> Unit,
    onOpenProfile: () -> Unit = {},
    onOpenAddMeal: (String) -> Unit = {},
    onUpdateBaseGoal: (Int) -> Unit = {},
    onOpenReminders: () -> Unit = {}
) {
    val uiState = viewModel.ui.collectAsState()
    var selectedIndex by remember { mutableStateOf(0) }

    var meals by remember { mutableStateOf<Map<String, List<Map<String, Any>>>>(emptyMap()) }
    var dailyGoal by remember { mutableStateOf(DailyGoal(baseGoal = uiState.value.baseGoal)) }
    val scope = rememberCoroutineScope()

    // SMP del día (mock por ahora)
    var smpDay by remember { mutableStateOf(78) }

    // 🔥 Peso
    var lastWeight by remember { mutableStateOf<Float?>(null) }
    var showWeightDialog by remember { mutableStateOf(false) }




    // ===== Avatar mini: trae photoUrl (Firestore/Storage) o de Auth y úsalo en el AppBar
    var fotoMiniUrl by remember {
        mutableStateOf<String?>(FirebaseAuth.getInstance().currentUser?.photoUrl?.toString())
    }
    LaunchedEffect(Unit) {
        runCatching {
            val perfil = UserRepository.getUserProfileOrNull()
            if (!perfil?.photoUrl.isNullOrBlank()) fotoMiniUrl = perfil.photoUrl
            lastWeight = UserRepository.getLatestWeight()
        }
    }

    suspend fun reloadMealsAndGoal() {
        meals = FoodRepository.getMealsForToday()
        val consumed = meals.values.flatten().sumOf { (it["kcal"] as? Long ?: 0L).toInt() }
        val goal = GoalsRepository.getDailyGoalOrDefault(uiState.value.baseGoal)
        val base = goal.baseGoal
        GoalsRepository.setTotals(base, consumed)
        dailyGoal = DailyGoal(baseGoal = base, consumed = consumed)
        smpDay = calculateDailySmpPredicted(meals)
        lastWeight = UserRepository.getLatestWeight()

    }

    LaunchedEffect(Unit) { runCatching { reloadMealsAndGoal() } }
    LaunchedEffect(selectedIndex) { if (selectedIndex == 1) runCatching { reloadMealsAndGoal() } }

    val pendingCount = 0

    // ======== Historias / Artículos ========
    var showStory by remember { mutableStateOf(false) }
    var startStoryIndex by remember { mutableStateOf(0) }

    val storyItems = listOf(
        StoryItem(
            "Resistencia a la Insulina",
            "https://nutribiotica.es/wp-content/webpc-passthru.php?src=https://nutribiotica.es/wp-content/uploads/2023/05/43.resistencia-insulina.jpg&nocache=1",
            "La resistencia a la insulina es una condición en la que las células del cuerpo no responden adecuadamente a esta hormona, dificultando el control de la glucosa. Entre sus señales más comunes se encuentran el aumento de grasa abdominal, el cansancio persistente y el oscurecimiento de pliegues de la piel, como en el cuello o axilas. También puede presentarse un aumento del apetito y antojos frecuentes de carbohidratos. Identificar estos signos tempranamente permite intervenir con cambios en el estilo de vida y evitar la progresión hacia prediabetes o diabetes tipo 2."
        ),
        StoryItem(
            "Diabetes Tipo 2",
            "https://irp.cdn-website.com/a979b4f7/dms3rep/multi/DIABETES+TIPO+2.jpg",
            "La diabetes tipo 2 suele desarrollarse de manera progresiva y muchas veces pasa desapercibida en sus primeras etapas. Entre sus señales más frecuentes se encuentran la sed excesiva, el aumento de la micción y la fatiga persistente. También es común experimentar visión borrosa, heridas que tardan en sanar y hormigueos en manos o pies. Algunas personas notan pérdida de peso sin razón aparente o infecciones recurrentes. Reconocer estos síntomas permite buscar atención médica temprana y evitar complicaciones a largo plazo."
        ),
        StoryItem(
            "Índice y Carga Glucémica",
            "https://tse3.mm.bing.net/th/id/OIP.yWbAE_eD0HrvcLpPnf5_swHaDt?rs=1&pid=ImgDetMain&o=7&rm=3",
            "El índice glucémico (IG) mide la velocidad con la que un alimento eleva la glucosa en sangre, clasificándolo como bajo, medio o alto. Sin embargo, el IG no considera la cantidad real de carbohidratos que se consume. Por eso existe la carga glucémica (CG), que combina el IG con la porción ingerida, ofreciendo una visión más precisa del impacto glucémico. Alimentos con IG alto pueden tener una CG baja si la porción es pequeña, y viceversa. Controlar ambos valores ayuda a elegir carbohidratos que mantengan niveles estables de glucosa y mejoren la respuesta metabólica."
        ),
        StoryItem(
            "Inflamación Crónica",
            "https://www.fawellness.net/upload/noticias/109/inflamacion-cronica.jpg",
            "La inflamación crónica es una respuesta persistente del sistema inmunológico que permanece activa incluso cuando no hay una amenaza real. Puede originarse por estrés, mala alimentación, falta de sueño, obesidad o infecciones no resueltas. Con el tiempo, este estado inflamatorio contribuye al desarrollo de enfermedades como diabetes tipo 2, hígado graso, hipertensión y trastornos autoinmunes. Entre sus señales comunes están el cansancio continuo, dolores musculares, problemas digestivos y niebla mental. Reducirla implica mejorar hábitos de sueño, manejar el estrés y priorizar una alimentación rica en vegetales y antioxidantes."
        ),
        StoryItem(
            "Síndrome Metabólico",
            "https://www.escuelaculturismonatural.com/wp-content/uploads/2020/08/SINDROME-METABOLICO.jpg",
            "El síndrome metabólico es un conjunto de alteraciones que aumentan significativamente el riesgo de diabetes tipo 2 y enfermedades cardiovasculares. Se caracteriza por la presencia de al menos tres de estos factores: obesidad abdominal, presión arterial elevada, triglicéridos altos, colesterol HDL bajo y niveles alterados de glucosa. Este conjunto de desequilibrios suele relacionarse con resistencia a la insulina y hábitos de vida poco saludables. Entre sus signos frecuentes están el cansancio, aumento progresivo de peso y cambios en el metabolismo. Identificarlo temprano permite realizar ajustes en la alimentación, actividad física y manejo del estrés para prevenir complicaciones."
        ),
        StoryItem(
            "Historia 6",
            "https://via.placeholder.com/600x300.png?text=Historia+6",
            "Texto de ejemplo historia 6..."
        ),
        StoryItem(
            "Historia 7",
            "https://via.placeholder.com/600x300.png?text=Historia+7",
            "Texto de ejemplo historia 7..."
        )
    )


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
                        if (!fotoMiniUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = fotoMiniUrl,
                                contentDescription = "Perfil de usuario",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, White.copy(0.6f), CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = "Perfil de usuario",
                                tint = White
                            )
                        }
                    }
                },
                actions = {
                    BadgedBox(
                        badge = { if (pendingCount > 0) Badge { Text(pendingCount.toString()) } }
                    ) {
                        IconButton(onClick = onOpenReminders) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Pendientes/recordatorios a 90’"
                            )
                        }
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
            // ⭐⭐⭐ AÑADE ESTE BLOQUE AQUÍ (ANTES DEL WHEN)
            SmpAssistantBot(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 90.dp)
            ) {
                // Acción al tocar el bot (por ahora abre un "modo chat")
                selectedIndex = 99
            }
            when (selectedIndex) {
                0 -> {
                    val remaining = (dailyGoal.baseGoal - dailyGoal.consumed).coerceAtLeast(0)
                    val totalGl = remember(meals) {
                        meals.values.flatten().sumOf { (it["gl"] as? Double) ?: 0.0 }
                    }
                    PrincipalContent(
                        smpScore = smpDay,
                        pendingCount = pendingCount,
                        baseGoal = dailyGoal.baseGoal,
                        food = dailyGoal.consumed,
                        exercise = uiState.value.exercise,
                        remaining = remaining,
                        steps = uiState.value.steps,
                        stepsGoal = uiState.value.stepsGoal,
                        exerciseMinutes = uiState.value.exerciseMinutes,
                        lastWeight = lastWeight,
                        totalGl = totalGl,  // AÑADIDO
                        onAddWeightClick = { showWeightDialog = true },

                        onEditGoal = { newGoal ->
                            scope.launch {
                                runCatching {
                                    GoalsRepository.setBaseGoal(newGoal)
                                    val consumed = meals.values.flatten()
                                        .sumOf { (it["kcal"] as? Long ?: 0L).toInt() }
                                    GoalsRepository.setTotals(newGoal, consumed)
                                    dailyGoal = DailyGoal(baseGoal = newGoal, consumed = consumed)
                                    smpDay = calculateDailySmpPredicted(meals)
                                }
                            }
                            onUpdateBaseGoal(newGoal)
                        },
                        stories = storyItems.take(5),                       // 👈 pasas los StoryItem completos ,      // 👈 títulos de las 5 primeras
                        onStoryClick = { index ->                           // 👈 abre modal en historia index
                            startStoryIndex = index
                            showStory = true
                        },
                        onSeeMoreStories = {                                // 👈 abre desde la 6ta
                            startStoryIndex = 5
                            showStory = true
                        }
                    )
                }

                1 -> TrackingContent(
                    meals = meals,
                    onAddItem = { mealType -> onOpenAddMeal(mealType) }
                )
                2 -> ProgressScreen()

            }
            StoryViewerModal(
                visible = showStory,
                items = storyItems,
                startIndex = startStoryIndex,
                onClose = { showStory = false }
            )
        }
    }
    // ====================== DIÁLOGO AGREGAR PESO ======================
    if (showWeightDialog) {
        var text by remember { mutableStateOf("") }
        var saving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!saving) showWeightDialog = false },
            title = { Text("Agregar peso") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.all { ch -> ch.isDigit() || ch == '.' }) text = it },
                    placeholder = { Text("Ej: 72.4 kg") }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !saving,
                    onClick = {
                        val w = text.toFloatOrNull()
                        if (w != null && w > 25f && w < 350f) {
                            saving = true
                            scope.launch {
                                UserRepository.addWeightRecord(w)
                                lastWeight = w
                                showWeightDialog = false
                                saving = false
                            }
                        }
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { showWeightDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

}


/** MOCK SMP: sustituir por cálculo real. */

/**
 * 🔬 NUEVO BLOQUE SMP — CÁLCULO PREDICHO REAL
 * Sustituye el antiguo calculateDailySmpMock
 */
private const val DEFAULT_IG = 55.0
private const val MAX_FIBER_BONUS = 10.0
private const val MAX_PROT_BONUS  = 10.0
private const val GL_COEF = 1.5
private const val IG_COEF = 0.5
private const val KCAL_SOFT_CAP = 650.0
private const val KCAL_PEN_STEP = 50.0
private const val KCAL_PEN_PER_STEP = 1.0
private const val KCAL_PEN_MAX = 12.0

private data class MealMetrics(
    val igPlate: Double,
    val glTotal: Double,
    val carbs: Double,
    val protein: Double,
    val fiber: Double,
    val kcal: Double
)

private fun Map<String, Any>.num(key: String): Double {
    val v = this[key]
    return when (v) {
        is Number -> v.toDouble()
        is String -> v.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }
}

private fun computeMealMetrics(items: List<Map<String, Any>>): MealMetrics {
    var carbs = 0.0
    var prot  = 0.0
    var fiber = 0.0
    var kcal  = 0.0
    var glTot = 0.0
    var igWeightedNum = 0.0

    items.forEach { m ->
        val ig = m.num("ig").takeIf { it > 0 } ?: DEFAULT_IG
        val gl = m.num("gl")

        // 🔥 FIX: estas son las llaves correctas
        val c  = m.num("carbs_g")
        val p  = m.num("protein_g")
        val f  = m.num("fiber_g")

        val k  = m.num("kcal")

        carbs += c
        prot  += p
        fiber += f
        kcal  += k
        glTot += gl
        igWeightedNum += ig * c
    }

    val igPlate = if (carbs > 0.0) igWeightedNum / carbs else DEFAULT_IG
    return MealMetrics(igPlate, glTot, carbs, prot, fiber, kcal)
}

private data class MealSmp(
    val score: Int,
    val reasons: List<String> = emptyList()
)

private fun smpForMeal(metrics: MealMetrics): MealSmp {
    val reasons = mutableListOf<String>()

    val penGL = metrics.glTotal * GL_COEF
    val penIG = metrics.igPlate * IG_COEF

    val fiberBonus = (metrics.fiber.coerceAtMost(10.0) / 10.0) * MAX_FIBER_BONUS
    val protBonus  = (metrics.protein.coerceAtMost(25.0) / 25.0) * MAX_PROT_BONUS

    if (fiberBonus >= 1.0) reasons += "+${fiberBonus.toInt()} fibra"
    if (protBonus  >= 1.0) reasons += "+${protBonus.toInt()} proteína"

    val kcalPen = if (metrics.kcal > KCAL_SOFT_CAP) {
        val steps = ((metrics.kcal - KCAL_SOFT_CAP) / KCAL_PEN_STEP)
        (steps * KCAL_PEN_PER_STEP).coerceAtMost(KCAL_PEN_MAX)
    } else 0.0
    if (kcalPen >= 1.0) reasons += "−${kcalPen.toInt()} kcal altas"

    var score = 100.0 - penGL - penIG + fiberBonus + protBonus - kcalPen
    score = score.coerceIn(0.0, 100.0)
    return MealSmp(score.toInt(), reasons)
}

/** SMP diario ponderado por kcal (predicho) */
private fun calculateDailySmpPredicted(
    meals: Map<String, List<Map<String, Any>>>
): Int {
    if (meals.isEmpty()) return 100
    var totalKcal = 0.0
    var weighted = 0.0
    meals.values.forEach { list ->
        if (list.isEmpty()) return@forEach
        val metrics = computeMealMetrics(list)
        val smp = smpForMeal(metrics)
        weighted += smp.score * metrics.kcal
        totalKcal += metrics.kcal
    }
    return if (totalKcal > 0) (weighted / totalKcal).toInt() else 100
}


@Composable
private fun PrincipalContent(
    smpScore: Int,
    pendingCount: Int,
    baseGoal: Int,
    food: Int,
    exercise: Int,
    remaining: Int,
    steps: Int,
    stepsGoal: Int,
    exerciseMinutes: Int,
    lastWeight: Float?,
    totalGl: Double,  // AÑADIDO
    onAddWeightClick: () -> Unit,
    onEditGoal: (Int) -> Unit,
    stories: List<StoryItem>,
    onStoryClick: (Int) -> Unit,
    onSeeMoreStories: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())   // 🔥🔥🔥 SCROLL VERTICAL
            .padding(16.dp)
    ) {

        // 🔥 Tarjeta SMP
        SmpSummaryCard(score = smpScore, pendingCount = pendingCount)

        Spacer(Modifier.height(12.dp))

        // 🔥 Meta calórica
        CalorieGoalCard(
            baseGoal = baseGoal,
            remaining = remaining,
            onEditBaseGoal = onEditGoal
        )

        Spacer(Modifier.height(12.dp))

// EXTRA: GL TOTAL DEL DÍA
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = White.copy(alpha = 0.10f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "GL total del día",
                    color = White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium)
                Text(
                    text = String.format("%.1f", totalGl),
                    color = Orange,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 🔥 Pasos + ejercicio
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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

        // 🔥 Peso
        // 🔥 Peso con último registro mostrado
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = White.copy(alpha = 0.15f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Peso (últimos 90 días)", color = White)

                Spacer(Modifier.height(6.dp))

                if (lastWeight != null) {
                    Text(
                        "Último registro: ${"%.1f".format(lastWeight)} kg",
                        color = Orange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                } else {
                    Text("Aún no registras tu peso", color = White.copy(0.8f))
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = onAddWeightClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange)
                ) {
                    Text("Agregar peso", color = White)
                }
            }
        }


        Spacer(Modifier.height(18.dp))

        // ⭐⭐⭐ Carrusel de historias
        StoriesRow(
            items = stories,
            onClickStory = onStoryClick,
            onSeeMore = onSeeMoreStories
        )



        Spacer(Modifier.height(40.dp)) // un poco de espacio al final
    }
}

@Composable
private fun TrackingContent(
    meals: Map<String, List<Map<String, Any>>>,
    onAddItem: (String) -> Unit
) {
    val totalKcal = remember(meals) {
        meals.values.flatten().sumOf { (it["kcal"] as? Long)?.toInt() ?: 0 }
    }
    val totalGl = remember(meals) {
        meals.values.flatten().sumOf { (it["gl"] as? Double) ?: 0.0 }
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
                remaining = (2200 - totalKcal).coerceAtLeast(0),
                totalGl = totalGl
            )
        }

        listOf("desayuno", "almuerzo", "cena", "extras").forEach { type ->
            item {
                val list = meals[type].orEmpty()
                MealSectionCard(
                    title = type.replaceFirstChar { it.uppercase() },
                    items = list,  // <-- directo, sin mapear a MealItem
                    onAdd = { onAddItem(type) }
                )
            }
        }
    }
}

@Composable
private fun DayHeaderCard(totalKcal: Int, remaining: Int, totalGl: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Resumen del día", color = White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("GL total", color = White.copy(0.9f), fontSize = 13.sp)
                    Text(String.format("%.1f", totalGl), color = Orange, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Calorías", color = White.copy(0.9f), fontSize = 13.sp)
                    Text("$totalKcal kcal", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MealSectionCard(
    title: String,
    items: List<Map<String, Any>>,
    onAdd: () -> Unit
) {
    val totalGl = items.sumOf { (it["gl"] as? Double) ?: 0.0 }
    val totalKcal = items.sumOf { (it["kcal"] as? Long)?.toInt() ?: 0 }

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
                    Text(
                        "GL total: ${String.format("%.1f", totalGl)}",
                        color = Orange,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
                Button(
                    onClick = onAdd,
                    colors = ButtonDefaults.buttonColors(containerColor = Orange)
                ) {
                    Text("Agregar", color = White)
                }
            }

            Spacer(Modifier.height(12.dp))

            if (items.isEmpty()) {
                Text("Sin ítems aún", color = White.copy(0.7f), fontSize = 14.sp)
            } else {
                items.forEach { item ->
                    MealRow(item)
                    if (item != items.last()) {
                        Divider(color = White.copy(0.1f), modifier = Modifier.padding(vertical = 6.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "$totalKcal kcal",
                color = White.copy(0.6f),
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun MealRow(item: Map<String, Any>) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = item["name"] as? String ?: "Alimento",
                color = White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Row {
                Text(
                    text = "IG ${item["ig"]}",
                    color = Orange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(20.dp))
                Text(
                    text = "GL ${(item["gl"] as? Double)?.let { String.format("%.1f", it) } ?: "0"}",
                    color = Orange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${item["kcal"]} kcal",
                color = White.copy(0.6f),
                fontSize = 12.sp
            )
            Text(
                text = item["portion_text"] as? String ?: "",
                color = White.copy(0.7f),
                fontSize = 11.sp
            )
        }
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
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(130.dp), contentAlignment = Alignment.Center) {
                CalorieLavaRing(progress = progress, bgAlpha = 0.2f)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$remaining", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
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
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing))
    )
    val wavePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(1800, easing = LinearEasing))
    )
    val animatedProgress by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), animationSpec = tween(600), label = "ringProgress")

    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = 16.dp.toPx()
        val sizeArc = Size(size.minDimension - stroke, size.minDimension - stroke)
        val topLeft = Offset((this.size.width - sizeArc.width) / 2, (this.size.height - sizeArc.height) / 2)

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
            colors = listOf(Orange.copy(alpha = 0.2f), Orange, Orange.copy(alpha = 0.6f), Orange)
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
                onValueChange = { new -> if (new.all { it.isDigit() } && new.length <= 5) text = new },
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
