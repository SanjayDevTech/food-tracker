package com.example.foodtracker.ui.screens.you.sheets

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.foodtracker.data.NutrientGoals
import com.example.foodtracker.ui.screens.you.TARGET_COUNT
import com.example.foodtracker.ui.screens.you.TargetList
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Spacing

/**
 * Every daily target the profile produces — design-plan §4.3 / §4.16.
 *
 * Read-only on purpose. The numbers are derived from the profile by
 * [com.example.foodtracker.domain.GoalCalculator], so the way to change one is
 * to change the body or the goal that produced it; an editable list here would
 * promise an override the app does not have. The one line of copy below says so
 * rather than leaving the user to discover it by tapping.
 *
 * The same sheet opens from the You page's DAILY TARGETS band and from the edit
 * page's live preview, which is why it takes goals rather than reading a view
 * model: on the edit page the goals it shows are the *draft's*, recomputed on
 * every drag.
 *
 * Thirty rows is the dense register — [TargetList] renders them at
 * `numberM` against `body` labels, not the `numberL`-over-eyebrow treatment the
 * four headline targets get. House rule 2 reserves `numberM` for exactly this.
 */
@Composable
fun AllTargetsSheet(goals: NutrientGoals, onDismiss: () -> Unit) {
    YouSheet(title = "All $TARGET_COUNT targets", onDismiss = onDismiss) {
        Text(
            "Worked out from your age, sex, height, weight, activity and goal. " +
                "Change those and every number here moves.",
            style = AppText.bodyS,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xxl))
        TargetList(goals)
    }
}
