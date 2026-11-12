package com.example.foodtracker.domain

import com.example.foodtracker.data.NutrientField
import com.example.foodtracker.data.NutrientGoals
import com.example.foodtracker.data.db.FoodEntity

/** A food plus why it ranked, ready to render. */
data class RichFood(
    val food: FoodEntity,
    /** The weakest selected nutrient's share of goal. Meaningful only against peers. */
    val score: Double,
    /** Per selected nutrient, in field order: the amount in one serving. */
    val amounts: List<Double>,
    /** Per selected nutrient: that amount as a fraction of the daily goal. */
    val goalFractions: List<Double>,
)

/**
 * Rank foods by how much of the selected nutrients one serving delivers.
 *
 * Three decisions do the real work here.
 *
 * **The common unit is "fraction of your daily goal", not grams.** Grams
 * cannot be compared across nutrients — 8 g of fiber and 31 g of protein are
 * not 8 and 31 of the same thing, and combining them would rank by whichever
 * nutrient happens to be measured in bigger numbers. Dividing by the user's
 * own goal turns both into "how much of today's need this covers", which is
 * comparable and is also the thing the user wants to know. Each fraction is
 * capped at 1.0 first: a food supplying triple your protein should not thereby
 * win a search you ran for fiber.
 *
 * **It measures one serving, not 100 g.** Per 100 g is a property of the food;
 * per serving is a property of eating it. Ranked per 100 g these foods put
 * fried papad and idli podi at the top of a protein+fiber search — true of the
 * powder, useless as advice, since nobody eats 100 g of either.
 *
 * **It ranks, and never filters.** A threshold ("must have >=X") looked
 * obvious and is unusable: swept over these foods for protein+fiber it returns
 * 54 results at 5% of goal, 27 at 10%, 4 at 15% and 0 at 20% — a cliff within
 * a few points, in a different place for every pair of nutrients, and an empty
 * screen the user cannot account for.
 *
 * ### Why the score is the minimum
 *
 * With several nutrients picked, a food scores by its WEAKEST one, so nothing
 * can ride in on a single strong number: chicken biryani (34% of the protein
 * goal, 13% of the fiber goal) ranks 14th in a protein+fiber search, below
 * every pulse.
 *
 * The obvious alternatives are compensatory and were measured against these
 * foods before being rejected. A geometric mean lifts that same biryani to 6th
 * on protein+fiber and to 1st on protein+fiber+fat, because multiplying lets a
 * big protein figure buy off a small fiber one; a harmonic mean does the same
 * more steeply (8th). That trade-off is structural, not a tuning problem — a
 * minimum is the only combination a strong nutrient cannot offset a weak one
 * through, which is exactly what "rich in all of them" promises.
 *
 * The minimum is also the only rule the user can check. The results show one
 * bar per nutrient, so under a minimum the ordering rule is "look at the
 * shorter bar" and a reader can confirm at a glance that the first row belongs
 * above the second. Under either mean the order cannot be worked out by eye,
 * which makes a correct ranking indistinguishable from a broken one.
 *
 * The cost is a narrow one: a food with a better floor outranks one that is
 * far better overall on everything else — horse gram (18%/29%) above whole
 * urad (16%/46%). That is the guarantee being honest rather than a defect.
 * Horse gram really does deliver more of whichever nutrient you are shortest
 * on. Where the floors tie, [rank] falls back to the total, which is what
 * separates two dals both capped by the same protein figure.
 */
object NutrientRanking {

    /** Below this share of goal on its weakest pick, a food isn't worth showing. */
    private const val MIN_SCORE = 0.01

    fun rank(
        foods: List<FoodEntity>,
        fields: List<NutrientField>,
        goals: NutrientGoals,
    ): List<RichFood> {
        if (fields.isEmpty()) return emptyList()
        return foods
            .mapNotNull { food ->
                val perServing = food.nutrientsPer100g() * (food.servingGrams / 100.0)
                val amounts = fields.map { it.read(perServing) }
                val fractions = fields.mapIndexed { i, field ->
                    val goal = field.goal(goals)
                    if (goal <= 0.0) 0.0 else (amounts[i] / goal).coerceAtMost(1.0)
                }
                // With one nutrient picked this is simply that nutrient, so the
                // single- and multi-nutrient cases need no special casing.
                val score = fractions.min()
                if (score < MIN_SCORE) return@mapNotNull null
                RichFood(food, score, amounts, fractions)
            }
            .sortedWith(
                compareByDescending<RichFood> { it.score }
                    .thenByDescending { it.goalFractions.sum() }
                    .thenBy { it.food.name },
            )
    }
}
