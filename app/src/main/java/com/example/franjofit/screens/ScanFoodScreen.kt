package com.example.franjofit.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.franjofit.BuildConfig
import com.example.franjofit.data.recognizeWithOpenAI
import com.example.franjofit.ui.theme.DeepBlue
import com.example.franjofit.ui.theme.Orange
import com.example.franjofit.ui.theme.White
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanFoodScreen(
    onCancel: () -> Unit = {},
    onUseResult: (name: String, kcal: Int, portion: String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var labels by remember { mutableStateOf<List<LabeledFood>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

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

            if (isProcessing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Text("Resultados", color = White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

            labels.take(5).forEach { item ->
                ResultRow(
                    item = item,
                    onUse = { onUseResult(item.displayName, item.kcalEstimate, item.portion) }
                )
                Divider(color = White.copy(0.1f))
            }

            if (labels.isEmpty() && !isProcessing) {
                Text("Toma una foto o elige de la galería para comenzar.", color = White.copy(0.8f))
            }
        }
    }
}


private data class LabeledFood(
    val displayName: String,
    val confidence: Float,
    val kcalEstimate: Int,
    val portion: String
)

private suspend fun analyzeWithOpenAI(
    bmp: Bitmap,
    onResult: (List<LabeledFood>) -> Unit,
    onError: (String) -> Unit,
    setBusy: (Boolean) -> Unit
) {
    try {
        setBusy(true)

        if (BuildConfig.OPENAI_API_KEY.isBlank()) {
            onError("OPENAI_API_KEY vacío. Configúralo en local.properties")
            return
        }

        val raw = recognizeWithOpenAI.analyzeImage(bmp, BuildConfig.OPENAI_API_KEY)

        runCatching {
            val err = org.json.JSONObject(raw).optJSONObject("error")
            if (err != null) {
                val msg = err.optString("message", "Error desconocido de OpenAI")
                onError(msg)
                return
            }
        }

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
                val obj = arr.getJSONObject(i)
                parsed.add(
                    LabeledFood(
                        displayName = obj.optString("nombre", "Desconocido"),
                        confidence = obj.optDouble("confianza", 0.9).toFloat(),
                        kcalEstimate = obj.optInt("kcal", 100),
                        portion = obj.optString("porcion", "100 g")
                    )
                )
            }
            true
        }.getOrDefault(false)

        if (!okJson) {
            parsed.add(
                LabeledFood(
                    displayName = cleanText.take(80),
                    confidence = 0.9f,
                    kcalEstimate = 100,
                    portion = "100 g"
                )
            )
        }

        onResult(parsed)

    } catch (e: Exception) {
        onError("Error con OpenAI: ${e.message ?: e.javaClass.simpleName}")
    } finally {
        setBusy(false)
    }
}


@Composable
private fun ResultRow(
    item: LabeledFood,
    onUse: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.displayName, color = White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${(item.confidence * 100).toInt()}% conf • ${item.kcalEstimate} kcal • ${item.portion}",
                color = White.copy(0.85f),
                fontSize = 13.sp
            )
        }
        Button(
            onClick = onUse,
            colors = ButtonDefaults.buttonColors(containerColor = Orange),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) { Text("Usar", color = White) }
    }
}
