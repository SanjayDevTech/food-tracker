# Experience notes from building FoodTracker

A portable reference for starting a new Android tracker app (e.g. a gym tracker). These are the patterns, pitfalls, and decisions that worked through extensive iteration on FoodTracker. Skip what doesn't fit; the design-system section and the Compose-pitfalls section are the most reusable.

---

## Stack

- **Kotlin** + **Jetpack Compose** + **Material 3** (BOM `2026.05.02` alpha for Expressive APIs)
- **Room** for the relational store, **DataStore Preferences** for settings/profile
- **Flow + StateFlow + combine** for VM → UI; collect with `collectAsStateWithLifecycle`
- **Jetpack Glance** for home-screen widgets
- **Type-safe Navigation Compose** with `@Serializable object` routes
- **kotlinx.serialization** for backup/export payloads (plain JSON)
- **AlarmManager** (`setAndAllowWhileIdle`) for daily reminders — no WorkManager periodic
- `minSdk = 31` (Android 12+), `compileSdk = 37`

## Design system (the part most worth copying)

The whole app reads as one design language because every grouped block uses the same outlined-card primitive. AMOLED-friendly: most pixels stay `#000000`; structure comes from outlines, not fills.

```kotlin
Column(
    modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            shape = RoundedCornerShape(18.dp),
        )
        .padding(horizontal = 14.dp, vertical = 12.dp),
) {
    // titleMedium SemiBold heading with 8dp bottom gap
    // content...
}
```

**Variants** (same primitive, swap the border color):
- Default section → `outlineVariant`
- Blocking warning → `error` (notifications-blocked card)
- Soft warning → `tertiary` (battery-opt nudge)
- Selected state on `SelectableRow` → `primary` at 1.5dp

**Hero pieces** that need extra emphasis (goal banner, privacy tagline, daily-targets preview) use a *filled* `tertiaryContainer` / `primaryContainer` / `secondaryContainer` `Card` with **symmetric 20dp corners**. Avoid asymmetric corner shapes (28/8/28/8) — they read too chromey next to the outlined cards.

**Stat tiles** (Water / Burned / Weight on Home) use the same outlined idiom in a `Row` with `weight(1f)` per tile — the outline signals tappability without the heaviness of a filled card.

**Section heading** style inside cards: `titleMedium`, `SemiBold`, 8dp gap below.

## Lists — picking the right container

| Item count | Pattern |
|---|---|
| 2–6 known rows (Meals, Reminders) | Wrap inside **one outlined section** with `HorizontalDivider(color = outlineVariant.copy(alpha = 0.35f))` between rows |
| Unbounded (history, daily logs) | **Heading as a standalone `LazyColumn` item** + lazy plain rows with the same dividers. Don't wrap everything in one section item — that eagerly composes the whole list and defeats `LazyColumn` |
| Pickers, day's worth of meals | Standard `LazyColumn` with `key = { it.id }`; no per-row card border |

## Single-select / multi-select controls

| Use case | Component |
|---|---|
| Short-label exclusive (Sex, Goal, Meal type) | `ExclusiveButtonGroup` (M3 Expressive `ButtonGroup` of `ToggleButton`s — segmented "connected" look) |
| Long-label exclusive (Activity level, Weight-change rate) | `HapticFilterChip` column **or** `SelectableRow` (outlined card with check icon on selected) |
| Many tabs that could overflow | `PrimaryScrollableTabRow` — text + underline, scrolls horizontally, never clips |
| Binary on/off in context | A single right-aligned `HapticFilterChip` ("Show hidden") reads better than a 2-button segmented control |

`ExclusiveButtonGroup` with `fillWidth = false` is `horizontalScroll`-wrapped — handles overflow naturally for medium-length labels. But for many short tabs, `PrimaryScrollableTabRow` is the cleaner Material idiom.

## Compose pitfalls I burned cycles on

- **Removing `Scaffold` breaks status-bar insets** — the title overlaps the notch / dynamic island. Always wrap screens with `Scaffold { padding -> ... }` and apply `padding` to the root column.
- **`ModalBottomSheet` + plain `Column` doesn't scroll** when content overflows the sheet. Convert to `LazyColumn`; the sheet's nested-scroll connection then handles drag-to-dismiss correctly.
- **Shared `LazyListState` across tab switches** preserves scroll position from the previous tab — the user lands mid-list when switching. Reset to `0`:
  ```kotlin
  val listState = rememberLazyListState()
  LaunchedEffect(tab, subfilter, query) { listState.scrollToItem(0) }
  ```
- **`ExperimentalMaterial3ExpressiveApi`** annotations propagate aggressively — opt in once per file at the top.
- **`OutlinedCard` defaults to a tinted fill on dynamic color** — explicitly set `containerColor = Color.Transparent` if you want AMOLED-friendly. Or skip `OutlinedCard` entirely and use the `clip + border + padding` primitive — cleaner.
- **Asymmetric corner shapes (28/8/28/8)** felt "playful" but read inconsistently next to outlined cards. Symmetric 18–20dp wins.
- **`Icons.Default.ArrowBack` is deprecated** — use `Icons.AutoMirrored.Filled.ArrowBack`.
- **`rememberModalBottomSheetState(skipPartiallyExpanded = true)` is deprecated** — newer M3 wants `rememberBottomSheetState`. Migration is mechanical.

