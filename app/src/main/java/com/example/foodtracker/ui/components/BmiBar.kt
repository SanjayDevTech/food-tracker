package com.example.foodtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodtracker.domain.Bmi
import kotlin.math.abs

private val UnderweightColor = Color(0xFF4FC3F7) // blue
private val NormalColor      = Color(0xFF66BB6A) // green
private val OverweightColor  = Color(0xFFFFB74D) // amber
private val ObeseColor       = Color(0xFFEF5350) // red

/**
 * Horizontal coloured band showing BMI categories with a triangular marker
 * for the user's current BMI and (optionally) one for their goal weight's
 * BMI. Used in the onboarding Basics page and the profile screen.
 *
 * Markers use theme tokens (onSurface for Now, primary for Goal) so they
 * stay legible against both the AMOLED black background and the light surface.
 */
@Composable
fun BmiBar(
    currentBmi: Double,
    goalBmi: Double? = null,
    modifier: Modifier = Modifier,
    currentLabel: String = "Now",
    goalLabel: String = "Goal",
) {
    val cat = Bmi.category(currentBmi)
    val currentMarkerFill = MaterialTheme.colorScheme.onSurface
    val goalMarkerFill = MaterialTheme.colorScheme.primary
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "BMI ${"%.1f".format(currentBmi)} · ${cat.label}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (goalBmi != null) {
                val delta = goalBmi - currentBmi
                val arrow = if (delta > 0) "↑" else if (delta < 0) "↓" else "→"
                Text(
                    "Goal $arrow ${"%.1f".format(goalBmi)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
        ) {
            val barTop = size.height * 0.55f
            val barHeight = size.height * 0.32f

            val range = Bmi.DISPLAY_MAX - Bmi.DISPLAY_MIN
            fun fracFor(bmi: Double): Float =
                ((bmi - Bmi.DISPLAY_MIN) / range).toFloat().coerceIn(0f, 1f)

            // Coloured bands.
            val bands = listOf(
                UnderweightColor to fracFor(Bmi.UNDERWEIGHT_MAX),
                NormalColor      to fracFor(Bmi.NORMAL_MAX),
                OverweightColor  to fracFor(Bmi.OVERWEIGHT_MAX),
                ObeseColor       to 1f,
            )
            var prev = 0f
            bands.forEach { (color, end) ->
                drawRect(
                    color = color,
                    topLeft = Offset(prev * size.width, barTop),
                    size = Size((end - prev) * size.width, barHeight),
                )
                prev = end
            }

            fun drawMarker(bmi: Double, fill: Color) {
                val x = fracFor(bmi) * size.width
                val tipY = barTop
                val baseY = tipY - 12.dp.toPx()
                val half = 7.dp.toPx()
                val path = Path().apply {
                    moveTo(x, tipY)
                    lineTo(x - half, baseY)
                    lineTo(x + half, baseY)
                    close()
                }
                drawPath(path, fill)
            }
            drawMarker(currentBmi, currentMarkerFill)
            if (goalBmi != null && abs(goalBmi - currentBmi) > 0.05) {
                drawMarker(goalBmi, goalMarkerFill)
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BandLabel("15")
            BandLabel("18.5")
            BandLabel("25")
            BandLabel("30")
            BandLabel("40")
        }
        if (goalBmi != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LegendMarker(currentLabel, currentMarkerFill)
                LegendMarker(goalLabel, goalMarkerFill)
            }
        }
    }
}

@Composable
private fun BandLabel(text: String) {
    Text(
        text,
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun LegendMarker(label: String, fill: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(width = 10.dp, height = 8.dp)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                val path = Path().apply {
                    moveTo(size.width / 2f, 0f)
                    lineTo(0f, size.height)
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(path, fill)
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
