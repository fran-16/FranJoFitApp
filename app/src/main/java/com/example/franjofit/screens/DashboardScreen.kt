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
import androidx.compose.ui.zIndex
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
import com.example.franjofit.ui.theme.BorderBlue
import com.example.franjofit.ui.theme.CardBackground
import com.example.franjofit.ui.theme.CardBorderSoft

import com.example.franjofit.ui.components.StoriesRow
import com.example.franjofit.ui.theme.LightCardBlue

import com.example.franjofit.ui.theme.ScreenBackground
import com.example.franjofit.ui.theme.StoryCardBlue
import com.example.franjofit.ui.theme.TextColorDarkBlue
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset

//Semáforo
enum class SmpColor { GREEN, AMBER, RED }

fun smpColorFrom(score: Int): SmpColor = when {
    score >= 80 -> SmpColor.GREEN
    score >= 60 -> SmpColor.AMBER
    else        -> SmpColor.RED
}


//Dibuja el semáforo
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

    //Dibujamos un foco del semáforo
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
    //Su contenodor principal de todo el semaforo
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

//La tarjeta resumen de smp
@Composable
fun SmpSummaryCard(score: Int, pendingCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderSoft, MaterialTheme.shapes.large),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(8.dp)
            ) {
                TrafficLight(score = score)
            }
            //Dejamos un espacio vacio entre el semáforo y su texto
            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {

            //Este es el texto de resumen del smp
                Text(
                    text = "SMP del día: $score",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(6.dp))


                Text(
                    text = when (smpColorFrom(score)) {
                        SmpColor.GREEN -> "Verde: vas muy bien, mantén fibra/proteína."
                        SmpColor.AMBER -> "Ámbar: OK, ajusta porciones o camina 10–15 min."
                        SmpColor.RED   -> "Rojo: alta carga glucémica hoy, prueba swaps."
                    },
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )


                if (pendingCount > 0) {
                    Spacer(Modifier.height(10.dp))

                    AssistChip(
                        onClick = { /* TODO abrir lista de pendientes */ },
                        label = {
                            Text(
                                text = "📩 $pendingCount pendiente${
                                    if (pendingCount > 1) "s" else ""
                                } a 90’",
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    }
}



enum class MealType { DESAYUNO, ALMUERZO, CENA, EXTRAS }
data class MealItem(val name: String, val kcal: Int)


//Diseño del bot
@Composable
fun SmpAssistantBot(modifier: Modifier = Modifier, onClick: () -> Unit) {
    //Hacemos el efecto del latido del bot
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

    //Cara el bot
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

            Canvas(modifier = Modifier.fillMaxSize()) {

                drawRoundRect(
                    color = Color(0xFF1E1E1E),
                    size = size * 0.9f,
                    cornerRadius = CornerRadius(40f, 40f),
                    topLeft = Offset(size.width * 0.05f, size.height * 0.12f)
                )
                //Son los dos ojos del bot
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
                //Brillo de la cabeza
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
    onOpenReminders: () -> Unit = {},
    mealsChanged: Boolean = false,
    onMealsChangeHandled: () -> Unit = {},
    onOpenSmpBot: () -> Unit = {}
) {
    val uiState = viewModel.ui.collectAsState()
    var selectedIndex by remember { mutableStateOf(0) }

    var meals by remember { mutableStateOf<Map<String, List<Map<String, Any>>>>(emptyMap()) }
    var dailyGoal by remember { mutableStateOf(DailyGoal(baseGoal = uiState.value.baseGoal)) }
    val scope = rememberCoroutineScope()

    // SMP del día
    var smpDay by remember { mutableStateOf(78) }

    //El último peso del usuario
    var lastWeight by remember { mutableStateOf<Float?>(null) }
    //Se usa para mostrar la ventana del ingresar peso
    var showWeightDialog by remember { mutableStateOf(false) }

    //Foto de perfil del usuario
    var fotoMiniUrl by remember {
        mutableStateOf<String?>(FirebaseAuth.getInstance().currentUser?.photoUrl?.toString())
    }
    //Corutina
    LaunchedEffect(Unit) {
        runCatching {
            val perfil = UserRepository.getUserProfileOrNull()
            if (!perfil?.photoUrl.isNullOrBlank()) fotoMiniUrl = perfil.photoUrl
            lastWeight = UserRepository.getLatestWeight()
        }
    }
    //CMABIO

    suspend fun reloadMealsAndGoal() {
        // 1. Comidas de hoy
        meals = FoodRepository.getMealsForToday()

        // 2. Calorías consumidas
        val consumed = meals.values.flatten()
            .sumOf { (it["kcal"] as? Number ?: 0L).toInt() }

        // 3. Obtener meta del día (ya trae smpCurrent si existe)
        val goalFromRepo = GoalsRepository.getDailyGoalOrDefault(uiState.value.baseGoal)
        val base = goalFromRepo.baseGoal

        // 4. Actualizar totales kcal en Firestore (pero NO tocar smpCurrent aquí)
        GoalsRepository.setTotals(base, consumed)

        // 5. Actualizar estado local
        dailyGoal = goalFromRepo.copy(
            consumed = consumed,
            remaining = (base - consumed).coerceAtLeast(0)
            // smpCurrent lo dejamos igual, viene del repo
        )

        // 6. Lo que va a pintar el semáforo
        smpDay = goalFromRepo.smpCurrent ?: 100
    }




//TERMINA CAMBIO


    //Esto se carga al entrar por primera vez a la app
    LaunchedEffect(Unit) { runCatching { reloadMealsAndGoal() } }

    //Se recarga al ir a la pestaña de seguimiento
    LaunchedEffect(selectedIndex) {
        if (selectedIndex == 1) runCatching { reloadMealsAndGoal() }
    }

    //Se recarga cuando agregamos comida
    LaunchedEffect(mealsChanged) {
        if (mealsChanged) {
            runCatching { reloadMealsAndGoal() }
            onMealsChangeHandled() // resetea el flag en el NavHost
        }
    }
    val pendingCount = 0


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
    var botOffset by remember { mutableStateOf(IntOffset(0, 0)) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (selectedIndex) {
                            1 -> "Seguimiento • Hoy"
                            2 -> "Progreso"
                            else -> "Hola ${uiState.value.username}"
                        },
                        color = MaterialTheme.colorScheme.onPrimary
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
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                                        CircleShape
                                    )
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = "Perfil de usuario",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (pendingCount > 0) {
                                Badge {
                                    Text(
                                        pendingCount.toString(),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = onOpenReminders) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Pendientes/recordatorios a 90’",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
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
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.primary,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedIndex == 0,
                    onClick = { selectedIndex = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Principal") },
                    label = { Text("Principal") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = selectedIndex == 1,
                    onClick = { selectedIndex = 1 },
                    icon = { Icon(Icons.Filled.List, contentDescription = "Seguimiento") },
                    label = { Text("Seguimiento") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = selectedIndex == 2,
                    onClick = { selectedIndex = 2 },
                    icon = { Icon(Icons.Filled.Star, contentDescription = "Progreso") },
                    label = { Text("Progreso") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                    )
                )
            }
        }
    )
    { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBackground)
                .padding(padding)
        ) {

            when (selectedIndex) {
                0 -> {
                    val remaining = (dailyGoal.baseGoal - dailyGoal.consumed).coerceAtLeast(0)
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
                        onAddWeightClick = { showWeightDialog = true },
//CAMBIO
                        onEditGoal = { newGoal ->
                            scope.launch {
                                runCatching {
                                    //Actualiza la meta base en Firestore
                                    GoalsRepository.setBaseGoal(newGoal)

                                    //Recalcula calorías consumidas
                                    val consumed = meals.values.flatten()
                                        .sumOf { (it["kcal"] as? Long ?: 0L).toInt() }

                                    //Actualiza los totales del día
                                    GoalsRepository.setTotals(newGoal, consumed)

                                    //Mantenemos el SMP actual (no lo tocamos aquí)
                                    val currentSmp = GoalsRepository.getTodaySmpCurrent()

                                    //Actualiza la UI del Dashboard
                                    dailyGoal = dailyGoal.copy(
                                        baseGoal = newGoal,
                                        consumed = consumed,
                                        remaining = (newGoal - consumed).coerceAtLeast(0),
                                        smpCurrent = currentSmp
                                    )

                                    smpDay = currentSmp
                                }.onFailure {
                                    it.printStackTrace()
                                }
                            }


                            onUpdateBaseGoal(newGoal)
                        },


                        stories = storyItems.take(5),
                        onStoryClick = { index ->
                            startStoryIndex = index
                            showStory = true
                        },
                        onSeeMoreStories = {
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
            SmpAssistantBot(
                modifier = Modifier
                    //Movmiento del bot
                    .align(Alignment.BottomEnd)
                    .offset { botOffset }
                    .padding(end = 20.dp, bottom = 90.dp)
                    .zIndex(3f)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            botOffset = IntOffset(
                                botOffset.x + dragAmount.x.toInt(),
                                botOffset.y + dragAmount.y.toInt()
                            )
                        }
                    }
            ){
                onOpenSmpBot()
            }
        }
    }


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
    onAddWeightClick: () -> Unit,
    onEditGoal: (Int) -> Unit,
    stories: List<StoryItem>,
    onStoryClick: (Int) -> Unit,
    onSeeMoreStories: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        SmpSummaryCard(score = smpScore, pendingCount = pendingCount)

        Spacer(Modifier.height(12.dp))

        CalorieGoalCard(
            baseGoal = baseGoal,
            remaining = remaining,
            onEditBaseGoal = onEditGoal
        )

        Spacer(Modifier.height(12.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            //PASOS
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, CardBorderSoft, MaterialTheme.shapes.medium),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Pasos",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Pasos: $steps",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Meta: $stepsGoal",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
            }

            //EJERCICIO
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, CardBorderSoft, MaterialTheme.shapes.medium),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Ejercicio",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Ejercicio: $exercise cal",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Tiempo: $exerciseMinutes min",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
            }
        }


        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorderSoft, MaterialTheme.shapes.large),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = MaterialTheme.shapes.large
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Peso (últimos 90 días)",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(6.dp))

                if (lastWeight != null) {
                    Text(
                        "Último registro: ${"%.1f".format(lastWeight)} kg",
                        color = MaterialTheme.colorScheme.primary,    // celeste
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                } else {
                    Text(
                        "Aún no registras tu peso",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = onAddWeightClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        "Agregar peso",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }



        Spacer(Modifier.height(18.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .border(1.dp, CardBorderSoft, MaterialTheme.shapes.large),
            colors = CardDefaults.cardColors(
                containerColor = CardBackground
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Column(Modifier.padding(16.dp)) {

                Text(
                    text = "Historias y salud",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(12.dp))

                StoriesRow(
                    items = stories,
                    onClickStory = onStoryClick,
                    onSeeMore = onSeeMoreStories
                )
            }
        }


        Spacer(Modifier.height(40.dp))
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
                remaining = (2200 - totalKcal).coerceAtLeast(0)
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
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderSoft, MaterialTheme.shapes.large),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Calorías del día",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Consumidas",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                    Text(
                        "$totalKcal kcal",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Restantes",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                    Text(
                        "${remaining.coerceAtLeast(0)} kcal",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
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
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderSoft, MaterialTheme.shapes.large),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(16.dp)) {


            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "$total kcal",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = onAdd,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        "Agregar",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (items.isEmpty()) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        )
                        .padding(14.dp)
                ) {
                    Text(
                        "Sin ítems aún",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            } else {
                items.forEachIndexed { index, item ->
                    MealRow(item)
                    if (index != items.lastIndex) {
                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                    }
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
        Text(
            item.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp
        )
        Text(
            "${item.kcal} kcal",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            fontSize = 14.sp
        )
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
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderSoft, MaterialTheme.shapes.large),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
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
                CalorieLavaRing(
                    progress = progress,
                    bgAlpha = 0.20f,
                    color = MaterialTheme.colorScheme.primary
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$remaining",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "kcal\nrestantes",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {

                Text(
                    "Calorías",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Objetivo base:",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Text(
                        "$baseGoal kcal",
                        color = MaterialTheme.colorScheme.primary, // CELESTE
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable { showDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    "Consumido: ${(progress * baseGoal).toInt()} kcal",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Text(
                    "Restantes: $remaining kcal",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
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
    bgAlpha: Float = 0.15f,
    color: Color
) {
    val infinite = rememberInfiniteTransition()
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        )
    )

    val wavePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing)
        )
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "ringProgress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {

        val stroke = 16.dp.toPx()
        val sizeArc = Size(size.minDimension - stroke, size.minDimension - stroke)
        val topLeft = Offset(
            (this.size.width - sizeArc.width) / 2,
            (this.size.height - sizeArc.height) / 2
        )


        drawArc(
            color = color.copy(alpha = bgAlpha * 0.6f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = sizeArc,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        val brush = Brush.sweepGradient(
            colors = listOf(
                color.copy(alpha = 0.20f),
                color.copy(alpha = 0.90f),
                color.copy(alpha = 0.45f),
                color.copy(alpha = 0.90f)
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
            val angle = (-90f + animatedProgress * 360f) * (Math.PI / 180).toFloat()
            val edgeX = cx + r * cos(angle)
            val edgeY = cy + r * sin(angle)

            val path = Path().apply {
                moveTo(edgeX, edgeY)
                val wobble = 8.dp.toPx() * sin(wavePhase)
                relativeQuadraticBezierTo(-wobble, -wobble, -wobble * 2, wobble)
            }

            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    listOf(
                        color,
                        color.copy(alpha = 0.4f)
                    )
                ),
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