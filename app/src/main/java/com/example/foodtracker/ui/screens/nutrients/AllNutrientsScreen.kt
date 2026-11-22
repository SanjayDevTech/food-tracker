package com.example.foodtracker.ui.screens.nutrients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.data.NutrientCategory
import com.example.foodtracker.data.NutrientField
import com.example.foodtracker.data.onTrack
import com.example.foodtracker.ui.appViewModel
import com.example.foodtracker.ui.components.NutrientCell
import com.example.foodtracker.ui.components.NutrientContributionSheet
import com.example.foodtracker.ui.components.atoms.AppDivider
import com.example.foodtracker.ui.components.atoms.AppTopBar
import com.example.foodtracker.ui.components.readingsFor
import com.example.foodtracker.ui.screens.today.TodayViewModel
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Spacing
import com.example.foodtracker.util.DateUtil
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Every tracked nutrient for one day — design-plan §4.7, as a page.
 *
 * **Why this stopped being a bottom sheet.** It was one, and tapping a nutrient
 * raised a second sheet over it. Sheet-over-sheet has no convention: whichever
 * way it is animated, the first one either vanishes (losing a two-screen scroll
 * position) or stacks (two scrims, two drag handles, two dismiss gestures that
 * mean different things). Swapping the content inside a single sheet hid the
 * seam rather than removing it — the height still jumped and back still had to
 * mean two things depending on an invisible state.
 *
 * As a page the drill-down is ordinary: a sheet raised over a page, back
 * returns to the page, the scroll position is simply still there because the
 * page never went away.
 *
 * The two-column grid is what makes 29 readings usable at all — about two
 * screens, where 29 three-line bars was about six.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllNutrientsScreen(
    epochDay: Long,
    onBack: () -> Unit,
    onOpenRichFoods: (String?) -> Unit,
) {
    val vm: TodayViewModel = appViewModel { TodayViewModel(it.repository, it.profileStore) }
    // The view model defaults to today; this page may have been opened on a
    // back-filled day, and must keep showing that day after process death.
    LaunchedEffect(epochDay) { vm.setDay(epochDay) }

    val state by vm.state.collectAsStateWithLifecycle()
    var detail by remember { mutableStateOf<NutrientField?>(null) }

    val readings = remember(state.totalNutrients, state.goals) {
        readingsFor(state.totalNutrients, state.goals)
    }
    val onTrackCount = readings.count { it.status.onTrack }
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = scheme.background,
        topBar = {
            AppTopBar(
                title = "All nutrients",
                subtitle = DateUtil.epochDayToDate(state.epochDay)
                    .format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())),
                onBack = onBack,
            )
        },
    ) { inner ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = inner.calculateTopPadding(),
                bottom = inner.calculateBottomPadding() + Spacing.xxxl,
                start = Spacing.gutter,
                end = Spacing.gutter,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            item("tally") {
                Text(
                    "$onTrackCount of ${readings.size} on track",
                    style = AppText.bodyS,
                    color = scheme.onSurfaceVariant,
                )
            }
            NutrientCategory.entries.forEach { category ->
                val inCategory = readings.filter { it.field.category == category }
                if (inCategory.isEmpty()) return@forEach
                item(category.name) {
                    Row(Modifier.padding(top = Spacing.s)) {
                        Text(
                            category.name.uppercase(),
                            style = AppText.eyebrow,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
                items(inCategory.chunked(2)) { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.l)) {
                        pair.forEach { r ->
                            NutrientCell(
                                r = r,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { detail = r.field },
                            )
                        }
                        // Keeps a lone trailing cell at half width rather than
                        // letting it stretch across the row.
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                item(category.name + "-rule") { AppDivider() }
            }
        }
    }

    detail?.let { field ->
        NutrientContributionSheet(
            field = field,
            consumed = state.totalNutrients,
            goals = state.goals,
            logs = state.logs,
            onDismiss = { detail = null },
            onFindRichFoods = { key -> detail = null; onOpenRichFoods(key) },
        )
    }
}
