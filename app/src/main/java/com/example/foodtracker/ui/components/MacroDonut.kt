package com.example.foodtracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

data class DonutSlice(val label: String, val value: Double, val color: Color)

/**
 * Single hero block on Home — the calorie ring plus the macro legend in one
 * card. Replaces the older arrangement (calorie ring + separate Macros donut
 * card) since the ring already provides the circular visualization; doubling
 * up read as redundant. The macro legend now serves as the "what's in those
 * calories" detail alongside the ring.
 *
 * Layout: ring on the left (160dp), per-macro legend column on the right,
 * and a compact Goal / Food row beneath the whole block.
 */
@Composable
fun CalorieHero(
    consumed: Double,
    goal: Double,
    macroSlices: List<DonutSlice>,
    modifier: Modifier = Modifier,
) {
    val remaining = (goal - consumed).coerceAtLeast(0.0)
    val ratio = (consumed / goal.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1.5f)
    val animated by animateFloatAsState(
        targetValue = ratio.coerceAtMost(1f),
        animationSpec = tween(700),
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(160.dp)) {
                    val stroke = Stroke(width = 18.dp.toPx())
                    val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                    val topLeft = Offset(stroke.width / 2f, stroke.width / 2f)
                    drawArc(
                        color = Color(0x22000000),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke,
                    )
                    drawArc(
                        color = if (ratio > 1.05f) Color(0xFFBA1A1A) else Color(0xFFE85D2D),
                        startAngle = -90f,
                        sweepAngle = animated * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${remaining.roundToInt()}",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "kcal left",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                macroSlices.forEach { slice ->
                    LegendRow(label = slice.label, value = slice.value, color = slice.color)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        // Compact stat strip — Goal / Food. Lived on the right of
        // the ring in the older two-block layout; moved down so the macro
        // legend can take that space.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CalorieStat("Goal", "${goal.roundToInt()} kcal")
            CalorieStat("Food", "${consumed.roundToInt()} kcal")
        }
    }
}

@Composable
private fun LegendRow(label: String, value: Double, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            "${value.roundToInt()} g",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun CalorieStat(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
