package com.example.foodtracker.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

/**
 * Centralised layout tokens. Every screen uses these so widths, heights, and
 * spacing read as one designed system rather than a patchwork of magic numbers.
 *
 * Usage rules:
 *  - `Screen*` tokens belong on top-level LazyColumn / Column padding & spacing.
 *  - `Card*` tokens belong inside SectionCard / Card content.
 *  - `Button*` tokens belong on Buttons — primary CTAs are 52dp; the hero CTA on
 *    Onboarding is allowed to be larger.
 */
object Dimens {
    // Top-level page layout
    val ScreenPadding = 16.dp
    val ScreenContentPadding = PaddingValues(16.dp)
    val ScreenItemSpacing = 12.dp

    // Card interior
    val CardPadding = 16.dp
    val CardItemSpacingTight = 6.dp
    val CardItemSpacing = 8.dp
    val CardItemSpacingLoose = 12.dp

    // Section spacing (between a section header and its content)
    val SectionHeaderGap = 8.dp

    // Action heights
    val ButtonHeight = 52.dp
    val ButtonHeightHero = 56.dp        // onboarding-style large CTAs only
    val IconButtonSize = 48.dp          // standard Material touch target

    // Shapes
    val FieldCornerRadius = 28.dp       // pill-shaped search/input fields
    val CardCornerRadius = 16.dp
}
