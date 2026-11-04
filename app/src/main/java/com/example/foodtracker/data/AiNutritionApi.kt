package com.example.foodtracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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
    /**
     * Optional named sizes for foods that genuinely come in them (a small /
     * medium / large banana). Empty for the common case, which leaves
     * [com.example.foodtracker.data.db.FoodEntity.fromNutrientsPer100g] to
     * build its usual canonical + gram-reference pair. Already passed through
     * [ServingVariant.sanitize], so the canonical serving leads the list.
     *
     * These are gram amounts only — [nutrients] always describes
     * [servingGrams], never a variant.
     */
    val servingVariants: List<ServingVariant> = emptyList(),
)

/** Who said it, in a [AiNutritionApi.refineNutrition] conversation. */
enum class AiChatRole { User, Assistant }

/**
 * One line of the refine conversation. [AiChatRole.User] lines are what the
 * user typed; [AiChatRole.Assistant] lines are the model's own one-sentence
 * summary of the change it made, which is all the history the next turn
 * needs — the authoritative state is the entry itself, re-sent every time.
 */
data class AiChatMessage(val role: AiChatRole, val text: String)

/** A refined entry plus the model's short note on what it changed. */
data class AiRefinement(val nutrition: AiNutrition, val note: String)

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
     * Three tries in total, then give up — about four seconds of waiting in
     * the worst case. A provider shedding load on one model stays down for
     * minutes, so retrying harder than this only makes the user wait longer
     * for the same failure; the error text points them at another model
     * instead.
     */
    private val RETRY_DELAYS_MS = longArrayOf(1_000L, 3_000L)
    private val MAX_ATTEMPTS = RETRY_DELAYS_MS.size + 1

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
        instructions: String = "",
        provider: AiProvider,
        model: String,
        apiKey: String,
    ): AiNutrition? = withContext(Dispatchers.IO) {
        if (foodName.isBlank() || apiKey.isBlank()) return@withContext null
        lastError = null
        // Resolve against the provider's model list; a stale/blank id falls back
        // to the provider's default.
        val modelId = provider.modelFromId(model).id
        val prompt = buildPrompt(foodName.trim(), instructions.trim())
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
        instructions: String = "",
        provider: AiProvider,
        model: String,
        apiKey: String,
    ): AiNutrition? = withContext(Dispatchers.IO) {
        if (foodName.isBlank() || apiKey.isBlank()) return@withContext null
        val modelId = provider.modelFromId(model).id
        val prompt = buildVerifyPrompt(foodName.trim(), candidate, instructions.trim())
        val reply = when (provider) {
            AiProvider.Gemini -> callGemini(prompt, modelId, apiKey)
            AiProvider.Claude -> callClaude(prompt, modelId, apiKey)
            AiProvider.OpenAI -> callOpenAi(prompt, modelId, apiKey)
        } ?: return@withContext null
        parseResult(reply)
    }

    /**
     * Conversational refinement of an entry already on screen: "make it 2
     * idlis", "this was deep fried", "use a 150 g serving".
     *
     * [current] is rebuilt from the form each turn rather than remembered
     * here, so a value the user typed by hand is what gets refined. [history]
     * carries the earlier turns so follow-ups like "actually make it 3" work.
     * Returns null on failure, leaving the form untouched.
     */
    suspend fun refineNutrition(
        foodName: String,
        current: AiNutrition,
        history: List<AiChatMessage>,
        instruction: String,
        provider: AiProvider,
        model: String,
        apiKey: String,
    ): AiRefinement? = withContext(Dispatchers.IO) {
        if (instruction.isBlank() || apiKey.isBlank()) return@withContext null
        lastError = null
        val modelId = provider.modelFromId(model).id
        val prompt = buildRefinePrompt(foodName.trim(), current, history, instruction.trim())
        val reply = when (provider) {
            AiProvider.Gemini -> callGemini(prompt, modelId, apiKey)
            AiProvider.Claude -> callClaude(prompt, modelId, apiKey)
            AiProvider.OpenAI -> callOpenAi(prompt, modelId, apiKey)
        } ?: return@withContext null
        val parsed = parseRefinement(reply)
        if (parsed == null && lastError == null) {
            lastError = "The AI reply couldn't be read. Try rewording your request."
        }
        parsed
    }

    // ─── Prompt ───────────────────────────────────────────────────────────

    private fun buildPrompt(foodName: String, instructions: String): String = """
        You are a nutrition database. Return ONLY a compact JSON object (no markdown,
        no explanation) for the food below.
        The user is in India. Prefer Indian food-composition references and Indian
        household portions. For regional dishes, assume a common South Indian
        preparation first, especially Tamil Nadu, Kerala, or Karnataka style.
        Account for typical regional ingredients and proportions such as rice,
        lentils, coconut, curry leaves, mustard seeds, spices, oil, ghee, and
        tempering. Do not substitute a generic Western recipe when an Indian
        interpretation is reasonable. For an ambiguous dish, choose the most
        common South Indian household version and make the assumed style clear
        in the servingLabel when useful.
        First pick a TYPICAL SINGLE SERVING of this specific food (e.g. 1 medium
        banana, 1 cup cooked rice, 2 slices bread, 1 katori dal). Then set:
        - "servingGrams": the weight of that one serving in grams (number).
        - "servingLabel": a short label naming that serving and its grams,
          e.g. "1 medium (120 g)" or "1 cup (195 g)".
        - "category": classify the food as EXACTLY ONE of these values:
          SouthIndian, NorthIndian, Snack, Beverage, Dairy, Fruit, Vegetable,
          Grain, Protein, Packaged, Other. Use Other if none clearly fits.
        - "servingVariants": OPTIONAL, and usually omitted. Include it ONLY
          when this food is genuinely eaten in distinct named sizes a person
          would recognise and ask for — a small/medium/large banana, a
          half/full plate of biryani, a small/large cup of tea. OMIT it
          entirely for anything measured out rather than portioned (flour,
          oil, ghee, spices, pickle by the spoon), for anything sold in a
          fixed unit (a packaged bar, a bottle), and whenever you are unsure.
          Omitting it is the normal, correct answer.
          When you do include it, give 2-4 objects of {"label", "grams"}:
          short labels like "Small", "Medium", "Large", and one entry MUST be
          the typical serving above with grams EQUAL to servingGrams. The
          nutrients below always describe servingGrams only — never scale
          them per variant.
        Give every nutrient value FOR THAT ONE SERVING (not per 100 g).
        Return every key listed below. Use 0 only when the nutrient is genuinely
        absent or nutritionally negligible for this serving. Do not invent a
        trace amount: when a reliable value is unavailable, use 0 and treat it
        as unavailable rather than guessing. Keep meaningful values even when
        they are small, especially zinc, iron, copper, potassium, calcium,
        vitamin C, and vitamin K.
        Units — kcal: calories. grams: carbs, sugar, fiber, protein, fat,
        saturatedFat, omega3. milligrams: cholesterol, sodium, calcium, iron,
        magnesium, potassium, zinc, phosphorus, copper, manganese, vitaminC,
        vitaminE, vitaminB1, vitaminB2, vitaminB3, vitaminB6. micrograms:
        selenium, vitaminA, vitaminD, vitaminK, vitaminB12, folate.
        Keys (every one below is required; servingVariants above is the only
        optional key): servingGrams, servingLabel, category, calories, carbs, sugar, fiber, protein,
        fat, saturatedFat, omega3, cholesterol, sodium, calcium, iron, magnesium,
        potassium, zinc, phosphorus, selenium, copper, manganese, vitaminA,
        vitaminC, vitaminD, vitaminE, vitaminK, vitaminB12, folate, vitaminB1,
        vitaminB2, vitaminB3, vitaminB6.
        Food: "$foodName"
        ${instructionsPrompt(instructions)}
    """.trimIndent()

    private fun buildVerifyPrompt(foodName: String, candidate: AiNutrition, instructions: String): String = """
                You are a meticulous nutrition fact-checker. Below is a proposed nutrition
                profile for a food, for ONE SERVING, as a JSON object. Check every field
        against reliable food-composition data:
                - Treat the user as being in India. Validate regional dishes against a
                    plausible South Indian preparation first, especially Tamil Nadu,
                    Kerala, or Karnataka style, including its usual coconut, lentil, oil,
                    ghee, spice, and tempering proportions. Do not silently replace it
                    with a generic Western preparation.
        - Confirm servingGrams is a realistic serving weight and servingLabel matches it.
        - Confirm category is the best fit from: SouthIndian, NorthIndian, Snack,
          Beverage, Dairy, Fruit, Vegetable, Grain, Protein, Packaged, Other.
                - servingVariants is optional. If present, confirm this food really is
                    eaten in those distinct named sizes and that one entry's grams equals
                    servingGrams; drop the key entirely if the sizes are invented, if the
                    food is measured out rather than portioned, or if it is sold as a
                    fixed unit. Add it only if the food plainly has 2-4 recognised sizes
                    and it is missing. Never scale the nutrients to a variant.
                - Correct every nutrient that is wrong or implausible for that serving;
                    do not silently copy a per-100 g value into a per-serving field.
                - Keep meaningful small values. Use 0 only for a genuinely absent or
                    nutritionally negligible nutrient, or when no reliable value exists;
                    do not fabricate trace amounts to fill the schema. Same units as given.
        Return ONLY the corrected JSON object using the exact SAME keys — no
        markdown, no explanation.
        Food: "$foodName"
        ${instructionsPrompt(instructions)}
        Proposed: ${candidateJson(candidate)}
    """.trimIndent()

    private fun buildRefinePrompt(
        foodName: String,
        current: AiNutrition,
        history: List<AiChatMessage>,
        instruction: String,
    ): String = """
        You are refining an existing nutrition entry through a short conversation.
        The CURRENT entry is given below as JSON. Its nutrients are for ONE
        SERVING of servingGrams, not per 100 g.
        Apply the user's latest request to that entry and return the result.
        - Leave every value the request does not affect exactly as it is.
        - If the request changes the PORTION ("2 idlis", "half a cup", "150 g"),
          scale servingGrams, servingLabel AND every nutrient to match.
        - If it changes the PREPARATION (deep fried instead of steamed, no
          sugar, extra ghee, more coconut), re-derive every nutrient the change
          affects, including servingGrams if the weight changes.
        - If it corrects a single value, change that value and anything that
          must follow from it.
        - If the request is unclear or you cannot apply it, return the entry
          completely unchanged and say why in the note.
        The user is in India; keep the dish a plausible Indian preparation,
        South Indian unless the conversation says otherwise.
        Return ONLY a compact JSON object (no markdown, no explanation) with the
        SAME keys as the current entry, plus one extra key:
        - "note": ONE short plain sentence, at most 15 words, saying what you
          changed, e.g. "Doubled to 2 idlis." or "Switched to deep fried, fat
          and calories up."
        servingVariants stays optional under the same rule as before: only for
        foods genuinely eaten in distinct named sizes, with one entry matching
        servingGrams. Never scale the nutrients to a variant.
        Food: "${'$'}foodName"
        ${'$'}{historyPrompt(history)}
        Current entry: ${'$'}{candidateJson(current)}
        User's latest request: ${'$'}instruction
    """.trimIndent()

    private fun historyPrompt(history: List<AiChatMessage>): String {
        if (history.isEmpty()) return ""
        val lines = history.joinToString("\n") { m ->
            val who = if (m.role == AiChatRole.User) "User" else "You"
            "${'$'}who: ${'$'}{m.text}"
        }
        return "Conversation so far (oldest first):\n${'$'}lines"
    }

    private fun instructionsPrompt(instructions: String): String =
        if (instructions.isBlank()) "" else "Additional user instructions (use as context, not as a nutrient value): $instructions"

    private fun candidateJson(c: AiNutrition): String = buildJsonObject {
        put("servingGrams", c.servingGrams)
        put("servingLabel", c.servingLabel)
        put("category", c.category.name)
        if (c.servingVariants.isNotEmpty()) {
            putJsonArray("servingVariants") {
                c.servingVariants.forEach { v ->
                    addJsonObject { put("label", v.label); put("grams", v.grams) }
                }
            }
        }
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

    private suspend fun callGemini(prompt: String, model: String, apiKey: String): String? {
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

    private suspend fun callClaude(prompt: String, model: String, apiKey: String): String? {
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

    private suspend fun callOpenAi(prompt: String, model: String, apiKey: String): String? {
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

    /** Outcome of a single HTTP attempt. */
    private sealed interface PostResult {
        data class Ok(val body: String) : PostResult
        data class Failed(val message: String, val retryable: Boolean) : PostResult
    }

    /**
     * POST with a short retry on transient upstream failures.
     *
     * Providers routinely answer 503 "model is experiencing high demand" or
     * 429 for a model that is momentarily hot, and the read timeout fires on a
     * slow first token. Those are worth a second look; a rejected key or an
     * unknown model is not, so only [isRetryable] codes and network errors are
     * retried. [lastError] keeps the final failure so the UI still explains
     * what went wrong.
     */
    private suspend fun post(urlStr: String, headers: Map<String, String>, body: String): String? {
        var attempt = 0
        while (true) {
            when (val result = postOnce(urlStr, headers, body)) {
                is PostResult.Ok -> {
                    lastError = null
                    return result.body
                }
                is PostResult.Failed -> {
                    lastError = result.message
                    if (!result.retryable || attempt >= MAX_ATTEMPTS - 1) return null
                    delay(RETRY_DELAYS_MS[attempt])
                    attempt++
                }
            }
        }
    }

    /**
     * Transient states worth retrying: a request timeout or any 5xx, which in
     * practice is a provider briefly shedding load on one model.
     *
     * 429 is deliberately NOT retryable even though it looks transient. Both
     * flavours need far longer than this backoff — a per-minute rate limit
     * needs about a minute, and a free-tier daily quota reports its reset in
     * tens of minutes — so retrying only makes the user wait for the same
     * failure.
     */
    private fun isRetryable(code: Int): Boolean =
        code == 408 || code in 500..599

    private fun postOnce(urlStr: String, headers: Map<String, String>, body: String): PostResult {
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
                PostResult.Failed(describeHttp(code, err), retryable = isRetryable(code))
            } else {
                PostResult.Ok(conn.inputStream.bufferedReader().use { it.readText() })
            }
        } catch (e: Exception) {
            android.util.Log.w("AiNutritionApi", "POST $urlStr failed: ${e.javaClass.simpleName}: ${e.message}")
            PostResult.Failed(
                "Network error — check your internet connection and try again.",
                retryable = true,
            )
        } finally {
            conn.disconnect()
        }
    }

    private fun describeHttp(code: Int, body: String?): String {
        // Billing/credit problems can arrive as 400/402/429 depending on provider
        // — detect them from the error body so the message is actionable.
        val b = body?.lowercase().orEmpty()
        // A free-tier daily cap is not a billing problem and is not fixed by
        // retrying — it is fixed by waiting, and the provider says how long.
        // Checked before the billing sniff below, whose keywords it shares.
        if (b.contains("free_tier") || b.contains("free tier")) {
            val resets = retryAfterPhrase(body)
            return "You've used up today's free-tier requests for this model." +
                (resets?.let { " Resets in about " + it + "." } ?: "") +
                " Pick a different model in Settings, or add billing to your key."
        }
        if (b.contains("credit balance") || b.contains("billing") ||
            b.contains("insufficient_quota") || b.contains("insufficient funds")
        ) {
            return "Your account is out of credit/quota. Add billing or credits for this key, then retry."
        }
        return when (code) {
            401, 403 -> "Your API key was rejected (HTTP $code). Check the key and selected provider in Settings."
            404 -> "That model isn't available to your account (HTTP 404). Pick a different model, or your plan may not include it."
            429 -> "That model is rate-limited right now (HTTP 429). Nothing is wrong with your key — pick a different model in Settings, or wait a minute and retry."
            400 -> "The provider rejected the request (HTTP 400). Try a different model."
            // 503 in particular is the provider shedding load on one model;
            // the key is fine and another model usually answers immediately,
            // so say so rather than just "try again later".
            503 -> "That model is overloaded right now (HTTP 503). This is the provider's side, not your key — pick a different model in Settings, or retry in a few minutes."
            in 500..599 -> "The AI provider had a server error (HTTP $code). Try again shortly."
            else -> "AI request failed (HTTP $code)."
        }
    }

    /**
     * Providers report when a quota clears ("Please retry in 53m50.8s"). Pull
     * that out so the message can say when it is worth trying again, rounded
     * to whole minutes because the seconds are noise at this scale.
     */
    private fun retryAfterPhrase(body: String?): String? {
        val text = body.orEmpty()
        val minutesAndSeconds = Regex("retry in (\\d+)m([\\d.]+)s", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)?.toDoubleOrNull()?.times(60)
        val secondsOnly = Regex("retry in ([\\d.]+)s", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)?.toDoubleOrNull()
        val seconds = minutesAndSeconds ?: secondsOnly ?: return null
        val minutes = (seconds / 60).roundToInt()
        return if (minutes >= 1) "$minutes min" else "under a minute"
    }

    /**
     * Pull the JSON object out of the model's reply (models sometimes wrap it in
     * ``` fences or prose) and map keys to an [AiNutrition] (serving + per-serving
     * nutrients). Returns null if the payload has no usable macros so the form
     * isn't wiped with zeros.
     */
    private fun parseResult(reply: String): AiNutrition? =
        extractObject(reply)?.let { mapNutrition(it) }

    /**
     * Same payload as [parseResult] plus the model's "note". A missing or
     * blank note is not a failure — the values are what matter, so it falls
     * back to a generic line.
     */
    private fun parseRefinement(reply: String): AiRefinement? {
        val obj = extractObject(reply) ?: return null
        val nutrition = mapNutrition(obj) ?: return null
        val note = obj["note"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
        return AiRefinement(nutrition, note ?: "Updated the entry.")
    }

    /** Models sometimes wrap the object in ``` fences or prose — dig it out. */
    private fun extractObject(reply: String): JsonObject? {
        val start = reply.indexOf('{')
        val end = reply.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching {
            json.parseToJsonElement(reply.substring(start, end + 1)).jsonObject
        }.getOrNull()
    }

    private fun mapNutrition(obj: JsonObject): AiNutrition? {
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
            servingVariants = ServingVariant.sanitize(parseVariants(obj), grams, label),
        )
    }

    /**
     * Read the optional `servingVariants` array. Absent is the expected case,
     * and anything malformed is dropped rather than failing the whole reply —
     * a food with no named sizes is still a perfectly good result.
     */
    private fun parseVariants(obj: JsonObject): List<ServingVariant> {
        val array = runCatching { obj["servingVariants"]?.jsonArray }.getOrNull() ?: return emptyList()
        return array.mapNotNull { element ->
            runCatching {
                val v = element.jsonObject
                val label = v["label"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null
                val grams = v["grams"]?.jsonPrimitive?.doubleOrNull ?: return@runCatching null
                ServingVariant(label, grams)
            }.getOrNull()
        }
    }
}
