package com.example.foodtracker.ui.screens.trends.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.foodtracker.ui.components.atoms.AppDivider
import com.example.foodtracker.ui.components.atoms.Section
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Spacing

/** How long a chart takes to swap when the range changes — §1's generic 200ms. */
private const val RANGE_FADE_MS = 200

/**
 * What a chart's headline number means for the goal behind it.
 *
 * Three values, not a `Color`, because house rule 3 is a rule about *meaning*:
 * `primary` is "on track" and `error` is "past a limit", and nothing else on
 * the page is allowed to be coloured. Handing [ChartCard] a colour would let a
 * call site tint a number because it looked nice; handing it a verdict means
 * the colour is decided in exactly one place, below.
 */
enum class InsightTone {
    /** No verdict — a number that is merely a number. */
    Neutral,

    /** At or inside the goal. */
    OnTrack,

    /** Past the goal, in the direction that matters. */
    Over,
}

/**
 * The reading a chart section leads with — a number, not a sentence.
 *
 * This replaces the `String` the insight line used to be ("2 981 kcal avg ·
 * 513 over goal"). That sentence was correct and completely inaudible: `body`
 * is the quiet register, so the one fact each section exists to deliver was set
 * in the same type as the empty-state apology underneath it, and the page read
 * as five paragraphs with pictures. House rule 2 puts the number at `numberL`
 * under an `eyebrow` and trails the rest as a caption, which is the same
 * anatomy as Today's hero and stat rows — one rhythm down the whole app.
 *
 * Splitting the sentence into fields is what makes that possible: the screen's
 * insight builders can no longer hand over prose, because there is nowhere to
 * put it.
 *
 * @param label the eyebrow over the number — what the number *is* ("VS GOAL").
 *        Uppercased here, like `Section` does with its title.
 * @param value the number itself, already formatted by `grouped`/`kgText` so
 *        this type stays presentation-only.
 * @param unit trailing unit in the quiet size ("kcal", "ml").
 * @param caption what the number is measured against ("goal 2 400 kcal").
 *        Optional: a number with no goal behind it has nothing to say.
 * @param tone colours the *number*, never the caption — a verdict belongs to
 *        the figure that earned it, which is what `CalorieHero` does on Today
 *        when the day goes over.
 */
data class ChartInsight(
    val label: String,
    val value: String,
    val unit: String? = null,
    val caption: String? = null,
    val tone: InsightTone = InsightTone.Neutral,
)

/**
 * One section of Trends: a standing head, its reading, its chart.
 *
 * **The card is gone.** This used to wrap everything in an `AppCard` — a 20dp
 * rounded hairline box — and five of them stacked down the page was the exact
 * Direction A anatomy the editorial pass exists to remove (house rules §1). The
 * section is a `Section` now: an uppercase head with the summary figure on its
 * baseline, a hairline **under the head**, then the content, open to the page.
 * The border is not replaced by a gap — house rule 1 is explicit that 32dp of
 * whitespace was the weakest and most expensive separator available — it is
 * replaced by that 1dp rule, and the gap between sections comes *down* to 24dp.
 *
 * Losing the box also gave the charts back the 16dp of inset the card took on
 * each side, which a 30-column heatmap and a 30-bar macro stack both spend on
 * data: both size themselves to whatever width they are handed.
 *
 * The name survives the card because the call sites, the ViewModel's docstrings
 * and design-plan §4.2 all talk about Trends in cards; what changed is the
 * anatomy, not what a reader of this screen calls each block.
 *
 * Five things it still standardises, each of which was being decided per screen
 * before `ProgressSection`/`WaterSection`/`WeightSection` were unified:
 *
 *  - **The summary figure.** [summary] is the one line in the head's trailing
 *    slot — "2 981 kcal avg", "72.4 kg now" — so every section on Trends states
 *    what its window amounts to in the same place, at the same size, as Today's
 *    "2 981 kcal" and "18 of 29 on track".
 *  - **The reading.** [insight] is the loud line under the rule: the verdict
 *    the summary figure earns, set as a number rather than as prose.
 *  - **Empty.** [empty] is the §4.2 rule in one parameter: under two points, a
 *    section keeps its head and shows a single muted line instead of its chart.
 *    Sections degrade one by one — a fortnight of weigh-ins with no water
 *    logged should cost you the water chart and nothing else — and that only
 *    stays true if no caller is tempted to hide the whole block.
 *  - **Loading.** There isn't one. A range change recomputes off a Room flow
 *    that is already warm; a spinner would flash for two frames and leave the
 *    page jumping. [crossfadeKey] dissolves the chart through the change
 *    instead (§4.2: "keep the previous chart on screen and crossfade. No
 *    spinner"). Both layers of the fade draw the *new* series rather than one
 *    drawing the old one — holding a second copy of a 365-point series alive
 *    purely to fade it out is not worth the frame it would buy, and what the
 *    eye reads either way is a soft swap instead of a jump.
 *  - **The foot.** A hairline, then [actionLabel] and whatever [footer]
 *    expands — the weight section's history, the heatmap's "show all".
 *
 * **Why the action moved out of the head.** [actionLabel] used to be an
 * `AppTextButton` in the header's trailing slot. That slot now belongs to the
 * summary figure, and house rule 1 wants every head to be the same object: a
 * 48dp pill parked in two of the five heads made those two heads three times
 * the height of the other three, and the rule under them stopped scanning as
 * one repeated element. At the foot it is a text link beside the section's
 * other link, which is where "what else can I do with this" already lives on
 * Today.
 *
 * @param crossfadeKey when non-null, [content] crossfades whenever this value
 *        changes. Pass the selected range; pass null for content that must not
 *        animate (anything that owns scroll or drag state of its own).
 */
