package com.example.foodtracker.data

import android.content.Context
import com.example.foodtracker.data.db.FoodEntity
import com.example.foodtracker.data.db.IndianFoodSeed
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

/**
 * The extended food catalogue, kept as data rather than code:
 * `assets/food_catalogue.json` holds every food beyond the hand-tuned
 * [IndianFoodSeed] list — nutrition per 100 g, serving, default cooking oil and
 * preparation choices, and a one-line "how it's made".
 *
 * Foods are seeded into Room alongside [IndianFoodSeed.ALL]; the "made" lines
 * stay in memory (they are reference data, not user data, so they need no
 * column, migration or backup).
 */
object FoodCatalogue {

    private const val ASSET = "food_catalogue.json"

    @Serializable
    private data class File(
        val foods: List<Entry> = emptyList(),
    )

    @Serializable
    private data class Entry(
        val name: String,
        val uuid: String,
        val category: String,
        val servingGrams: Double,
        val servingLabel: String,
        val per100g: Per100g,
        val oilGrams: Double = 0.0,
        val oil: String = "Palmolein",
        val styles: List<Style> = emptyList(),
        val made: String = "",
    )

    @Serializable
    private data class Style(val label: String, val ingredient: String? = null, val grams: Double = 0.0)

    @Serializable
    private data class Per100g(
        val calories: Double = 0.0, val carbs: Double = 0.0, val sugar: Double = 0.0, val fiber: Double = 0.0,
        /**
         * The soluble share of [fiber], held as a ratio for the same reason
         * [IndianFoodSeed] holds it that way: the two parts then add up to the
         * total shown above them instead of contradicting it.
         *
         * The seed's own fractions are looked up per ingredient from IFCT 2017
         * Table 1, and a composite dish is weighted by the fibre each
         * component contributes rather than by its mass. This file follows the
         * same rule, but the composition comes from the solve described on
         * [copper] below — so the class shares were not taken on faith, they
         * were fitted to the seed's own 151 sourced fractions and
         * cross-validated. Typical error is 0.05 on a fraction whose real
         * range is 0.07 to 0.50, or about 0.11 g of soluble fibre per 100 g.
         *
         * 0 means unknown, which the app hides, and is what the 28 foods with
         * no fibre at all get.
         */
        val solubleFiberFraction: Double = 0.0,
        val protein: Double = 0.0, val fat: Double = 0.0, val saturatedFat: Double = 0.0,
        /**
         * Like every other figure here, this is the finished dish **including
         * its default cooking oil** — which for most of the catalogue is all
         * the omega-3 there is. [CookingOil.withCookingOil] and
         * [withOilAmount] both adjust relative to that, so a dish that stored
         * 0 while frying in 35 g of ghee would lose the oil's share on every
         * swap. See `oilGrams` and `oil` below, which is where the figure
         * comes from.
         */
        val omega3: Double = 0.0,
        val cholesterol: Double = 0.0, val sodium: Double = 0.0, val potassium: Double = 0.0,
        val calcium: Double = 0.0, val iron: Double = 0.0, val magnesium: Double = 0.0, val zinc: Double = 0.0,
        val phosphorus: Double = 0.0,
        /**
         * Copper and manganese are **derived, not looked up**.
         *
         * No reference table carries them for a shorshe ilish or a rava
         * kesari, so each dish's composition is solved from the ten figures
         * already recorded for it — energy, the macros, fibre, and the four
         * minerals iron, magnesium, zinc and phosphorus — against a table of
         * food classes (wheat, rice, millet, dal, coconut, fish, dairy…), and
         * these two are read off that composition. The cooking oil is taken
         * out first, since `oilGrams` already states it.
         *
         * Checked against the 178 hand-sourced [IndianFoodSeed] foods, scoring
         * only the mixed dishes — the single ingredients are what the class
         * table was built from, so they grade the model on its own input.
         * Median error is 22% for copper and 26% for manganese, roughly two in
         * three within 1.5x of the real figure, with no systematic drift.
         *
         * Selenium is deliberately absent. The same method gives 36% on it and
         * misses by more than 1.5x half the time, because selenium follows the
         * soil a crop grew in and nothing in a dish's composition recovers
         * that. A wrong number is worse than a missing one.
         */
        val copper: Double = 0.0, val manganese: Double = 0.0,
        val choline: Double = 0.0,
        val vitaminA: Double = 0.0, val vitaminC: Double = 0.0,
        val vitaminE: Double = 0.0, val vitaminB1: Double = 0.0, val vitaminB2: Double = 0.0,
        val vitaminB3: Double = 0.0, val vitaminB6: Double = 0.0, val folate: Double = 0.0,
        val vitaminB12: Double = 0.0, val vitaminK: Double = 0.0,
    ) {
        fun toNutrients(): Nutrients {
            val soluble = fiber * solubleFiberFraction
            return base().copy(
                solubleFiber = soluble,
                insolubleFiber = if (solubleFiberFraction > 0.0) fiber - soluble else 0.0,
            )
        }

        private fun base() = Nutrients(
            calories = calories, carbs = carbs, sugar = sugar, fiber = fiber, protein = protein, fat = fat,
            saturatedFat = saturatedFat, omega3 = omega3,
            cholesterol = cholesterol, sodium = sodium, potassium = potassium,
            calcium = calcium, iron = iron, magnesium = magnesium, zinc = zinc, phosphorus = phosphorus,
            copper = copper, manganese = manganese, choline = choline,
            vitaminA = vitaminA, vitaminC = vitaminC, vitaminE = vitaminE, vitaminB1 = vitaminB1,
            vitaminB2 = vitaminB2, vitaminB3 = vitaminB3, vitaminB6 = vitaminB6, folate = folate,
            vitaminB12 = vitaminB12, vitaminK = vitaminK,
        )
    }

    class Loaded(
        val foods: List<FoodEntity>,
        val made: Map<String, String>,
    )

    @Volatile
    private var loaded: Loaded? = null

    private val json = Json { ignoreUnknownKeys = true }

    /** Parses the asset once per process; later calls return the same result. */
    fun load(context: Context): Loaded = loaded ?: synchronized(this) {
        loaded ?: parse(context).also { loaded = it }
    }

    /** One line on how the dish is made, when the catalogue has one. */
    fun madeOf(name: String): String? = loaded?.made?.get(key(name))

    private fun key(name: String) = name.lowercase(Locale.ROOT)

    private fun parse(context: Context): Loaded {
        val file = runCatching {
            context.assets.open(ASSET).use { json.decodeFromString(File.serializer(), it.reader().readText()) }
        }.getOrElse { File() } // A missing or broken asset leaves the hand-tuned seed working on its own.
        val foods = file.foods.map { e ->
            FoodEntity.fromNutrientsPer100g(
                name = e.name,
                clientUuid = e.uuid,
                category = FoodCategory.fromName(e.category).name,
                n = e.per100g.toNutrients(),
                servingGrams = e.servingGrams,
                servingLabel = e.servingLabel,
                oilGrams = e.oilGrams,
                oilDefault = CookingOil.entries.firstOrNull { it.name == e.oil } ?: CookingOil.Palmolein,
                styleOptions = e.styles.map { s ->
                    StyleOption(
                        label = s.label,
                        ingredient = VariantIngredient.entries.firstOrNull { it.name == s.ingredient },
                        grams = s.grams,
                    )
                },
                servingVariants = IndianFoodSeed.sizeVariantsFor(e.servingGrams, e.servingLabel),
            )
        }
        return Loaded(
            foods = foods,
            made = file.foods.filter { it.made.isNotBlank() }.associate { key(it.name) to it.made },
        )
    }
}
