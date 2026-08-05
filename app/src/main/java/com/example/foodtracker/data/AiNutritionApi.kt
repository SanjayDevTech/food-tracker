package com.example.foodtracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

/**
 * AI nutrition reply: a typical serving for the food plus that serving's
 * nutrients. [nutrients] are PER SERVING (not per 100 g).
 */
data class AiNutrition(
    val servingGrams: Double,
    val servingLabel: String,
    val category: FoodCategory,
    val nutrients: Nutrients,
)

/**
 * Optional AI nutrition lookup. Given a food name, asks the user's chosen LLM
 * provider ([AiProvider]) for per-100 g nutrition and maps the reply into
 * [Nutrients] for the add-food form to pre-fill.
 *
 * This is strictly opt-in: the caller only reaches here when the user has
 * enabled AI in settings AND supplied their own API key — there is NO bundled
 * token, so nothing is ever sent to a third party by default. Mirrors the
 * raw-`HttpURLConnection` + `kotlinx.serialization` style of [UsdaFoodApi],
 * with a POST body instead of a GET query. All failures return null so the
 * form silently falls back to manual entry.
 */
class AiNutritionApi {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Human-readable reason for the most recent failure (set on the fetch path),
     * so the UI can distinguish a rejected key from a quota/rate limit or a
     * network drop. Null after a success. Single active request at a time.
     */
    @Volatile
    var lastError: String? = null
        private set

    suspend fun fetchNutrition(
        foodName: String,
        provider: AiProvider,
        model: String,
        apiKey: String,
    ): AiNutrition? = withContext(Dispatchers.IO) {
        if (foodName.isBlank() || apiKey.isBlank()) return@withContext null
        lastError = null
        // Resolve against the provider's model list; a stale/blank id falls back
        // to the provider's default.
        val modelId = provider.modelFromId(model).id
        val prompt = buildPrompt(foodName.trim())
        val reply = when (provider) {
            AiProvider.Gemini -> callGemini(prompt, modelId, apiKey)
            AiProvider.Claude -> callClaude(prompt, modelId, apiKey)
            AiProvider.OpenAI -> callOpenAi(prompt, modelId, apiKey)
        } ?: return@withContext null
        val parsed = parseResult(reply)
        if (parsed == null && lastError == null) {
            lastError = "The AI reply couldn't be read. Try again or pick another model."
        }
        parsed
    }

    /**
     * Second pass: hand the first result back to the model as a fact-checker and
     * ask it to correct any wrong/implausible values, returning the same schema.
     * Returns null on failure so the caller keeps the already-prefilled values.
     */
    suspend fun verifyNutrition(
        foodName: String,
        candidate: AiNutrition,
        provider: AiProvider,
        model: String,
        apiKey: String,
    ): AiNutrition? = withContext(Dispatchers.IO) {
        if (foodName.isBlank() || apiKey.isBlank()) return@withContext null
        val modelId = provider.modelFromId(model).id
        val prompt = buildVerifyPrompt(foodName.trim(), candidate)
        val reply = when (provider) {
            AiProvider.Gemini -> callGemini(prompt, modelId, apiKey)
            AiProvider.Claude -> callClaude(prompt, modelId, apiKey)
            AiProvider.OpenAI -> callOpenAi(prompt, modelId, apiKey)
        } ?: return@withContext null
        parseResult(reply)
    }

    // ─── Prompt ───────────────────────────────────────────────────────────

    private fun buildPrompt(foodName: String): String = """
        You are a nutrition database. Return ONLY a compact JSON object (no markdown,
        no explanation) for the food below.
        First pick a TYPICAL SINGLE SERVING of this specific food (e.g. 1 medium
        banana, 1 cup cooked rice, 2 slices bread, 1 katori dal). Then set:
        - "servingGrams": the weight of that one serving in grams (number).
        - "servingLabel": a short label naming that serving and its grams,
          e.g. "1 medium (120 g)" or "1 cup (195 g)".
        - "category": classify the food as EXACTLY ONE of these values:
          SouthIndian, NorthIndian, Snack, Beverage, Dairy, Fruit, Vegetable,
          Grain, Protein, Packaged, Other. Use Other if none clearly fits.
        Give every nutrient value FOR THAT ONE SERVING (not per 100 g).
        Use 0 when a value is unknown or not applicable.
        Units — kcal: calories. grams: carbs, sugar, fiber, protein, fat,
        saturatedFat, omega3. milligrams: cholesterol, sodium, calcium, iron,
        magnesium, potassium, zinc, phosphorus, copper, manganese, vitaminC,
        vitaminE, vitaminB1, vitaminB2, vitaminB3, vitaminB6. micrograms:
        selenium, vitaminA, vitaminD, vitaminK, vitaminB12, folate.
        Keys: servingGrams, servingLabel, category, calories, carbs, sugar, fiber, protein,
        fat, saturatedFat, omega3, cholesterol, sodium, calcium, iron, magnesium,
        potassium, zinc, phosphorus, selenium, copper, manganese, vitaminA,
        vitaminC, vitaminD, vitaminE, vitaminK, vitaminB12, folate, vitaminB1,
        vitaminB2, vitaminB3, vitaminB6.
        Food: "$foodName"
    """.trimIndent()

