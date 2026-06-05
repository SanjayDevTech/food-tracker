package com.example.foodtracker.data

import com.example.foodtracker.BuildConfig
import com.example.foodtracker.data.db.FoodEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Searches USDA FoodData Central by name and maps results to per-100g [FoodEntity]s.
 *
 * The API key is sourced from BuildConfig.USDA_API_KEY (see app/build.gradle.kts).
 * Defaults to the public "DEMO_KEY" which is rate-limited but works out of the box;
 * users can supply their own free key from https://api.data.gov/signup/ via
 * `USDA_API_KEY=...` in local.properties.
 *
 * Indian home-cooked dishes are not USDA's strong suit — for those the curated
 * IndianFoodSeed is more authoritative. USDA shines for raw ingredients, generic
 * cooked items (white rice, chicken breast), and branded packaged products.
 */
class UsdaFoodApi {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Look up a single packaged product by GTIN/UPC barcode. USDA indexes
     * Branded foods by gtinUpc, so we search with the raw barcode and filter
     * the response for the first exact match. Returns null when nothing matches.
     *
     * For Branded entries USDA exposes `labelNutrients` (per-serving, from the
     * package's Nutrition Facts panel) alongside `foodNutrients` (nominally
     * per-100g but unit-inconsistent). We surface both so the user can confirm
     * which one to trust.
     */
    suspend fun lookupBarcode(barcode: String): BarcodeProduct? = withContext(Dispatchers.IO) {
        if (barcode.isBlank()) return@withContext null
        val encoded = URLEncoder.encode(barcode.trim(), "UTF-8")
        val url = URL(
            "https://api.nal.usda.gov/fdc/v1/foods/search" +
                "?api_key=${BuildConfig.USDA_API_KEY}" +
                "&query=$encoded" +
                "&pageSize=5" +
                "&dataType=Branded"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "FoodTracker/1.0 (Android)")
        }
        try {
            if (conn.responseCode != 200) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val root = json.parseToJsonElement(body).jsonObject
            val foods = root["foods"]?.jsonArray ?: return@withContext null
            val match = foods.firstOrNull {
                it.jsonObject.str("gtinUpc")?.trim() == barcode.trim()
            }?.jsonObject ?: foods.firstOrNull()?.jsonObject ?: return@withContext null

            val description = match.str("description")?.takeIf { it.isNotBlank() } ?: return@withContext null
            val brand = match.str("brandOwner") ?: match.str("brandName")
            val perHundredG = nutrientsFromFoodNutrients(match).takeIfNonEmpty()
            val perServing = nutrientsFromLabelNutrients(match).takeIfNonEmpty()
            if (perHundredG == null && perServing == null) return@withContext null
            val servingGrams = match.num("servingSize")?.takeIf { it > 0 } ?: 100.0
            val servingUnit = match.str("servingSizeUnit") ?: "g"
            val servingLabel = if (servingUnit.equals("g", ignoreCase = true)) {
                "1 serving (${servingGrams.toInt()} g)"
            } else {
                "1 serving (${servingGrams.toInt()} $servingUnit)"
            }
            BarcodeProduct(
                name = description,
                brand = brand,
                barcode = barcode,
                category = FoodCategory.Packaged.name,
                servingGrams = servingGrams,
                servingLabel = servingLabel,
                perHundredG = perHundredG,
                perServing = perServing,
                defaultBasis = inferBasis(perHundredG, perServing, servingGrams),
            )
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    suspend fun searchByName(query: String, limit: Int = 15): List<FoodEntity> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        // Foundation + SR Legacy give us authoritative whole-food entries; Branded fills in packaged items.
        // dataType is order-sensitive in the response — Foundation/SR show up first.
        val url = URL(
            "https://api.nal.usda.gov/fdc/v1/foods/search" +
                "?api_key=${BuildConfig.USDA_API_KEY}" +
                "&query=$encoded" +
                "&pageSize=$limit" +
                "&dataType=Foundation,SR%20Legacy,Branded"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "FoodTracker/1.0 (Android)")
        }
        try {
            if (conn.responseCode != 200) return@withContext emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val root = json.parseToJsonElement(body).jsonObject
            val foods = root["foods"]?.jsonArray ?: return@withContext emptyList()
            foods.mapNotNull { el ->
                val food = el.jsonObject
                val description = food.str("description")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val brand = food.str("brandOwner") ?: food.str("brandName")
                val nutrientsArray = food["foodNutrients"]?.jsonArray ?: return@mapNotNull null
                val nutrientMap = buildMap<Int, Double> {
                    nutrientsArray.forEach { item ->
                        val obj = item.jsonObject
                        val id = obj["nutrientId"]?.jsonPrimitive?.intOrNull
                            ?: obj["nutrient"]?.jsonObject?.get("id")?.jsonPrimitive?.intOrNull
                        val value = obj["value"]?.jsonPrimitive?.doubleOrNull
                            ?: obj["amount"]?.jsonPrimitive?.doubleOrNull
                        if (id != null && value != null) put(id, value)
                    }
                }
                val nutrients = nutrientsFromUsda(nutrientMap)
                if (nutrients.calories <= 0.0) return@mapNotNull null
                val servingGrams = food.num("servingSize")?.takeIf { it > 0 } ?: 100.0
                val servingUnit = food.str("servingSizeUnit") ?: "g"
                val servingLabel = if (servingUnit.equals("g", ignoreCase = true)) {
                    "1 serving (${servingGrams.toInt()} g)"
                } else {
                    "1 serving (${servingGrams.toInt()} $servingUnit)"
                }
                FoodEntity.fromNutrientsPer100g(
                    name = description,
                    category = FoodCategory.Packaged.name,
                    n = nutrients,
                    servingGrams = servingGrams,
                    servingLabel = servingLabel,
                    brand = brand,
                )
            }
        } catch (e: Exception) {
            emptyList()
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Extract per-100g nutrients from a USDA `foodNutrients` array. For
     * Foundation/SR Legacy entries this is reliably per-100g. For Branded
     * entries it's *nominally* per-100g — pair with [nutrientsFromLabelNutrients]
     * and let the user pick the basis on the resolved panel.
     */
    private fun nutrientsFromFoodNutrients(food: JsonObject): Nutrients {
        val arr = food["foodNutrients"]?.jsonArray ?: return Nutrients.Zero
        val byId = buildMap<Int, Double> {
            arr.forEach { item ->
                val obj = item.jsonObject
                val id = obj["nutrientId"]?.jsonPrimitive?.intOrNull
                    ?: obj["nutrient"]?.jsonObject?.get("id")?.jsonPrimitive?.intOrNull
                val value = obj["value"]?.jsonPrimitive?.doubleOrNull
                    ?: obj["amount"]?.jsonPrimitive?.doubleOrNull
                if (id != null && value != null) put(id, value)
            }
        }
        return nutrientsFromUsda(byId)
    }

    /**
     * Extract per-serving nutrients from USDA Branded `labelNutrients` block.
     * This mirrors the on-package Nutrition Facts panel verbatim, so it's the
     * source of truth for users who scan a packaged product and see "per serving"
     * values they expect to match.
     *
     * Sodium/cholesterol/minerals/vitamins come back in mg or µg already — USDA
     * keys them by name with a `value` Number, no `nutrientId` mapping needed.
     */
    private fun nutrientsFromLabelNutrients(food: JsonObject): Nutrients {
        val label = food["labelNutrients"]?.jsonObject ?: return Nutrients.Zero
        fun v(key: String): Double = label[key]?.jsonObject?.get("value")?.jsonPrimitive?.doubleOrNull ?: 0.0
        return Nutrients(
            calories = v("calories"),
            carbs = v("carbohydrates"),
            sugar = v("sugars"),
            fiber = v("fiber"),
            protein = v("protein"),
            fat = v("fat"),
            saturatedFat = v("saturatedFat"),
            cholesterol = v("cholesterol"),
            sodium = v("sodium"),
            calcium = v("calcium"),
            iron = v("iron"),
            potassium = v("potassium"),
            // labelNutrients omits magnesium/zinc/vitamins for most Branded entries;
            // leave them zero and let foodNutrients fill them in if the user picks 100g.
        )
    }

    private fun Nutrients.takeIfNonEmpty(): Nutrients? =
        if (calories > 0.0 || carbs > 0.0 || protein > 0.0 || fat > 0.0) this else null

    /** Map USDA nutrient ids (from foodNutrients) to our [Nutrients] struct. */
    private fun nutrientsFromUsda(byId: Map<Int, Double>) = Nutrients(
        calories = byId[1008] ?: 0.0,
        carbs = byId[1005] ?: 0.0,
        sugar = byId[2000] ?: byId[1063] ?: 0.0,
        fiber = byId[1079] ?: 0.0,
        protein = byId[1003] ?: 0.0,
        fat = byId[1004] ?: 0.0,
        saturatedFat = byId[1258] ?: 0.0,
        cholesterol = byId[1253] ?: 0.0,
        sodium = byId[1093] ?: 0.0,
        calcium = byId[1087] ?: 0.0,
        iron = byId[1089] ?: 0.0,
        magnesium = byId[1090] ?: 0.0,
        potassium = byId[1092] ?: 0.0,
        zinc = byId[1095] ?: 0.0,
        // Prefer RAE for Vit A; fall back to IU-based legacy id where needed.
        vitaminA = byId[1106] ?: 0.0,
        vitaminC = byId[1162] ?: 0.0,
        vitaminD = byId[1114] ?: byId[1110] ?: 0.0,
        vitaminE = byId[1109] ?: 0.0,
        vitaminK = byId[1185] ?: 0.0,
        vitaminB12 = byId[1178] ?: 0.0,
        folate = byId[1177] ?: 0.0,
    )

    private fun JsonObject.str(key: String): String? =
        this[key]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull }
            ?.takeIf { it.isNotBlank() }

    private fun JsonObject.num(key: String): Double? {
        val prim = this[key] as? kotlinx.serialization.json.JsonPrimitive ?: return null
        return prim.doubleOrNull ?: prim.contentOrNull?.toDoubleOrNull()
    }
}
