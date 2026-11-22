package com.example.foodtracker.ui.screens.you

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.BuildConfig
import com.example.foodtracker.ui.appViewModel
import com.example.foodtracker.ui.components.atoms.AppDivider
import com.example.foodtracker.ui.components.atoms.AppTopBar
import com.example.foodtracker.ui.components.atoms.Section
import com.example.foodtracker.ui.screens.you.sheets.AiAutofillSheet
import com.example.foodtracker.ui.screens.you.sheets.AllTargetsSheet
import com.example.foodtracker.ui.screens.you.sheets.CustomFoodsSheet
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Spacing

/**
 * You — design-plan §4.3, rebuilt against `docs/ui-house-rules.md`.
 * The third bottom-bar destination.
 *
 * Merges the old `ProfileScreen` and `SettingsScreen`. Settings rendered every
 * control inline — toggles, an API-key field, a backup list, export buttons, a
 * delete-all dialog — in one 662-line wall you had to scroll past in full to
 * reach anything. This is a list of rows that open a sheet (short) or a page
 * (long), with each row's current value on the right, so the state of all six
 * settings is readable without opening any of them.
 *
 * **The cards are gone.** This page used to be three stacked `AppCard`s and a
 * version line, which is precisely the anatomy the owner's verdict was aimed
 * at: new contents, old silhouette. It is now four [Section]s — PROFILE, GOAL,
 * DAILY TARGETS, APP — each a standing head with a hairline under it and its
 * content bleeding to the screen gutter below.
 *
 * The gap between them is 24dp, not the 32 the first attempt at this used. A
 * bigger gap was the wrong instrument: it cost a third of a screen on a page
 * that is already a scroll and still left one undifferentiated column, because
 * whitespace cannot say where a section *ended*. The rule says it in 1dp, so
 * the gap could come down rather than up.
 *
 * **The row anatomy changed with them.** §4.3 adopted reference shot 9's
 * icon-circle row; shot 9 is Direction A and §0 chose Direction B. See
 * [SettingRow] for the full argument — in short, once the enclosing card goes,
 * six tinted discs are the only filled shapes left in the column and they take
 * over as the page's structure.
 *
 * **No top-bar back arrow and no bottom bar here.** You is a bar destination,
 * not a page pushed onto one: the host `Scaffold` in the navigation graph owns
 * both, which is also why the four callbacks are the screen's entire contract
 * and nothing in this file knows a route.
 *
 * **There is no Appearance row.** With one theme there is nothing to choose,
 * and phase 1 deleted the palette and Light/Dark/System pickers outright.
 *
 * The goal meter is the old Today banner: it used to be a filled
 * `tertiaryContainer` card at the top of the diary, costing the one screen that
 * most needs vertical room. Goal context belongs beside the goal (§3), and as a
 * progress reading it is the one place on this page house rule 3 lets the
 * accent appear.
 */
