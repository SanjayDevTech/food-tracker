package com.example.foodtracker.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import com.example.foodtracker.data.FoodCategory
import com.example.foodtracker.data.db.FoodEntity
import kotlin.math.roundToInt

@Composable
fun FoodRow(
    food: FoodEntity,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val category = FoodCategory.fromName(food.category)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(category.bubbleColor()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = category.emoji(),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                food.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(
                listOfNotNull(
                    food.brand?.takeIf { it.isNotBlank() },
                    "${food.servingCalories().roundToInt()} kcal · ${food.servingLabel}",
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        trailing()
    }
}

private fun FoodCategory.emoji(): String = when (this) {
    FoodCategory.SouthIndian -> "🥞"
    FoodCategory.NorthIndian -> "🍛"
    FoodCategory.Snack -> "🍪"
    FoodCategory.Beverage -> "🥤"
    FoodCategory.Dairy -> "🥛"
    FoodCategory.Fruit -> "🍎"
    FoodCategory.Vegetable -> "🥦"
    FoodCategory.Grain -> "🌾"
    FoodCategory.Protein -> "🥚"
    FoodCategory.Packaged -> "📦"
    FoodCategory.Custom -> "✏️"
    FoodCategory.Other -> "🍽️"
}

private fun FoodCategory.bubbleColor(): Color = when (this) {
    FoodCategory.SouthIndian -> Color(0xFFFFE0B2)
    FoodCategory.NorthIndian -> Color(0xFFFFCCBC)
    FoodCategory.Snack -> Color(0xFFFFF59D)
    FoodCategory.Beverage -> Color(0xFFB3E5FC)
    FoodCategory.Dairy -> Color(0xFFE1F5FE)
    FoodCategory.Fruit -> Color(0xFFFFCDD2)
    FoodCategory.Vegetable -> Color(0xFFC8E6C9)
    FoodCategory.Grain -> Color(0xFFD7CCC8)
    FoodCategory.Protein -> Color(0xFFFFE082)
    FoodCategory.Packaged -> Color(0xFFE0E0E0)
    FoodCategory.Custom -> Color(0xFFD1C4E9)
    FoodCategory.Other -> Color(0xFFCFD8DC)
}
