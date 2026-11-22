package com.example.foodtracker.ui.screens.today.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Spacing
import com.example.foodtracker.util.DateUtil
import java.time.format.DateTimeFormatter
import java.util.Locale

/** How far back the strip scrolls. Three weeks covers every realistic back-fill. */
private const val DAYS_BACK = 21

/**
 * The date picker on Today — design-plan §3, independently confirmed by
 * reference shot 5.
 *
 * Replaces `InlineDayPicker`'s `[◀ Today ▶]` pill, which had the problem that
 * you had to already know what day you were on before you could get to another
 * one: it showed a label and two arrows, never a calendar.
 *
 * Forward-capped at today, because there is nothing to log in the future, and
 * scrolled to the end on first composition so "now" is where your thumb is.
 */
@Composable
fun DayStrip(
    selected: Long,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { DateUtil.todayEpochDay() }
    val days = remember(today) { (0 until DAYS_BACK).map { today - (DAYS_BACK - 1) + it } }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { listState.scrollToItem(days.lastIndex) }

    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(days, key = { it }) { day ->
            val date = DateUtil.epochDayToDate(day)
            val isSelected = day == selected
            val isToday = day == today
            val scheme = MaterialTheme.colorScheme
            Column(
                modifier = Modifier
                    .width(44.dp)
                    .clickable { onSelect(day) }
                    .padding(vertical = Spacing.s),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    date.format(DateTimeFormatter.ofPattern("EEEEE", Locale.getDefault())),
                    style = AppText.eyebrow,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    date.dayOfMonth.toString(),
                    style = AppText.numberM,
                    color = when {
                        isSelected -> scheme.onSurface
                        isToday -> scheme.primary
                        else -> scheme.onSurfaceVariant
                    },
                )
                Spacer(Modifier.height(Spacing.s))
                // A 2dp rule under the day, not a filled pill. The pill was the
                // last piece of Direction A's geometry above the fold — a
                // rounded accent block on a page that otherwise separates
                // everything with hairlines. A rule marks the selection in the
                // same language the rest of the screen already speaks.
                Box(
                    Modifier
                        .width(24.dp)
                        .height(2.dp)
                        .background(if (isSelected) scheme.onSurface else Color.Transparent)
                )
            }
        }
    }
}