## ViewModel scope gotcha

> Writing the value that flips the top-level `NavHost` (e.g. `markOnboarded = true`) **destroys the current ViewModel**. Anything in the same `viewModelScope.launch` after that point gets cancelled mid-coroutine.

Order writes so the navigation-flipping write happens **last** in the coroutine:

```kotlin
fun finish() = viewModelScope.launch {
    runCatching { saveBackupFolder(...) }
    runCatching { saveReminders(...) }
    profileStore.save(state, markOnboarded = true)  // LAST — flips AppNav
    _state.update { it.copy(saved = true) }
}
```

## Navigation + deep links

`Routes.kt` holds `@Serializable` route objects. `AppNav.kt` consumes them via the type-safe `composable<RouteX>` overload.

For widget taps and notification taps → screens, build a `DeepLink` sealed interface:

```kotlin
sealed interface DeepLink {
    data class LogMeal(val mealName: String) : DeepLink
    data object OpenWater : DeepLink
    data object OpenWeight : DeepLink

    companion object {
        fun logMealIntent(context: Context, meal: String): Intent = ...
        fun openWaterIntent(context: Context): Intent = ...
    }
}
```

`MainActivity` is `launchMode="singleTop"` with `onNewIntent` handling. Notification small icon **must** be a tintable monochrome silhouette in a 24×24 vector drawable — never the mipmap launcher icon (Android can't tint multi-color icons, renders them as a white square in the status bar).

## Repository + Room flow shape

The pattern that worked for screens that combine multiple data sources without race conditions:

```kotlin
val state: StateFlow<UiState> = selectedDay
    .flatMapLatest { day ->
        val core = combine(
            profileStore.profile,
            repository.observeLogForDay(day),
            repository.observeWaterTotalForDay(day),
        ) { profile, logs, water -> CoreState(profile, logs, water) }

        val quick = combine(
            repository.observeFavouriteFoods(),
            repository.observeRecentFoods(),
        ) { fav, recent -> QuickState(fav, recent) }

        combine(core, quick) { c, q -> UiState(...) }
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5_000), initial)
```

`SharingStarted.WhileSubscribed(5_000)` is the right default — keeps the upstream alive briefly across config changes / process death without leaking forever.

## Storage / backups / SAF

- **Folder picker**: `ActivityResultContracts.OpenDocumentTree()`. URI label decoding via `URLDecoder` + slicing on `/tree/` for a human label like `primary:Documents/Backups`.
- **Default backup destination**: `MediaStore.Downloads.EXTERNAL_CONTENT_URI` with `RELATIVE_PATH = "Downloads/FoodTracker/"`. **API 29+ only** — if your `minSdk < 29` you need a legacy `Environment.getExternalStoragePublicDirectory()` path plus `WRITE_EXTERNAL_STORAGE` permission.
- **Backup payload**: kotlinx.serialization JSON with `prettyPrint = true` and `ignoreUnknownKeys = true`. Survive schema evolution gracefully.
- **Retention**: keep newest N (e.g. 12), delete older automatically inside `saveLocalBackup()` to bound storage.

## Reminders / alarms

- Use **`setAndAllowWhileIdle(RTC_WAKEUP, ...)`** — Doze-aware, inexact (fires within a window). No `SCHEDULE_EXACT_ALARM` permission needed, works on API 23+.
- Alarms are **one-shot** — the `BroadcastReceiver` must reschedule for tomorrow after each fire, otherwise it never fires again.
- Reschedule on `ACTION_BOOT_COMPLETED`, `ACTION_MY_PACKAGE_REPLACED`, `ACTION_LOCKED_BOOT_COMPLETED`. Use `goAsync()` to keep the receiver alive long enough for a coroutine to finish.
- **Battery-opt nudge** — on OEMs (Xiaomi/Oppo/Vivo/Realme/Samsung) with aggressive App Sleep, `setAndAllowWhileIdle` can still delay by hours. Show an inline card with `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` intent when a reminder is enabled but the app isn't exempt. Auto-hides once exempt.

## Glance widgets

- Read from Room DAOs directly inside `provideGlance { ... }` (suspend context). No extra plumbing needed.
- Widget click → `actionStartActivity(deepLinkIntent)` using the same `DeepLink` factory used by notifications.
- For "one-tap log" widgets, **do not** open the picker sheet — write the row directly with sensible defaults (first variant, 1 serving, current-meal). Widgets are deliberately fewer taps; the in-app Quick log already routes through the shared `ServingSheet` for confirmation.
- Glance 1.1+ requires `minSdk = 23`.

## API-level guarding I learned the hard way

Always gate platform features so you can drop `minSdk` later without code changes:

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    // POST_NOTIFICATIONS permission — pre-13 just works
}

