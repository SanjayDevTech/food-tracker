package com.example.foodtracker.ui.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.foodtracker.data.MealType
import com.example.foodtracker.ui.LocalAppContainer
import com.example.foodtracker.ui.screens.activity.ActivityScreen
import com.example.foodtracker.ui.screens.addfood.AddFoodScreen
import com.example.foodtracker.ui.screens.barcode.BarcodeScreen
import com.example.foodtracker.ui.screens.home.HomeScreen
import com.example.foodtracker.ui.screens.log.LogFoodScreen
import com.example.foodtracker.ui.screens.onboarding.OnboardingScreen
import com.example.foodtracker.ui.screens.profile.ProfileScreen
import com.example.foodtracker.ui.screens.reminders.RemindersScreen
import com.example.foodtracker.ui.screens.trends.TrendsScreen
import com.example.foodtracker.ui.screens.water.WaterScreen
import com.example.foodtracker.ui.screens.weight.WeightScreen

private sealed class TopTab(
    val route: Any,
    val label: String,
    val icon: ImageVector,
) {
    data object Home : TopTab(HomeRoute, "Today", Icons.Default.Home)
    data object Activity : TopTab(ActivityRoute, "Move", Icons.Default.FitnessCenter)
    data object Trends : TopTab(TrendsRoute, "Trends", Icons.Default.BarChart)
    data object Profile : TopTab(ProfileRoute, "Profile", Icons.Default.Person)
}

private val TopTabs = listOf(TopTab.Home, TopTab.Activity, TopTab.Trends, TopTab.Profile)

@Composable
fun AppNav() {
    val container = LocalAppContainer.current
    val onboarded by produceState<Boolean?>(initialValue = null) {
        container.profileStore.isOnboarded.collect { value = it }
    }
    val navController = rememberNavController()

    when (onboarded) {
        null -> Unit // loading
        false -> RootNavHost(navController, start = OnboardingRoute)
        true -> RootNavHost(navController, start = HomeRoute)
    }
}

@Composable
private fun RootNavHost(navController: NavHostController, start: Any) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = TopTabs.any { tab ->
        currentDestination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
    }

    // Outer Scaffold owns the bottom NavigationBar. We let it apply system
    // insets via its default contentWindowInsets, then `consumeWindowInsets`
    // on the NavHost tells nested Scaffolds (LogFood, AddFood, Profile, etc.)
    // that those insets have already been handled — preventing the classic
    // double-padding artefact at the top app bar and bottom nav bar.
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopTabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.hasRoute(tab.route::class)
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(HomeRoute) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            composable<OnboardingRoute> {
                OnboardingScreen(onDone = {
                    navController.navigate(HomeRoute) {
                        popUpTo(OnboardingRoute) { inclusive = true }
                    }
                })
            }
            composable<HomeRoute> {
                HomeScreen(
                    onAddFoodToMeal = { meal -> navController.navigate(LogFoodRoute(meal.name)) },
                    onOpenWater = { navController.navigate(WaterRoute) },
                    onOpenActivity = { navController.navigate(ActivityRoute) },
                )
            }
            composable<LogFoodRoute> { entry ->
                val route: LogFoodRoute = entry.toRoute()
                LogFoodScreen(
                    initialMeal = MealType.fromName(route.mealType),
                    onBack = { navController.popBackStack() },
                    onScanBarcode = { navController.navigate(BarcodeRoute) },
                    onAddCustomFood = { navController.navigate(AddFoodRoute()) },
                )
            }
            composable<AddFoodRoute> { entry ->
                val route: AddFoodRoute = entry.toRoute()
                AddFoodScreen(
                    prefillBarcode = route.prefillBarcode,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<BarcodeRoute> {
                BarcodeScreen(
                    onBack = { navController.popBackStack() },
                    onAddManually = { barcode ->
                        navController.navigate(AddFoodRoute(prefillBarcode = barcode)) {
                            popUpTo(BarcodeRoute) { inclusive = true }
                        }
                    },
                )
            }
            composable<WaterRoute> {
                WaterScreen(onBack = { navController.popBackStack() })
            }
            composable<ActivityRoute> {
                ActivityScreen()
            }
            composable<ProfileRoute> {
                ProfileScreen(
                    onOpenReminders = { navController.navigate(RemindersRoute) },
                    onOpenWeight = { navController.navigate(WeightRoute) },
                )
            }
            composable<TrendsRoute> {
                TrendsScreen()
            }
            composable<WeightRoute> {
                WeightScreen(onBack = { navController.popBackStack() })
            }
            composable<RemindersRoute> {
                RemindersScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
