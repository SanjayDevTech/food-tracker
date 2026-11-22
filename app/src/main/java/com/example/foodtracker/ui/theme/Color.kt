package com.example.foodtracker.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The palette — design-plan §1, "a black instrument panel".
 *
 * ONE scheme. There is no light table and no dynamic table: decision 11 in the
 * plan dropped light mode outright, which is what removed the dual-palette
 * compromise that was forcing every colour here into the middle. The roles stay
 * semantic, so a light scheme later is a second value table, not a refactor.
 *
 * Structure in this app comes from hairlines and type weight, never from
 * elevation, so the `surfaceContainer*` family barely moves off black — it
 * exists mostly so stock Material components have somewhere to land.
 */

// ---- Surfaces -------------------------------------------------------------

val AppBlack = Color(0xFF000000)
val AppSurfaceLow = Color(0xFF070707)
val AppSurface = Color(0xFF0C0C0B)
val AppSurfaceHigh = Color(0xFF141312)
val AppSurfaceHighest = Color(0xFF1C1A18)

/** Not `#FFFFFF`: pure white on pure black haloes, and this is read in the dark. */
val AppOnSurface = Color(0xFFEFE9E0)
val AppOnSurfaceVariant = Color(0xFFA79E92)
val AppOutline = Color(0xFF36312B)
val AppOutlineVariant = Color(0xFF201D1A)

// ---- Accent ---------------------------------------------------------------
// Deep green (open question 9). Terracotta was rejected because it collides
// with the `over limit` hue, which has to be orange-red.

val AppPrimary = Color(0xFF63D2B0)
val AppOnPrimary = Color(0xFF00251B)
val AppPrimaryContainer = Color(0xFF123A2F)
val AppOnPrimaryContainer = Color(0xFFB3EEDC)
val AppPrimaryDeep = Color(0xFF1F6F5C)

val AppTertiary = Color(0xFFE0A961)
val AppOnTertiary = Color(0xFF2A1A00)
val AppTertiaryContainer = Color(0xFF2D2310)
val AppOnTertiaryContainer = Color(0xFFF3DBA5)

val AppError = Color(0xFFFF6B5E)
val AppOnError = Color(0xFF3A0A06)
val AppErrorContainer = Color(0xFF4A120C)
val AppOnErrorContainer = Color(0xFFFFD9D4)

// ---- Data palette ---------------------------------------------------------
// One hue per concept, shared by every chart and meter, so protein is the same
// colour everywhere. Tuned against #000000 only — a hue picked against white
// vibrates on black, so these are re-derived rather than flipped.

val MacroCarbs = Color(0xFFE3A13C)     // amber
val MacroProtein = Color(0xFFDD6B4C)   // coral
val MacroFat = Color(0xFFB9924B)       // warm tan
val MacroFiber = Color(0xFF8A9A4B)     // olive
val MacroWater = Color(0xFF5E9CBF)     // dusty blue
val MacroBurned = Color(0xFFBF7E99)    // dusty rose
val MacroWeight = Color(0xFF7FA98A)    // sage

// ---- Status palette -------------------------------------------------------
// The one hue in the app that is allowed to be loud, and only when something
// has gone past a cap. See [NutrientStatus].

val StatusOver = Color(0xFFF08A4B)     // orange — past a limit

// ---- BMI bands ------------------------------------------------------------
// Four fixed bands. Clinical categories, not brand colours, so they keep their
// conventional blue/green/amber/red reading — but muted for a black canvas.

val BmiUnderweight = Color(0xFF5E9CBF)
val BmiNormal = Color(0xFF7FA98A)
val BmiOverweight = Color(0xFFE0A961)
val BmiObese = Color(0xFFFF6B5E)