if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    // dynamicLightColorScheme / dynamicDarkColorScheme
}

if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    // Notification channels
}

if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    // PowerManager.isIgnoringBatteryOptimizations
}
```

Reasonable `minSdk` floors for a new tracker app today:
- **31** (Android 12) — ~70% device coverage, keeps dynamic colors, loses themed icon + runtime notification prompt
- **29** (Android 10) — ~85% coverage, also loses dynamic colors. `MediaStore.Downloads` works.
- **26** (Android 8) — ~95% coverage. Needs legacy-storage fork for backups + may need `coreLibraryDesugaring` for `java.time`.

## Theming

- Brand palette (e.g. mint primary + gold accent) defined in `Color.kt`.
- `ThemeMode` enum: `System` / `Light` / `Dark`. `ColorPalette` enum: `Normal` / `Dynamic` (wallpaper-tinted on 31+).
- Dark mode stays **pitch black** under both Normal and Dynamic — apply a `.withAmoledSurfaces()` extension that overrides surface tokens to `#000000`. AMOLED battery-friendly.
- Persist both axes separately in DataStore.

## Patterns I'd repeat verbatim in a gym tracker

1. **Outlined-card primitive** — define it once as a `Section { title, content }` composable; use it everywhere except hero pieces.
2. **`HomeViewModel` shape** — `selectedDay` MutableStateFlow + `flatMapLatest` into a combined screen state. Trivial to add "shift day ±1" controls.
3. **`ServingSheet`-style picker shared by Home + LogFood** — extract the bottom-sheet content as a public composable in `ui/components/`. Both surfaces pop the same UI.
4. **Quick stat tiles row** — 3 outlined tiles with icon + title + value + sub. Maps perfectly to gym (Volume / Sets / PRs).
5. **Trend line chart with goal reference** — `LineChart(points, lineColor, unit, referenceLine, referenceLabel)`. Y-axis expands to include the goal line so it stays visible.
6. **WeeklyBarChart** for 7-day rollups.
7. **`HapticSlider`** wrapping `Slider` — granularity-snapped + haptic ticks on tick crossings. Feels miles better than the raw component.
8. **`NumberStepper`** for ± fine adjustment alongside a slider for coarse drag.
9. **Soft-delete + restore for user-created items** — never wipe history on delete; provide a "deleted" tab the user can restore from. Custom foods → custom exercises.
10. **Seed catalog with versioning** — bump `SEED_VERSION` when you add canonical entries; upsert keyed by name so favourites/history survive.

## Things I'd skip / change for a gym tracker

- **Don't ship AI features**. Manual logging is fast enough; auto-detection adds support burden and rarely improves accuracy.
- **Don't auto-detect units** (kg vs lb basis, per-100g vs per-serving) from external data sources — it gets it wrong often. Make the user toggle once, then sticky-remember per item.
- **Don't reorder sections opportunistically when restyling** — a "rewrite UI" pass should keep the section order the user is used to. Reorder only when explicitly requested.
- **Don't put potentially-unbounded lists inside one section card**. Use a flat heading + lazy rows.
- **Don't bury the primary action in a topbar icon**. Surface it as a row in the relevant tab, or a FAB.
- **Don't over-document in code comments** — comment the *why* (hidden constraint, non-obvious decision), never the *what*.

## Project structure that scaled cleanly

```
data/
  db/                    Room entities + DAOs + AppDatabase
  backup/                BackupManager, BackupScheduler, BackupPayload
  ProfileStore.kt        DataStore wrapper for the user profile + settings
  FoodRepository.kt      Single source of truth combining Room + remote (OFF/USDA)
domain/
  GoalCalculator.kt      Pure math — testable in isolation
  Bmi.kt                 Pure math
reminder/
  ReminderStore.kt       DataStore for reminder configs
  ReminderScheduler.kt   AlarmManager wrapper
  ReminderReceiver.kt    BroadcastReceiver (fire + reboot rescheduling)
ui/
  components/            Reusable composables — Section, StatTile, ServingSheet, ...
  navigation/            AppNav.kt + Routes.kt
  screens/               One folder per top-level screen
  theme/                 Color.kt, Theme.kt, Dimens.kt, ThemeMode.kt
  Haptics.kt             rememberAppHaptics() — central tick/confirm vocabulary
widget/                  Glance widgets — each is one folder
```

`domain/` is intentionally pure — no Android imports. Unit-testable.

`ui/components/` is the central design-system surface. Anything used by more than one screen lives here. Resist screen-local copies; promote upward instead.

## Final note

The biggest UX wins came from **consistency**, not novelty:
- Same outlined-card primitive on every screen
- Same titleMedium SemiBold heading
- Same 18dp corners, same 14h/12v padding, same 8dp gap
- Same divider color (`outlineVariant.copy(alpha = 0.35f)`)
- Same single-select component for short labels (`ExclusiveButtonGroup`)
- Same haptic vocabulary (`light()` for taps, `confirm()` for commits)

Pick a small set of primitives, define them once, and use them everywhere. The screens that diverged from this pattern were always the ones that got rewritten.
