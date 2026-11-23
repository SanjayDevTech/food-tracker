# Design plan — Food Tracker UI/UX rewrite

> **Superseded in part — read `docs/ui-house-rules.md` first.**
>
> All eight phases below shipped. The result still read as the old app, because
> §1's thesis describes a look the app *already had*: a black canvas, hairline
> outlined cards and a mint accent. §0's faults were real but they were **code**
> faults — six card implementations, three meters, unthemed hex — and collapsing
> six cards into one makes the screen look like whichever one you standardised
> on. An editorial pass followed, taking the layout to what reference shot 4
> actually specifies. Where this document and the house rules disagree, the
> house rules win. Known conflicts:
>
> | § | Says | Now |
> |---|---|---|
> | §3, §4.1 | `CalorieHero` keeps a ring | No ring. Shot 4 has none — rings came from shots 1/5/9, the rejected Direction A. An oversized numeral over a thin rule. |
> | §4.1, §4.2, §4.3 | Screens are stacks of bordered cards | No cards. A standing head with a rule under it; content open to the gutter. |
> | §4.2 | Chart insight is a sentence in body text | A number with an eyebrow label. |
> | §4.2 | Trailing action sits in the card header | The head's trailing slot holds the section's figure; actions moved to the foot. |
> | §4.3 | `You` rows use shot 9's icon-circle anatomy | No discs, no icons — shot 9 is Direction A. Label / value / chevron. |
> | §4.1, §4.3 | Emoji on meal rows, stat tiles, settings rows | Gone. The food picker's per-category emoji stay (§8). |
> | §1, §2 | Selected chips and segments fill with `primaryContainer` | The accent means "on track" and belongs to data. Selection is a rule or a label at full strength. |
> | §5 | All-nutrients is a full-height bottom sheet | A page. A sheet that raises another sheet has no back convention; leaf pages hide the bar, so the objection there doesn't apply. |
> | §4.7 | "28 nutrients" | 29. `AllNutrientFields` has 30 entries; minus calories that is 29. |

## 0. Preliminaries

### Reference shots — seen 2026-11-22

The agent could not fetch any of the nine Dribbble links (HTTP 202, bot challenge). The owner
supplied all nine as screenshots afterwards. What follows is from the images.

**The set is not one direction, it is two, and they conflict.**

- **A — soft / friendly** (shots 1, 2, 5, 7, 8, 9): pastel tints, soft drop shadows, rounded
  filled cards, colour-coded meal blocks, emoji and illustration, centre-docked FAB. Every one
  of them is light-mode only.
- **B — editorial / instrument** (shots 4, 6, and the onboarding half of 3): near-monochrome
  plus a single accent, UPPERCASE micro-labels with letterspacing, oversized tabular numerals,
  hairlines instead of shadows, no decoration.

**DECISION: Direction B.** Shot 4 is effectively a visual spec for the thesis in §1 already —
`CALORIES CONSUMED / 1,684 KCAL`, `TARGET` and `REMAINING` as two columns over a thin bar,
three macro columns with bars, uppercase eyebrows throughout.

Direction A was rejected on the constraints, not on taste: soft shadows and pastel tints need a
light grey canvas to read against and turn to mud on `#000000`, and none of the A shots carries
more than about six nutrients without its padding fighting back.

**Adopted from specific shots:**

| From | Pattern | Changes |
|---|---|---|
| Shot 3 | **Ruler weight picker** — a large value over a horizontal tick scale | §4.9 — replaces the stepper + slider in the weigh-in sheet |
| Shot 5 | **Week day strip** with the selected day filled | §3 — independently confirms `DayStrip` |
| Shots 1, 9 | **Stacked rounded bar chart**, macros as stacked segments per day | §4.2 — replaces the four separate sparkline rows in the Macros card |
| Shot 9 | **Settings list row** — icon circle, label, value, chevron | §4.3 — confirms the `You` page row pattern |
| Shot 4 | **`TARGET` / `REMAINING` as two labelled columns** over a thin progress bar | §3 — replaces the single caption under the ring in `CalorieHero` |

**Explicitly refused:**

- **Food photography** (shots 2, 5, 8, 9 lean on it) — 166 seeded foods, no images, and none coming.
- **Glassmorphism** (shot 2) — unreadable on OLED black, and expensive to render.
- **Centre-docked FAB** (1, 5, 7, 9) — bottom-end was chosen for one-handed reach on a 412dp phone.
- **Dot-matrix numerals** (shot 4) — the best thing in the set, but illegible at small sizes and it
  needs a bundled font, which open question 7 ruled out.
- **Gradient fills and illustration** (1, 2, 7, 8) — §1 is zero-elevation hairline.

**What survives the move to dark, and what does not.** Every shot is light-mode, but that matters
far less than it first appears:

- **Layout, type, density, proportion and component anatomy are theme-independent** and transfer
  unchanged. All five adopted patterns above are of this kind. Shot 4's value was never its colour
  — it was the uppercase eyebrow over an oversized tabular numeral, and the two-column
  `TARGET`/`REMAINING` split. That reads the same on black.
- **Colour values do not invert.** A hue tuned against white needs lower saturation and higher
  lightness on black or it vibrates; shot 4's `#FF4A21` would glare. The stacked-bar segment
  colours borrowed from shots 1 and 9 therefore need re-tuning, not flipping.
- **Shadow-based structure has no dark equivalent.** Shots 1, 2, 5, 8 and 9 build hierarchy from
  soft drop shadows, which do not exist on `#000000`. That is a different structural system, not a
  modification — and it is the sharpest argument for Direction B, because **shot 4 already builds
  structure from hairlines**, the way a dark UI has to.

So the only thing that genuinely lost its reference when light mode was dropped is **the palette**,
and a palette is derived from contrast targets rather than copied from a shot. The practical method:
measure the relationships in shot 4 — accent-to-canvas, label-to-value, hairline-to-background —
and reproduce those ratios on black.

**Independently unreferenced, in any theme:** no shot in the set handles more than ~6 nutrients, so
the 28-cell strip and the nutrient heatmap remain original to this plan. That gap has nothing to do
with light versus dark.

**Accent re-checked against the references and unchanged.** Shot 4's orange-red is the most striking
thing in the set, but it is the sole accent in an app with no "over limit" concept. This app has one,
and it must be orange-red — the same collision that ruled out terracotta in open question 9. Deep
green stands.

### What I actually read

All of `ui/` (~11.6k lines), plus `data/Nutrients.kt`, `NutrientGoals.kt`, `MealType.kt`, `CookingOil.kt`, `FoodStyle.kt`, `ServingVariant.kt`, `db/LogEntities.kt`, `db/FoodDao.kt`, `db/FoodEntity.kt`, `FoodRepository.kt`, the manifest, and the widget inventory.

### Four concrete faults I found while reading (these drive several "rewrite" calls)

1. **Six implementations of the same card.** `SectionCard` (22dp, OutlinedCard), `FormSection` (18dp, border), plus private near-identical copies `HomeSection` (HomeScreen.kt), `WaterSection`, `WeightSection`, `ProgressSection`. Same visual intent, four radii, three padding pairs.
2. **Unthemed hardcoded colors.** `MacroDonut.kt` draws the ring track as `Color(0x22000000)` — invisible on OLED black — and the arc as a literal `0xFFE85D2D`/`0xFFBA1A1A`. `WeeklyBarChart.kt` hardcodes the goal line `0xFFE85D2D`. `FoodRow.kt` has 12 hardcoded pastel category bubbles that only work in light. `BmiBar.kt` has 4 hardcoded band colors.
3. **Three separate meter implementations** — `NutrientBar`'s inner Box pair, `ContributionRow`'s, and `RichFoodsScreen.NutrientReadout`'s. All the same thing.
4. **You cannot edit a logged entry.** `MealEntryRow` offers delete only, and `FoodLogDao` has `insert` but no `@Update`. Wrong serving? Delete and re-log. This is the biggest behavioural hole in the app.

Also: the hard constraints are not currently met. `Theme.kt` uses `MaterialExpressiveTheme`, `ExclusiveButtonGroup` is built on the Expressive `ButtonGroup`/`toggleableItem`, `LogFoodScreen` uses the Expressive `LoadingIndicator`, and `ColorPalette.Dynamic` wires up wallpaper theming with a Settings row to pick it. All of that has to come out. The README's "Stack" section also advertises Material 3 Expressive.

---

## 1. Design direction

### Thesis

**A black instrument panel.** One theme, always dark. Structure comes from hairlines, whitespace
and type weight — never from elevation, never from filled cards. The canvas is quiet and the
numbers are loud: a tracker is read in two-second glances, so the only thing allowed to be big is
a number. One accent, one warning hue, one small data palette shared by every chart and meter so
protein is the same colour everywhere. The background is `#000000` and stays there — cards are
outlines on black, not grey rectangles.

Dropping light mode (decision 11) removes the "warm paper by day" half of the original thesis.
That is a gain in coherence, not a loss: the plan no longer has to make one accent and one data
palette work against both beige and black, which is what forced every colour here into a
compromise.

### Colour roles — one scheme

| Role | Value |
|---|---|
| `background` / `surface` / `surfaceContainerLowest` / `surfaceDim` | `#000000` |
| `surfaceContainerLow` | `#070707` |
| `surfaceContainer` | `#0C0C0B` |
| `surfaceContainerHigh` | `#141312` |
| `surfaceContainerHighest` | `#1C1A18` |
| `onSurface` | `#EFE9E0` |
| `onSurfaceVariant` | `#A79E92` |
| `outline` | `#36312B` |
| `outlineVariant` | `#201D1A` |
| `primary` | `#63D2B0` |
| `onPrimary` | `#00251B` |
| `primaryContainer` | `#123A2F` |
| `onPrimaryContainer` | `#B3EEDC` |
| `tertiary` | `#E0A961` |
| `error` | `#FF6B5E` |

`onSurface` is deliberately `#EFE9E0`, not `#FFFFFF`. Pure white on pure black haloes badly for
a lot of people, and this app is read for minutes at a time in a dark room.

**Tokens stay semantic, not literal.** Every role above keeps its Material name and every
component refers to the role, never the hex. Adding a light scheme later is then a second value
table in `Color.kt` and nothing else — no component changes. That is the whole cost of keeping
the door open, so keep it open.

