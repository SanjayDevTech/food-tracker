package com.example.foodtracker.data

enum class MealType(val displayName: String, val emoji: String) {
    Breakfast("Breakfast", "🌞"),
    Lunch("Lunch", "🍛"),
    Snack("Snack", "🍪"),
    Dinner("Dinner", "🌙");

    companion object {
        fun fromName(name: String): MealType = entries.firstOrNull { it.name == name } ?: Snack
    }
}

enum class FoodCategory(val displayName: String) {
    SouthIndian("South Indian"),
    NorthIndian("North Indian"),
    Snack("Snack"),
    Beverage("Beverage"),
    Dairy("Dairy"),
    Fruit("Fruit"),
    Vegetable("Vegetable"),
    Grain("Grain"),
    Protein("Protein"),
    Packaged("Packaged"),
    Custom("Custom"),
    Other("Other");

    companion object {
        fun fromName(name: String): FoodCategory = entries.firstOrNull { it.name == name } ?: Other
    }
}

enum class ActivityType(val displayName: String, val metValue: Double) {
    Walking("Walking", 3.5),
    BriskWalking("Brisk Walking", 4.5),
    Running("Running", 9.8),
    Cycling("Cycling", 7.5),
    Swimming("Swimming", 8.0),
    Yoga("Yoga", 3.0),
    WeightTraining("Weight Training", 5.0),
    HIIT("HIIT", 9.0),
    Dancing("Dancing", 5.0),
    Cardio("Cardio", 7.0),
    Other("Other", 4.0);

    companion object {
        fun fromName(name: String): ActivityType = entries.firstOrNull { it.name == name } ?: Other
    }
}

enum class Sex(val displayName: String) {
    Male("Male"), Female("Female");

    companion object {
        fun fromName(name: String): Sex = entries.firstOrNull { it.name == name } ?: Male
    }
}

enum class ActivityLevel(val displayName: String, val multiplier: Double) {
    Sedentary("Sedentary (little/no exercise)", 1.2),
    Light("Light (1-3 days/week)", 1.375),
    Moderate("Moderate (3-5 days/week)", 1.55),
    Active("Active (6-7 days/week)", 1.725),
    VeryActive("Very Active (athlete)", 1.9);

    companion object {
        fun fromName(name: String): ActivityLevel = entries.firstOrNull { it.name == name } ?: Moderate
    }
}

enum class Goal(val displayName: String, val calorieAdjustment: Double) {
    Lose("Lose weight", -500.0),
    Maintain("Maintain weight", 0.0),
    Gain("Gain weight", 300.0);

    companion object {
        fun fromName(name: String): Goal = entries.firstOrNull { it.name == name } ?: Maintain
    }
}
