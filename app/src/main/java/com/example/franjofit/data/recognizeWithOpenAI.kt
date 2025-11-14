package com.example.franjofit.data

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.TlsVersion
import okhttp3.CipherSuite
import okhttp3.ConnectionSpec
import java.util.concurrent.TimeUnit

object recognizeWithOpenAI {

    private const val TAG = "OpenAI"

    private val tlsSpec: ConnectionSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
        .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
        .cipherSuites(
            CipherSuite.TLS_AES_128_GCM_SHA256,
            CipherSuite.TLS_AES_256_GCM_SHA384,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
            CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
            CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
        )
        .build()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionSpecs(listOf(tlsSpec, ConnectionSpec.CLEARTEXT))
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun analyzeImage(
        bitmap: Bitmap,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {

        require(apiKey.isNotBlank()) { "OPENAI_API_KEY vacío" }


        val maxDim = 512
        val w = bitmap.width
        val h = bitmap.height
        val scale = minOf(maxDim / w.toFloat(), maxDim / h.toFloat(), 1f)
        val scaledBmp = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
        } else bitmap

        // === convertir a base64 ===
        val byteStream = java.io.ByteArrayOutputStream()
        scaledBmp.compress(Bitmap.CompressFormat.JPEG, 80, byteStream)
        val base64Image = android.util.Base64.encodeToString(
            byteStream.toByteArray(),
            android.util.Base64.NO_WRAP
        )
// este es el prompt
        val promptText = """
Eres un asistente nutricional experto.

Analiza la imagen y detecta **TODOS los alimentos presentes**:
- En un plato, bandeja, vaso, tazón, mesa o fondo.
- Aunque estén parcialmente visibles.

Devuelve **ÚNICAMENTE un JSON array**, sin texto adicional ni markdown:
[
  {
    "nombre": "nombre del alimento",
    "kcal": calorías estimadas de la cantidad visible,
    "porcion": "descripción de la porción visible (ej: '1 unidad', '1 taza', '150 g')",
    "confianza": número entre 0 y 1
  }
]

Reglas:
- Múltiples alimentos = múltiples objetos.
- Estima kcal de lo que realmente se ve.
- Si un alimento aparece varias veces, ajusta porción y kcal.
- Si no hay alimentos, devuelve: [].
""".trimIndent()

        val promptEsc = org.json.JSONObject.quote(promptText)

        //este es el request
        val json = """
        {
          "model": "gpt-4o-mini",
          "temperature": 0,
          "max_tokens": 300,
          "messages": [
            {
              "role": "user",
              "content": [
                { "type": "text", "text": $promptEsc },
                { "type": "image_url", "image_url": { "url": "data:image/jpeg;base64,$base64Image" } }
              ]
            }
          ]
        }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), json))
            .build()

        try {
            client.newCall(request).execute().use { resp ->

                val bodyStr = resp.body?.string().orEmpty()
                Log.d(TAG, "HTTP ${resp.code} ${resp.message}")
                Log.d(TAG, "Body: $bodyStr")


                if (resp.code == 429 || bodyStr.contains("Rate limit", ignoreCase = true)) {
                    return@use """{"error":"Has alcanzado el límite de la API."}"""
                }

                bodyStr.ifBlank { """{"error":"respuesta_vacia"}""" }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción llamando a OpenAI", e)
            """{"error":"${e.javaClass.simpleName}: ${e.message ?: "sin mensaje"}"}"""
        }
    }
}
