package com.example.foodtracker.ui.components.serving

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.foodtracker.data.CookingOil
import com.example.foodtracker.data.StyleOption
import com.example.foodtracker.data.db.FoodEntity
import com.example.foodtracker.ui.components.HapticFilterChip
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Spacing

/**
 * *How it's made* — design-plan §4.5 change 3 and §6.3.
 *
 * Two axes, one heading. They used to be two titled sections, "Cooked in" and
 * "Made with", which read as two separate decisions when they are one: *how was
 * this prepared*. Here they are labelled sub-rows, and a food that offers only
 * one of them simply shows one row.
 *
 * **The binary-style rule is kept exactly as it was**, because it is right:
 * "Plain" is the absence of an addition, not a peer of it. When the axis is
 * binary — one plain option, one addition — there is nothing to choose
 * *between*, only something to switch on, so it renders as a single checkable
 * toggle and off means plain. An axis with two or more additions is a genuine
 * either/or (you cannot have a ghee roast *and* a butter roast), so that falls
 * back to the full exclusive set with Plain shown, rather than silently
 * dropping options a toggle cannot express.
 *
 * Oil options stay restricted to [CookingOil.optionsFor] with the dish's own
 * default leading: the list is a claim about what this dish is plausibly cooked
 * in, not a catalogue of every fat in the model.
 *
 * Both handlers persist on tap rather than on confirm — see `rememberVariant`.
 * Someone who opens a dosa, switches to ghee roast and then backs out has still
 * told us something about their kitchen.
 */
@Composable
internal fun PreparationBlock(
    food: FoodEntity,
    oil: CookingOil,
    style: StyleOption?,
    onOilChange: (CookingOil) -> Unit,
    onStyleChange: (StyleOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        if (food.usesCookingOil()) {
            SubLabel("Cooked in")
            ChipRow {
                CookingOil.optionsFor(food.defaultOil).forEach { option ->
                    HapticFilterChip(
                        selected = option == oil,
                        onClick = { onOilChange(option) },
                        label = { Text(option.label, maxLines = 1) },
                    )
                }
            }
        }

        if (food.hasStyleAxis()) {
            if (food.usesCookingOil()) Spacer(Modifier.height(Spacing.m))
            SubLabel("Added")
            val plain = food.styleOptions.firstOrNull { it.ingredient == null }
            val additions = food.styleOptions.filter { it.ingredient != null }
            val isBinary = plain != null && additions.size == 1
            ChipRow {
                if (isBinary) {
                    val addition = additions.first()
                    val on = style?.ingredient != null
                    HapticFilterChip(
                        selected = on,
                        // `plain!!` is safe by construction: isBinary is only
                        // true when a plain option exists.
                        onClick = { onStyleChange(if (on) plain!! else addition) },
                        label = { Text(addition.label, maxLines = 1) },
                        leadingIcon = if (on) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else null,
                    )
                } else {
                    food.styleOptions.forEach { option ->
                        HapticFilterChip(
                            selected = option.label == style?.label,
                            onClick = { onStyleChange(option) },
                            label = { Text(option.label, maxLines = 1) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * A sub-row label inside the block.
 *
 * It was `bodyS`, on the argument that an eyebrow would make each axis look
 * like a section of its own again. That argument expired when the sections
 * grew rules: a section is now a header *plus a hairline*, so an eyebrow with
 * no rule under it is unambiguously a label within one. House rule 2 asks for
 * eyebrows on labels and it is right to here — "Cooked in" in body text next
 * to "HOW IT'S MADE" in an eyebrow was two registers inside one block.
 */
@Composable
private fun SubLabel(text: String) {
    Text(
        text.uppercase(),
        style = AppText.eyebrow,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.s))
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) { content() }
}