@Composable
fun YouScreen(
    onOpenReminders: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenDataBackup: () -> Unit,
) {
    val vm: YouViewModel = appViewModel {
        YouViewModel(it.profileStore, it.reminderStore, it.backupManager, it.repository)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme

    var showAllTargets by remember { mutableStateOf(false) }
    var showAiAutofill by remember { mutableStateOf(false) }
    var showCustomFoods by remember { mutableStateOf(false) }

    // Counted here rather than in the view model's init so the filesystem is
    // only read once the page is on screen — and so the count is right again
    // when the user comes back from Data & backup having made or deleted one.
    LaunchedEffect(Unit) { vm.refreshBackupCount() }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("You")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.gutter,
                end = Spacing.gutter,
                top = Spacing.l,
                bottom = Spacing.xxxl,
            ),
            // 24dp between sections — house rule 1. The hairline under each
            // head is what separates them; the gap only has to keep them from
            // touching.
            verticalArrangement = Arrangement.spacedBy(Spacing.xxl),
        ) {
            item("profile") {
                ProfileHeader(profile = state.profile, onEdit = onOpenEditProfile)
            }

            // Omitted rather than rendered empty: an absent item costs no gap,
            // whereas an item that draws nothing would leave 24dp of hole.
            state.goalProgress?.let { progress ->
                item("goal") {
                    GoalSection(
                        profile = state.profile,
                        progress = progress,
                        fraction = state.goalFraction,
                    )
                }
            }

            item("targets") {
                Section(
                    title = "Daily targets",
                    // Every section states its total in the same place: four
                    // are shown, and this says how many there are in all.
                    trailing = {
                        Text(
                            "$TARGET_COUNT in all",
                            style = AppText.bodyS,
                            color = scheme.onSurfaceVariant,
                        )
                    },
                ) {
                    // Four, not thirty. These are the numbers a day is actually
                    // steered by; the other twenty-six are a tap away and were
                    // never read at a glance. Being only four is what lets them
                    // read at numberL instead of as a dense list.
                    HeadlineTarget("Calories", state.goals.calories, "kcal")
                    AppDivider()
                    HeadlineTarget("Protein", state.goals.protein, "g")
                    AppDivider()
                    HeadlineTarget("Carbs", state.goals.carbs, "g")
                    AppDivider()
                    HeadlineTarget("Water", state.goals.waterMl.toDouble(), "ml")
                    SeeAllTargetsRow(onClick = { showAllTargets = true })
                }
            }

            item("app") {
                Section("App") {
                    // A rule between every row, none around the set and none
                    // under the last: the head's own hairline opens the list
                    // and the next section's head closes it. A border drawn
                    // round the rows would be a second boundary doing the first
                    // one's job (house rule 1).
                    SettingRow(
                        label = "Reminders",
                        value = state.reminderSummary,
                        onClick = onOpenReminders,
                    )
                    AppDivider()
                    SettingRow(
                        label = "AI autofill",
                        value = state.aiSummary,
                        onClick = { showAiAutofill = true },
                    )
                    AppDivider()
                    SettingRow(
                        label = "Data & backup",
                        value = state.backupSummary,
                        onClick = onOpenDataBackup,
                    )
                    AppDivider()
                    SettingRow(
                        label = "Privacy",
                        onClick = onOpenPrivacy,
                    )
                    AppDivider()
                    SettingRow(
                        label = "Custom foods",
                        value = state.customFoodCount.toString(),
                        onClick = { showCustomFoods = true },
                    )
                    AppDivider()
                    SettingToggleRow(
                        label = "Haptic feedback",
                        checked = state.hapticsEnabled,
                        onChange = vm::setHapticsEnabled,
                    )
                }
            }

            item("version") {
                // Left-aligned, not centred: a centred line under a page of
                // flush-left rules is the only thing on the page that does not
                // share its left edge, and it reads as a stray.
                Text(
                    "Food Tracker ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · offline-first",
                    style = AppText.bodyS,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.s),
                )
            }
        }
    }

    if (showAllTargets) {
        AllTargetsSheet(goals = state.goals, onDismiss = { showAllTargets = false })
    }

    if (showAiAutofill) {
        AiAutofillSheet(
            enabled = state.aiEnabled,
            provider = state.aiProvider,
            model = state.aiModel,
            apiKey = state.aiApiKey,
            onEnabledChange = vm::setAiEnabled,
            onProviderChange = vm::setAiProvider,
            onModelChange = vm::setAiModel,
            onApiKeyChange = vm::setAiApiKey,
            onDismiss = { showAiAutofill = false },
        )
    }

    if (showCustomFoods) {
        CustomFoodsSheet(
            count = state.customFoodCount,
            message = state.message,
            onDeleteAll = vm::deleteAllCustomFoods,
            onDismiss = {
                showCustomFoods = false
                vm.consumeMessage()
            },
        )
    }
}

/** The four steering numbers, in the loud register — see [TargetEmphasis]. */
@Composable
private fun HeadlineTarget(label: String, value: Double, unit: String) {
    TargetRow(label = label, value = value, unit = unit, emphasis = TargetEmphasis.Headline)
}