    private fun buildVerifyPrompt(foodName: String, candidate: AiNutrition): String = """
        You are a meticulous nutrition fact-checker. Below is a proposed nutrition
        profile for a food, for ONE SERVING, as a JSON object. Check every field
        against reliable food-composition data:
        - Confirm servingGrams is a realistic serving weight and servingLabel matches it.
        - Confirm category is the best fit from: SouthIndian, NorthIndian, Snack,
          Beverage, Dairy, Fruit, Vegetable, Grain, Protein, Packaged, Other.
        - Correct any nutrient that is wrong or implausible for that serving; keep
          the ones that are already right. Same units as given.
        Return ONLY the corrected JSON object using the exact SAME keys — no
        markdown, no explanation.
        Food: "$foodName"
        Proposed: ${candidateJson(candidate)}
    """.trimIndent()

    private fun candidateJson(c: AiNutrition): String = buildJsonObject {
        put("servingGrams", c.servingGrams)
        put("servingLabel", c.servingLabel)
        put("category", c.category.name)
        val n = c.nutrients
        put("calories", n.calories); put("carbs", n.carbs); put("sugar", n.sugar)
        put("fiber", n.fiber); put("protein", n.protein); put("fat", n.fat)
        put("saturatedFat", n.saturatedFat); put("omega3", n.omega3)
        put("cholesterol", n.cholesterol); put("sodium", n.sodium)
        put("calcium", n.calcium); put("iron", n.iron); put("magnesium", n.magnesium)
        put("potassium", n.potassium); put("zinc", n.zinc); put("phosphorus", n.phosphorus)
        put("selenium", n.selenium); put("copper", n.copper); put("manganese", n.manganese)
        put("vitaminA", n.vitaminA); put("vitaminC", n.vitaminC); put("vitaminD", n.vitaminD)
        put("vitaminE", n.vitaminE); put("vitaminK", n.vitaminK); put("vitaminB12", n.vitaminB12)
        put("folate", n.folate); put("vitaminB1", n.vitaminB1); put("vitaminB2", n.vitaminB2)
        put("vitaminB3", n.vitaminB3); put("vitaminB6", n.vitaminB6)
    }.toString()

    // ─── Provider calls ─────────────────────────────────────────────────────

