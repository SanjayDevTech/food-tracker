package com.example.foodtracker.ui.components.serving

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.foodtracker.data.MealType
import com.example.foodtracker.ui.components.HapticFilterChip
import com.example.foodtracker.ui.components.atoms.AppButton
import com.example.foodtracker.ui.rememberAppHaptics
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Spacing

/**
 * The pinned bottom of the serving sheet.
 *
 * One line: where it goes, then the thing you came to do. The meal picker is a
 * dropdown chip here rather than the full-width 4-way segmented control it used
 * to be (decision 8) — it arrives correct from the route that opened the picker
 * or from the wall clock on nearly every log, and ~80dp of permanent chrome for
 * a control that is almost never touched was paid for by the amount and
 * preparation controls that always are.
 *
 * The chip still *shows* the meal at all times, which is the part that mattered:
 * demoting a control is only safe while it keeps answering the question without
 * being opened.
 *
 * [onDelete] renders the destructive row underneath. It is a text row, not a
 * second button: editing puts remove and save in the same footer, and two
 * equally-weighted buttons there is how a user deletes an entry they meant to
 * correct. The button you came for keeps the filled slot; removal is
 * deliberately quieter, and it is undoable from the snackbar either way.
 */
@Composable
internal fun SheetFooter(
    meal: MealType,
    onMealChange: (MealType) -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    onDelete: (() -> Unit)?,
    deleteText: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.gutter)
            .padding(top = Spacing.m, bottom = Spacing.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MealChip(meal = meal, onMealChange = onMealChange)
            Spacer(Modifier.width(Spacing.m))
            AppButton(
                text = confirmText,
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
            )
        }
        if (onDelete != null) {
            Spacer(Modifier.height(Spacing.xs))
            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(Spacing.s))
                Text(deleteText, style = AppText.label)
            }
        }
    }
}

/**
 * `( Lunch ▾ )` with a four-item menu.
 *
 * The caret is part of the label rather than a trailing icon slot because the
 * chip is already as narrow as it can be next to a full-width primary action,
 * and a 18dp icon plus its gap is another 26dp off that button.
 *
 * The chip is drawn `selected` so the meal it is showing reads as a *current
 * value* rather than as an empty slot — which is the whole basis on which
 * decision 8 demoted this control. That is affordable now only because
 * selection stopped being a filled accent block: a brighter hairline and a
 * full-strength label say "this is set" without putting a mint capsule next to
 * the sheet's one primary button.
 *
 * The menu's emoji are gone — house rule 4. They were four glyphs decorating
 * four words that already say which meal they are, in the friendly register
 * the plan refused; the current meal is marked by weight instead, which is
 * also the thing a reader actually wants to find in an open menu.
 */
@Composable
private fun MealChip(meal: MealType, onMealChange: (MealType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val haptics = rememberAppHaptics()
    val scheme = MaterialTheme.colorScheme
    Box {
        HapticFilterChip(
            selected = true,
            onClick = { expanded = true },
            label = { Text("${meal.displayName} ▾", maxLines = 1) },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = scheme.surfaceContainerHigh,
        ) {
            MealType.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option.displayName,
                            style = AppText.body,
                            color = if (option == meal) scheme.onSurface else scheme.onSurfaceVariant,
                        )
                    },
                    onClick = {
                        haptics.light()
                        onMealChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
