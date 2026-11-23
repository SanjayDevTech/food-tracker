package com.example.foodtracker.ui.screens.today.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.foodtracker.data.MealType
import com.example.foodtracker.data.db.FoodLogWithFood
import com.example.foodtracker.data.db.nutrients
import com.example.foodtracker.ui.components.atoms.AppDivider
import com.example.foodtracker.ui.components.atoms.Section
import com.example.foodtracker.ui.components.atoms.countText
import com.example.foodtracker.ui.components.atoms.grouped
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Spacing

/**
 * The day's diary.
 *
 * One section with four rows rather than four cards. The reasoning in the original
 * `MealsSection` docstring was right and is kept: on a normal day two or three
 * meals are empty, and four mostly-empty cards is a screen of borders around
 * nothing.
 *
 * Two behavioural changes land here:
 *  - An entry row is **tappable → edit sheet**. Until this, a wrong serving
 *    meant deleting the row and logging it again; `FoodLogDao` had `insert`
 *    but no `@Update`, which made editing impossible rather than merely absent.
 *  - Delete moved from an always-visible trash icon to **swipe, with undo**.
 *    Four trash icons per meal is four chances to lose data, and they were
 *    live at all times on the screen you scroll past most.
 */

/** Entry rows sit indented under their meal so the hierarchy reads without a box. */
private val ENTRY_INDENT = 16.dp

@Composable
fun MealsSection(
    groups: Map<MealType, List<FoodLogWithFood>>,
    activeMeal: MealType,
    totalKcal: Double,
    onAdd: (MealType) -> Unit,
    onEditEntry: (FoodLogWithFood) -> Unit,
    onDeleteEntry: (FoodLogWithFood) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Which meal is open. Defaults to the one the wall clock says you are in,
    // which on a normal visit is the one you came to look at.
    var expanded by rememberSaveable { mutableStateOf(activeMeal.name) }
    // No enclosing card. The meals are the longest block on the screen, and a
    // border around a list that already has rules between its rows is a second
    // boundary doing the first one's job — structure is hairlines, not
    // boxes, and a box drawn around rules reads as Direction A's card stack.
    Section(
        title = "Meals",
        modifier = modifier,
        trailing = {
            Text(
                "${grouped(totalKcal)} kcal",
                style = AppText.bodyS,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    ) {
        MealType.entries.forEachIndexed { index, meal ->
            if (index > 0) AppDivider()
            val rows = groups[meal].orEmpty()
            MealGroup(
                meal = meal,
                rows = rows,
                isNow = meal == activeMeal,
                isExpanded = expanded == meal.name && rows.isNotEmpty(),
                onToggle = { expanded = if (expanded == meal.name) "" else meal.name },
                onAdd = { onAdd(meal) },
                onEditEntry = onEditEntry,
                onDeleteEntry = onDeleteEntry,
            )
        }
    }
}

@Composable
private fun MealGroup(
    meal: MealType,
    rows: List<FoodLogWithFood>,
    isNow: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAdd: () -> Unit,
    onEditEntry: (FoodLogWithFood) -> Unit,
    onDeleteEntry: (FoodLogWithFood) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val kcal = rows.sumOf { it.nutrients().calories }
    // The "now" row is one of the five fills the rules allow, at 35% so it reads as a
    // highlight rather than as a selected state. Composited against the canvas
    // rather than left translucent because the entry rows below need an opaque
    // colour to cover the swipe backdrop with — a 35% row would let the delete
    // red bleed through at rest.
    // The 35% primaryContainer wash is gone with the card: on an unenclosed
    // list a tinted band reads as a selected row, and "now" is a fact about the
    // clock, not a selection. The accent-coloured label carries it instead.
    val rowFill = scheme.background
    Column(Modifier.fillMaxWidth().background(rowFill)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = rows.isNotEmpty(), onClick = onToggle)
                .padding(top = Spacing.m, bottom = Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // No emoji. The app adopted Direction B, whose shots carry none; the
            // glyphs are the friendly register the plan explicitly refused, and
            // on a four-row list they are decoration rather than wayfinding.
            Text(
                meal.displayName.uppercase(),
                style = AppText.eyebrow,
                color = if (isNow) scheme.primary else scheme.onSurfaceVariant,
            )
            // No badge. The label above already goes accent-coloured when this
            // is the current meal, and a filled primaryContainer pill beside it
            // marked the same fact twice — in the one register house rule 3
            // reserves for data.
            Spacer(Modifier.weight(1f))
            Text(
                if (rows.isEmpty()) "Nothing yet" else "${grouped(kcal)} kcal · ${rows.size}",
                style = AppText.bodyS,
                color = scheme.onSurfaceVariant,
            )
            if (rows.isNotEmpty()) {
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Spacing.xs).size(18.dp),
                )
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, "Add to ${meal.displayName}", tint = scheme.onSurfaceVariant)
            }
        }
        if (isExpanded) {
            rows.forEach { row ->
                MealEntryRow(
                    row = row,
                    containerColor = rowFill,
                    onEdit = { onEditEntry(row) },
                    onDelete = { onDeleteEntry(row) },
                )
            }
        }
    }
}

/**
 * One logged food. Tap to edit, swipe either way to delete.
 *
 * The swipe is bidirectional on purpose: this row is reached one-handed with a
 * thumb, and forcing a direction means half the gestures do nothing. Undo lives
 * in the caller's snackbar, which is what makes a destructive swipe acceptable
 * at all.
 *
 * **The delete must not live in `confirmValueChange`.** That parameter is a
 * *predicate* — Compose calls it to ask whether a target state is allowed, and
 * it may call it speculatively, more than once, and while re-resolving anchors
 * on an ordinary layout change rather than only in response to a gesture. An
 * earlier version deleted from inside it and returned false; a layout change
 * was then enough to make every visible row answer the question by deleting
 * itself, which wiped the day. The predicate is now pure, and the delete runs
 * once from an effect keyed on the state actually reaching a dismissed value.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealEntryRow(
    row: FoodLogWithFood,
    containerColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    // Keyed on the entry: these rows are emitted by a forEach, so an unkeyed
    // remember is positional and would hand row N's half-swiped state to
    // whatever row lands at index N after a delete.
    val dismissState = key(row.log.id) { rememberSwipeToDismissBoxState() }

    LaunchedEffect(dismissState.currentValue, row.log.id) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            onDelete()
            // Put the box back: the row's removal arrives through the database
            // flow, and an undo has to be able to restore it. Left dismissed,
            // a restored row would re-render already swiped away.
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(scheme.errorContainer)
                    .padding(horizontal = Spacing.xl),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = scheme.onErrorContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(containerColor)
                .clickable(onClick = onEdit)
                .padding(
                    start = ENTRY_INDENT,
                    end = 0.dp,
                    top = Spacing.s,
                    bottom = Spacing.s,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                row.food.name,
                style = AppText.body,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Spacing.s))
            Text(
                "${countText(row.log.servings)} ×  ·  ${grouped(row.nutrients().calories)} kcal",
                style = AppText.bodyS,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}
