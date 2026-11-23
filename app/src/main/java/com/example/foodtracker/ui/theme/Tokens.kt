package com.example.foodtracker.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

/**
 * Layout tokens — spacing, radii and the three surviving heights.
 *
 * Replaces `Dimens`, which carried five radii (16/18/20/22/28) and three
 * different card paddings. Six implementations of the same card is what that
 * freedom bought, so the scale is deliberately short: if a value isn't here,
 * the answer is one of these, not a new one.
 */

/** `4 · 8 · 12 · 16 · 20 · 24 · 32`. Nothing else. */
object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp

    /** Screen gutter. */
    val gutter = 16.dp

    /** Card interior padding — one value, where there used to be three. */
    val cardPadding = 16.dp

    /** Gap between rows inside a card. */
    val inCard = 12.dp

    /** Gap between cards — up from 12; the screens read cramped. */

    /** Top-level scrolling-content padding. */
    val screenContent = PaddingValues(16.dp)
}

/** Four radii, down from five plus a pill. */
object Radius {
    /** Badges, meter caps, status cells. */
    val sm = 8.dp

    /** Inner rows, stat tiles, list rows. */
    val md = 14.dp

    /** Cards. */
    val lg = 20.dp

    /** Bottom sheets — top corners only. */
    val sheet = 24.dp

    /** Chips, buttons, segmented controls, the FAB. */
    val pill = 999.dp
}

/** The three heights that survived. The 56dp "hero" button did not. */
object Sizes {
    val button = 48.dp
    val iconButton = 48.dp
    val touchTarget = 48.dp
}

/**
 * Zero elevation anywhere. Structure is a 1dp [AppOutlineVariant] hairline.
 * Kept as a named token so the rule is greppable rather than folklore.
 */
object Elevation {
    val none = 0.dp
    val hairline = 1.dp
}
