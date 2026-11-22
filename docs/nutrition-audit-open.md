# Nutrition seed audit — status

Audited 2026-11-22 against `IndianFoodSeed.kt`. Four passes: my own structural checks
(invariants, Atwater energy, oil floors, oil-swap physics, family outliers, USDA calibration of
raw foods), plus three independent agents — micronutrients, recipe reconstruction, and
raw-vs-cooked basis.

| Tier | What it means | Status |
|---|---|---|
| 1 | Self-contradictions provable from the file alone | **Done** — 18 foods, `ed2b4e6`, `SEED_VERSION` 20 |
| 2 | Real errors needing an external reference or a recipe assumption | **Done** — 15 foods, `SEED_VERSION` 21 |
| 3 | Low confidence, or near the "matters at serving size" bar | **Done** — 9 foods + 30 magnesium omissions, `SEED_VERSION` 21 |

Values below are per 100 g unless stated. "Per serving" uses the entry's own `servingGrams`.

---

## Still open

Three things survived. None is a number I could derive; each needs a decision.

### 1. "Drumstick (Moringa)" names two different foods — needs a migration, not an edit

Vitamin C is fixed (141 → 41; 141 was the raw-pod figure and boiling destroys ~70%). The naming
problem is not.

"Moringa" reads to most people as *murungai keerai*, the **leaves**, which are a different food:
64 kcal, 9.4 g protein, 378 µg RAE vitamin A, 185 mg calcium, against the pods' 37 / 2.1 / 4 / 30.
Someone logging leaves against this entry is off by −27 kcal, −7.3 g protein, −374 µg vitamin A
per serving.

**Why I did not just rename it.** `FoodRepository.refreshSeedIfStale` matches seeded rows by
lowercased `name`. Rename the seed entry and the old row matches nothing, so it is left behind
untouched while the renamed one is inserted as a brand-new food — a visible duplicate, with the
user's favourite/hidden/last-oil state stranded on the stale copy. Splitting pods and leaves into
two entries has the same shape. Either needs a migration that rewrites the stored name first.

### 2. Whole-wheat bread phosphorus — found during this pass, not fixed

The wheat breads sit far below what their own atta row implies:

| Food | Stored P | Implied by its own carbs × the file's atta (P 355) |
|---|---|---|
| Chapati / Roti | 160 | 272 |
| Plain Paratha | 120 | 222 |
| Aloo Paratha | 110 | 153 |

~100 mg/serving, 14% of the 700 mg goal, and consistent across the family — so either all three
breads are wrong or `Whole Wheat Flour (atta)` at P 355 is (that figure is verbatim USDA, 357).
One basis has to give, and picking which is a reference decision, not arithmetic. Left alone
deliberately: a guess here moves four entries at once.

### 3. Mixture (South Indian) — sodium 700

Commercial namkeen runs 900–1200; homemade and local-shop mixture runs lower. 700 is inside the
genuine range and I have no new evidence that would justify swapping one point estimate for
another. ~5% of the sodium goal per 30 g bowl either way.

---

## Fixed in `SEED_VERSION` 21

### Tier 2 — 15 foods

