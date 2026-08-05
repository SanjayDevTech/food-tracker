package com.example.foodtracker.data

/**
 * A selectable model for a provider. [id] is the value sent to the API.
 * [premium] marks the pricier/stronger models — for those the two-pass autofill
 * skips its second "verify" call (they're accurate enough on the first pass),
 * saving a request.
 */
data class AiModel(val id: String, val label: String, val premium: Boolean = false)

/**
 * AI providers the optional nutrition-autofill can call. The user supplies
 * their own API key (there is no bundled token) and picks one provider — see
 * [AiNutritionApi] for the per-provider request wiring.
 *
 * Each provider offers a short [models] list ordered cheapest-first; the first
 * entry ([defaultModel]) is the cheapest model that's still accurate enough for
 * nutrition lookups and is what's selected by default. Model ids track what the
 * providers publish and may need bumping as new versions ship.
 */
enum class AiProvider(
    val displayName: String,
    /** Short label for where the user creates a key (shown as the link text). */
    val keyHint: String,
    /** Full URL to the provider's API-key page — opened when the hint is tapped. */
    val keyUrl: String,
    val models: List<AiModel>,
) {
    Gemini(
        "Gemini",
        "aistudio.google.com/apikey",
        "https://aistudio.google.com/apikey",
        listOf(
            AiModel("gemini-3.1-flash-lite", "3.1 Flash Lite"),
            AiModel("gemini-3.6-flash", "3.6 Flash"),
            AiModel("gemini-3.1-pro-preview", "3.1 Pro", premium = true),
        ),
    ),
    Claude(
        "Claude",
        "console.anthropic.com",
        "https://console.anthropic.com/settings/keys",
        listOf(
            AiModel("claude-haiku-4-5-20251001", "Haiku 4.5"),
            AiModel("claude-sonnet-5", "Sonnet 5", premium = true),
        ),
    ),
    OpenAI(
        "ChatGPT",
        "platform.openai.com/api-keys",
        "https://platform.openai.com/api-keys",
        listOf(
            AiModel("gpt-5.4-mini", "5.4 mini"),
            AiModel("gpt-5.5", "5.5", premium = true),
        ),
    );

    /** Cheapest capable model — the default selection. */
    val defaultModel: AiModel get() = models.first()

    /** Resolve a stored id to one of this provider's models, else the default. */
    fun modelFromId(id: String?): AiModel = models.firstOrNull { it.id == id } ?: defaultModel

    companion object {
        fun fromName(name: String?): AiProvider =
            entries.firstOrNull { it.name == name } ?: Gemini
    }
}
