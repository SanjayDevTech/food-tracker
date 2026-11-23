package com.example.foodtracker.ui.components.serving

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.example.foodtracker.data.Nutrients
import com.example.foodtracker.ui.components.atoms.ValueText
import com.example.foodtracker.ui.components.atoms.formatValue
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.MacroCarbs
import com.example.foodtracker.ui.theme.MacroFat
import com.example.foodtracker.ui.theme.MacroProtein
import com.example.foodtracker.ui.theme.Spacing

/** Motion token — the same 450ms every meter and ring in the app uses. */
private const val ANIM_MS = 450

/**
 * The pinned top of the serving sheet.
 *
 * Name, optional brand, optional provenance line, and the live readout. The
 * readout is the reason this is pinned rather than scrolled: it is the answer
 * to the question the whole sheet exists to ask, and while you drag the amount
 * slider it is the only thing on screen that is changing.
 *
 * The calorie figure interpolates over [ANIM_MS] rather than snapping, which is
 * what makes a variant tap read as *this is now a different amount* instead of
 * a flicker. Every numeric style in the app carries `tnum`, so the digits hold
 * their columns for the whole animation instead of jittering.
 *
 * The three macros are tagged in their own hues, the same ones the ring legend
 * and every chart use — a reader who has learned that coral is protein on Today
 * has learned it here, and the single letter then costs no legend. They stay
 * `bodyS` against the headline calorie figure on purpose: three macros at the
 * same weight as the number they add up to is three competing rhythms, and the
 * one thing you are adjusting has to be the one thing you can read at a glance.
 */
@Composable
internal fun SheetHeader(
    name: String,
    brand: String?,
    subtitle: String?,
    nutrients: Nutrients,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val kcal by animateFloatAsState(
        targetValue = nutrients.calories.toFloat(),
        animationSpec = tween(ANIM_MS),
        label = "sheetKcal",
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.gutter)
            .padding(bottom = Spacing.l),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    style = AppText.titleL,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!brand.isNullOrBlank()) {
                    Text(
                        brand,
                        style = AppText.bodyS,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (subtitle != null) {
                Spacer(Modifier.width(Spacing.s))
                Text(
                    subtitle,
                    style = AppText.bodyS,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(Spacing.s))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            // House rule 2: this is the sheet's one figure, so it is a headline
            // number, not a grid cell. At `numberM` it was the same 18sp as the
            // amount readout further down and smaller than the food's own name
            // — the answer printed quieter than the question.
            ValueText(value = kcal.toDouble(), unit = "kcal", style = AppText.numberL)
            MacroTag(nutrients.carbs, "C", MacroCarbs)
            MacroTag(nutrients.protein, "P", MacroProtein)
            MacroTag(nutrients.fat, "F", MacroFat)
        }
    }
}

/** `28 C` with the C in its macro hue. Grams are implied — every macro is in g. */
@Composable
private fun MacroTag(value: Double, letter: String, color: Color) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            formatValue(value),
            style = AppText.bodyS,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(" $letter", style = AppText.bodyS, color = color)
    }
}
