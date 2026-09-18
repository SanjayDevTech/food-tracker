package com.example.foodtracker.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.foodtracker.DeepLink
import com.example.foodtracker.data.MealType
import com.example.foodtracker.ui.LocalAppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import com.example.foodtracker.ui.screens.activity.ActivityScreen
import com.example.foodtracker.ui.screens.addfood.AddFoodScreen
import com.example.foodtracker.ui.screens.barcode.BarcodeScreen
import com.example.foodtracker.ui.screens.home.HomeScreen
import com.example.foodtracker.ui.screens.log.LogFoodScreen
import com.example.foodtracker.ui.screens.onboarding.OnboardingScreen
import com.example.foodtracker.ui.screens.profile.ProfileScreen
import com.example.foodtracker.ui.screens.progress.ProgressScreen
import com.example.foodtracker.ui.screens.reminders.RemindersScreen
import com.example.foodtracker.ui.screens.settings.SettingsScreen
import com.example.foodtracker.ui.screens.water.WaterScreen
import com.example.foodtracker.ui.screens.weight.WeightScreen

@Composable
fun AppNav(
    pendingDeepLink: MutableStateFlow<DeepLink?> = MutableStateFlow(null),
) {
    val container = LocalAppContainer.current
    val onboarded by produceState<Boolean?>(initialValue = null) {
        container.profileStore.isOnboarded.collect { value = it }
    }
    val navController = rememberNavController()

    // Consume widget / reminder-notification deep-links once onboarding is
    // complete. Cleared after navigation so process death / back press don't
    // re-fire.
    //
    // Both gates are awaited *inside* the coroutine rather than keyed on the
    // effect, and the order matters:
    //  1. onboarding done - the onboarding graph has no LogFood/Water/Weight
    //     destination to navigate to;
    //  2. a NavHost has actually run setGraph. currentBackStackEntryFlow only
    //     emits once a graph is installed, so awaiting it is what keeps
    //     navigate() from throwing "You must call setGraph before calling
    //     getGraph".
    // Keying the effect on `onboarded` is not enough: the body re-reads the
    // state when it runs, so the run launched while `onboarded` was still null
    // (when the `when` below composes nothing at all) can observe `true` and
    // navigate a frame before the NavHost first composes. That is the cold
    // start from a notification tap.
    LaunchedEffect(navController) {
        snapshotFlow { onboarded }.first { it == true }
        navController.currentBackStackEntryFlow.first()
        pendingDeepLink.filterNotNull().collect { link ->
            // launchSingleTop so a second tap on the same reminder - or
            // tomorrow's firing while the app is still open on that screen -
            // reuses the destination instead of stacking another copy of it.
            when (link) {
                is DeepLink.LogMeal -> {
                    val meal = link.mealName?.let { MealType.fromName(it) } ?: currentMealForTime()
                    navController.navigate(LogFoodRoute(meal.name)) { launchSingleTop = true }
                }
                DeepLink.OpenWater -> navController.navigate(WaterRoute) { launchSingleTop = true }
                DeepLink.OpenWeight -> navController.navigate(WeightRoute) { launchSingleTop = true }
            }
            pendingDeepLink.value = null
        }
    }

    when (onboarded) {
        null -> Unit // loading
        false -> NavHost(
            navController = navController,
            startDestination = OnboardingRoute,
            modifier = Modifier.fillMaxSize(),
            enterTransition = navEnter,
            exitTransition = navExit,
            popEnterTransition = navPopEnter,
            popExitTransition = navPopExit,
            predictivePopEnterTransition = navPredictivePopEnter,
            predictivePopExitTransition = navPredictivePopExit,
        ) {
            graph(navController)
        }
        true -> NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.fillMaxSize(),
            enterTransition = navEnter,
            exitTransition = navExit,
            popEnterTransition = navPopEnter,
            popExitTransition = navPopExit,
            predictivePopEnterTransition = navPredictivePopEnter,
            predictivePopExitTransition = navPredictivePopExit,
        ) {
            graph(navController)
        }
    }
}

/**
 * Standard Android push/pop: the new screen slides in from the right and pushes
 * the previous one ~30% to the left (parallax); back reverses the motion. 300ms
 * matches the Material spec.
 *
 * The `predictivePop*` pair matters as much as the `pop*` pair. Since
 * navigation-compose 2.10, a pop driven by the predictive-back *gesture* uses
 * its own transitions rather than popEnter/popExit, and
 * `DefaultNavTransitions` defines those as fadeIn + scaleOut(0.7f) - which is
 * the zoom-out you get if you only override popEnter/popExit. Pointing them at
 * the same slide keeps the gesture and the back button producing one motion.
 * The unused Int the predictive lambdas receive is the swipe edge; the screen
 * slides off to the right either way, matching the non-gesture pop.
 */
private const val NAV_ANIM_MS = 300

private val navEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(SlideDirection.Start, tween(NAV_ANIM_MS))
}

private val navExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(SlideDirection.Start, tween(NAV_ANIM_MS)) { it / 3 }
}

private val navPopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(SlideDirection.End, tween(NAV_ANIM_MS)) { it / 3 }
}

private val navPopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(SlideDirection.End, tween(NAV_ANIM_MS))
}

private val navPredictivePopEnter:
    AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> EnterTransition = {
    slideIntoContainer(SlideDirection.End, tween(NAV_ANIM_MS)) { it / 3 }
}

private val navPredictivePopExit:
    AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> ExitTransition = {
    slideOutOfContainer(SlideDirection.End, tween(NAV_ANIM_MS))
}

private fun androidx.navigation.NavGraphBuilder.graph(navController: androidx.navigation.NavHostController) {
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
            onOpenWeight = { navController.navigate(WeightRoute) },
            onOpenProfile = { navController.navigate(ProfileRoute) },
            onOpenSettings = { navController.navigate(SettingsRoute) },
            onOpenProgress = { navController.navigate(ProgressRoute) },
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
        ActivityScreen(onBack = { navController.popBackStack() })
    }
    composable<ProfileRoute> {
        ProfileScreen(onBack = { navController.popBackStack() })
    }
    composable<SettingsRoute> {
        SettingsScreen(
            onBack = { navController.popBackStack() },
            onOpenReminders = { navController.navigate(RemindersRoute) },
        )
    }
    composable<WeightRoute> {
        WeightScreen(onBack = { navController.popBackStack() })
    }
    composable<ProgressRoute> {
        ProgressScreen(onBack = { navController.popBackStack() })
    }
    composable<RemindersRoute> {
        RemindersScreen(onBack = { navController.popBackStack() })
    }
}

private fun currentMealForTime(): MealType {
    val hour = java.time.LocalTime.now().hour
    return when {
        hour < 11 -> MealType.Breakfast
        hour < 15 -> MealType.Lunch
        hour < 19 -> MealType.Snack
        else -> MealType.Dinner
    }
}