    private fun callGemini(prompt: String, model: String, apiKey: String): String? {
        val body = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    putJsonArray("parts") {
                        addJsonObject { put("text", prompt) }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("response_mime_type", "application/json")
            }
        }.toString()
        val resp = post(
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent",
            mapOf("x-goog-api-key" to apiKey),
            body,
        ) ?: return null
        return runCatching {
            json.parseToJsonElement(resp).jsonObject["candidates"]
                ?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull
        }.getOrNull()
    }

    private fun callClaude(prompt: String, model: String, apiKey: String): String? {
        val body = buildJsonObject {
            put("model", model)
            put("max_tokens", 1024)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    put("content", prompt)
                }
            }
        }.toString()
        val resp = post(
            "https://api.anthropic.com/v1/messages",
            mapOf("x-api-key" to apiKey, "anthropic-version" to "2023-06-01"),
            body,
        ) ?: return null
        return runCatching {
            json.parseToJsonElement(resp).jsonObject["content"]
                ?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull
        }.getOrNull()
    }

    private fun callOpenAi(prompt: String, model: String, apiKey: String): String? {
        val body = buildJsonObject {
            put("model", model)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    put("content", prompt)
                }
            }
            putJsonObject("response_format") { put("type", "json_object") }
        }.toString()
        val resp = post(
            "https://api.openai.com/v1/chat/completions",
            mapOf("Authorization" to "Bearer $apiKey"),
            body,
        ) ?: return null
        return runCatching {
            json.parseToJsonElement(resp).jsonObject["choices"]
                ?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("message")?.jsonObject
                ?.get("content")?.jsonPrimitive?.contentOrNull
        }.getOrNull()
    }

    // ─── HTTP + parsing ─────────────────────────────────────────────────────

    private fun post(urlStr: String, headers: Map<String, String>, body: String): String? {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("User-Agent", "FoodTracker/1.0 (Android)")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        return try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
                android.util.Log.w("AiNutritionApi", "POST $urlStr -> HTTP $code: ${err?.take(800)}")
                lastError = describeHttp(code, err)
                return null
            }
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            android.util.Log.w("AiNutritionApi", "POST $urlStr failed: ${e.javaClass.simpleName}: ${e.message}")
            lastError = "Network error — check your internet connection and try again."
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun describeHttp(code: Int, body: String?): String {
        // Billing/credit problems can arrive as 400/402/429 depending on provider
        // — detect them from the error body so the message is actionable.
        val b = body?.lowercase().orEmpty()
        if (b.contains("credit balance") || b.contains("billing") ||
            b.contains("insufficient_quota") || b.contains("insufficient funds")
        ) {
            return "Your account is out of credit/quota. Add billing or credits for this key, then retry."
        }
        return when (code) {
            401, 403 -> "Your API key was rejected (HTTP $code). Check the key and selected provider in Settings."
            404 -> "That model isn't available to your account (HTTP 404). Pick a different model, or your plan may not include it."
            429 -> "Rate limit or quota exceeded (HTTP 429). Your plan may be out of quota — wait a bit, switch model, or enable billing for your key."
            400 -> "The provider rejected the request (HTTP 400). Try a different model."
            in 500..599 -> "The AI provider had a server error (HTTP $code). Try again shortly."
            else -> "AI request failed (HTTP $code)."
        }
    }

    /**
     * Pull the JSON object out of the model's reply (models sometimes wrap it in
     * ``` fences or prose) and map keys to an [AiNutrition] (serving + per-serving
     * nutrients). Returns null if the payload has no usable macros so the form
     * isn't wiped with zeros.
     */
    private fun parseResult(reply: String): AiNutrition? {
        val start = reply.indexOf('{')
        val end = reply.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val obj = runCatching {
            json.parseToJsonElement(reply.substring(start, end + 1)).jsonObject
        }.getOrNull() ?: return null

        fun num(key: String): Double = obj[key]?.jsonPrimitive?.doubleOrNull ?: 0.0
        fun str(key: String): String? =
            obj[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

        val n = Nutrients(
            calories = num("calories"),
            carbs = num("carbs"),
            sugar = num("sugar"),
            fiber = num("fiber"),
            protein = num("protein"),
            fat = num("fat"),
            saturatedFat = num("saturatedFat"),
            omega3 = num("omega3"),
            cholesterol = num("cholesterol"),
            sodium = num("sodium"),
            calcium = num("calcium"),
            iron = num("iron"),
            magnesium = num("magnesium"),
            potassium = num("potassium"),
            zinc = num("zinc"),
            phosphorus = num("phosphorus"),
            selenium = num("selenium"),
            copper = num("copper"),
            manganese = num("manganese"),
            vitaminA = num("vitaminA"),
            vitaminC = num("vitaminC"),
            vitaminD = num("vitaminD"),
            vitaminE = num("vitaminE"),
            vitaminK = num("vitaminK"),
            vitaminB12 = num("vitaminB12"),
            folate = num("folate"),
            vitaminB1 = num("vitaminB1"),
            vitaminB2 = num("vitaminB2"),
            vitaminB3 = num("vitaminB3"),
            vitaminB6 = num("vitaminB6"),
        )
        if (!(n.calories > 0.0 || n.protein > 0.0 || n.carbs > 0.0 || n.fat > 0.0)) return null

        // Fall back to a 100 g serving if the model omitted or botched the size.
        val grams = num("servingGrams").takeIf { it > 0.0 } ?: 100.0
        val label = str("servingLabel") ?: "1 serving (${grams.roundToInt()} g)"
        // fromName returns Other for anything unexpected/missing.
        val category = FoodCategory.fromName(str("category") ?: "")
        return AiNutrition(
            servingGrams = grams,
            servingLabel = label,
            category = category,
            nutrients = n,
        )
    }
}
