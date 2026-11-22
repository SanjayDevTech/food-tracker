package com.example.foodtracker.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Every destination — design-plan §5. Eleven in-app plus onboarding, down from
 * thirteen flat pages with no bar.
 *
 * What left: `WaterRoute` and `WeightRoute` became bottom sheets (a whole route
 * for four buttons was over-provisioned, and weight's chart belongs beside the
 * other charts on Trends), and `ProfileRoute` + `SettingsRoute` merged into
 * [YouRoute] with two pages hanging off it.
 *
 * The three bar destinations — [TodayRoute], [TrendsRoute], [YouRoute] — are
 * peers; everything else pushes onto whichever tab is current.
 */

@Serializable
object OnboardingRoute

// ---- Bottom-bar destinations ----------------------------------------------

@Serializable
object TodayRoute

@Serializable
object TrendsRoute

@Serializable
object YouRoute

// ---- Leaf pages ------------------------------------------------------------

@Serializable
data class LogFoodRoute(val mealType: String)

@Serializable
data class AddFoodRoute(val prefillBarcode: String? = null)

@Serializable
object BarcodeRoute

/**
 * @param nutrientKey preselects the nutrient to rank by. Set when arriving from
 *        a contribution sheet's "Find foods rich in iron"; null from the Log
 *        picker's ⚗ action, which has no nutrient in mind yet.
 */
@Serializable
data class RichFoodsRoute(val nutrientKey: String? = null)

/**
 * Every tracked nutrient for one day.
 *
 * A page, not a sheet. It is 29 rows — about two screens — and a scroll that
 * long inside a modal fights the drag-to-dismiss gesture the whole way down.
 * Making it a page is also what lets the contribution detail stay a sheet: a
 * sheet raised over a page is an ordinary transition, where a sheet raised over
 * another sheet has no convention at all.
 *
 * §5 argued against a page here on the grounds that it "would want a bar slot
 * it doesn't deserve" — but leaf pages hide the bottom bar in this nav graph,
 * so it never asks for one.
 *
 * @param epochDay which day to read. Carried explicitly so the page is
 *        restorable after process death without depending on Today's state.
 */
@Serializable
data class AllNutrientsRoute(val epochDay: Long)

@Serializable
object EditProfileRoute

@Serializable
object DataBackupRoute

@Serializable
object PrivacyRoute

@Serializable
object RemindersRoute
