package com.example.foodtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class DailyValue(val label: String, val value: Double)

@Composable
fun WeeklyBarChart(
    bars: List<DailyValue>,
    goal: Double?,
    barColor: Color,
    unit: String,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val measurer = rememberTextMeasurer()
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(top = 8.dp, bottom = 20.dp, start = 4.dp, end = 4.dp),
        ) {
            if (bars.isEmpty()) return@Canvas
            val maxVal = (bars.maxOf { it.value }.coerceAtLeast(goal ?: 0.0)).coerceAtLeast(1.0)
            val w = size.width
            val h = size.height
            val gap = 8.dp.toPx()
            val barWidth = (w - gap * (bars.size + 1)) / bars.size

            // grid lines: 0, mid, max
            val gridColor = outline
            for (i in 0..3) {
                val y = h - (h * i / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 0.6f,
                )
            }

            // goal line
            if (goal != null && goal > 0) {
                val goalRatio = (goal / maxVal).toFloat().coerceIn(0f, 1f)
                val y = h - h * goalRatio
                drawLine(
                    color = Color(0xFFE85D2D),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.5f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        intervals = floatArrayOf(8f, 6f),
                    ),
                )
            }

            bars.forEachIndexed { index, bar ->
                val x = gap + index * (barWidth + gap)
                val ratio = (bar.value / maxVal).toFloat().coerceIn(0f, 1f)
                val barH = h * ratio
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, h - barH),
                    size = Size(barWidth, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                )
                // label
                val labelLayout = measurer.measure(
                    AnnotatedString(bar.label),
                    style = TextStyle(fontSize = 10.sp, color = onSurfaceVariant, textAlign = TextAlign.Center),
                )
                drawText(
                    textLayoutResult = labelLayout,
                    topLeft = Offset(
                        x + barWidth / 2f - labelLayout.size.width / 2f,
                        h + 4.dp.toPx(),
                    ),
                )
            }
        }
    }
}
