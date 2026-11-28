package com.example.foodtracker.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

/** Guards `assets/food_catalogue.json`, the extended food list. */
class FoodCatalogueTest {
    private val root: JsonObject = Json.parseToJsonElement(
        File("src/main/assets/food_catalogue.json").readText()
    ).jsonObject
    private val foods = root.getValue("foods").jsonArray.map { it.jsonObject }

    @Test fun catalogueBringsTheAppPast500Foods() {
        // 178 hand-tuned seed foods plus this file.
        assertTrue("only ${178 + foods.size} foods", 178 + foods.size >= 500)
    }

    /**
     * Omega-3 is the cooking oil's, and must stay exactly that.
     *
     * Every figure in this file describes the finished dish *including* its
     * default oil — which is what [withCookingOil] and [withOilAmount] adjust
     * against when the user picks a different oil or less of it. For almost
     * all of these dishes the oil is the only omega-3 present, so the stored
     * value is `oilGrams x oil.omega3 / 100` and nothing else. A dish that
     * stored 0 while frying in 35 g of ghee would lose the oil's share on
     * every swap.
     *
     * If a food ever gains a genuine base contribution of its own — a fish
     * curry, something built on flax or walnut — it will fail here, and that
     * is the point: the exception should be deliberate, not a drifted number.
     */
    @Test fun omega3IsTheCookingOilPlusADeclaredBase() {
        val perGram = mapOf(
            "Palmolein" to 0.20, "Coconut" to 0.0, "Groundnut" to 0.0,
            "Gingelly" to 0.30, "Ghee" to 1.4,
        )
        foods.forEach { f ->
            val name = f.str("name")
            val oil = f.str("oil")
            val grams = f.getValue("oilGrams").jsonPrimitive.double
            val fromOil = grams * (perGram[oil] ?: error("unknown oil $oil")) / 100
            val actual = f.getValue("per100g").jsonObject.getValue("omega3").jsonPrimitive.double
            val base = actual - fromOil
            // Never less than its oil: that is the floor the oil axis subtracts against.
            assertTrue("$name: $actual is below its oil's $fromOil", base > -0.0005)
            if (name !in OMEGA3_SOURCES) {
                assertEquals("$name has omega-3 beyond its oil but is not a declared source", 0.0, base, 0.0005)
            } else {
                assertTrue("$name is declared an omega-3 source but adds nothing", base > 0.0005)
            }
        }
    }

    /**
     * Copper and manganese are derived from each dish's composition, so the
     * thing worth guarding is not a value but the fact that they were derived
     * at all: a food added later without them would read as a true zero and
     * quietly understate the day's total.
     *
     * The tell is the minerals they came from. Copper travels with the plant
     * matter and animal tissue that also carry magnesium and phosphorus, and
     * manganese with anything that has fibre, so neither can be absent where
     * those are present. Milk, cola and the other fibreless liquids really do
     * have no manganese — the seed records 0 for milk and curd too — so they
     * are the only foods left sitting at zero.
     */
    @Test fun copperAndManganeseAreDerivedForEveryFood() {
        foods.forEach { f ->
            val n = f.getValue("per100g").jsonObject
            fun v(k: String) = n[k]?.jsonPrimitive?.double ?: error("${f.str("name")} has no $k")
            val cu = v("copper")
            val mn = v("manganese")
            assertTrue("${f.str("name")}: copper $cu is negative", cu >= 0.0)
            assertTrue("${f.str("name")}: manganese $mn is negative", mn >= 0.0)
            // Nothing in this catalogue is richer than a seed or a spice mix.
            assertTrue("${f.str("name")}: copper $cu is implausibly high", cu <= 5.0)
            assertTrue("${f.str("name")}: manganese $mn is implausibly high", mn <= 6.0)
            if (v("magnesium") >= 20.0 && v("phosphorus") >= 50.0) {
                assertTrue(
                    "${f.str("name")} carries magnesium and phosphorus but no copper",
                    cu > 0.0,
                )
            }
            if (v("fiber") >= 0.5) {
                assertTrue("${f.str("name")} carries fibre but no manganese", mn > 0.0)
            }
        }
    }