**Rule governing the containers:** the `surfaceContainer*` ramp exists only for *modal* surfaces (bottom sheets, menus, dialogs) and *pressed* states. Cards on a page never fill. In dark that means ≥95% of lit pixels are type and hairlines.

**Data palette** — tuned for black only; freed from dual-theme compromise, these can run brighter

```
carbs    #E3A13C     protein  #DD6B4C     fat      #B9924B
fiber    #8A9A4B     water    #5E9CBF     weight   #7FA98A
minerals #8A9A4B     vitamins #5E9CBF     (reuse — see §3 on NutrientVisuals)
```

**Status palette (new — the micronutrient system depends on it)**

```
unmet     outlineVariant            (<50% of goal)
partial   primary @ 40% alpha       (50–99%)
met       primary                   (≥100% goal, or ≤100% limit)
headroom  outline                   (limit field, under cap)
over      #C2410C / dark #F08A4B    (limit field, 100–125%)
far-over  error                     (limit field, >125%)
```

### Type scale

Stay on `FontFamily.Default` — the repo just shipped "Trim the APK" and a bundled font undoes part of that. Add `fontFeatureSettings = "tnum"` to every numeric style so digits stop jittering as values animate.

| Token | Size/line | Weight | Use |
|---|---|---|---|
| `numberXL` | 40/44, tnum | SemiBold | hero kcal-left |
| `numberL` | 28/32, tnum | SemiBold | screen headline numbers |
| `numberM` | 18/24, tnum | SemiBold | stat tiles, nutrient values |
| `titleL` | 20/28 | SemiBold | screen & sheet titles |
| `titleM` | 16/22 | SemiBold | card titles, food names |
| `body` | 15/22 | Normal | primary reading size (up from 14) |
| `bodyS` | 13/18 | Normal | secondary lines |
| `label` | 13/18 | Medium | chips, buttons |
| `eyebrow` | 11/16, +0.5sp | Medium, UPPERCASE | section headers |

That is nine styles replacing the current nine — but `displaySmall 32` becomes `numberXL 40` (the hero number is currently undersized for its 160dp ring) and `bodyMedium 14` becomes `body 15`.

### Spacing scale

`4 · 8 · 12 · 16 · 20 · 24 · 32`. Nothing else. Screen gutter **16**. Card padding **16** (today it is 18 in `SectionCard`, 14h/12v in the five others). Gap inside a card **12**. Gap between cards **16** (up from 12 — the screens read cramped).

### Radii

Current: 16, 18, 20, 22, 28, plus pill. Reduce to four:

```
r.sm   = 8dp    badges, meter caps, status cells
r.md   = 14dp   inner rows, stat tiles, list rows
r.lg   = 20dp   cards; sheets use 24dp top-only
r.pill = 999    chips, buttons, segmented, FAB
```

### Elevation & border

**Zero elevation anywhere.** No `CardDefaults` shadow, no `FloatingActionButtonDefaults.elevation(6.dp)` (the current FAB has 6/12). Structure = `1dp outlineVariant` hairline. Only five things get a fill: bottom sheets/dialogs (`surfaceContainer`), the primary button and FAB (`primary`), a selected chip (`primaryContainer`), the "now" meal row (`primaryContainer` @ 35%), and the day-strip's today cell. `TopAppBar` gets `containerColor = background` and no scroll-elevation tint.

### Motion

| Event | Spec |
|---|---|
| generic state change | 200ms `FastOutSlowIn` |
| meter / ring / number | 450ms `easeOutCubic`, value-interpolated |
| nav push & pop | **keep exactly as is** — 300ms slide + the `predictivePop*` pair in `AppNav.kt`. It is correct, it handles predictive-back properly, and the comment explaining why is right. |
| bottom-bar destination switch | fade-through: 90ms fadeOut → 210ms fadeIn. No slide; peers don't slide. |
| bottom sheet | Material default |
| banned | spring overshoot, shape-morph on press, shared-element — that is the Expressive feel being removed |

---

## 2. Atoms

New package `ui/components/atoms/`. Mapped against the current `components/` list.

