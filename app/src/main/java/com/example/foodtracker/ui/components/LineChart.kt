package com.example.foodtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class LinePoint(val label: String, val value: Double)

@Composable
fun LineChart(
    points: List<LinePoint>,
    lineColor: Color,
    unit: String,
    modifier: Modifier = Modifier,
    minOverride: Double? = null,
    maxOverride: Double? = null,
    /** Optional horizontal dashed reference line — e.g. goal weight. */
    referenceLine: Double? = null,
    /** Caption painted near the reference line (only when [referenceLine] is set). */
    referenceLabel: String? = null,
) {
    val measurer = rememberTextMeasurer()
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val referenceColor = MaterialTheme.colorScheme.tertiary

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(top = 8.dp, bottom = 22.dp, start = 8.dp, end = 8.dp),
        ) {
            if (points.isEmpty()) return@Canvas
            val values = points.map { it.value }
            val dataMin = values.min()
            val dataMax = values.max()
            // Expand bounds so the reference line stays in view even when the
            // user is far from their goal.
            val refExpandedMin = referenceLine?.let { minOf(dataMin, it) } ?: dataMin
            val refExpandedMax = referenceLine?.let { maxOf(dataMax, it) } ?: dataMax
            // Add 10% headroom so the line never grazes the top/bottom.
            val padding = ((refExpandedMax - refExpandedMin) * 0.1).coerceAtLeast(0.5)
            val minV = (minOverride ?: (refExpandedMin - padding))
            val maxV = (maxOverride ?: (refExpandedMax + padding)).coerceAtLeast(minV + 0.5)
            val w = size.width
            val h = size.height

            // grid lines
            for (i in 0..3) {
                val y = h - (h * i / 3f)
                drawLine(
                    color = outline,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 0.6f,
                )
            }

            val stepX = if (points.size > 1) w / (points.size - 1).toFloat() else 0f
            fun toPx(value: Double): Float {
                val ratio = ((value - minV) / (maxV - minV)).toFloat().coerceIn(0f, 1f)
                return h - h * ratio
            }

            // Goal-line overlay drawn under the data path so the line crosses
            // it visually — gives an "are we close?" read at a glance.
            if (referenceLine != null) {
                val refY = toPx(referenceLine)
                drawLine(
                    color = referenceColor,
                    start = Offset(0f, refY),
                    end = Offset(w, refY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                )
                if (referenceLabel != null) {
                    val layout = measurer.measure(
                        AnnotatedString(referenceLabel),
                        style = TextStyle(fontSize = 10.sp, color = referenceColor),
                    )
                    // Right-align so it doesn't collide with the leftmost data point.
                    val labelX = (w - layout.size.width - 4.dp.toPx()).coerceAtLeast(0f)
                    val labelY = (refY - layout.size.height - 2.dp.toPx()).coerceAtLeast(0f)
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(labelX, labelY),
                    )
                }
            }

            val path = Path()
            points.forEachIndexed { index, point ->
                val x = index * stepX
                val y = toPx(point.value)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx()),
            )

            // dots + sparse labels
            points.forEachIndexed { index, point ->
                val x = index * stepX
                val y = toPx(point.value)
                drawCircle(color = lineColor, radius = 3.5.dp.toPx(), center = Offset(x, y))

                if (index == 0 || index == points.lastIndex || (points.size > 6 && index % (points.size / 4) == 0)) {
                    val layout = measurer.measure(
                        AnnotatedString(point.label),
                        style = TextStyle(fontSize = 10.sp, color = onSurfaceVariant),
                    )
                    val labelX = (x - layout.size.width / 2f).coerceIn(0f, w - layout.size.width)
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(labelX, h + 4.dp.toPx()),
                    )
                }
            }
        }
    }
}
