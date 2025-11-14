package com.example.franjofit.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.franjofit.BuildConfig
import com.example.franjofit.data.FoodRepository
import com.example.franjofit.data.recognizeWithOpenAI
import com.example.franjofit.ui.theme.DeepBlue
import com.example.franjofit.ui.theme.Orange
import com.example.franjofit.ui.theme.White
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanFoodScreen(
    onCancel: () -> Unit = {},
    onUseResult: (name: String, kcal: Int, portion: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var labels by remember { mutableStateOf<List<LabeledFood>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Cargar catálogo (para métricas completas)
    var catalog by remember { mutableStateOf<List<FoodRepository.CatalogUiItem>>(emptyList()) }
    LaunchedEffect(Unit) {
        runCatching { FoodRepository.listCatalogForUi(context) }
            .onSuccess { catalog = it }
    }

    // Cámara
    val takePreviewLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            preview = bmp
            scope.launch {
                analyzeWithOpenAI(
                    bmp,
                    onResult = { labels = it },
                    onError = { errorMsg = it },
                    setBusy = { isProcessing = it }
                )
            }
        }
    }

    val requestCameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) takePreviewLauncher.launch(null)
        else errorMsg = "Permiso de cámara denegado"
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bmp: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }

                preview = bmp
                scope.launch {
                    analyzeWithOpenAI(
                        bmp,
                        onResult = { labels = it },
                        onError = { errorMsg = it },
                        setBusy = { isProcessing = it }
                    )
                }
            } catch (e: Exception) {
                errorMsg = "No se pudo leer la imagen: ${e.message}"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escanear alimento") },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Cancelar") } }
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { requestCameraPermission.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = Orange)
                ) { Text("Tomar foto", color = White) }

                OutlinedButton(
                    onClick = {
                        pickMediaLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) { Text("Galería", color = White) }
            }

            preview?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }

            if (isProcessing) LinearProgressIndicator(Modifier.fillMaxWidth())
            errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Text("Resultados", color = White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

            if (labels.isEmpty() && !isProcessing) {
                Text("Toma una foto o elige de la galería.", color = White.copy(0.8f))
            } else {
                labels.take(5).forEach { item ->

                    val match = catalog.firstOrNull {
                        it.name.equals(item.displayName, ignoreCase = true)
                    }

                    ScanResultCard(
                        detected = item,
                        catalogItem = match,
                        onUse = {
                            val kcal = match?.preview?.kcal ?: item.kcalEstimate
                            val portion = match?.portionLabel ?: item.portion
                            val displayName = match?.name ?: item.displayName

                            onUseResult(displayName, kcal, portion)
                        }
                    )

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}


// ==== MODELO ====

private data class LabeledFood(
    val displayName: String,
    val confidence: Float,
    val kcalEstimate: Int,
    val portion: String
)


// ==== LÓGICA OPENAI ====

private suspend fun analyzeWithOpenAI(
    bmp: Bitmap,
    onResult: (List<LabeledFood>) -> Unit,
    onError: (String) -> Unit,
    setBusy: (Boolean) -> Unit
) {
    try {
        setBusy(true)

        if (BuildConfig.OPENAI_API_KEY.isBlank()) {
            onError("OPENAI_API_KEY vacío. Configúralo.")
            return
        }

        val raw = recognizeWithOpenAI.analyzeImage(bmp, BuildConfig.OPENAI_API_KEY)

        val textContent = runCatching {
            val json = org.json.JSONObject(raw)
            json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }.getOrElse { raw }

        val cleanText = textContent
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val parsed = mutableListOf<LabeledFood>()
        val okJson = runCatching {
            val arr = org.json.JSONArray(cleanText)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                parsed.add(
                    LabeledFood(
                        displayName = o.optString("nombre", "Desconocido"),
                        confidence = o.optDouble("confianza", 0.9).toFloat(),
                        kcalEstimate = o.optInt("kcal", 100),
                        portion = o.optString("porcion", "100 g")
                    )
                )
            }
            true
        }.getOrDefault(false)

        if (!okJson) {
            parsed.add(
                LabeledFood(
                    displayName = cleanText.take(60),
                    confidence = 0.9f,
                    kcalEstimate = 100,
                    portion = "100 g"
                )
            )
        }

        onResult(parsed)

    } catch (e: Exception) {
        onError("Error OpenAI: ${e.message}")
    } finally {
        setBusy(false)
    }
}


// ==== CARD ====

@Composable
private fun ScanResultCard(
    detected: LabeledFood,
    catalogItem: FoodRepository.CatalogUiItem?,
    onUse: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rot by animateFloatAsState(if (expanded) 180f else 0f)

    val kcal = catalogItem?.preview?.kcal ?: detected.kcalEstimate
    val portion = catalogItem?.portionLabel ?: detected.portion

    Card(
        colors = CardDefaults.cardColors(containerColor = White.copy(0.15f)),
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {

        Column {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(detected.displayName, color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("$kcal kcal • $portion", color = White.copy(0.85f), fontSize = 13.sp)
                    Text("${(detected.confidence * 100).toInt()}% conf", color = White.copy(0.7f), fontSize = 11.sp)
                }
                Icon(Icons.Filled.ExpandMore, null, tint = White, modifier = Modifier.rotate(rot))
            }

            if (expanded) {
                Divider(color = White.copy(0.15f))

                if (catalogItem != null) {
                    ScanMetricsGrid(catalogItem.preview)
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onUse,
                        colors = ButtonDefaults.buttonColors(containerColor = Orange)
                    ) {
                        Icon(Icons.Filled.Add, null, tint = White)
                        Spacer(Modifier.width(6.dp))
                        Text("Agregar", color = White)
                    }
                }
            }
        }
    }
}


// ==== MÉTRICAS ====

@Composable
private fun ScanMetricsGrid(p: FoodRepository.PortionPreview) {
    Column(Modifier.padding(14.dp)) {
        Text("Métricas de la porción", color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricChip("IG", p.ig.toString())
            MetricChip("Gramos", "${p.grams} g")
            MetricChip("GL", String.format(Locale.US, "%.1f", p.gl))
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricChip("Carbohidratos", "${p.carbsG} g")
            MetricChip("Proteína", "${p.proteinG} g")
            MetricChip("Fibra", "${p.fiberG} g")
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricChip("Energía", "${p.kcal} kcal")
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
