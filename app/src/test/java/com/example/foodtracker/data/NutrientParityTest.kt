package com.example.foodtracker.data

import com.example.foodtracker.data.db.IndianFoodSeed
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The two halves of the food list must carry the same nutrients.
 *
 * Foods reach Room from two places — [IndianFoodSeed], hand-tuned in Kotlin,
 * and `assets/food_catalogue.json`, authored in bulk — and the app merges them
 * into one table that nothing else ever distinguishes. A user cannot tell
 * which half a food came from, so a nutrient present in one and missing from
 * the other does not read as a known limit. It reads as "idli contributes
 * selenium and masala dosa contributes none", and it silently understates
 * every goal computed from it.
 *
 * That is not hypothetical. The two halves were written months apart against
 * different field sets, and selenium, copper, manganese and the fibre split
 * sat on 178 foods and no others for as long as nobody thought to check. This
 * is the check.
 *
 * A field has to be populated on both sides or on neither. Where that is
 * genuinely not possible the field goes in [ONE_SIDED] **with its reason** —
 * the point is that skipping one is a decision somebody wrote down, not an
 * oversight that nobody noticed.
 */
class NutrientParityTest {

    /** Carried by more than this share of a half: the field is "present" there. */
    private val PRESENT = 0.50

    /** Carried by less than this share: the field is "absent" there. */
    private val ABSENT = 0.02

    /**
     * Fields allowed to sit on one side only, and why.
     *
     * Each of these is a live decision, not a dead entry — delete one from
     * this set the moment its nutrient is filled in, so the guard starts
     * holding it.
     */
    private val ONE_SIDED = mapOf(
        // Derived figures land within 36% at best and miss by more than 1.5x
        // half the time: selenium follows the soil a crop grew in, and no
        // dish composition recovers that. Goal-tracked, so still a real gap —
        // it wants sourcing, not estimating.
        "selenium" to "no method reaches it; needs per-food sourcing",
    )

    // Not listed above, and deliberately so: vitaminD (8% of the seed, none of
    // the catalogue), essentialAminoAcids and leucine (16% / none). Each is
    // sparse in BOTH halves rather than carried by one and missing from the
    // other, which is a different thing and not what this guards. Vitamin D is
    // genuinely zero in almost every Indian dish; the amino breakdown only
    // ever applied to ~25 single ingredients and is being dropped. If any of
    // them is ever filled in across the seed and left out of the catalogue,
    // that IS a parity break and this test will say so.

    /** Every tracked nutrient, by its catalogue key and its [Nutrients] field. */
    private val FIELDS: List<Pair<String, (Nutrients) -> Double>> = listOf(
        "carbs" to { it.carbs }, "sugar" to { it.sugar }, "fiber" to { it.fiber },
        "protein" to { it.protein }, "fat" to { it.fat },
        "saturatedFat" to { it.saturatedFat }, "omega3" to { it.omega3 },
        "cholesterol" to { it.cholesterol }, "sodium" to { it.sodium },
        "calcium" to { it.calcium }, "iron" to { it.iron },
        "magnesium" to { it.magnesium }, "potassium" to { it.potassium },
        "zinc" to { it.zinc }, "phosphorus" to { it.phosphorus },
        "selenium" to { it.selenium }, "copper" to { it.copper },
        "manganese" to { it.manganese }, "vitaminA" to { it.vitaminA },
        "vitaminC" to { it.vitaminC }, "vitaminD" to { it.vitaminD },
        "vitaminE" to { it.vitaminE }, "vitaminK" to { it.vitaminK },
        "vitaminB12" to { it.vitaminB12 }, "folate" to { it.folate },
        "vitaminB1" to { it.vitaminB1 }, "vitaminB2" to { it.vitaminB2 },
        "vitaminB3" to { it.vitaminB3 }, "vitaminB6" to { it.vitaminB6 },
        "solubleFiber" to { it.solubleFiber }, "insolubleFiber" to { it.insolubleFiber },
        "essentialAminoAcids" to { it.essentialAminoAcids }, "leucine" to { it.leucine },
    )

    private val catalogue: List<JsonObject> = Json
        .parseToJsonElement(File("src/main/assets/food_catalogue.json").readText())
        .jsonObject.getValue("foods").jsonArray.map { it.jsonObject.getValue("per100g").jsonObject }

    private val seed = IndianFoodSeed.ALL.map { it.nutrientsPer100g() }

    /**
     * The catalogue stores the soluble share as a ratio, so the two fibre
     * fields are derived there rather than stored — the same arithmetic
     * `FoodCatalogue` does on the way into Room.
     */
    private fun JsonObject.value(key: String): Double {
        val raw = { k: String -> this[k]?.jsonPrimitive?.double ?: 0.0 }
        val fraction = raw("solubleFiberFraction")
        return when (key) {
            "solubleFiber" -> raw("fiber") * fraction
            "insolubleFiber" -> if (fraction > 0.0) raw("fiber") - raw("fiber") * fraction else 0.0
            else -> raw(key)
        }
    }

    @Test fun everyNutrientIsCarriedByBothHalvesOrNeither() {
        val complaints = mutableListOf<String>()
        FIELDS.forEach { (key, read) ->
            val inSeed = seed.count { read(it) > 0.0 }.toDouble() / seed.size
            val inCatalogue = catalogue.count { it.value(key) > 0.0 }.toDouble() / catalogue.size
            val lopsided = (inSeed > PRESENT && inCatalogue < ABSENT) ||
                (inCatalogue > PRESENT && inSeed < ABSENT)
            val excused = key in ONE_SIDED
            if (lopsided && !excused) {
                complaints += "$key: ${pct(inSeed)} of the seed, ${pct(inCatalogue)} of the " +
                    "catalogue. Fill the empty side, or add it to ONE_SIDED with the reason."
            }
            if (!lopsided && excused) {
                complaints += "$key is listed in ONE_SIDED but is no longer one-sided " +
                    "(${pct(inSeed)} / ${pct(inCatalogue)}). Delete the entry so the guard holds it."
            }
        }
        assertTrue(complaints.joinToString("\n"), complaints.isEmpty())
    }

    /** Both halves exist and are the size the rest of the suite assumes. */
    @Test fun bothHalvesArePresent() {
        assertTrue("seed is empty", seed.size > 100)
        assertTrue("catalogue is empty", catalogue.size > 300)
    }

    private fun pct(share: Double) = "${(share * 100).toInt()}%"
}
