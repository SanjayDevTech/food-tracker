package com.example.foodtracker.ui.components.atoms

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * The one rounding rule, pinned — including which locale it rounds in.
 *
 * Every number the user reads goes through [formatValue], and it has no UI of
 * its own, so a regression here reads as "the numbers look wrong" on a dozen
 * screens at once and shows up in no diff.
 *
 * The locale cases are the point of the class. [grouped] has always pinned
 * Locale.US, while the decimal halves used to follow the device — which on a
 * comma-decimal phone printed `1,234` and `1,5` from the same function, one
 * comma meaning thousands and the other the decimal point. These tests run the
 * whole suite under a comma-decimal default so that can never come back
 * silently.
 */
class ValuesTest {

    private lateinit var original: Locale

    /** Germany: `,` for decimals and `.` for thousands — the inverse of US. */
    @Before fun useACommaDecimalLocale() {
        original = Locale.getDefault()
        Locale.setDefault(Locale.GERMANY)
    }

    @After fun restoreLocale() = Locale.setDefault(original)

    @Test fun groupedUsesCommasWhateverTheDeviceLocaleSays() {
        assertEquals("2,468", grouped(2468.0))
        assertEquals("1,234,567", grouped(1234567.4))
        assertEquals("999", grouped(999.0))
        assertEquals("-1,200", grouped(-1200.0))
    }

    @Test fun decimalsUseAFullStopWhateverTheDeviceLocaleSays() {
        assertEquals("2.5", formatValue(2.5))
        assertEquals("0.4", formatValue(0.4))
        assertEquals("1.5", countText(1.5))
    }

    /** The two halves of [formatValue] must not disagree about what a comma is. */
    @Test fun groupingAndDecimalMarksAgree() {
        assertEquals("1,234", formatValue(1234.0))
        assertEquals("1.5", formatValue(1.5))
    }

    @Test fun precisionFollowsMagnitude() {
        assertEquals("2,740", formatValue(2740.0)) // mg of sodium: grouped, no decimal
        assertEquals("12", formatValue(12.4)) // >= 10: whole
        assertEquals("2.5", formatValue(2.5)) // >= 1: one decimal
        assertEquals("0.4", formatValue(0.4)) // µg of B12: two, trimmed
    }

    @Test fun countTextDropsTheDecimalOnWholeServings() {
        assertEquals("1", countText(1.0))
        assertEquals("2", countText(2.0))
    }

    @Test fun neverPrintsADecoratedZero() {
        assertEquals("0", formatValue(0.0))
        assertEquals("0", formatValue(-1.0))
        // Rounds to nothing at two decimals — must not read "0." or "0.00".
        assertEquals("0", formatValue(0.004))
    }
}
