package com.example.foodtracker.ui.components.serving

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.foodtracker.data.CookingOil
import com.example.foodtracker.data.MealType
import com.example.foodtracker.data.OilAmount
import com.example.foodtracker.data.ServingVariant
import com.example.foodtracker.data.StyleOption
import com.example.foodtracker.data.db.FoodEntity
import com.example.foodtracker.ui.components.atoms.AppDivider
import com.example.foodtracker.ui.components.atoms.Section
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Radius
import com.example.foodtracker.ui.theme.Spacing
import kotlinx.coroutines.delay

/**
 * Sentinel prefix on a [ServingVariant.label] marking the free-form
 * custom-grams mode. The remainder of the label is the grams value, kept
 * human-readable so the diary still renders sensibly.
 *
 * **This is load-bearing well beyond this package.** It is the one bit of state
 * that tells the log-write maths which multiplication to do: in custom mode
 * `variant.grams` *is* the gram total and `servings` is pinned to 1.0, so
 * `servings × variant.grams` stays correct in both modes and no caller needs a
 * branch. It is also persisted — `food_log.serving_label` on every entry ever
 * logged this way starts with it — so the prefix cannot change without a
 * migration, and the sheet must keep opening such an entry back in custom mode.
 */
const val CUSTOM_GRAMS_PREFIX = "Custom · "

/** True when this variant is the free-form gram amount rather than a real serving. */
val ServingVariant.isCustomGrams: Boolean get() = label.startsWith(CUSTOM_GRAMS_PREFIX)

/**
 * A custom-grams variant for [grams].
 *
 * `toInt()` rather than rounding, deliberately: this exact formatting is what
 * sits in `food_log.serving_label` for entries already in the database, and
 * re-opening one of those has to produce the same label it was written with.
 */
fun customGramsVariant(grams: Double): ServingVariant =
    ServingVariant("$CUSTOM_GRAMS_PREFIX${grams.toInt()} g", grams)

/** How long a preparation change stays flagged in the preview. */
private const val FLASH_MS = 2_000L

