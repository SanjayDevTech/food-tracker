package com.example.foodtracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import kotlin.math.roundToInt

data class DonutSlice(val label: String, val value: Double, val color: Color)

@Composable
fun MacroDonut(
    slices: List<DonutSlice>,
    centerCaption: String,
    centerValue: String,
    modifier: Modifier = Modifier,
) {
    val total = slices.sumOf { it.value }.coerceAtLeast(0.0001)
    val progress by animateFloatAsState(targetValue = 1f, animationSpec = tween(600))

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(140.dp)) {
                val stroke = Stroke(width = 22.dp.toPx())
                val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                val topLeft = Offset(stroke.width / 2f, stroke.width / 2f)

                drawArc(
                    color = Color(0x14000000),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )

                var start = -90f
                slices.forEach { slice ->
                    val sweep = (slice.value / total * 360.0).toFloat() * progress
                    drawArc(
                        color = slice.color,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke,
                    )
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    centerValue,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    centerCaption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(20.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
        ) {
            slices.forEach { slice ->
                LegendRow(label = slice.label, value = slice.value, color = slice.color)
                Spacer(Modifier.height(6.dp))
            }
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
fun CalorieRing(
    consumed: Double,
    burned: Double,
    goal: Double,
    modifier: Modifier = Modifier,
) {
    val net = (consumed - burned).coerceAtLeast(0.0)
    val remaining = (goal - net).coerceAtLeast(0.0)
    val ratio = (net / goal.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1.5f)
    val animated by animateFloatAsState(targetValue = ratio.coerceAtMost(1f), animationSpec = tween(700))

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
        Column {
            CalorieStat("Goal", "${goal.roundToInt()} kcal")
            Spacer(Modifier.height(10.dp))
            CalorieStat("Food", "${consumed.roundToInt()} kcal")
            Spacer(Modifier.height(10.dp))
            CalorieStat("Exercise", "− ${burned.roundToInt()} kcal")
        }
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
