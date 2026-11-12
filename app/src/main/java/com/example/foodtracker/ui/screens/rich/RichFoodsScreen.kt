package com.example.foodtracker.ui.screens.rich

import androidx.compose.foundation.layout.Arrangement
import com.example.foodtracker.ui.components.color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import com.example.foodtracker.ui.components.EmptyState
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.data.NutrientCategory
import com.example.foodtracker.data.NutrientField
import com.example.foodtracker.domain.RichFood
import com.example.foodtracker.ui.appViewModel
import com.example.foodtracker.ui.components.ExclusiveButtonGroup
import com.example.foodtracker.ui.components.FoodRow
import com.example.foodtracker.ui.components.HapticFilterChip
import com.example.foodtracker.ui.components.SectionCard
import com.example.foodtracker.ui.theme.Dimens
import kotlin.math.roundToInt

/**
 * Find foods rich in one or more nutrients, ranked, with no network call and
 * no AI — it reads the food table that is already on the device.
 *
 * The ranking rationale lives on [com.example.foodtracker.domain.NutrientRanking];
 * the short version is that results are ordered by how much of the user's own
 * daily goal one serving covers, never filtered by a threshold.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RichFoodsScreen(onBack: () -> Unit) {
    val vm: RichFoodsViewModel = appViewModel {
        RichFoodsViewModel(it.repository, it.profileStore)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    var category by rememberSaveable { mutableStateOf(NutrientCategory.Macros) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rich in…") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.selectedKeys.isNotEmpty()) {
                        TextButton(onClick = vm::clear) { Text("Clear") }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = Dimens.ScreenContentPadding,
            verticalArrangement = Arrangement.spacedBy(Dimens.ScreenItemSpacing),
        ) {
            item("picker") {
                SectionCard(title = "Pick nutrients") {
                    ExclusiveButtonGroup(
                        options = NutrientCategory.entries,
                        selected = category,
                        onSelect = { category = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { it.name },
                    )
                    Spacer(Modifier.height(Dimens.CardItemSpacing))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RichSearchableFields
                            .filter { it.category == category }
                            .forEach { field ->
                                HapticFilterChip(
                                    selected = field.key in state.selectedKeys,
                                    onClick = { vm.toggle(field.key) },
                                    label = { Text(field.label) },
                                )
                            }
                    }
                }
            }

            if (state.selectedKeys.isEmpty()) {
                item("empty") {
                    EmptyState(
                        icon = Icons.Default.Tune,
                        title = "Nothing picked yet",
                        message = "Choose a nutrient above and every food on this " +
                            "phone gets ranked by how much of your daily goal one " +
                            "serving of it covers.",
                    )
                }
            } else {
                if (state.results.isEmpty()) {
                    item("noResults") {
                        NoResults(state, onDropBlocker = { vm.toggle(it.key) })
                    }
                } else {
                    item("heading") {
                        Column {
                            Text(
                                "${state.results.size} foods · per serving, as a share of your daily goal",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            // Only with more than one pick: with a single
                            // nutrient "the one it does worst on" is just that
                            // nutrient, and saying so would puzzle rather than
                            // explain.
                            if (state.selectedKeys.size > 1) {
                                Text(
                                    "Ranked by the nutrient each food does worst on, so " +
                                        "nothing gets in on one strong number.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }
                    items(state.results, key = { it.food.id }) { result ->
                        RichFoodRow(result, state.selectedFields)
                    }
                }
            }
        }
    }
}

/**
 * What to say when nothing ranks, which is a dead end unless it explains
 * itself. A food scores by its weakest pick, so a single nutrient that no food
 * on file carries — omega-3, across most of this food table — drives every
 * score to zero and empties the list on its own.
 *
 * The offer is to drop that nutrient, not to loosen what the screen means. The
 * query is what is wrong here, and fixing the query leaves every remaining
 * result as trustworthy as before; quietly switching to a weaker rule would
 * fill the screen with foods that do not answer what was asked.
 */
@Composable
private fun NoResults(state: RichFoodsState, onDropBlocker: (NutrientField) -> Unit) {
    val blocker = state.blockingField
    EmptyState(
        icon = Icons.Default.SearchOff,
        title = if (blocker != null) {
            "No food is rich in all ${state.selectedKeys.size}"
        } else {
            "Nothing ranks for that"
        },
        message = if (blocker != null) {
            "${blocker.label} is ruling everything out — few foods here carry a " +
                "figure for it, and a food is only as good as its weakest pick."
        } else {
            "No food on file carries enough of it to rank. Custom foods you add " +
                "with real figures will show up here."
        },
        action = blocker?.let {
            {
                FilledTonalButton(onClick = { onDropBlocker(it) }) {
                    Text("Remove ${it.label.lowercase()}")
                }
            }
        },
    )
}

/**
 * One result: the food, then one [NutrientReadout] per nutrient picked.
 *
 * There is deliberately no separate "compact" and "expanded" row. A single
 * pick renders exactly the same readout as each of five picks does — one line
 * instead of five — so the two cases are the same component at different
 * lengths rather than two designs a reader has to learn separately. It also
 * means the one-nutrient case cannot drift out of sync with the many-nutrient
 * case when either is changed.
 *
 * The readouts sit under the food rather than to its right because the right
 * edge cannot hold a label. Unlabelled numbers stacked in a corner require the
 * reader to remember which nutrient they picked first, and silently become
 * unreadable at three picks; they also squeeze the name column until long food
 * names truncate.
 */
@Composable
private fun RichFoodRow(result: RichFood, fields: List<NutrientField>) {
    // A hairline rather than a card: the border's job is to group a food with
    // its own readouts so two results never bleed into one another, not to
    // draw attention. Half-alpha outlineVariant is about as faint as a 1dp
    // line can be and still read as a boundary in both themes — the same
    // outline SectionCard uses, stepped back so a list of these does not look
    // busier than the sections above it.
    Box(
        Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(Dimens.CardCornerRadius),
            ),
    ) {
      Column(Modifier.padding(bottom = 12.dp)) {
        FoodRow(food = result.food, onClick = {})
        Column(
            // Aligned with the food name above: 16dp row padding + 40dp
            // category bubble + 12dp gap.
            modifier = Modifier.padding(start = 68.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            fields.forEachIndexed { i, field ->
                NutrientReadout(
                    field = field,
                    amount = result.amounts[i],
                    goalFraction = result.goalFractions[i],
                )
            }
        }
      }
    }
}

/**
 * One nutrient's contribution from one serving: what it is, how much, and how
 * far that goes towards the day's goal.
 *
 * The bar is the point. The ranking is on the goal fraction, so without a
 * visual for it the order looks arbitrary next to the gram figures — 16 g of
 * fiber outranking 18 g of protein only makes sense once you can see that the
 * first fills 46% of its goal and the second 20% of its own.
 */
@Composable
private fun NutrientReadout(field: NutrientField, amount: Double, goalFraction: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            field.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(96.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(goalFraction.toFloat().coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(field.color()),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "${format(amount)} ${field.unit.symbol}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(62.dp),
        )
        Text(
            "${(goalFraction * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp),
        )
    }
}

private fun format(v: Double): String = when {
    v >= 100 -> v.roundToInt().toString()
    v >= 10 -> "%.0f".format(v)
    v >= 1 -> "%.1f".format(v)
    else -> "%.2f".format(v)
}