| Food | Change | Why |
|---|---|---|
| Drumstick (Moringa) | vitC 141 → 41 | 141 is raw pods; boiling destroys ~70%. −100 mg/serving, over a full day's RDA |
| Rava Upma | kcal 175→132, carbs 30→20, protein 5.0→3.6, fiber 1.8→1.4 | Stored carbs implied ~41 g dry rava per 100 g; realistic is 22–27%. The unit trap: 1 cup rava : 2 cups water is 1:2 by volume but 1:2.8 by mass. Set at 26%, the conservative end |
| Khara Bhath | kcal 185→140, carbs 28→18, protein 5.0→3.2, fiber 2.4→2.0 | Same rava base with more vegetables, so it must sit below Upma on both counts. It now does |
| Veg Biryani | kcal 195→170, carbs 30→24, protein 5.0→3.4 | Ceiling: the highest-protein component is the rice itself at 2.7–3.0. Oil 0, vegetables 1.8, yogurt 3.1, cashews 3% of the plate. No mixture reaches 5.0 |
| Sweet Pongal | kcal 270→260, protein 5.0→2.8 | Carbs, sugar *and* fat independently pin the batch yield at ~1059 g. At that yield 200 g rice + 50 g moong dal + nuts give 2.5–2.8. Reaching 5.0 needs more protein than 250 g of grain can supply |
| Ven Pongal | kcal 195→180, carbs 27→24.5, protein 6.0→4.6, fiber 2.5→2.1 | Stored carb:protein of 4.5 is a 1:1 rice-to-moong pongal. Conventional 2:1 gives 24.5 / 4.6, and that is now self-consistent to two figures |
| Pav Bhaji | vitC 28 → 12 | Potato/tomato/peas/capsicum mashed and simmered 20–30 min; the pav contributes 0. USDA boiled potato 13, raw tomato 14 |
| Horse Gram (cooked) | Fe 4.5→2.8, Ca 78→118, **K 287→490** | The entry's own 2.44× dilution back-solved iron to 11 mg/100 g raw, above every published figure (IFCT, Gopalan, all ~6.8). Calcium was the mirror image, 34% low. **Potassium was a transposition** — 287 is the published raw *calcium* figure sitting in the potassium field |
| Rajma | Cu 0.30→0.18, Mg 50→33, K 460→340 | All three were the undiluted plain-boiled-bean values. The entry's own protein (6.0 vs 8.67) puts it at ~69% beans. A gravy cannot be more mineral-dense than the bean |
| Chole | protein 7.0→5.6, Cu 0.35→0.23, Mg 48→32, kcal 140→135 | Cu and Mg were exactly USDA plain boiled chickpeas. Carbs, fat, fibre and calories all reconstruct at 60% chickpeas, which fixes protein at 5.6 and the minerals with it |
| Kadala Curry | Cu 0.32→0.25, Mg 48→35 | Same pattern, same fix basis (68% chickpeas from its own protein) |
| Papad (fried) | serving 10 g → 13 g | 10 g was the **pre-fry** weight. 13 g fried = 11.6 g solids + 1.4 g oil, which reproduces the entry's own 11% oil. +11 kcal/serving, and 49 kcal/papad is in the usual 45–60 range |
| Soya Chunks (cooked) | 11 fields | Did not reconcile with the `Soya Chunks (dry)` entry added in v20. Rebuilt at the dilution its own protein asserts (52.0 dry → 16.0 cooked = 3.25×), keeping sodium (added salt) and potassium (leaches into the cooking water). Worst field was **iron 5.6 → 2.8**, +2.8 mg/serving = 16% of goal; carbs 6→10 and B2 0.25→0.08 were the two the audit had named |
| Bisi Bele Bath | kcal 165→145, carbs 26→22, protein 5.5→4.0 | Reconstruction (200 g rice + 100 g toor + 300 g veg + 30 g fat) gives 19.6–21.8 carbs; took the top of the band |
| Sambar Sadam | kcal 145→132, carbs 24→21, protein 4.5→4.0 | Reconstruction gives 20.2 / 3.7; took the top of the band |

### Tier 3 — 9 foods

| Food | Change | Why |
|---|---|---|
| Idli | kcal 145→130, carbs 30→27, fat 1.0→0.4, satFat 0.25→0.10 | Carbs implied 39 g flour per 100 g (61% moisture) where protein implied 36 g. Idli is 65–70% water. Fat 1.0 with `oilGrams` 0 where rice + urad give ~0.3 |
| Rasam | kcal 35→33, protein 1.4→0.9 | Tamarind water, tomato and a little dal water |
| Avial | vitC 22 → 10 | None of ash gourd / plantain / yam / carrot / beans is C-rich after boiling |
| Bhindi Masala | vitC 22 → 13 | USDA okra *boiled* is 16.3 (raw 23), at 65–70% okra |
| Filter Coffee | B12 0.30→0.20, +Mg 7 | Its calcium, phosphorus, protein, fat and cholesterol all put it at ~55% milk, which is what Tea (milk, sugar) uses at the same calcium |
| Tomato Rice | kcal 175→155, carbs 30→25 | Ceiling: plain cooked rice is 28. Adding tomato masala and oil can only dilute |
| Coconut Rice | kcal 210→192, carbs 30→24, protein 3.5→3.0 | Same ceiling. Its own satFat 7.8 pins the grated coconut at ~17.5 g per 100 g, which leaves 76 g rice, not 100 |
| Nimbu Pani | sodium 180→80 | 180 was a masala/black-salt recipe the bare name doesn't state — 450 mg per glass, attributed silently. 80 is lemon, sugar, water and a pinch of salt, which is what the name says |
| Sundal (Channa) | P 120 → 150 | The one member of the `phosphorus = 120.0` cluster that was materially wrong (see below) |

### Magnesium omissions — 30 entries

The field was simply absent while the rest of each row was an exact USDA match. Added at USDA
per-100 g values: 12 fruits (papaya 21, guava 22, jackfruit 27, pineapple 12, sapota 12,
pomegranate 12, mango 10, watermelon 10, orange 10, mosambi 10, grapes 7, apple 5), 6 vegetables
(French beans 25, cauliflower 15, cucumber 13, cabbage 12, tomato 11, bottle gourd 11), 8
beverages (sugarcane 13, lassi 12, chaas 10, filter coffee 7, masala chai 7, tea 6, ginger tea 3,
black tea 3), and 4 composites (mint chutney 30, onion chutney 14, paal payasam 12, semiya
payasam 11). Worst was papaya at 31 mg/serving, 8% of goal.

Five entries still carry no magnesium and should not: Ghee (0), Sugar (0), Butter (2),
Lemon Juice (2), Nimbu Pani (1).

---

## Examined and dismissed — do not re-flag

- **Akki Roti / Ragi Roti flour fraction.** Flagged as 0.52 and 0.57 against the wheat breads'
  0.73–0.81. But a real akki roti is 1 cup rice flour (~160 g) to ~1 cup water, patted on the
  tawa, yielding four ~80 g rotis — which reproduces 0.50. Rice and ragi flour hydrate far more
  than wheat, and neither is rolled from a kneaded dough. Not an outlier, a different bread.