/**
 * The serving sheet, the densest surface in the app.
 *
 * It answers three questions about one food — *how much*, *how it's made*, and
 * *where does it go* — and it is the only place those are asked. Logging a
 * dosa, fixing yesterday's dosa and logging a scanned packet are
 * the same decision with different starting values, so they are the same sheet;
 * the barcode screen's bespoke serving editor and meal chips were ~200 lines
 * re-asking "how much" in a second dialect.
 *
 * **Why it is pinned at both ends.** It used to be one ~600dp scroll column
 * with the primary action at the very bottom, so on a short phone you scrolled
 * *past* the thing you came to tap, and the live calorie readout — the number
 * you are adjusting — could be off-screen while you dragged the slider. Header
 * and footer now stay; only the middle scrolls. The middle is given
 * `weight(1f, fill = false)` so a food with no preparation axis still produces
 * a short sheet instead of a tall one with a hole in it.
 *
 * **Why the meal picker is a footer chip** (decision 8): it arrives correct —
 * from the route that opened the picker, or the wall clock — on nearly every
 * log, and a full-width 4-way segmented control was spending ~80dp on a control
 * that is almost never touched, at the expense of the two that always are.
 *
 * **Why the preview flashes.** Tapping "Coconut" moves nothing a user can see:
 * calories and total fat are identical across oils *by design* (the oil-*amount*
 * row is the one that moves them, and it needs no flash because the header
 * figure changes under your thumb), which made the most domain-specific control
 * in the app look broken. The old answer was two
 * extra always-on rows (`showFatDetail`), which was the right instinct but
 * passive — you had to already know which rows to watch. Now the fields that
 * actually moved announce themselves for two seconds. See [LivePreview].
 *
 * **What the editorial pass changed** (docs/ui-house-rules.md): the three
 * headings became [Section]s — a standing head with a rule under it — the
 * variant and preparation chips lost their filled-accent selected state
 * (rule 3 — see `HapticFilterChip`), and the live preview's labels became
 * eyebrows over its numbers (rule 2). Nothing about the flow, the state or the
 * arithmetic moved; this sheet was already the right three questions in the
 * right order, it just asked them in the old app's voice.
 *
 * The three heads matter more here than anywhere else in the app. This is the
 * densest surface the app has — chips over a stepper over a slider over a
 * second chip row over an eight-cell grid — and before the rules there was
 * nothing but a 20dp gap telling the eye where one question ended and the next
 * began, so the whole middle read as one undifferentiated field of controls.
 *
 * [Section] is imported from `screens/today/components/` rather than copied:
 * rule 1 names that file as the one implementation, and a sheet that invents
 * its own eight-and-sixteen is how the app ended up with six cards last time.
 * The dependency points at a screen package, which is backwards, but a second
 * copy of the rule's geometry would be worse than an awkward import.
 *
 * State is hoisted in full: every caller already owns a view-model or a
 * `remember` holding these five axes, and the sheet writing to its own copy is
 * how a picker and the row it is editing drift apart.
 *
 * @param subtitle a trailing header line for callers with provenance to show —
 *        `"Lunch · 1:05 PM"` when editing an entry. Null while logging, where
 *        there is nothing true to say yet.
 * @param onDelete renders the destructive row under the footer. Null (the
 *        default) when there is nothing to delete, which is the logging case.
 * @param extraBlock an axis only one caller knows about, rendered above *How
 *        much* because it reprices everything below it. The barcode screen puts
 *        its per-100 g / per-serving basis here rather than this file learning
 *        what a barcode is.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServingSheet(
    food: FoodEntity,
    meal: MealType,
    variant: ServingVariant,
    servings: Double,
    onVariantChange: (ServingVariant) -> Unit,
    onServingsChange: (Double) -> Unit,
    onMealChange: (MealType) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "Add to diary",
    subtitle: String? = null,
    /**
     * The cooking fat to price this entry in. Null means "whatever this food
     * remembers", which is what every caller with no opinion should pass.
     */
    oil: CookingOil? = null,
    onOilChange: (CookingOil) -> Unit = {},
    /** Preparation option, for the few seeded foods that offer a choice. */
    style: StyleOption? = null,
    onStyleChange: (StyleOption) -> Unit = {},
    /**
     * How much of the cooking fat to price in. Null means "whatever this food
     * remembers", same as [oil]. Only read for foods on the oil-amount axis.
     */
    oilAmount: OilAmount? = null,
    onOilAmountChange: (OilAmount) -> Unit = {},
    onDelete: (() -> Unit)? = null,
    /** Label for the destructive row. Only read when [onDelete] is non-null. */
    deleteText: String = "Remove from diary",
    extraBlock: (@Composable () -> Unit)? = null,
) {
    // Custom-grams mode is signalled by the sentinel label. In that mode
    // `variant.grams` IS the gram value the user dialled and servings is pinned
    // to 1.0, so the log-write maths (servings × variant.grams) is unchanged.
    val isCustom = variant.isCustomGrams
    val grams = if (isCustom) variant.grams else variant.grams * servings

    // Only foods with added cooking fat can be re-priced; for everything else
    // the picker is hidden and this resolves to the stored figures unchanged.
    val showsOil = food.usesCookingOil()
    val showsStyle = food.hasStyleAxis()
    val showsOilAmount = food.usesOilAmountAxis()
    val effectiveOil = oil ?: food.preselectedOil()
    val effectiveStyle = style ?: food.preselectedStyle()
    val effectiveOilAmount = oilAmount ?: food.preselectedOilAmount()
    val nutrients =
        food.nutrientsPer100g(effectiveOil, effectiveStyle, effectiveOilAmount) * (grams / 100.0)

    // The flash baseline is captured in the chip's own click handler, where
    // `nutrients` is still the pre-change value — reconstructing "what it was"
    // from a later recomposition means racing the state change that caused it.
    var flash by remember { mutableStateOf<PrepFlash?>(null) }
    LaunchedEffect(flash?.token) {
        if (flash != null) {
            delay(FLASH_MS)
            flash = null
        }
    }
    fun flagPrepChange() {
        flash = PrepFlash(before = nutrients, token = (flash?.token ?: 0) + 1)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet),
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            SheetHeader(
                name = food.name,
                brand = food.brand,
                subtitle = subtitle,
                nutrients = nutrients,
            )
            AppDivider()

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.gutter, vertical = Spacing.l),
            ) {
                if (extraBlock != null) {
                    extraBlock()
                    Spacer(Modifier.height(Spacing.xxl))
                }

                Section("How much") {
                    AmountRow(
                        food = food,
                        variant = variant,
                        servings = servings,
                        onVariantChange = onVariantChange,
                        onServingsChange = onServingsChange,
                    )
                }

                if (showsOil || showsStyle || showsOilAmount) {
                    Spacer(Modifier.height(Spacing.xxl))
                    // One heading, two axes. "Cooked in" and "Made with" were
                    // two headings answering the same question — how was this
                    // prepared — and the style axis read oddly as a titled
                    // section of its own when it is usually a single toggle.
                    Section("How it's made") {
                        PreparationBlock(
                            food = food,
                            oil = effectiveOil,
                            style = effectiveStyle,
                            oilAmount = effectiveOilAmount,
                            onOilChange = { picked -> flagPrepChange(); onOilChange(picked) },
                            onStyleChange = { picked -> flagPrepChange(); onStyleChange(picked) },
                            onOilAmountChange = { picked ->
                                flagPrepChange(); onOilAmountChange(picked)
                            },
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.xxl))
                // The head's trailing slot carries this section's summary
                // figure, as rule 1 asks — here it is what the eight numbers
                // underneath are *of*, which is the one thing a reader has to
                // hold in mind to use them and the one thing they cannot read
                // off the grid itself.
                Section(
                    "What's in it",
                    trailing = {
                        Text(
                            "per ${grams.toInt()} g",
                            style = AppText.bodyS,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                ) {
                    LivePreview(
                        nutrients = nutrients,
                        previous = flash?.before,
                        showFatDetail = showsOil || showsStyle,
                    )
                }
            }

            AppDivider()
            SheetFooter(
                meal = meal,
                onMealChange = onMealChange,
                confirmText = confirmText,
                onConfirm = onConfirm,
                onDelete = onDelete,
                deleteText = deleteText,
            )
        }
    }
}