    /**
     * Fibre is split by a ratio, so the split cannot contradict the total.
     *
     * The band is what keeps this honest: every fraction IFCT records for an
     * Indian food falls between roughly 0.07 (urad-free whole pulses) and 0.50
     * (watermelon), and a composite dish blends toward the middle, so anything
     * outside 0.05-0.60 is a arithmetic slip rather than a surprising food.
     */
    @Test fun fibreIsSplitBySourceWhereverThereIsAnyFibre() {
        foods.forEach { f ->
            val n = f.getValue("per100g").jsonObject
            val fiber = n.getValue("fiber").jsonPrimitive.double
            val frac = n["solubleFiberFraction"]?.jsonPrimitive?.double ?: 0.0
            if (fiber >= 0.05) {
                assertTrue("${f.str("name")} has ${fiber}g fibre but no soluble share", frac > 0.0)
                assertTrue("${f.str("name")}: soluble share $frac is outside 0.05-0.60",
                    frac in 0.05..0.60)
            } else {
                assertEquals("${f.str("name")} has no fibre to split", 0.0, frac, 1e-9)
            }
        }
    }

    @Test fun namesAndIdsAreUnique() {
        assertEquals(foods.size, foods.map { it.str("name").lowercase() }.toSet().size)
        assertEquals(foods.size, foods.map { it.str("uuid") }.toSet().size)
    }

    @Test fun energyAgreesWithMacros() {
        foods.forEach { f ->
            val n = f.getValue("per100g").jsonObject
            fun v(k: String) = n.getValue(k).jsonPrimitive.double
            val kcal = v("calories")
            val est = 4 * v("carbs") + 4 * v("protein") + 9 * v("fat")
            if (kcal > 20) assertTrue("${f.str("name")}: $kcal vs $est", abs(kcal - est) / kcal <= 0.18)
            assertTrue("${f.str("name")}: fiber > carbs", v("fiber") <= v("carbs") + 0.01)
        }
    }

    @Test fun servingLabelSplitsIntoTwoTags() {
        assertEquals("1 bowl" to "200 g", com.example.foodtracker.ui.components.servingParts("1 bowl (200 g)"))
        assertEquals("1 glass" to "250 ml", com.example.foodtracker.ui.components.servingParts("1 glass (250 ml)"))
        assertEquals("100 g" to null, com.example.foodtracker.ui.components.servingParts("100 g"))
    }

    /**
     * The foods that carry omega-3 of their own, on top of their cooking oil.
     *
     * Fish and seafood, the leafy greens, and walnut — nothing else in this
     * catalogue is an omega-3 source, so for every other food the oil is the
     * whole figure. Composite dishes take the source's density scaled by how
     * much of the dish it is: for fish that share is read off the dish's own
     * protein against the species', and for greens it is a flat 0.30, because
     * a leaf contributes far less protein than it does mass.
     *
     * Listing them here rather than inferring them is the point. A new food
     * that quietly gained omega-3 beyond its oil fails this test, and so does
     * one listed here that lost its base.
     */
    private val OMEGA3_SOURCES = setOf(
        "Amaranth Leaves (Chaulai, raw)",
        "Amritsari Fish Fry",
        "Chepala Pulusu (Andhra Fish Curry)",
        "Chingri Malai Curry",
        "Dal Palak",
        "Fish Fry (Tawa, Surmai/Seer)",
        "Fish Molee",
        "Karimeen Pollichathu",
        "Kerala Meen Curry (Fish Curry)",
        "Kerala Prawn Roast (Chemmeen Roast)",
        "Machher Jhol (Fish Curry)",
        "Methi Aloo",
        "Methi Leaves (Fenugreek, raw)",
        "Methi Muthia",
        "Methi Thepla",
        "Palakura Pappu (Spinach Dal)",
        "Pomfret (grilled/steamed)",
        "Prawn Biryani",
        "Prawns (cooked)",
        "Rohu Fish (cooked)",
        "Sarson Ka Saag",
        "Shorshe Ilish",
        "Tuna (canned in water)",
        "Vanjaram Fry (Seer Fish Fry)",
        "Walnuts (Akhrot)",
    )

    private fun JsonObject.str(k: String) = getValue(k).jsonPrimitive.content
}