| Atom | Verdict | Reason |
|---|---|---|
| `AppCard` | **new** | One container replaces `SectionCard`, `FormSection`, and the four private clones (`HomeSection`, `WaterSection`, `WeightSection`, `ProgressSection`). |
| `SectionCard` | **delete** | Folded into `AppCard`. |
| `FormSection` / `FormLabel` | **delete / keep** | `FormSection` → `AppCard`; `FormLabel` → `SectionHeader`. The "they deliberately differ" docstring is defending a distinction nobody can see. |
| `Meter` | **new** | One goal/limit-aware linear meter; replaces the three copies in `NutrientBar`, `ContributionRow`, `NutrientReadout`. |
| `Ring` | **new** | Arc-progress primitive extracted out of `CalorieHero`, properly themed. |
| `StatusCell` | **new** | 10×10 rounded square in one of six status colors. The unit of the nutrient strip and the trends heatmap. |
| `ValueText` | **new** | `(value, unit, style)` with tnum + the smart-rounding rule. Kills ~40 ad-hoc `"%.1f".format()` + Text pairs. |
| `SectionHeader` | **new** | Eyebrow caption + optional trailing slot. |
| `IconCircle` | **new** | Tinted disc; four copies today (`EmptyState`, `FoodRow`, `AddCustomFoodRow`, `AvatarEmoji`). |
| `SegmentedControl` | **rewrite** | `ExclusiveButtonGroup` is built on Expressive `ButtonGroup`/`toggleableItem` — must go. Same API (generic, `fillWidth`, label lambda), hand-rolled from `Row` + `Surface`. |
| `Pill` | **revise** | `HapticFilterChip` → hand-rolled pill so it survives the Expressive removal and matches `r.pill`; keeps the haptic tick, which is a good idea. |
| `Stepper` | **revise** | `NumberStepper` kept; buttons 32→36dp, value uses `ValueText`, tonal fill → outline. |
| `AppSlider` | **keep** | `HapticSlider`'s bucket-crossing tick is genuinely good; only the track colors change. |
| `EmptyState` | **keep** | Centred disc + title + capped-measure message + optional action is correct; the docstring's reasoning holds. |
| `AppButton` / `AppTextButton` | **revise** | Height 52→**48**; pill; no elevation. Drop `ButtonHeightHero` (56) — onboarding uses the same button as everywhere else. |
| `AppTopBar` | **revise** | Thin wrapper on `TopAppBar` with `containerColor = background`, no scroll tint, title in `titleL`. |
| `Spinner` | **new** | Wraps `CircularProgressIndicator`; replaces the Expressive `LoadingIndicator` in `LogFoodScreen`. |
| `Divider` | **new** | Hairline at `outlineVariant`; the `.copy(alpha = 0.35f)` appears in four files. |
| `TimePickerDialog` | **keep** | Fine. Only its Card fill changes. |
| `BatteryOptCard` | **revise** | Drop the `tertiaryContainer` fill (the only filled card left on a page); `AppCard` + tertiary-colored title. |
| `LineChart` | **revise** | Keep the whole drawing model. Theme the dashed goal line, add a 12%→0% vertical gradient under the path, and make the dot radius 3.5→3dp. |
| `WeeklyBarChart` | **revise** | Same; de-hardcode `0xFFE85D2D`. |
| `BmiBar` | **revise** | Keep the band+marker design (it's good). Move the four band colors to tokens so it isn't four more hardcoded hex values. |
| `MacroDonut.kt` / `CalorieHero` | **rewrite** | Split: `Ring` atom + `CalorieHero` molecule. Fix the unthemed track/arc. |
| `NutrientColor` | **revise → `NutrientVisuals`** | Keep the "one hue per concept" principle; add `NutrientField.status(consumed, goal): NutrientStatus` beside `.color()`, since every new surface needs status, not just hue. |
| `NutrientSubRows` | **keep** | Deliberately plain, behind a tap, with an honest coverage note. Nothing to improve. |
| `FoodRow` | **revise** | Keep emoji (scanning 166 foods, it earns its place). Replace the 12 hardcoded pastel bubbles with a single `surfaceContainerHigh` disc — they are light-theme-only and vanish on `#000`. |
| `NumberField` (AddFood-local) | **promote** | Becomes an atom with a compact 2-column variant. |
| `Haptics` | **keep** | Untouched. |

---

## 3. Molecules

| Molecule | Verdict | Reason |
|---|---|---|
| `CalorieHero` | **rewrite** | Ring left + macro legend right is right; the execution isn't. Ring 160→140dp, stroke 18→14dp, number `numberXL`, a `860 / 2 100 kcal` caption under the ring, and legend rows gain a 3px macro-colored meter so "74 g protein" reads against its target instead of floating. |
| `NutrientRow` (was `NutrientBar`) | **revise** | Drop the third line (`"62% of goal"` — the `x / y unit` line already says it). Two lines, 3dp meter, **limit-aware**: for a `Limit` field the track shows a cap tick and the fill is neutral below 80%, `over` at 100%, `far-over` past 125%. Today a sodium reading of 40% looks like failure. |
| `NutrientStatusStrip` | **new** | 28 `StatusCell`s in a wrapped row, one per nutrient, colored by status. The whole day's micronutrient state in one 40dp band. **This is the core answer to the "30 micros is unusually data-heavy" problem** — see §4, Today. |
| `NutrientCell` | **new** | Half-width cell: label, `ValueText`, 3dp meter. 28 of these in a 2-column grid = 14 rows ≈ 2 screens, versus 28 three-line bars ≈ 6 screens. |
| `NutrientHeatmap` | **new** | nutrients × days grid of `StatusCell`s for Trends. Answers "am I *chronically* low on B12", which no amount of per-day bars can. |
| `MealGroup` + `MealEntryRow` | **revise** | Keep the combined-card-with-four-rows design (the docstring's reasoning — four mostly-empty cards was worse — is correct). Two changes: the entry row becomes **tappable → edit sheet**, and delete moves from an always-visible trash icon to a swipe-to-delete with undo snackbar. Four trash icons per meal is four chances to lose data. |
| `DayStrip` | **new** | Replaces `InlineDayPicker`. Seven day cells (letter + date), today filled, horizontally scrollable back in time, forward-capped at today. The current `[◀ Today ▶]` pill requires you to know what day you're on before you can get there. |
| `GreetingHeader` | **delete** | "Good evening, Ananya 👋" costs the full top line of the one screen that most needs vertical room, and the owner knows their own name. Replaced by a plain `AppTopBar("Today")` + date subtitle. |
| `OverflowMenu` | **delete** | Stats, Rich in…, Profile, Settings are hidden behind three dots. They become bottom-bar destinations / a top-bar action. |
| `GoalProgressBanner` | **revise** | Stop being a filled `tertiaryContainer` card on Today. The content moves to the **You** page as a meter under the profile header, where goal context belongs; Today keeps only a one-line variant *if* within 1kg of target ("0.4 kg to go"). |
| `StatTile` | **keep** | Text-forward, outlined, two-up. Correct. Only radius→`r.md`, value→`numberM`. |
| `ChartCard` | **new** | Title + insight sentence + chart + optional trailing action. Unifies `ProgressSection`/`WaterSection`/`WeightSection`. |
| `QuickLogSection` | **DELETE** | Approved. See §7 phase 3 for what it frees. |
| `ContributionRow` | **keep** | Name / grams / amount / share% / meter. Good. Rebuilt on `Meter`. |
| `RichFoodRow` + `NutrientReadout` | **keep** | The "one readout per pick, same component at any length" reasoning is right, and the goal-fraction meter is what makes the ranking legible. Rebuilt on `Meter`. |
| `OnlineSearchSection` | **revise** | Stop living at the bottom of the local results list where it's only found by scrolling. Becomes a pinned footer bar on the Search state: `No luck? [ Search online ]`. |
| `DeleteOptionRow` → `ChoiceRow` | **keep, rename** | Icon + title + subtitle in an outlined button. Reused by the delete sheets *and* the new entry-edit sheet. |
| `AddCustomFoodRow` | **keep** | Pinned atop the Custom tab. The docstring is right that a top-bar icon was worse. |
| `AiProgressDialog` | **keep** | Untouched. |
| `ProfileHeaderCard` / `StatBlock` / `TargetRow` | **revise** | Keep structure; add the goal-progress meter, swap to `ValueText`. |
| `BasisRow` / `CategoryChips` | **revise** | Rebuilt on the new `SegmentedControl` / `Pill`; `CategoryChips`'s 4 rows × `ButtonGroup` becomes one `FlowRow` of pills. |

### Serving-sheet internals (all **new** — see §6 for behaviour)

`SheetHeader` (name + brand + live kcal/macro readout) · `AmountRow` (`[−] 2 × idli (70 g) [+]` + slider) · `PreparationBlock` (oil axis + style axis under one heading) · `LivePreview` (nutrient grid with change-highlighting) · `SheetFooter` (meal chip + primary action, pinned).

---

## 4. Organisms & screen layouts

Viewport assumed: 412×915dp (a typical modern phone), ~640dp of content between top bar and bottom bar.

### 4.1 Today (bottom destination 1)

```
┌────────────────────────────────────────────┐
│ Today                              ⚙   👤  │  56dp  AppTopBar, no tint
│ Sat 22 Nov                                 │        date = bodyS, onSurfaceVariant
├────────────────────────────────────────────┤
│  M    T    W    T    F   ╭S╮   S           │  56dp  DayStrip (scrolls away)
│  17   18   19   20   21  │22│  23          │
├────────────────────────────────────────────┤
│                                            │
│      ╭─────────╮     ▪ Carbs      212 g    │
│     │   1 240   │    ▰▰▰▰▰▰▱▱             │ 170dp CalorieHero
│     │   left    │    ▪ Protein      74 g   │
│      ╰─────────╯     ▰▰▰▰▱▱▱▱             │
│    860 / 2 100 kcal  ▪ Fat         48 g    │
│                      ▪ Fiber       21 g    │
├────────────────────────────────────────────┤
│ ┌──────────────────┐ ┌──────────────────┐  │
│ │ 💧 WATER         │ │ ⚖ WEIGHT         │  │  84dp
│ │ 1 200 ml         │ │ 72.4 kg          │  │
│ │ of 2 600 ml      │ │ 3 days ago       │  │
│ └──────────────────┘ └──────────────────┘  │
├────────────────────────────────────────────┤   ← fold ≈ here
│ ┌────────────────────────────────────────┐ │
│ │ MEALS                      860 kcal    │ │
│ │ 🌞 Breakfast      320 kcal · 2     ⌄ + │ │
│ │ ─────────────────────────────────────  │ │
│ │ 🍛 Lunch  [Now]   540 kcal · 3       + │ │   current meal auto-expanded
│ │     Masala dosa      1 × · 180 kcal    │ │   rows tappable → edit sheet
│ │     Sambar           1 × · 120 kcal    │ │   swipe-left → delete + undo
│ │     Filter coffee    1 × ·  90 kcal    │ │
│ │ ─────────────────────────────────────  │ │
│ │ 🍪 Snack          Nothing yet        + │ │
│ │ ─────────────────────────────────────  │ │
│ │ 🌙 Dinner         Nothing yet        + │ │
│ └────────────────────────────────────────┘ │
├────────────────────────────────────────────┤
│ ┌────────────────────────────────────────┐ │
│ │ NUTRIENTS            21 of 28 on track │ │
│ │ ▪▪▪▪▫▪▪▪▪▪▪▫▪▪▪▪▪▪▪▪▫▫▪▪▪▪▪◆          │ │  ← NutrientStatusStrip
│ │                                        │ │
│ │ SHORT ON                               │ │
│ │  Vitamin B12    0.4 / 2.4 µg  ▰▱▱▱▱   │ │
│ │  Calcium        380 / 1000 mg ▰▰▱▱▱   │ │
│ │  Iron           6.1 / 17 mg   ▰▰▱▱▱   │ │
│ │ OVER                                   │ │
│ │  Sodium       2 740 / 2 300 mg ▰▰▰▰◆  │ │
│ │                                        │ │
│ │ ──────── See all 28 nutrients ───────  │ │
│ └────────────────────────────────────────┘ │
│                                 ╭────────╮ │
│                                 │ +  Log │ │  FAB, pill, no shadow
│                                 ╰────────╯ │
├────────────────────────────────────────────┤
│   ●Today        ○Trends        ○You        │  80dp bottom bar
└────────────────────────────────────────────┘
```

**Scroll:** one `LazyColumn`. Top bar and bottom bar are fixed; everything else scrolls, including the day strip. FAB is `Scaffold`-hosted, bottom-end, and does *not* hide on scroll (one-handed reach matters more than seeing one more row).

**Above the fold:** day, kcal-left, macros, water, weight, and the top of Meals. That is the whole "how am I doing" question answered without a scroll, which the current layout does not manage because the greeting row and the goal banner eat 120dp first.

**Why the nutrient card is built this way** — this is the part the Dribbble genre can't help with. 28 fields against goals cannot be 28 bars; at three lines each that is six screens of scrolling nobody reads. So the card answers three questions at three depths:
- *Is today broadly fine?* → the headline count and the 28-cell strip. One glance, zero reading.
- *What specifically is wrong?* → up to 4 "short on" rows (lowest goal fraction) and up to 2 "over" rows (`Limit` fields past cap). Only the exceptions get names.
- *Everything?* → one tap to the full-nutrient sheet.

The strip is also tappable per-cell (tooltip-style: tap a cell → scrolls the full sheet to that nutrient).

**Empty state (nothing logged):** ring empty, centre reads `2 100` / `to go`; macro legend all `0 g` in `onSurfaceVariant`; stat tiles `—` with "Tap to log"; meals all "Nothing yet"; the nutrients card collapses to an `EmptyState` — *"Log something and your 28 nutrient targets appear here."*

**Loading state:** there isn't one and shouldn't be. `HomeViewModel.state` has a non-null initial value built from a default profile. Render that immediately; the ring's `animateFloatAsState` absorbs the transition when real data arrives. No skeleton, no spinner.

---

### 4.2 Trends (bottom destination 2)

Replaces `ProgressScreen` and absorbs the chart halves of `WeightScreen` and `WaterScreen`.

```
┌────────────────────────────────────────────┐
│ Trends                                     │  pinned
│ ┌──────┬──────┬──────┬──────┐              │  pinned range control
│ │ 7 d  │ 30 d │ 90 d │ 1 y  │              │
│ └──────┴──────┴──────┴──────┘              │
├────────────────────────────────────────────┤
│ ┌────────────────────────────────────────┐ │
│ │ CALORIES                               │ │
│ │ 1 980 avg · 120 under goal             │ │   ← insight sentence
│ │ ╭──────────────────────────────────╮   │ │
│ │ │       ╱╲    ╱╲                   │   │ │
│ │ │ ─ ─ ─╱─ ╲─ ╱─ ╲─ ─ ─ ─ goal ─ ─  │   │ │
│ │ │ ╱╲  ╱    ╲╱    ╲╱╲               │   │ │
│ │ ╰──────────────────────────────────╯   │ │
│ │  17   19   21   23   25   27   29      │ │
│ └────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────┐ │
│ │ MACROS                                 │ │
│ │ Protein hit target on 19 of 30 days    │ │
│ │  Carbs    ╭────╮ ▁▃▅▂▆▃▄▅▂▇▃▄ 248 g   │ │  ← 4 sparkline rows
│ │  Protein  ╭────╮ ▅▄▆▇▃▅▆▄▅▃▆▅  96 g   │ │
│ │  Fat      ╭────╮ ▃▄▂▅▃▄▃▅▄▂▃▄  62 g   │ │
│ │  Fiber    ╭────╮ ▂▃▁▄▂▃▂▄▃▁▂▃  24 g   │ │
│ └────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────┐ │
│ │ NUTRIENT CONSISTENCY                   │ │
│ │ 6 nutrients missed on most days        │ │
│ │            17 18 19 20 21 22 ...       │ │  ← NutrientHeatmap
│ │ Calcium    ▪  ▫  ▫  ▪  ▫  ▫            │ │     rows = nutrients
│ │ Iron       ▫  ▫  ▪  ▫  ▫  ▪            │ │     cols = days
│ │ Vit B12    ▫  ▫  ▫  ▫  ▫  ▫            │ │     sorted worst-first
│ │ Folate     ▪  ▪  ▫  ▪  ▪  ▪            │ │
│ │ Vit D      ▫  ▫  ▫  ▫  ▫  ▫            │ │
│ │ Sodium     ◆  ◆  ▪  ◆  ▪  ◆            │ │
│ │ ───────────── Show all 28 ───────────  │ │
│ └────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────┐ │
│ │ WEIGHT                      [ Log ⚖ ]  │ │  ← trailing action → weigh-in sheet
│ │ −1.2 kg over 30 days · 72.4 kg now     │ │
│ │ ╭──────────────────────────────────╮   │ │
│ │ │ ╲╲__                             │   │ │
│ │ │     ╲╲___╱╲__                    │   │ │
│ │ │ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ Goal 68.0 ─  │   │ │
│ │ ╰──────────────────────────────────╯   │ │
│ │ ──────────── History (34) ───────────  │ │  ← expands inline, lazily
│ └────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────┐ │
│ │ WATER                        [ + 💧 ]  │ │
│ │ 2 180 ml / day avg · goal 2 600        │ │
│ │  ▂ ▅ ▃ ▆ ▄ ▇ ▅   M T W T F S S         │ │
│ └────────────────────────────────────────┘ │
├────────────────────────────────────────────┤
│   ○Today        ●Trends        ○You        │
└────────────────────────────────────────────┘
```

**Scroll:** `LazyColumn` of `ChartCard`s; the range control is pinned outside it (changing range mid-scroll should not require scrolling back up).
**Above the fold:** range + the calories chart + the top of Macros.
**Empty:** each card independently degrades — `<2` points → the card keeps its header and shows one muted line ("Log two or more days in this range to see the trend"). Matches today's `EmptyChart`/`ProgressChart` behaviour, which is already right.
**Loading:** range changes recompute off a Room flow; keep the previous chart on screen and crossfade. No spinner.

Note the heatmap is the only genuinely new analysis, and it's the one that justifies tracking 30 micronutrients at all: a single day's B12 is noise, thirty days of it is a finding.

---

### 4.3 You (bottom destination 3)

Merges `ProfileScreen` + `SettingsScreen`. Settings today renders every control inline (toggles, API-key field, backup list, export buttons, delete-all dialog) in one 662-line wall; the rewrite makes it a list of rows that open sheets (short) or pages (long).

```
┌────────────────────────────────────────────┐
│ You                                        │
├────────────────────────────────────────────┤
│ ┌────────────────────────────────────────┐ │
│ │  ╭──╮  Ananya                          │ │
│ │  │👨│  Lose · 0.50 kg/week             │ │
│ │  ╰──╯                                  │ │
│ │  AGE 34   HEIGHT 176cm   WEIGHT 72.4kg │ │
│ │  BMR 1 640   TDEE 2 540   Moderate     │ │
│ │                                        │ │
│ │  72.4 ▰▰▰▰▰▰▰▱▱▱ 68.0 kg               │ │  ← goal meter (was the Today banner)
│ │  4.4 kg to lose · ~9 weeks             │ │
│ │                                        │ │
│ │  [        Edit profile & goals       ] │ │
│ └────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────┐ │
│ │ DAILY TARGETS                          │ │
│ │  Calories                  2 100 kcal  │ │
│ │  Protein                       118 g   │ │
│ │  Carbs                         236 g   │ │
│ │  Water                       2 600 ml  │ │
│ │  ───────── See all 31 targets ───────  │ │  → sheet
│ └────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────┐ │
│ │ APP                                    │ │
│ │  🔔  Reminders        Meals, water   > │ │  → page
│ │  ✨  AI autofill              Off    > │ │  → sheet
│ │  ☁️  Data & backup      Daily, 4     > │ │  → page
│ │  🛡️  Privacy                         > │ │  → page
│ │  📝  Custom foods             14     > │ │  → sheet
│ │  📳  Haptic feedback          [ ●]     │ │  inline toggle
│ └────────────────────────────────────────┘ │
│   Food Tracker 1.6 (7) · offline-first     │
├────────────────────────────────────────────┤
│   ○Today        ○Trends        ●You        │
└────────────────────────────────────────────┘
```

**Scroll:** plain `LazyColumn`. **Above the fold:** the whole profile card + the start of targets.
**Empty:** cannot be empty — onboarding guarantees a profile. Blank name renders "Welcome" (current behaviour, keep).
**Loading:** DataStore-backed, initial value present. None needed.

Note there is no Appearance row at all: with one theme there is nothing to choose.

---

### 4.4 Log picker (full page, from the FAB and from each meal's `+`)

Constraint says this survives mostly intact. Three changes only, each justified.

```
┌────────────────────────────────────────────┐
│ ←  Add to Lunch                   ⚗  ⌗     │  ⚗ = Rich in…, ⌗ = barcode
├────────────────────────────────────────────┤
│  🔍  Search 166 foods…                     │  pinned, pill, not auto-focused
├────────────────────────────────────────────┤
│ ┌──────┬──────┬──────┬──────┬──────┐       │  pinned, scrollable
│ │Recent│Freq. │ Favs │ All  │Custom│       │  ← default tab is Recent, not Search
│ └──────┴──────┴──────┴──────┴──────┘       │
│                                 12 foods   │
├────────────────────────────────────────────┤
│  ╭─╮  Masala dosa                     ☆    │
│  │🥞│  180 kcal · 1 dosa (150 g)           │
│  ╰─╯                                       │
│  ╭─╮  Sambar                          ★    │
│  │🍛│  120 kcal · 1 cup (200 g)            │
│  ╰─╯                                       │
│  ╭─╮  Filter coffee                   ☆    │
│  │🥤│  90 kcal · 1 tumbler (150 g)         │
│  ╰─╯                                       │
│       …                                    │
├────────────────────────────────────────────┤
│  Nothing matching?   [ Search online ]     │  pinned footer, only while query≠""
└────────────────────────────────────────────┘
```

**Change 1 — the "Search" tab is gone; the search field filters whichever tab is active.** Today `Search` is a tab *and* a field, and the field only affects that tab, so searching your favourites is impossible. One mental model: tab = which set, field = narrow it. Small `LogFoodViewModel` change (apply the query filter to all five list flows rather than only `resultsFlow`).

**Change 2 — default tab is Recent.** Today it's Search showing all 166 foods, which is the least likely thing you want.

**Change 3 — the "Search online" affordance becomes a pinned footer** instead of an item at the bottom of the local list, where it is only discoverable by scrolling past everything.

Everything else — `FoodRow`, the star toggle, the "Show hidden" chip on Custom, the `AddCustomFoodRow`, long-press → delete sheet, the hidden-food sheet, the per-tab scroll reset — **keep**. It works.

**Empty per tab:** keep the existing per-tab `EmptyTabHint` copy; it is well-written. Render it inside the shared `EmptyState` atom instead of a bespoke box.
**Loading:** local lists are Room flows — instant. Online search keeps its three phases (Loading/Empty/Error) in the footer area, with `Spinner` replacing the Expressive `LoadingIndicator`.

---

### 4.5 Serving sheet (modal, from Log picker / barcode / entry edit) — **rewrite**

The densest surface in the app and currently a single ~600dp scroll column with the primary action at the very bottom.

```
╭────────────────────────────────────────────╮
│                ───                         │   drag handle
│ Masala dosa                                │   pinned header
│ 185 kcal · 28 C · 4 P · 6 F                │   live, animates on every change
├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
│                                       ⇕    │   scrollable middle
│ HOW MUCH                                   │
│ ( 1 dosa·150g )( half·75g )( 100 g )( ⌨ g )│   variant pills + custom
│                                            │
│       ╭───╮                      ╭───╮     │
│       │ − │   1.0 × dosa  150 g  │ + │     │   AmountRow (tnum)
│       ╰───╯                      ╰───╯     │
│       ●━━━━━━━━━━━━━━━━━━━━━━━━━━━━        │   slider 0.25–5
│                                            │
│ HOW IT'S MADE                              │   ← ONE heading, two axes
│  Cooked in                                 │
│  (Palmolein)(Coconut)(Groundnut)(Gingelly) │
│  Added                                     │
│  ( ✓ Ghee roast )                          │
│                                            │
│ WHAT'S IN IT                               │
│  Calories  185      Carbs     28.0 g       │
│  Protein    4.1 g   Fat        6.2 g       │
│  Sat fat    3.8 g ↑ Vitamin E  1.1 mg ↓    │   ← ↑↓ flash 2s after a prep change
│  Fiber      2.1 g   Sodium   290 mg        │
│                                            │
├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
│  ( Lunch ▾ )   [     Add to diary     ]    │   pinned footer
╰────────────────────────────────────────────╯
```

Four structural changes:

1. **Header and footer pin; only the middle scrolls.** Today the Add button is below the fold on a short phone, so the user scrolls past the thing they came to tap. Pinning also keeps the live kcal readout visible while you drag the slider — right now the number you are adjusting can be off-screen.

2. **The meal picker demotes from a full-width 4-way segmented control to a dropdown chip in the footer.** It is pre-set from the route *and* the wall clock, so it is almost never changed. That reclaims ~80dp for the controls that *are* used.

3. **Oil and style merge under one "How it's made" heading.** "Cooked in" and "Made with" are two headings answering the same question, and the binary-style-as-single-toggle rule (which is good — keep the logic exactly as written in `ServingSheet.kt`) reads oddly as its own titled section. Sub-labels inside one block.

4. **Amount gets one control, not two.** Today the non-custom path is a label + slider and the custom path is a label + stepper + slider. Unify: `[−] value [+]` row above a slider in both modes, with the row's text switching between `1.0 × dosa 150 g` and `150 g`.

Plus one behavioural addition: **the preview flashes what changed.** The existing `showFatDetail` flag exists precisely because tapping "Coconut" moves nothing visible — calories and total fat are identical by design. Adding the two fat rows was the right instinct but it is passive. Instead, after any oil/style tap, the moved fields get a 2-second ↑/↓ delta badge and a color pulse. That makes the single most domain-specific feature in the app legible.

**Empty/loading:** none — the sheet always opens with a food in hand.

---

### 4.6 Entry-edit sheet (modal, from tapping a logged row) — **new**

```
╭────────────────────────────────────────────╮
│                ───                         │
│ Masala dosa                     Lunch · 1pm│
│ 185 kcal · 28 C · 4 P · 6 F                │
├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
│   …identical body to the serving sheet…    │
├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
│  ( Lunch ▾ )   [    Save changes      ]    │
│  🗑 Remove from diary                      │
╰────────────────────────────────────────────╯
```

Same component, seeded from the `FoodLogEntity` instead of defaults, footer verb changed, destructive action added as a text row. `FoodLogEntity` already stores `grams`, `servings`, `servingLabel`, `mealType`, `oil`, `style` — everything needed. See §7 for the one DAO addition.

---

### 4.7 All-nutrients sheet (modal, full-height, from Today's "See all 28")

```
╭────────────────────────────────────────────╮
│                ───                         │
│ Nutrients                       Sat 22 Nov │
│ 21 of 28 on track                          │
│ ┌──────────┬──────────┬──────────┐         │  pinned
│ │  Macros  │ Minerals │ Vitamins │         │
│ └──────────┴──────────┴──────────┘         │
├────────────────────────────────────────────┤
│ ┌──────────────────┐┌──────────────────┐   │  2-col NutrientCell grid
│ │ Carbs            ││ Protein          │   │
│ │ 212 / 236 g      ││ 74 / 118 g       │   │
│ │ ▰▰▰▰▰▰▰▰▱        ││ ▰▰▰▰▰▱▱▱▱        │   │
│ └──────────────────┘└──────────────────┘   │
│ ┌──────────────────┐┌──────────────────┐   │
│ │ Fiber            ││ Sugar      LIMIT │   │
│ │ 21 / 30 g        ││ 44 / 50 g        │   │
│ │ ▰▰▰▰▰▰▱▱▱        ││ ▰▰▰▰▰▰▰▰▏        │   │  ▏ = cap tick
│ └──────────────────┘└──────────────────┘   │
│       …14 rows total across 3 tabs…        │
╰────────────────────────────────────────────╯
```

Tap any cell → the existing `NutrientContributionSheet` content replaces the grid (push-within-sheet, back returns). **Keep that sheet's content entirely** — the ranked contributions with per-food share meters, the `Limit` badge and its "staying under this is the win" line, and the `SubBreakdown` with its honest coverage note. It is the single best-reasoned piece of UI in the app. One addition: a `[ Find foods rich in Vitamin B12 ]` button at the bottom, which navigates to Rich in… with that nutrient preselected. That turns a diagnosis into an action and gives the Rich screen the entry point it currently lacks.

---

### 4.8 Water sheet (modal, from Today's water tile or Trends' water card)

Replaces `WaterScreen` as a route.

```
╭────────────────────────────────────────────╮
│                ───                         │
│ Water                                      │
│ 1 200 ml                                   │  numberL
│ of 2 600 ml · 1 400 to go                  │
│ ▰▰▰▰▰▰▰▰▰▱▱▱▱▱▱▱▱▱▱▱                       │
├────────────────────────────────────────────┤
│ ( 100 ml )( 200 ml )( 250 ml )( 500 ml )   │
│ [            + 1 L bottle            ]     │
├────────────────────────────────────────────┤
│ TODAY                                      │
│  250 ml          8:14 AM              🗑    │
│  500 ml         11:02 AM              🗑    │
│  450 ml          1:35 PM              🗑    │
╰────────────────────────────────────────────╯
```
**Empty:** "No water logged yet 💧" (keep the copy). **Loading:** none.

### 4.9 Weigh-in sheet (modal, from Today's weight tile or Trends' weight card)

Replaces `WeightScreen` as a route; the chart and history move into the Trends weight card.

```
╭────────────────────────────────────────────╮
│                ───                         │
│ Log weigh-in                   Sat 22 Nov  │
│                                            │
│        ╭───╮                  ╭───╮        │
│        │ − │    72.4 kg       │ + │        │  0.1 steps
│        ╰───╯                  ╰───╯        │
│        ●━━━━━━━━━━━━━━━━━━━━━━━━━          │
│                                            │
│  Note (optional)                           │
│  ┌──────────────────────────────────────┐  │
│  └──────────────────────────────────────┘  │
│                                            │
│  Update profile weight                [●]  │
│  Keeps calorie & nutrient targets in sync  │
│                                            │
│  [            Save weight             ]    │
╰────────────────────────────────────────────╯
```
The free-text `OutlinedTextField(keyboardType = Decimal)` becomes a stepper+slider — you weigh within ~1kg of last time, so a keyboard is the wrong instrument. Keep the 20–400kg validation and the sync-to-profile switch (good behaviour, good explanatory line).

### 4.10 Rich in… (full page; from Log picker's ⚗ action and from a nutrient sheet)

**Keep almost entirely.** The ranking rationale, the weakest-pick explanation, the blocker-removal empty state, and the `NutrientReadout` row design are all well-judged. Two changes: it accepts a preselected nutrient from the nutrient sheet, and the picker card collapses to a one-line summary (`Vitamin B12, Calcium · Change`) once results exist, so the results aren't pushed below the fold.

```
┌────────────────────────────────────────────┐
│ ←  Rich in…                        Clear   │
├────────────────────────────────────────────┤
│ ┌────────────────────────────────────────┐ │
│ │ Vitamin B12 · Calcium        [Change]  │ │  collapsed picker
│ └────────────────────────────────────────┘ │
│ 14 foods · per serving, as a share of goal │
│ Ranked by the nutrient each food does      │
│ worst on, so nothing gets in on one        │
│ strong number.                             │
│ ┌────────────────────────────────────────┐ │
│ │ ╭─╮ Curd (homemade)              ☆     │ │
│ │ │🥛│ 98 kcal · 1 cup (200 g)           │ │
│ │ ╰─╯                                    │ │
│ │     Vitamin B12 ▰▰▰▰▰▱▱  1.4 µg   58%  │ │
│ │     Calcium     ▰▰▰▱▱▱▱  296 mg   30%  │ │
│ └────────────────────────────────────────┘ │
│       …                                    │
└────────────────────────────────────────────┘
```
**Expanded picker:** segmented Macros/Minerals/Vitamins + `FlowRow` of pills — unchanged.
**Empty (nothing picked):** keep the current `EmptyState`. **Empty (no results):** keep `NoResults` and the "Remove omega-3" action verbatim — it is the best empty state in the app.

### 4.11 Add / edit custom food (full page) — **revise**

Today: 34 `OutlinedTextField`s in one `verticalScroll`, ~2 500dp tall, AI button buried in the middle.

```
┌────────────────────────────────────────────┐
│ ←  Add custom food                         │
├────────────────────────────────────────────┤
│ ┌────────────────────────────────────────┐ │
│ │ BASICS                                 │ │
│ │ ┌───────────────────────────────┬────┐ │ │
│ │ │ Name                          │ ✨ │ │ │  AI moves here, beside Name
│ │ └───────────────────────────────┴────┘ │ │
│ │ ┌──────────────────────────────────┐   │ │
│ │ │ Brand (optional)                 │   │ │
│ │ └──────────────────────────────────┘   │ │
│ │ Category                               │ │
│ │ (South Indian)(North Indian)(Snack)    │ │  one FlowRow, not 4 ButtonGroups
│ │ (Beverage)(Dairy)(Fruit)(Vegetable)…   │ │
│ │ ┌────────────┬───────────────────────┐ │ │
│ │ │ Serving g  │ Serving label         │ │ │
│ │ └────────────┴───────────────────────┘ │ │
│ │ Values I'm entering are                │ │
│ │ ┌───────────────┬────────────────────┐ │ │
│ │ │  Per 100 g    │ Per 150 g serving  │ │ │
│ │ └───────────────┴────────────────────┘ │ │
│ └────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────┐ │
│ │ MACROS  per 100 g                   ⌄  │ │  open by default
│ │ ┌─────────────┐┌─────────────┐         │ │  2-col compact fields
│ │ │ Calories    ││ Carbs g     │         │ │
│ │ └─────────────┘└─────────────┘         │ │
│ │ ┌─────────────┐┌─────────────┐         │ │
│ │ │ Sugar g     ││ Fiber g     │         │ │
│ │ └─────────────┘└─────────────┘         │ │
│ │   …10 fields…                          │ │
│ └────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────┐ │
│ │ MINERALS  9 fields · 0 filled       ›  │ │  collapsed
│ └────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────┐ │
│ │ VITAMINS  11 fields · 0 filled      ›  │ │  collapsed
│ └────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────┐ │
│ │ OPTIONAL DETAIL  4 fields           ›  │ │  collapsed
│ └────────────────────────────────────────┘ │
├────────────────────────────────────────────┤
│ [              Save food               ]   │  pinned footer
└────────────────────────────────────────────┘
```
Two-column compact number fields halve the page height; collapsed accordions with an "n filled" counter mean the AI-autofill path (the normal path) never shows you 30 empty boxes. Keep the per-section "per 100 g / per 150 g serving" suffix — it's necessary and well done. Keep the AI instructions dialog and `AiProgressDialog` as they are.

**Empty:** it *is* the empty state. **Loading:** `AiProgressDialog` already covers the only async path. Keep.

### 4.12 Barcode scanner (full page) — **revise**

Keep the four phases (`Scanning` / `Resolved` / `Cached` / `NotFound`) and the permission prompt. **Delete the bespoke `ServingEditor` + "Add to:" meal chips + "Add 180 g to Lunch" button (~200 lines) and present the resolved product in the standard serving sheet instead.** Logging a scanned packet should be the same three gestures as logging a dosa.

```
┌────────────────────────────────────────────┐
│ ←  Scan barcode                            │
├────────────────────────────────────────────┤
│                                            │
│         ┌─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─┐            │
│         ╷                     ╷            │  camera preview full-bleed
│                                            │  corner brackets only
│         ╵                     ╵            │
│         └─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─┘            │
│          Point at a barcode                │
│                                            │
├────────────────────────────────────────────┤
│  ⌨  Enter code manually                    │  new, small
└────────────────────────────────────────────┘
      ↓ on resolve → ServingSheet (§4.5)
      ↓ on not-found → sheet: "Not found" +
        [ Add manually with this barcode ]
        [ Scan another ]
```
**Loading:** a 2-line lookup state in a small bottom bar rather than `CenterLoading` over the whole preview, so the camera keeps running.

### 4.13 Reminders (full page) — **keep, restyle only**

```
┌────────────────────────────────────────────┐
│ ←  Reminders                               │
├────────────────────────────────────────────┤
│ [NotificationsBlockedCard, if blocked]     │
│ [BatteryOptCard, if not exempt]            │
│ ┌────────────────────────────────────────┐ │
│ │ DAILY REMINDERS                        │ │
│ │ 🌞 Breakfast          8:00 AM     [●]  │ │
│ │ 🍛 Lunch              1:00 PM     [●]  │ │
│ │ 🍪 Snack              5:00 PM     [ ]  │ │
│ │ 🌙 Dinner             8:30 PM     [●]  │ │
│ │ 💧 Water             every 2 h    [●]  │ │
│ │ ⚖ Weigh-in       Mon 7:30 AM      [●]  │ │
│ └────────────────────────────────────────┘ │
└────────────────────────────────────────────┘
```
The structure is already right. `RemindersSection` → `AppCard`, `BatteryOptOutlinedCard` → the shared `BatteryOptCard`. No behaviour change.

### 4.14 Privacy (full page) — **keep verbatim**

Prose page, three network callouts, no interaction. Restyle to `AppCard` + the new type scale; touch nothing else. The copy is the product.

### 4.15 Data & backup (full page) — **new, merges three Settings sections**

```
┌────────────────────────────────────────────┐
│ ←  Data & backup                           │
├────────────────────────────────────────────┤
│ ┌────────────────────────────────────────┐ │
│ │ AUTOMATIC BACKUP                  [●]  │ │
│ │ Folder  Documents/FoodTracker   Change │ │
│ │ Last run  today 3:04 AM · 412 KB       │ │
│ │ ───────────────────────────────────    │ │
│ │ 2026-11-22  412 KB          [Restore]  │ │
│ │ 2026-11-21  409 KB          [Restore]  │ │
│ │ 2026-11-20  407 KB          [Restore]  │ │
│ └────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────┐ │
│ │ MANUAL SNAPSHOT                        │ │
│ │ [   Export   ] [   Import   ]          │ │
│ └────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────┐ │
│ │ EXPORT LOGS FOR AI                     │ │
│ │ ( 7 d )( 30 d )( 90 d )( All )         │ │
│ │ [     Export logs (30 days)      ]     │ │
│ └────────────────────────────────────────┘ │
└────────────────────────────────────────────┘
```
Confirm dialogs for restore/import: **keep as-is**, they are correctly worded and correctly scary.

### 4.16 Edit profile & goals (full page) — **revise**

Replace the top-bar edit/cancel/save tri-state (`ProfileScreen` toggles `state.editing`) with a dedicated page that is always editing, with a pinned Save.

```
┌────────────────────────────────────────────┐
│ ←  Edit profile & goals         [ Save ]   │
├────────────────────────────────────────────┤
│ ┌────────────────────────────────────────┐ │
│ │ ABOUT YOU                              │ │
│ │ ┌──────────────────────────────────┐   │ │
│ │ │ Name                             │   │ │
│ │ └──────────────────────────────────┘   │ │
│ │ Sex   ┌──────────┬──────────┐          │ │
│ │       │   Male   │  Female  │          │ │
│ │       └──────────┴──────────┘          │ │
│ │ Age       [−]  34 years  [+]           │ │
│ │           ●━━━━━━━━━━━━━━━━            │ │
│ │ Height    [−]  176 cm    [+]           │ │
│ │           ●━━━━━━━━━━━━━━━━            │ │
│ │ Weight    [−]  72.4 kg   [+]           │ │
│ │           ●━━━━━━━━━━━━━━━━            │ │
│ └────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────┐ │
│ │ ACTIVITY                               │ │
│ │ (Sedentary)(Light)(Moderate)(Active)…  │ │  FlowRow, was a vertical chip list
│ │ Training type              Both        │ │
│ │ Gym ●━━━━━━━━━━━━━━━ Running           │ │
│ │ Lifting raises protein more than cardio│ │
│ └────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────┐ │
│ │ GOAL                                   │ │
│ │ ┌────────┬──────────┬────────┐         │ │
│ │ │  Lose  │ Maintain │  Gain  │         │ │
│ │ └────────┴──────────┴────────┘         │ │
│ │ Rate  (Mild)(Standard)(Aggr.)(Very)    │ │
│ │ Goal weight  [−]  68.0 kg  [+]         │ │
│ │              ●━━━━━━━━━━━━━━            │ │
│ │                                        │ │
│ │  15   18.5    25     30      40        │ │
│ │  ▓▓▓▓▓▓▓▒▒▒▒▒▒▒▒▓▓▓▓▓▓▒▒▒▒▒▒▒▒▒        │ │  BmiBar
│ │         ▲Goal    ▲Now                  │ │
│ └────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────┐ │
│ │ THESE TARGETS                   LIVE   │ │  recomputes as you drag
│ │  Calories 2 100 → 2 040 kcal           │ │
│ │  Protein    118 →   124 g              │ │
│ │  ─────── See all 31 targets ───────    │ │
│ └────────────────────────────────────────┘ │
└────────────────────────────────────────────┘
```
The live-targets preview is the one addition: today you change activity level and have to save, back out, and find the Daily targets section to see what moved. `GoalCalculator.goalsFor(draft)` is pure, so this is free.

### 4.17 Onboarding — **revise, 5 pages → 3**

```
 ●○○                          ●●○                      ●●●
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ About you    │    │ Your goal    │    │ Optional     │
│              │    │              │    │              │
│ Name         │    │ Lose/Maint/  │    │ ☁ Backups  › │
│ Sex          │    │     Gain     │    │ 🔔 Reminders›│
│ Age   ●━━━   │    │ Rate         │    │ ✨ AI       ›│
│ Height●━━━   │    │ Goal weight  │    │              │
│ Weight●━━━   │    │ ▓▓▒▒▓▓ BMI   │    │ Skip any of  │
│ Activity     │    │              │    │ these; all   │
│              │    │ YOUR TARGETS │    │ are in       │
│              │    │ 2 100 kcal   │    │ Settings.    │
│              │    │ 118 g protein│    │              │
│              │    │ 2 600 ml     │    │ 🔒 Nothing   │
│              │    │ +28 more     │    │ leaves this  │
│              │    │              │    │ phone.       │
│  [  Next  ]  │    │ [  Next  ]   │    │ [ Start  ]   │
└──────────────┘    └──────────────┘    └──────────────┘
```
Pages 3/4/5 (Backups, Reminders, AI) are all optional and all duplicated in Settings. Collapsing them into three expandable rows on one page cuts two full swipes out of first-run without removing anything. Keep the pager, the dots, the privacy tagline, and the "Optional" badge idiom.

---

## 5. Navigation & surface model

### Destinations: 11 in-app + onboarding (was 13 flat, all pages, no bar)

**Bottom bar — 3 items, `NavigationBar`, labels always shown, no badge.** Height 80dp, `background` container, hairline top border, selected = `primary` icon + label with a `primaryContainer` pill behind the icon.

| | Destination | Holds |
|---|---|---|
| ◉ | **Today** | the diary — the 90% screen |
| ◻ | **Trends** | all history: calories, macros, nutrient consistency, weight, water |
| ◻ | **You** | profile, goals, and every app setting |

Why three and not four: logging is an *action*, not a place, so it is the FAB. Why a bar at all: four of the current app's screens (Stats, Rich in…, Profile, Settings) are behind a three-dot overflow in the greeting row, which is where features go to be forgotten.

**Center/end FAB — `+ Log`** on Today only (Trends and You have no logging context). Bottom-end, not centre-docked — one-handed reach on a 412dp-wide phone beats symmetry.

### Every secondary surface, with the call

| Surface | Decision | Why |
|---|---|---|
| Log picker | **full page** | Search + 5 tabs + a long list + its own top-bar actions. Needs a back stack entry of its own. |
| Serving sheet | **bottom sheet** (keep) | A confirmation step on top of a list you came from; a page would lose the list. |
| Entry edit | **bottom sheet** | Same shape as above, same component. |
| Nutrient detail | **bottom sheet** (keep) | Transient detail on one row of a list. |
| All-nutrients | **full-height bottom sheet** | Long scroll, but it's a drill-down from one card, not a place you navigate to. A page would want a bar slot it doesn't deserve. |
| Water | **bottom sheet** (was a page) | Four buttons and a short list. A whole route for 4 buttons is over-provisioned. |
| Weigh-in | **bottom sheet** (was a page) | One number and a toggle. The chart/history half goes to Trends, where it belongs beside the other charts. |
| Rich in… | **full page** (was page, promoted) | Multi-select state + a ranked list. Stays a page; gains two real entry points instead of an overflow item. |
| Add/edit custom food | **full page** (keep) | 34 fields and an async AI call. |
| Barcode | **full page** (keep) | Full-bleed camera. |
| Edit profile & goals | **full page** (was an inline edit mode) | ~12 controls; an inline mode on the view page means two layouts in one file and an awkward top-bar tri-state. |
| Reminders | **full page** (keep) | Six rows + two system-permission cards. |
| Privacy | **full page** (keep) | Long prose. |
| Data & backup | **full page** (merged from 3 Settings sections) | File pickers, a backup list, destructive restores. |
| AI autofill | **bottom sheet** | A toggle, a provider picker, a key field. |
| Custom foods | **bottom sheet** | A count + one destructive action. |
| All targets | **bottom sheet** | A read-only 31-row list. |
| Restore / import / delete-all confirms | **dialog** (keep) | Destructive, needs a hard modal stop. |
| Time picker | **dialog** (keep) | Material convention. |

### Back-stack behaviour

- **Bar destinations are peers.** Switching uses `popUpTo(graph.startDestinationId) { saveState = true }` + `launchSingleTop` + `restoreState`. So Today→Trends→You→back lands on **Today**, not Trends. Each tab keeps its own scroll position.
- **System back on Today** exits the app. On Trends or You it returns to Today (standard Android, and the reason for the `popUpTo` above).
- **Leaf pages push onto the current tab's stack.** Log picker from Today's FAB → back returns to Today, scrolled where you left it.
- **Log → serving sheet → confirm** pops the Log page entirely and returns to Today. Keep today's `vm.confirmLog(onDone = onBack)`.
- **Barcode → not found → Add manually** keeps `popUpTo(BarcodeRoute) { inclusive = true }` so back from the form doesn't re-open the camera. Correct today; keep.
- **Sheets are not back-stack entries.** Back dismisses the sheet only, via `ModalBottomSheet`'s own handling.
- **Onboarding** stays a separate start destination; completing it does `popUpTo(OnboardingRoute) { inclusive = true }`. Keep.
- **Deep links** (`DeepLink.LogMeal` / `OpenWater` / `OpenWeight`) keep their `launchSingleTop`. `OpenWater`/`OpenWeight` now resolve to *Today + open the relevant sheet* rather than navigating to a deleted route. The `LaunchedEffect` gating in `AppNav.kt` — awaiting `onboarded == true` then `currentBackStackEntryFlow.first()` — is subtle and correct; **keep it verbatim**, it is the cold-start-from-notification fix.
- **Nav transitions:** keep the 300ms slide + `predictivePop*` pair exactly. Add only the fade-through for bar switches.

---

## 6. Behaviour specs

### 6.1 Logging a food

| Step | Action | Feedback |
|---|---|---|
| 1 | Tap FAB `+ Log` on Today (or a meal row's `+`) | Push transition; title reads **Add to Lunch** (slot from the `+` tapped, else wall clock) |
| 2 | Land on **Recent** tab | List renders instantly from Room; row count caption `12 foods` |
| 3 | (optional) Type in search | Filters the *active* tab live; count caption updates; if 0 local matches and query ≠ "", the footer bar appears |
| 4 | (optional) Tap **Search online** | Footer shows `Spinner` + "Searching food databases…"; results append above the footer with a source tag; error → inline red line + retry |
| 5 | Tap a food row | Light haptic; serving sheet rises. Seeded with variant[0], servings 1.0, the food's remembered oil/style, the route's meal |
| 6 | Pick a variant pill | Light haptic; pill fills; servings reset to 1.0; the **pinned header number re-animates** over 450ms |
| 7 | Drag the slider / tap ± | Haptic tick per 0.25 bucket (existing `HapticSlider` behaviour); amount row and header both update live |
| 8 | Tap an oil or style chip | Light haptic; chip fills; the 4–6 affected fields in the preview pulse and show a ↑/↓ delta badge for 2s; the pick is persisted immediately via `rememberVariant` (existing behaviour — keep) |
| 9 | (optional) Tap the meal chip in the footer | Small dropdown, 4 items |
| 10 | Tap **Add to diary** | Confirm haptic; sheet dismisses; Log page pops; Today appears with the new row **highlighted for 1.2s** (primaryContainer fade-out) and the ring + nutrient strip re-animating to their new values |

Step 10's highlight is the one addition: today the entry just appears and the user has to find it.

### 6.2 Editing a logged entry — **new**

| Step | Action | Feedback |
|---|---|---|
| 1 | Tap an entry row inside a meal group | Light haptic; the serving sheet rises, seeded from the `FoodLogEntity` (its grams, servings, label, oil, style, meal) |
| 2 | Header shows `Masala dosa` + `Lunch · 1:05 PM` | — |
| 3 | Change anything | Same live feedback as logging |
| 4 | Tap **Save changes** | Confirm haptic; `repository.updateFoodLog(entry)`; sheet dismisses; the row re-renders in place with a 1.2s highlight; ring and strip re-animate |
| 5 | Or tap **Remove from diary** | Sheet dismisses; row animates out; **snackbar "Removed · UNDO"** for 5s |

Separately, **swipe-left on an entry row** deletes with the same undo snackbar, replacing the four always-visible trash icons.

### 6.3 Changing a serving / preparation variant

Three axes in one sheet, each with its own feedback shape:

- **Serving size** — pill row, horizontally scrollable, last pill is `⌨ g`. Tapping `⌨ g` seeds custom mode from the *current* gram total (keep this; `ServingSheet.kt` already does it and it's the right call). The amount row's text switches form. **Feedback:** header kcal re-animates.
- **Servings** — ± steps by 0.25, slider 0.25–5 with a haptic tick per bucket. In custom mode ± steps by 5g, slider 5–500. **Feedback:** amount row + header, both tnum so digits don't jitter.
- **Preparation** — under one heading, two labelled sub-rows. The binary-style rule stays exactly as implemented (`plain` + exactly one addition → a single checkable toggle; two or more additions → the full exclusive set including Plain). Oil options stay restricted by `CookingOil.optionsFor(food.defaultOil)` with the food's own default leading. **Feedback:** the delta flash described above — this is the whole point, since calories and total fat are identical across oils by design and without it the control looks broken.

The pick persists on tap, not on confirm (existing `rememberVariant` behaviour on both the Home and Log paths). Keep.

### 6.4 Reviewing a day

| Step | Action | Feedback |
|---|---|---|
| 1 | Open Today | Everything renders at once from the Room flows; ring animates 0→value over 700ms on first composition only |
| 2 | Tap a past day in the DayStrip | Light haptic; cell fills; **every number on the page cross-animates** (the ring, the macro meters, the meal totals, the nutrient strip). The strip scrolls to keep the selection centred. |
| 3 | Read the nutrient strip | 28 cells. Any `unmet` or `over` cell is visually loud against a field of `met`. |
| 4 | Tap a cell, or a "short on" row | All-nutrients sheet opens scrolled to that nutrient, with its detail expanded |
| 5 | Read the contribution list | Ranked foods with share meters; the `Limit` badge and "staying under this is the win" line for capped fields; the sub-breakdown with its coverage caveat |
| 6 | Tap **Find foods rich in Calcium** | Sheet dismisses; Rich in… pushes with calcium preselected and ranked |
| 7 | Back | Returns to Today, same day still selected, same scroll position |

### 6.5 Setting goals

| Step | Action | Feedback |
|---|---|---|
| 1 | You → **Edit profile & goals** | Push |
| 2 | Drag any slider (age/height/weight/activity/goal/rate) | Haptic tick per unit; the **THESE TARGETS** card at the bottom recomputes live and shows `old → new` for the two or three figures that moved |
| 3 | Watch BmiBar | The `Now` and `Goal` markers slide as weight/goal-weight change |
| 4 | Tap **Save** | Confirm haptic; pop to You; profile card and goal meter re-render; a snackbar confirms `Targets updated` |
| 5 | Back on Today | Every goal-relative meter and the ring re-animate to the new denominators |

Weight changed via the weigh-in sheet with **Update profile weight** on takes the same path — targets recompute silently and the Today ring shifts. (Current behaviour; keep, and keep the explanatory subtitle.)

---

## 7. Migration plan

Eight phases. Each is independently shippable — the app builds, runs, and is usable at every phase boundary.

### Phase 1 — Theme foundation *(unblocks everything; disruptive in breadth, shallow in depth)*

Remove Expressive and dynamic color; install the new tokens.

- `theme/Color.kt` — rewritten as ONE dark scheme
- `theme/Theme.kt` — `MaterialExpressiveTheme` → `MaterialTheme`; delete `withAmoledSurfaces()`, `dynamicLight/DarkColorScheme`, the `useDynamic` branch, the `supportsDynamic` check, AND the `isSystemInDarkTheme()` / light-scheme branch. `AppTheme` takes no mode argument.
- `theme/ThemeMode.kt` — **delete the file.** `ColorPalette` and `ThemeMode` both go.
- `ProfileStore.kt` — stop reading the theme-mode key (leave the key; no migration)
- `MainActivity.kt` — pin `isAppearanceLightStatusBars = false`; the app no longer follows the system setting
- `theme/Type.kt` — new scale with `tnum`
- `theme/Dimens.kt` → `theme/Tokens.kt` — spacing + radii + the three remaining heights
- `components/ExclusiveButtonGroup.kt` — rewrite off `ButtonGroup`/`toggleableItem`
- `screens/log/LogFoodScreen.kt` — Expressive `LoadingIndicator` → `CircularProgressIndicator`
- `screens/settings/SettingsScreen.kt` + `SettingsViewModel.kt` — remove the whole Appearance section: the palette picker AND the Light/Dark/System rows
- `screens/home/HomeScreen.kt`, `screens/addfood/*` — drop `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`
- `components/MacroDonut.kt`, `WeeklyBarChart.kt`, `FoodRow.kt`, `BmiBar.kt` — de-hardcode the colors listed in §0
- `README.md` — the Stack line
- `app/build.gradle.kts` — drop any expressive opt-in compiler args

Result: same app, one dark skin, every hard constraint satisfied. **Phase 1 shrinks** — one
palette to author, one to verify, and every later phase gets checked once instead of twice.

### Phase 2 — Atom layer *(pure refactor, zero visual intent change, large line reduction)*

Add `components/atoms/`; migrate call sites.
- New: `AppCard`, `Meter`, `Ring`, `ValueText`, `SectionHeader`, `IconCircle`, `Divider`, `AppButton`, `AppTopBar`, `Spinner`, `StatusCell`
- Delete `SectionCard.kt`, `FormSection.kt` and the private clones in `HomeScreen.kt`, `WaterScreen.kt`, `WeightScreen.kt`, `ProgressScreen.kt`
- Collapse the three meter implementations in `NutrientBar.kt`, `HomeScreen.ContributionRow`, `RichFoodsScreen.NutrientReadout`
- Collapse the four icon-disc implementations

Touches most files shallowly. Expect ~600–900 net lines removed.

### Phase 3 — Remove quick-log *(approved; pure deletion)*

**Delete:**
- `widget/QuickLogWidget.kt` (421 lines) — `QuickLogWidget`, `QuickLogWidgetReceiver`, `QuickLogFeedback`, `QuickLogActionCallback`, `RecentFoodsGrid`, `MealPill`, `RecentFoodChip`, `PlusButton`
- `res/xml/quick_log_widget_info.xml` and the `<receiver>` block in `AndroidManifest.xml`
- `HomeScreen.QuickLogSection` (~70 lines) and the six `quickLog*` state variables plus the duplicate `ModalBottomSheet`/`ServingSheet` host at the bottom of `HomeScreen` (~60 lines)
- `HomeViewModel.quickLog(...)` and — once the widget is gone — `rememberVariant` on the Home path
- `HomeUiState.favourites` / `.recent` and the `observeFavouriteFoods` + `observeRecentFoods` legs of `quickFlow` (`latestWeight` stays for the stat tile)

**What this frees, concretely:**
- ~550 lines, one of three widgets, and one `WidgetUpdater` refresh path
- A second, divergent serving-sheet host (Home's copy and Log's copy could drift)
- **The one-tap-no-confirmation write path.** `QuickLogActionCallback` writes a log row straight from a widget tap with no serving, no oil, no meal confirmation and no undo. With it gone, *every* write to the diary goes through the serving sheet — which is what makes the preparation-variant model trustworthy.
- A chunk of `HomeUiState` and two Room flows per Home recomposition
- ~180dp of vertical space on Today, which the nutrient card now occupies

`GoalLeftWidget` and `MacrosWidget` are untouched and keep working.

### Phase 4 — Navigation & surface model *(the most disruptive phase)*

- `navigation/Routes.kt` — delete `WaterRoute`, `WeightRoute`; add `TrendsRoute`, `YouRoute`, `EditProfileRoute`, `DataBackupRoute`; `RichFoodsRoute` gains an optional `nutrientKey` arg
- `navigation/AppNav.kt` — `Scaffold` with `NavigationBar` wrapping the `NavHost`; `saveState`/`restoreState` wiring; deep-link targets for water/weight become Today + a sheet flag
- `MainActivity.kt` — `DeepLink` handling unchanged in shape
- `screens/water/WaterScreen.kt` → `components/sheets/WaterSheet.kt`
- `screens/weight/WeightScreen.kt` → `components/sheets/WeighInSheet.kt`; its chart + history move to Trends
- `screens/progress/` → `screens/trends/`
- `screens/settings/SettingsScreen.kt` + `screens/profile/ProfileScreen.kt` → `screens/you/YouScreen.kt` + `screens/you/EditProfileScreen.kt` + `screens/you/DataBackupScreen.kt` + three sheets
- `screens/home/HomeScreen.kt` — delete `GreetingHeader` and `OverflowMenu`

ViewModels mostly survive: `WaterViewModel` and `WeightViewModel` move packages unchanged; `ProgressViewModel` gains the nutrient-consistency query.

### Phase 5 — Today rewrite

`screens/home/HomeScreen.kt` (currently 1 171 lines) splits into `TodayScreen.kt` + `components/DayStrip.kt`, `CalorieHero.kt`, `MealGroup.kt`, `NutrientSummarySection.kt`. `HomeViewModel` unchanged apart from the phase-3 deletions.

### Phase 6 — Nutrient system

- `data/Nutrients.kt` — add `enum NutrientStatus` and `NutrientField.statusFor(consumed, goal)`. Pure addition, no data change.
- `components/NutrientVisuals.kt` (was `NutrientColor.kt`) — add status→color
- New: `NutrientStatusStrip`, `NutrientCell`, `AllNutrientsSheet`, `NutrientHeatmap`
- `NutrientBar` → `NutrientRow`, limit-aware
- `ProgressViewModel`/`TrendsViewModel` — a range query that returns per-day `Nutrients` (`repository.observeLogForRange` already exists; just fold per day)
- `NutrientContributionSheet` — add the "Find foods rich in…" CTA

### Phase 7 — Serving sheet + entry editing

- `components/ServingSheet.kt` → `components/serving/` (`ServingSheet`, `AmountRow`, `PreparationBlock`, `LivePreview`, `SheetFooter`)
- **`db/FoodDao.kt` — add `@Update suspend fun update(entry: FoodLogEntity)`** to `FoodLogDao`. This is the **only data-layer change in the whole plan, and it is not a schema change** — `FoodLogEntity` already has every column an edit needs (`grams`, `servings`, `servingLabel`, `mealType`, `oil`, `style`, `dateEpochDay`). No Room version bump, no migration.
- `FoodRepository.kt` — `suspend fun updateFoodLog(entry: FoodLogEntity)`
- `HomeViewModel` — `editLog(...)`; swipe-delete with undo (hold the deleted entity in the VM for 5s)
- `screens/log/LogFoodViewModel.kt` — the search-filters-all-tabs change and the default-tab change

### Phase 8 — Secondary screens

`AddFoodScreen` accordion + 2-column fields · `BarcodeScreen` → serving sheet (deletes `ServingEditor`, `ResolvedPanel`'s and `CachedPanel`'s meal pickers) · `RichFoodsScreen` collapsed picker + preselect arg · `OnboardingScreen` 5→3 pages · `RemindersScreen`/`PrivacyScreen` restyle only.

### Data & schema summary

| Change | Type | Migration? |
|---|---|---|
| `FoodLogDao.update()` | DAO method on an existing table | **No** |
| `ColorPalette` DataStore key | stops being read | **No** — leave the key, ignore it |
| Everything else | — | None |

There are no Room schema changes, no seed changes, and no backup-format changes in this plan. `BackupPayload`/`AnalysisExport` are untouched.

---

## 8. Out of scope / deliberately kept

- **Room schema, `IndianFoodSeed` (1 456 lines), the oil and style models.** The domain modelling in `CookingOil.kt` and `FoodStyle.kt` is the best thing in this codebase. The UI gets rebuilt around it; it does not move.
- **`GoalCalculator`, `NutrientRanking`, `Bmi`.** Pure functions, correct, well-documented.
- **Network clients** — `AiNutritionApi`, `UsdaFoodApi`, `OpenFoodFactsApi`, and the three-network-calls privacy story they back.
- **Backup / export / import format** — `BackupManager`, `BackupPayload`, `AnalysisExport`, `BackupWorker`, `BackupScheduler`. Only the screen around them changes.
- **Reminder scheduling** — `ReminderScheduler`, `ReminderReceiver`, `BatteryOptHelper`, `ReminderStore`. Only the screen's styling changes.
- **`GoalLeftWidget` and `MacrosWidget`** plus `WidgetColors`/`WidgetData`/`WidgetUpdater`. Only `QuickLogWidget` goes. (Whether the two survivors get restyled to the new palette is open question 1.)
- **Barcode scanning pipeline** — CameraX + ML Kit, `BarcodeScannerView`. Only the panels on top change.
- **Nav transitions and the predictive-back wiring in `AppNav.kt`.** Correct, hard-won, and the comment explaining `predictivePop*` is worth keeping verbatim.
- **The deep-link gating `LaunchedEffect`.** Subtle, correct, documented. Do not touch.
- **`Haptics.kt` and the haptic-on-tick pattern.** The app feels good; that's why.
- **Privacy screen copy** and the privacy tagline on onboarding.
- **Per-category food emoji.** Scanning 166 foods, they work. Only the pastel discs go.
- **Light / Dark / System switching.** Kept, minus the Dynamic option.
- **`NutrientSubRows` and its coverage caveat**, and the `Limit`-field copy in the contribution sheet. Both are examples of the app being honest about its own data, which is rarer than it should be.
- **No tablet, no landscape, no Wear, no accessibility-scaling audit** beyond "the layouts must not break at 1.3× font scale."
- **No new nutrients, no new foods, no barcode-database changes, no AI prompt changes.**

---

## 9. Open questions — RESOLVED 2026-11-22

All ten settled. Decisions are binding on the phases named.

| # | Question | Decision | Reasoning |
|---|---|---|---|
| 1 | Restyle the two surviving widgets? | **Yes, in phase 1** | A token swap in `WidgetColors.kt`. Two visual languages side by side on the home screen is worse than the hour it costs. |
| 2 | Recent vs Frequent as separate tabs? | **Merge into one list** | On a repetitive diet the two lists are ~80% identical, and the search field now filters the active tab, so staples stay one keystroke away. Four tabs, not five. |
| 3 | Bottom bar at all? | **Yes — 3-tab: Today / Trends / You** | Four features currently hide behind a three-dot overflow. The 80dp is worth making Trends and You first-class. Confirms phases 4 and 5 as written. |
| 4 | "On track" threshold | **≥80% of RDA, no time weighting** | RDAs sit ~2 SD above average requirement to cover 97.5% of people; the EAR is near 80%. So 80% is genuinely adequate and the strip stops crying wolf over trivial shortfalls. No time weighting — the strip should mean the same thing whenever you look at it. |
| 5 | Do `Limit` fields count in the tally? | **Yes — under cap is a pass** | Staying under sodium genuinely is on track, and the denominator then matches the 28 cells in the strip directly above it. |
| 6 | Weight: sheet or full page? | **Split as planned** | Chart + history into Trends beside the other charts; a weigh-in sheet for entry. Consistent with water, and keeps all history in one place. |
| 7 | Bundle Inter for tabular figures? | **No** | The repo just shipped "Trim the APK"; ~300KB undoes part of it. System default + `tnum`, verified on the owner's own device. |
| 8 | Demote the serving-sheet meal picker? | **Yes — footer dropdown chip** | Pre-set correctly from route + wall clock nearly always. Reclaims ~80dp for the controls that are actually used. |
| 9 | Accent colour | **Deep green `#1F6F5C` / `#63D2B0`** | Keeps the green family but drops the cyan cast, so it sits warm on `#FBF7F0`. Terracotta was rejected: it collides with the `over` limit hue, which also has to be orange-red. |
| 10 | Swipe-to-delete on diary rows? | **Yes, with undo snackbar** | Discoverability was the only argument against, and the owner wrote the app. Removes four always-live trash icons per meal. |

| 11 | Light mode at all? | **No — dark only** | One theme, `#000000`. Removes the dual-palette compromise that was forcing every colour choice. `ThemeMode.kt` deleted, Appearance sheet with it. Tokens stay semantic so a light scheme is a second value table later, not a refactor. |

### Consequent edits to the sections above

- **§1** — the mint-teal `primary` is replaced by deep green throughout; the status palette is unchanged.
- **§3 / §4.1 / §4.7** — `NutrientField.statusFor()` uses a **0.80** goal fraction for `met`, and `Limit` fields resolve to `met` while under cap, so both feed the "n of 28" count.
- **§4.4** — the Log picker is now **four** tabs: Often / Favs / All / Custom. `LogFoodViewModel` merges the recent and frequent flows.
- **§7 phase 1** — also restyle `WidgetColors.kt` for `GoalLeftWidget` and `MacrosWidget`.
- **§7 phase 8** — no bundled-font work.

