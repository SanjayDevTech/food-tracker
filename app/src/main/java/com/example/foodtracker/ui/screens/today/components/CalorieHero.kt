package com.example.foodtracker.ui.screens.today.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.foodtracker.data.NutrientGoals
import com.example.foodtracker.data.Nutrients
import com.example.foodtracker.ui.components.atoms.Meter
import com.example.foodtracker.ui.components.atoms.grouped
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.MacroCarbs
import com.example.foodtracker.ui.theme.MacroFat
import com.example.foodtracker.ui.theme.MacroFiber
import com.example.foodtracker.ui.theme.MacroProtein
import com.example.foodtracker.ui.theme.Spacing
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The day's calories — reference shot 4, which §0 called "effectively a visual
 * spec for the thesis in §1 already".
 *
 * **The ring is gone.** It came from shots 1, 5 and 9 — Direction A, the one
 * §0 rejected — and it was the single loudest piece of Direction A's anatomy
 * left on the main screen. Shot 4, the direction actually chosen, draws this as
 * an oversized tabular numeral over a thin rule with two labelled columns
 * underneath, and that is what this is now.
 *
 * What a ring cost that this does not: 140dp of height to encode one fraction,
 * a 40sp cap on the number inside it, and a second circular shape language on a
 * screen whose structure is otherwise rectilinear. The number can now be 64sp,
 * which is the point — "the only thing allowed to be big is a number".
 */
@Composable
fun CalorieHero(
    consumed: Nutrients,
    goals: NutrientGoals,
    modifier: Modifier = Modifier,
    loaded: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val left = goals.calories - consumed.calories
    val fraction = if (goals.calories > 0) (consumed.calories / goals.calories).toFloat() else 0f
    val over = left < 0
    val pct = if (goals.calories > 0) ((consumed.calories / goals.calories) * 100).roundToInt() else 0

    Column(modifier.fillMaxWidth()) {
        Text(
            if (over) "OVER BY" else "REMAINING",
            style = AppText.eyebrow,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xs))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (loaded) grouped(abs(left)) else "\u2014",
                style = AppText.numberXXL,
                // The accent is reserved for data that is on track. A number
                // that is merely large is not news, so it reads in the normal
                // foreground; only going over earns a colour.
                color = if (over) scheme.error else scheme.onSurface,
            )
            Spacer(Modifier.width(Spacing.s))
            Text(
                "kcal",
                style = AppText.bodyS,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.s),
            )
        }

        Spacer(Modifier.height(Spacing.l))
        Meter(
            fraction = if (loaded) fraction else 0f,
            color = if (over) scheme.error else scheme.primary,
            height = 2.dp,
        )
        Spacer(Modifier.height(Spacing.s))
        Row(Modifier.fillMaxWidth()) {
            HeroColumn(
                "Eaten",
                if (loaded) grouped(consumed.calories) else "\u2014",
                if (loaded) "$pct%" else null,
                Modifier.weight(1f),
            )
            HeroColumn(
                "Target",
                if (loaded) grouped(goals.calories) else "\u2014",
                null,
                Modifier.weight(1f),
                alignEnd = true,
            )
        }

        Spacer(Modifier.height(Spacing.xxl))
        MacroLegendRow("Carbs", consumed.carbs, goals.carbs, MacroCarbs)
        Spacer(Modifier.height(Spacing.m))
        MacroLegendRow("Protein", consumed.protein, goals.protein, MacroProtein)
        Spacer(Modifier.height(Spacing.m))
        MacroLegendRow("Fat", consumed.fat, goals.fat, MacroFat)
        Spacer(Modifier.height(Spacing.m))
        MacroLegendRow("Fiber", consumed.fiber, goals.fiber, MacroFiber)
    }
}

/** `EATEN` / `2 981` — shot 4's two labelled columns under the bar. */
@Composable
private fun HeroColumn(
    label: String,
    value: String,
    caption: String?,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
) {
    Column(
        modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        Text(
            label.uppercase(),
            style = AppText.eyebrow,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = AppText.numberM, color = MaterialTheme.colorScheme.onSurface)
            if (caption != null) {
                Spacer(Modifier.width(Spacing.s))
                Text(
                    caption,
                    style = AppText.bodyS,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * `CARBS · 468 g` over its own hairline.
 *
 * The label is an eyebrow rather than body text, so the row reads label-then-
 * number instead of two competing words, and the number gets the weight.
 */
@Composable
private fun MacroLegendRow(label: String, value: Double, goal: Double, color: Color) {
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Box(Modifier.size(6.dp).clip(RoundedCornerShape(1.dp)).background(color))
            Spacer(Modifier.width(Spacing.s))
            Text(
                label.uppercase(),
                style = AppText.eyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${value.roundToInt()}",
                style = AppText.numberM,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                " / ${goal.roundToInt()} g",
                style = AppText.bodyS,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Spacing.s))
        Meter(
            fraction = if (goal > 0) (value / goal).toFloat() else 0f,
            color = color,
            height = 2.dp,
        )
    }
}
