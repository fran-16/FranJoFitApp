package com.example.franjofit.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

data class Food(
    val id: String,
    val nombre: String,
    val ig: Int,
    val carbs100g: Double,
    val protein100g: Double,
    val fiber100g: Double,
    val kcal100g: Int
)

private data class PortionCalc(
    val grams: Int,
    val carbsG: Double,
    val proteinG: Double,
    val fiberG: Double,
    val kcal: Int,
    val gl: Double
)

object FoodRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private const val ASSET_FILE = "alimentos.csv"   // nombre que usas en assets
    private var catalogLoaded = false
    private var foods: List<Food> = emptyList()


    suspend fun addMealItemAuto(
        context: Context,
        mealType: String,
        displayName: String,
        portionTextOverride: String? = null
    ) {
        val uid = auth.currentUser?.uid ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val food = findByNameFuzzy(context, displayName) ?: return

        val preset = presetFor(displayName)
        val grams = preset?.grams ?: 100
        val portionText = portionTextOverride ?: preset?.text ?: "$grams g"

        val calc = calcPortion(food, grams)

        val mealData = mapOf(
            "id" to food.id,
            "name" to food.nombre,
            "grams" to grams,
            "ig" to food.ig,
            "carbs_g" to calc.carbsG,
            "protein_g" to calc.proteinG,
            "fiber_g" to calc.fiberG,
            "kcal" to calc.kcal,
            "gl" to calc.gl,
            "portion_text" to portionText
        )

        val mealDoc = db.collection("users")
            .document(uid)
            .collection("meals")
            .document(today)

        val snapshot = mealDoc.get().await()
        val existingMeals = snapshot.get(mealType) as? List<Map<String, Any>> ?: emptyList()
        val updatedMeals = existingMeals + mealData

        mealDoc.set(mapOf(mealType to updatedMeals), SetOptions.merge()).await()
    }

    suspend fun getMealsForToday(): Map<String, List<Map<String, Any>>> {
        val uid = auth.currentUser?.uid ?: return emptyMap()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val doc = db.collection("users")
            .document(uid)
            .collection("meals")
            .document(today)
            .get()
            .await()

        return doc.data?.mapValues { (_, value) ->
            value as? List<Map<String, Any>> ?: emptyList()
        } ?: emptyMap()
    }

    data class PortionPreview(
        val ig: Int,
        val grams: Int,
        val carbsG: Double,
        val proteinG: Double,
        val fiberG: Double,
        val kcal: Int,
        val gl: Double
    )

    suspend fun getPortionPreview(
        context: Context,
        displayName: String
    ): PortionPreview? {
        val food = findByNameFuzzy(context, displayName) ?: return null
        val preset = presetFor(displayName)
        val grams = preset?.grams ?: 100
        val calc = calcPortion(food, grams)
        return PortionPreview(
            ig = food.ig,
            grams = grams,
            carbsG = calc.carbsG,
            proteinG = calc.proteinG,
            fiberG = calc.fiberG,
            kcal = calc.kcal,
            gl = calc.gl
        )
    }

    private suspend fun loadCatalog(context: Context) = withContext(Dispatchers.IO) {
        if (catalogLoaded) return@withContext
        val input = context.assets.open(ASSET_FILE)
        val br = input.bufferedReader()
        foods = parseCsv(br)
        catalogLoaded = true
    }

    private suspend fun findByNameFuzzy(context: Context, query: String): Food? {
        loadCatalog(context)
        val q = norm(query)
        // 1) exacto
        foods.firstOrNull { norm(it.nombre) == q }?.let { return it }
        // 2) contiene
        val contains = foods.filter { norm(it.nombre).contains(q) }
        if (contains.size == 1) return contains.first()
        if (contains.isNotEmpty()) return bestTokenMatch(q, contains)
        // 3) tokens
        val tokenCandidates = foods.filter { tokenOverlap(q, norm(it.nombre)) > 0 }
        if (tokenCandidates.isEmpty()) return null
        return bestTokenMatch(q, tokenCandidates)
    }

    private fun parseCsv(br: BufferedReader): List<Food> {
        val out = mutableListOf<Food>()
        br.readLine() // header
        br.lineSequence().forEach { raw ->
            if (raw.isBlank()) return@forEach
            // CSV simple (sin comillas). Tus nombres no tienen comas.
            val cols = raw.split(',').map { it.trim() }
            if (cols.size < 7) return@forEach
            try {
                out += Food(
                    id = cols[0],
                    nombre = cols[1],
                    ig = cols[2].toInt(),
                    carbs100g = cols[3].toDouble(),
                    protein100g = cols[4].toDouble(),
                    fiber100g = cols[5].toDouble(),
                    kcal100g = cols[6].toInt()
                )
            } catch (_: Exception) { /* ignora fila mala */ }
        }
        return out
    }

    private fun norm(s: String): String =
        Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun tokenOverlap(a: String, b: String): Int {
        val ta = a.split(' ').filter { it.isNotBlank() }.toSet()
        val tb = b.split(' ').filter { it.isNotBlank() }.toSet()
        return ta.intersect(tb).size
    }

    private fun bestTokenMatch(q: String, candidates: List<Food>): Food =
        candidates.maxBy { cand ->
            val c = norm(cand.nombre)
            val overlap = tokenOverlap(q, c)
            overlap * 10 - abs(c.length - q.length)
        }

    private data class PortionPreset(val grams: Int, val text: String)

    private fun presetFor(displayName: String): PortionPreset? {
        val key = norm(displayName)
        return when (key) {
            // TUS 5 BÁSICOS (los de la pantalla)
            norm("Avena cocida") -> PortionPreset(grams = 200, text = "1 taza (240 ml)")
            norm("Pan integral") -> PortionPreset(grams = 28,  text = "1 rebanada (28 g)")
            norm("Huevo cocido") -> PortionPreset(grams = 50,  text = "1 unidad (50 g)")
            norm("Yogur natural sin azúcar") -> PortionPreset(grams = 150, text = "1 pote (150 g)")
            norm("Manzana") -> PortionPreset(grams = 182, text = "1 unidad (182 g)")

            // Extras comunes (útiles)
            norm("Pan francés (blanco)") -> PortionPreset(50, "1 unidad (50 g)")
            norm("Arroz blanco cocido") -> PortionPreset(150, "1/2 taza (150 g)")
            norm("Arroz integral cocido") -> PortionPreset(150, "1/2 taza (150 g)")
            norm("Papa blanca cocida") -> PortionPreset(150, "1 porción (150 g)")
            norm("Camote cocido") -> PortionPreset(150, "1 porción (150 g)")
            norm("Choclo cocido") -> PortionPreset(100, "1/2 choclo (100 g)")
            norm("Quinua cocida") -> PortionPreset(150, "1/2 taza (150 g)")
            norm("Leche descremada") -> PortionPreset(240, "1 vaso (240 ml)")
            norm("Gaseosa azucarada") -> PortionPreset(355, "1 lata (355 ml)")
            else -> null
        }
    }

    private fun calcPortion(food: Food, grams: Int): PortionCalc {
        val carbs  = food.carbs100g   * grams / 100.0
        val fiber  = food.fiber100g   * grams / 100.0
        val prot   = food.protein100g * grams / 100.0
        val kcal   = (food.kcal100g   * grams / 100.0).toInt()

        // Carbohidratos netos
        val netCarbs = carbs - fiber
        val gl = (food.ig / 100.0) * netCarbs

        return PortionCalc(
            grams = grams,
            carbsG = round1(carbs),
            proteinG = round1(prot),
            fiberG = round1(fiber),
            kcal = kcal,
            gl = round1(gl)
        )
    }

    data class CatalogUiItem(
        val name: String,
        val portionLabel: String,
        val kcal: Int,
        val preview: PortionPreview
    )

    suspend fun listCatalogForUi(context: Context): List<CatalogUiItem> {
        loadCatalog(context)
        return foods.map { f ->
            val preset = presetFor(f.nombre)
            val grams = preset?.grams ?: 100
            val label = preset?.text ?: "$grams g"
            val calc = calcPortion(f, grams)
            CatalogUiItem(
                name = f.nombre,
                portionLabel = label,
                kcal = calc.kcal,
                preview = PortionPreview(
                    ig = f.ig,
                    grams = grams,
                    carbsG = calc.carbsG,
                    proteinG = calc.proteinG,
                    fiberG = calc.fiberG,
                    kcal = calc.kcal,
                    gl = calc.gl
                )
            )
        }.sortedBy { it.name.lowercase(Locale.ROOT) }
    }

    private fun round1(v: Double) = round(v * 10.0) / 10.0
}
