package com.example.foodtracker.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object OnboardingRoute

@Serializable
object HomeRoute

@Serializable
data class LogFoodRoute(val mealType: String)

@Serializable
data class AddFoodRoute(val prefillBarcode: String? = null)

@Serializable
object BarcodeRoute

@Serializable
object WaterRoute

@Serializable
object ActivityRoute

@Serializable
object ProfileRoute

@Serializable
object TrendsRoute

@Serializable
object WeightRoute

@Serializable
object RemindersRoute