- **Chapati flour fraction.** The audit's own "verified clean" list uses chapati as the
  calibration anchor for the entire reconstruction method ("reconstructs to the decimal:
  76.5 g atta + 1.5 g oil → carb 55.0, protein 9.9 against stored 55 / 10.0"). The Tier 3 row
  contradicting it was marked low confidence. Kept the anchor.
- **Carrot Poriyal shrinkage.** Flagged at 1.08× where cabbage is 1.6× and snake gourd 1.8×. But
  Beetroot Poriyal — the other dense root — shows the identical ~1.0×, while cabbage (1.30×) and
  beans (1.23×) are high-water vegetables that collapse. Carrot is in family with beetroot. The
  comparison group was wrong, not the entry.
- **Papad macros.** At the entry's own `oilGrams` 11 the solids are 89 g, and fibre 8.0
  (= 9.0 × 0.89) and fat 12.0 (= 11 oil + 1.3) already reconstruct exactly. Only the serving
  weight was wrong, and that is fixed.
- **`phosphorus = 120.0` on the other six.** Computed each against its own ingredients: Pesarattu
  128, Palak Paneer 114, Chole 111, Rajma 105, Dal Makhani ~95–120. All within range — the
  repetition was coincidence, not a placeholder. Only Sundal (chickpea-dominant at 22 g carbs and
  8 g protein, so it must exceed Chole's 120) was materially low.

---

## Systematic patterns worth guarding

1. **Grain dishes: the divisor is too small.** The finished-dish weight used to divide by was
   understated, so every grain-derived nutrient scaled up together. Hit Rava Upma, Khara Bhath,
   Veg Biryani, Ven Pongal, Bisi Bele Bath, Sambar Sadam — all now fixed.
   **Proposed permanent guard:** for any dish based on boiled or steamed grain, the implied dry
   grain per 100 g must be strictly less than `35.9 × (1 − oilGrams/100 − vegetableFraction)`,
   since 35.9 g/100 g is the file's own plain-cooked-rice density and every such dish is wetter
   than plain rice. All six breached it before the fix; none does now.
2. **Composite dishes copying an ingredient value undiluted.** Rajma Cu/Mg/K, Chole Cu/Mg,
   Kadala Cu/Mg, Soya B2 — and Ragi Mudde Ca/Mg and Rasam vitamin C in Tier 1.
3. **Two entries for the same food drifting apart.** Adding `Soya Chunks (dry)` in v20 made the
   cooked row checkable for the first time, and 11 of its fields did not survive. Any future
   raw/dry pair should be generated from one basis, not written twice.
4. **Ceilings beat estimates.** The cleanest fixes here came from "a mixture cannot exceed its
   richest ingredient" (Veg Biryani protein, Tomato Rice and Coconut Rice carbs), not from
   recipe reconstruction.

---

## Verified clean — do not re-audit

- `satFat ≤ fat`, `sugar ≤ carbs`, `fiber ≤ carbs` across all 168 foods — 0 violations, including
  after every oil swap and every style addition. Re-checked at v21.
- Atwater energy balance — worst case under 25 kcal/serving at v21, and the top offenders are all
  fruit, where naive `4 × carbs` over-counts fiber and the stored USDA value is right.
- The whole cooking-oil model: every `oilGrams` plausible (15 g in a 250 g biryani, 9.6 g in a
  samosa, 9 g in 30 g of banana chips, 1–3 g for tempering), every oil floor satisfied, no swap
  produces `satFat > fat`, and all residual flags under 0.01 per serving.
- 17 raw/basic foods against USDA — 16 exact (banana 89, egg 155, paneer 265/18.3/20.8, ghee 900).
  Toor dal's 95 kcal vs USDA's 121 is **not** an error: USDA measures a thicker preparation, and 95
  is what the dilution Indian paruppu actually uses gives. Do not "fix" it toward USDA.
- All 168 `servingLabel` gram figures match `servingGrams` exactly. Re-checked at v21.
- B12 appears on exactly 21 entries, all dairy, egg or chicken. No plant-food violations.
- Sodium across 30+ high-sodium entries — all defensible.
- Iron and folate on leafy greens and dals — clean. Cooked-dal folate figures are verbatim USDA.
- Aloo Gobi (32) and Cabbage Poriyal (30) vitamin C — **checked and cleared**: boiled cauliflower
  genuinely retains 44.3 mg/100 g and boiled cabbage ~32.
- Reconstructs cleanly: Rajma (macros), Dosa, Masala Dosa, Appam, Idiyappam, Avial (macros),
  Kerala Stew, Kerala Parotta, Palak Paneer, Veg Kurma, Uttapam, Medu Vada, Chicken Biryani.
- Whole Moong / Whole Urad / Kala Chana / Cowpea (cooked) are exact USDA boiled-drained rows.
- All 13 fruit and 13 vegetable raw entries match USDA and carry raw-appropriate cup weights.