@Composable
fun ChartCard(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    insight: ChartInsight? = null,
    empty: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    crossfadeKey: Any? = null,
    footer: @Composable (ColumnScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Section(
        title = title,
        modifier = modifier,
        trailing = if (summary != null) {
            {
                Text(
                    summary,
                    style = AppText.bodyS,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else null,
    ) {
        if (insight != null) {
            InsightLine(insight)
            Spacer(Modifier.height(Spacing.l))
        }

        if (empty != null) {
            EmptyChartLine(empty)
        } else if (crossfadeKey != null) {
            Crossfade(
                targetState = crossfadeKey,
                animationSpec = tween(RANGE_FADE_MS, easing = FastOutSlowInEasing),
                label = "chart",
            ) { _ ->
                // The key is read for its identity only; `content` closes over
                // the state it draws, which has already changed with the key.
                content()
            }
        } else {
            content()
        }

        val hasAction = actionLabel != null && onAction != null
        if (hasAction || footer != null) {
            Spacer(Modifier.height(Spacing.l))
            AppDivider()
            if (actionLabel != null && onAction != null) {
                SectionLink(text = actionLabel, onClick = onAction)
            }
            footer?.invoke(this)
        }
    }
}

/**
 * `VS GOAL` / `+513 kcal · goal 2 400 kcal`.
 *
 * Eyebrow above, number on the baseline, unit and caption trailing it — the
 * same three parts in the same order as `CalorieHero` and `StatRow` on Today.
 * The number is the only thing here allowed a colour, and only for a verdict:
 * see [InsightTone].
 */
@Composable
private fun InsightLine(insight: ChartInsight) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Text(
            insight.label.uppercase(),
            style = AppText.eyebrow,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xs))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                insight.value,
                style = AppText.numberL,
                color = when (insight.tone) {
                    InsightTone.Neutral -> scheme.onSurface
                    InsightTone.OnTrack -> scheme.primary
                    InsightTone.Over -> scheme.error
                },
            )
            if (insight.unit != null) {
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    insight.unit,
                    style = AppText.bodyS,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.xs),
                )
            }
            if (insight.caption != null) {
                Spacer(Modifier.width(Spacing.s))
                Text(
                    insight.caption,
                    style = AppText.bodyS,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.xs),
                )
            }
        }
    }
}

/**
 * The one shape a section's links take — an action, a disclosure, either way a
 * line of accent text with a real tap target.
 *
 * It was a centred `AppTextButton` inside the card: a pill-shaped control
 * floating in the middle of a hairline strip. Left-aligned accent text in the
 * section's own gutter is what Today's "See all nutrients" does, and the two
 * are the same gesture. The vertical padding is what keeps it a 48dp target
 * without a shape — house rule 3 allows the accent for a text link and nothing
 * pill-shaped.
 */
@Composable
fun SectionLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text,
        style = AppText.label,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.l),
    )
}

/**
 * The one muted line a section shows instead of a chart.
 *
 * Deliberately not an `EmptyState` with its 72dp disc: that component is for a
 * whole screen with nothing on it, and five of them stacked down Trends would
 * be a page of illustrations saying nothing. One sentence per section is the
 * honest size of the problem — the chart will fill in on its own as days
 * accumulate, and this says which days are missing.
 */
@Composable
fun EmptyChartLine(message: String, modifier: Modifier = Modifier) {
    Text(
        message,
        style = AppText.bodyS,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth(),
    )
}
