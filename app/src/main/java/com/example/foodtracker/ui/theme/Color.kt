package com.example.foodtracker.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Brand accent (single mint-teal family) -------------------------------
// Cool, fresh, and easy on the eye — feels healthy without being saccharine.
// Primary is the brightest mint; secondary is a paler tint of the same hue;
// tertiary is a warm-gold accent used sparingly for contrast moments.

// Primary — mint teal
val MintPrimary = Color(0xFF5FE0CC)
val MintPrimaryDeep = Color(0xFF0F8F7E)            // light-theme primary
val MintOnPrimary = Color(0xFF00322B)
val MintPrimaryContainer = Color(0xFF15403A)        // deep teal-ink for dark scheme
val MintPrimaryContainerLight = Color(0xFFB7F0E6)
val MintOnPrimaryContainer = Color(0xFFB7F0E6)
val MintOnPrimaryContainerLight = Color(0xFF00201B)

// Secondary — lighter, paler mint of the same family
val MintSecondary = Color(0xFFA5DDD3)
val MintSecondaryDeep = Color(0xFF4FA697)
val MintOnSecondary = Color(0xFF0B2A26)
val MintSecondaryContainer = Color(0xFF14302C)
val MintSecondaryContainerLight = Color(0xFFD6EFE9)
val MintOnSecondaryContainer = Color(0xFFD0EFE7)
val MintOnSecondaryContainerLight = Color(0xFF0F2724)

// Tertiary — warm gold accent for moments needing contrast against the cool primary
val GoldTertiary = Color(0xFFE6C673)
val GoldTertiaryDeep = Color(0xFFA9854A)
val GoldOnTertiary = Color(0xFF332300)
val GoldTertiaryContainer = Color(0xFF2D2310)
val GoldOnTertiaryContainer = Color(0xFFF3DBA5)

val ErrorRed = Color(0xFFBA1A1A)
val ErrorRedBright = Color(0xFFFF6E68)
val ErrorContainer = Color(0xFFFFDAD6)
val ErrorContainerDark = Color(0xFF5C0008)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// ---- Light surfaces (soft mint-tinted cream) ------------------------------
val SurfaceLight = Color(0xFFF6FBF9)
val OnSurfaceLight = Color(0xFF161D1B)
val SurfaceVariantLight = Color(0xFFDBE5E1)
val OnSurfaceVariantLight = Color(0xFF3F4946)
val OutlineLight = Color(0xFF6F7976)

// ---- AMOLED dark ----------------------------------------------------------
// Aggressive OLED-saving palette: everything tier-lifted barely moves off
// pure black. Containers exist mostly so Compose components don't blow up,
// but visually we rely on type + 1dp outlines for structure instead of fills.
// The faint mint tint is preserved so chrome that DOES use a container stays
// on-brand without burning pixels.
val AmoledBlack = Color(0xFF000000)
val AmoledOnSurface = Color(0xFFE9F1EE)
val AmoledOnSurfaceVariant = Color(0xFFB9C5C1)

val AmoledSurfaceContainerLowest = Color(0xFF000000)
val AmoledSurfaceContainerLow = Color(0xFF030505)
val AmoledSurfaceContainer = Color(0xFF070A0A)
val AmoledSurfaceContainerHigh = Color(0xFF0B0F0F)
val AmoledSurfaceContainerHighest = Color(0xFF111515)
val AmoledSurfaceVariant = Color(0xFF0B0F0F)
val AmoledOutline = Color(0xFF2A3431)
val AmoledOutlineVariant = Color(0xFF141B19)

// ---- Macro chart palette --------------------------------------------------
// Data viz needs distinguishable hues. These are intentionally outside the
// brand mint family so charts remain readable. The carbs/fat/fiber trio used
// to crowd the same brown family, so fiber's hue is pushed a touch toward
// olive-green while everything else gets a small luminance bump for better
// readability against AMOLED black — without losing the muted aesthetic.
val MacroCarbs = Color(0xFFF0AB42)     // amber (brighter)
val MacroProtein = Color(0xFFFF8B5F)   // coral (slightly more saturated)
val MacroFat = Color(0xFFE8B870)       // warm tan (lifted, separated from fiber)
val MacroFiber = Color(0xFFB0AE5A)     // olive-green (shifted away from tan)
val MacroWater = Color(0xFF72B3D1)     // dusty blue (lifted)
val MacroBurned = Color(0xFFD687A6)    // dusty rose (lifted)
val MacroWeight = Color(0xFF8FBC97)    // sage green (calm, neutral)
