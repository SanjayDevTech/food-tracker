package com.example.foodtracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The type scale — design-plan §1.
 *
 * Nine styles, all on `FontFamily.Default`: open question 7 refused a bundled
 * font because the repo had just shipped "Trim the APK" and Inter costs ~300KB
 * of it back. Every numeric style instead carries `fontFeatureSettings = "tnum"`,
 * which is what actually matters — tabular figures stop the digits jittering as
 * a meter or ring animates its value.
 *
 * The canvas is quiet and the numbers are loud: a tracker is read in two-second
 * glances, so the only thing allowed to be big is a number.
 */

private const val TNUM = "tnum"

object AppText {
    /**
     * The one number the screen exists for.
     *
     * Direction B's whole character is an oversized numeral against tiny
     * uppercase labels, and 40sp was not oversized — it was merely large. At
     * 64/60 with tight tracking the figure carries the screen on its own, which
     * is what lets the ring around it go away.
     */
    val numberXXL = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 64.sp, lineHeight = 60.sp, letterSpacing = (-2).sp,
        fontFeatureSettings = TNUM,
    )

    /** Section numbers — stat values, meal totals. */
    val numberXL = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp, lineHeight = 44.sp, fontFeatureSettings = TNUM,
    )

    /** Screen headline numbers. */
    val numberL = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, lineHeight = 32.sp, fontFeatureSettings = TNUM,
    )

    /** Stat tiles, nutrient values. */
    val numberM = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 24.sp, fontFeatureSettings = TNUM,
    )

    /** Screen and sheet titles. */
    val titleL = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 28.sp,
    )

    /** Card titles, food names. */
    val titleM = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp,
    )

    /** Primary reading size — up from the old 14. */
    val body = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp,
    )

    /** Secondary lines. Numeric, so it gets tnum too. */
    val bodyS = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 18.sp, fontFeatureSettings = TNUM,
    )

    /** Chips, buttons. */
    val label = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp,
    )

    /** Section headers. Always rendered UPPERCASE by the caller. */
    /**
     * Section headers, always UPPERCASE.
     *
     * Tracking is 1.2sp, not the 0.5sp this started at: at 11sp a half-point of
     * letterspacing is invisible, and the letterspaced micro-label is half of
     * what makes Direction B read as editorial rather than as small grey text.
     */
    val eyebrow = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 1.2.sp,
    )
}

/**
 * Material slots pointed at the nine styles so stock components inherit them.
 * Reach for [AppText] directly when a slot would be a lie about intent.
 */
val Typography = Typography(
    displayLarge = AppText.numberXXL,
    displayMedium = AppText.numberL,
    displaySmall = AppText.numberM,
    headlineLarge = AppText.numberL,
    headlineMedium = AppText.numberM,
    headlineSmall = AppText.titleL,
    titleLarge = AppText.titleL,
    titleMedium = AppText.titleM,
    titleSmall = AppText.label,
    bodyLarge = AppText.body,
    bodyMedium = AppText.body,
    bodySmall = AppText.bodyS,
    labelLarge = AppText.label,
    labelMedium = AppText.label,
    labelSmall = AppText.eyebrow,
)
