package com.example.foodtracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.example.foodtracker.ui.components.atoms.AppDivider
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Elevation

/**
 * The three bar destinations — design-plan §5.
 *
 * Why three and not four: logging is an *action*, not a place, so it is the
 * FAB on Today. Why a bar at all: four of the old app's screens — Stats, Rich
 * in…, Profile, Settings — lived behind a three-dot overflow in the greeting
 * row, which is where features go to be forgotten.
 */
enum class BarDestination(
    val label: String,
    val icon: ImageVector,
) {
    Today("Today", Icons.Outlined.Today),
    Trends("Trends", Icons.Outlined.Insights),
    You("You", Icons.Outlined.Person),
}

/** Whether [this] destination is (or sits under) one of the three bar tabs. */
fun NavDestination?.barDestination(): BarDestination? = when {
    this == null -> null
    hierarchy.any { it.hasRoute(TodayRoute::class) } -> BarDestination.Today
    hierarchy.any { it.hasRoute(TrendsRoute::class) } -> BarDestination.Trends
    hierarchy.any { it.hasRoute(YouRoute::class) } -> BarDestination.You
    else -> null
}

/**
 * Structure is a hairline, not elevation (§1), so the bar draws its own top
 * border and sits on `background` — `NavigationBar`'s default container is a
 * tonal surface, which on a black canvas reads as a grey slab.
 */
@Composable
fun AppBottomBar(
    current: BarDestination?,
    onSelect: (BarDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(modifier) {
        AppDivider()
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = Elevation.none,
            windowInsets = NavigationBarDefaults.windowInsets,
        ) {
            BarDestination.entries.forEach { dest ->
                val selected = dest == current
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(dest) },
                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                    label = { Text(dest.label, style = AppText.eyebrow) },
                    // Labels always shown: three items with room to spare, and
                    // a label that appears only when selected is a label you
                    // cannot read before you need it.
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}
