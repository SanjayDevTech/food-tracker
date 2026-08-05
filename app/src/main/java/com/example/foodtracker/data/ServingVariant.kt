package com.example.foodtracker.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * One way to portion a food (e.g. "1 idli" = 35 g, "1 cup" = 220 g, "100 g" = 100 g).
 * Foods carry an ordered list of these; the first one is the default. The base
 * FoodEntity.servingGrams/servingLabel always remain the canonical "1 serving"
 * reference — variants are alternate ways to express it.
 */
@Immutable
@Serializable
data class ServingVariant(
    val label: String,
    val grams: Double,
) {
    companion object {
        val Default = ServingVariant("1 serving (100 g)", 100.0)

        /** Build a sensible variant list given the canonical serving + label. */
        fun defaultVariantsFor(servingGrams: Double, servingLabel: String): List<ServingVariant> {
            val canonical = ServingVariant(servingLabel, servingGrams)
            val gramRef = ServingVariant("100 g", 100.0)
            val small = ServingVariant("50 g", 50.0)
            return if (servingGrams == 100.0) listOf(canonical, small)
            else listOf(canonical, gramRef)
        }
    }
}
