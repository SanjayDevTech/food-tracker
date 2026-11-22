# UI house rules — the editorial pass

Written after the phase 1–8 rewrite shipped and still read as the old app. The
plan's §1 described a look the old app *already had* (black canvas, hairline
outlined cards, mint accent), so following it faithfully produced a tidier
version of the same thing. These rules are the difference, derived from
reference shot 4 — the one §0 actually chose.

Today (`ui/screens/today/`) is the reference implementation. Match it.

## 1. A standing head with a rule under it — not a box, not a gap

A section is an uppercase head, a hairline **under the head**, then its content.
**Not** an `AppCard`: a border drawn around a list that already has rules
between its rows is a second boundary doing the first one's job, and a stack of
full-width rounded boxes is the Direction A anatomy this pass exists to remove.

Equally **not** whitespace alone. Big gaps are the weakest separator and the
most expensive: 32dp between sections cost a third of a screen and still read as
one undifferentiated column, because nothing told the eye where a section
*ended*. The rule does it in 1dp.

- Use `ui/screens/today/components/Section.kt` — `Section(title, trailing) { … }`
  It renders: head (+ right-aligned summary figure) · 8dp · full-bleed rule ·
  16dp · content.
- `Arrangement.spacedBy(Spacing.xxl)` between sections — 24dp, **down** from 32,
  because the rule now does the separating.
- The rule belongs to the heading, not to a container around the content, so
  sections stay open to the page and content keeps bleeding to the 16dp gutter.
- Every section states its total in the same place: the `trailing` slot on the
  head's baseline — `2 981 kcal`, `18 of 29 on track`.

`AppCard` survives only where something must be visually separated from an
adjacent *different* thing. If in doubt, don't use it.

## 2. The number is the loudest thing on screen

- Values: `AppText.numberL` (28) in rows, `numberXXL` (64) for a screen's one
  hero figure. `numberM` (18) is for dense grids only.
- Labels: `AppText.eyebrow` — 11sp, **uppercase**, 1.2sp tracking. Not `body`.
- Units and captions: `bodyS` in `onSurfaceVariant`, trailing the number.
- Row rhythm is **label left, number right**, every time. One rhythm down the
  page beats three competing ones.

## 3. The accent is for data, not chrome

`primary` means "on track". It is not a decoration.

- Allowed: on-track meters and status cells, the primary action (FAB / primary
  button), the selected bottom-bar item, text links.
- Not allowed: selected pills and chips as filled accent blocks, tinted "now"
  rows, accent-filled day cells. Use a 2dp rule or an accent-coloured *label*
  instead of a filled shape.
- Over-limit is `StatusOver` / `error`. Nothing else is coloured.

## 4. No emoji

§0 adopted Direction B, whose reference shots carry none. Per-category food
emoji in the picker are the one exception the plan kept (§8) — they earn their
place scanning 166 foods. Everywhere else — meal rows, stat rows, settings rows,
section headers — they are decoration in the friendly register the plan refused.

## 5. Spacing

`4 · 8 · 12 · 16 · 20 · 24 · 32`, via `Spacing.*`. Nothing else. The first pass
leaked ~40 raw values — `6.dp` fourteen times, `2.dp` seven — straight out of
the old screens, and that stale rhythm is a large part of why the rewrite felt
unchanged. Raw `.dp` is for sizes (icon diameters, chart heights), never gaps.

## 6. Geometry

`Radius.sm` (8) and `Radius.md` (14) for anything that still needs a corner;
`Radius.lg` (20) only for a genuine card; `Radius.pill` only for the FAB and
primary buttons. Zero elevation anywhere, including focused and hovered states.

## 7. A control's size token is a minimum, never a height

`heightIn(min = Sizes.button)`, not `height(Sizes.button)`. The system font
scale runs from 0.85 to 2.0 on a real Android device, and `AppText.label` is
13sp on an 18sp line — a 36sp line box at the top of that range, which a 48dp
box cannot hold. A fixed height does not shrink the text, it crops it.

The same rule covers horizontal space: a label that cannot fit its slot is
rendered wrong, not small. Compose clips rather than ellipsises by default, so
the water sheet's four quick-add chips read "10 · 20 · 25 · 50" at 2.0 — right
through the test, since nothing throws and nothing logs. Where a row of equal
columns can run out of room, measure the widest label at the live scale and let
it break (`QuickAddRow` in `WaterSheet.kt`) rather than hard-coding a font-scale
threshold.

Sheets are the place this shows up first, because a sheet is sized by its own
content: pass `skipPartiallyExpanded = true` so one taller than half the screen
cannot open at the half-screen anchor with its own footer hanging off the bottom
edge.
