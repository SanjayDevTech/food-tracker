package com.example.foodtracker.ui.screens.plan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.foodtracker.data.CookingOil
import com.example.foodtracker.data.MealType
import com.example.foodtracker.data.OilAmount
import com.example.foodtracker.data.ServingVariant
import com.example.foodtracker.data.StyleOption
import com.example.foodtracker.data.db.FoodEntity
import com.example.foodtracker.data.db.PlanItemEntity
import com.example.foodtracker.ui.components.serving.ServingSheet
import com.example.foodtracker.ui.components.serving.isCustomGrams

/**
 * Decide how much of a food goes on the plan.
 *
 * The same [ServingSheet] as logging and editing, with the verb changed. That
 * is the whole point: *how much* is one question, and a plan that asked it in
 * its own dialect would be a second serving model to keep in step with the
 * first — which is exactly how the barcode screen ended up with ~200 lines
 * re-asking it before the rewrite.
 *
 * **It captures the preparation axes too**, not just the amount. A plan line
 * carries the oil and the style because checking it off should not re-open the
 * same question every single morning: the user decided on ghee-roast dosa when
 * they built the plan, and the check sheet opens on that answer. Foods with no
 * cooking fat and no style axis — which is most of them — never see the
 * controls at all, so this costs nothing where it buys nothing.
 *
 * The meal arrives answered — you got here from "add to Breakfast" — but the
 * footer chip stays live, because a control that opens a menu and then ignores
 * the choice is worse than one that is not there. Picking a different meal here
 * is a correction, and the repository's upsert handles the awkward case of
 * moving a line that already exists into a slot that already holds the same
 * food.
 */
@Composable
fun PlanAmountSheet(
    food: FoodEntity,
    meal: MealType,
    existing: PlanItemEntity? = null,
    onConfirm: (PlanItemEntity) -> Unit,
    onDismiss: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    // Pre-fill from the existing plan line when there is one, so "change the
    // amount" opens on the amount rather than on the food's default. Matched
    // back to a known variant the same way EntryEditSheet does, including the
    // custom-grams sentinel that tells the sheet which mode to open in.
    var variant by remember(food.id, existing?.id) {
        val stored = existing?.servingLabel
        val known = food.variantsOrDefault().firstOrNull { it.label == stored }
        mutableStateOf(
            when {
                known != null -> known
                existing != null -> ServingVariant(
                    label = existing.servingLabel.ifBlank { ServingVariant.Default.label },
                    grams = if (existing.servings > 0) existing.grams / existing.servings else existing.grams,
                )
                else -> food.variantsOrDefault().first()
            }
        )
    }
    var servings by remember(food.id, existing?.id) { mutableStateOf(existing?.servings ?: 1.0) }
    var mealChoice by remember(food.id, existing?.id) { mutableStateOf(meal) }
    var oil by remember(food.id, existing?.id) {
        mutableStateOf(existing?.oil?.let { CookingOil.fromName(it) })
    }
    var style by remember(food.id, existing?.id) {
        mutableStateOf(StyleOption.find(food.styleOptions, existing?.style))
    }
    var oilAmount by remember(food.id, existing?.id) {
        mutableStateOf(OilAmount.fromName(existing?.oilAmount))
    }

    ServingSheet(
        food = food,
        meal = mealChoice,
        variant = variant,
        servings = servings,
        onVariantChange = { variant = it },
        onServingsChange = { servings = it },
        onMealChange = { mealChoice = it },
        onConfirm = {
            val isCustom = variant.isCustomGrams
            onConfirm(
                PlanItemEntity(
                    id = existing?.id ?: 0,
                    foodId = food.id,
                    mealType = mealChoice.name,
                    servings = if (isCustom) 1.0 else servings,
                    grams = if (isCustom) variant.grams else variant.grams * servings,
                    servingLabel = variant.label,
                    // Recorded only where the axis exists, matching the log
                    // write: a plan line for a banana claiming "Palmolein"
                    // would be noise the check sheet then has to ignore.
                    oil = if (food.usesCookingOil()) (oil ?: food.preselectedOil()).name else null,
                    style = if (food.hasStyleAxis()) (style ?: food.preselectedStyle())?.label else null,
                    oilAmount = if (food.usesOilAmountAxis()) {
                        (oilAmount ?: food.preselectedOilAmount()).name
                    } else null,
                    sortIndex = existing?.sortIndex ?: 0,
                )
            )
            onDismiss()
        },
        onDismiss = onDismiss,
        confirmText = if (existing == null) "Add to plan" else "Save amount",
        subtitle = "Planned for ${mealChoice.displayName}",
        oil = oil,
        onOilChange = { oil = it },
        style = style,
        onStyleChange = { style = it },
        oilAmount = oilAmount,
        onOilAmountChange = { oilAmount = it },
        onDelete = onRemove?.let { remove -> { remove(); onDismiss() } },
        deleteText = "Remove from plan",
    )
}
