package com.healthtrackai.app.data.services

data class MealAnalysisResult(
    val mealTitle: String,
    val identifiedItems: List<String>,
    val estimatedCalories: Int,
    val estimatedProteinGrams: Float,
    val estimatedCarbsGrams: Float,
    val estimatedFatGrams: Float,
    val dietaryFeedback: String,
    val isVegetableRich: Boolean,
    val disclaimerText: String = "⚠️ Nutrition facts are AI estimations for general wellness tracking and not intended for clinical dietary prescriptions."
)

interface NutritionAnalysisService {
    suspend fun analyzeMeal(imageUri: String? = null, mealDescription: String = ""): MealAnalysisResult
}

class LocalRuleBasedNutritionService : NutritionAnalysisService {

    override suspend fun analyzeMeal(imageUri: String?, mealDescription: String): MealAnalysisResult {
        val desc = mealDescription.lowercase()

        return when {
            desc.contains("salad") || desc.contains("bowl") || desc.contains("greens") -> {
                MealAnalysisResult(
                    mealTitle = "Mediterranean Salad Bowl",
                    identifiedItems = listOf("Mixed Leafy Greens", "Cherry Tomatoes", "Cucumber", "Olive Oil Dressing", "Feta Cheese"),
                    estimatedCalories = 360,
                    estimatedProteinGrams = 12f,
                    estimatedCarbsGrams = 24f,
                    estimatedFatGrams = 22f,
                    dietaryFeedback = "Excellent fiber and micronutrient density. High in antioxidants and healthy fats.",
                    isVegetableRich = true
                )
            }
            desc.contains("chicken") || desc.contains("protein") || desc.contains("rice") || desc.contains("meat") -> {
                MealAnalysisResult(
                    mealTitle = "Grilled Chicken with Brown Rice & Broccoli",
                    identifiedItems = listOf("Lean Chicken Breast (150g)", "Steamed Brown Rice (1 cup)", "Broccoli Florets"),
                    estimatedCalories = 480,
                    estimatedProteinGrams = 38f,
                    estimatedCarbsGrams = 46f,
                    estimatedFatGrams = 10f,
                    dietaryFeedback = "Well-balanced lean protein meal supporting muscle recovery and sustained energy.",
                    isVegetableRich = true
                )
            }
            desc.contains("oat") || desc.contains("breakfast") || desc.contains("fruit") || desc.contains("yogurt") -> {
                MealAnalysisResult(
                    mealTitle = "Berry & Nut Oatmeal Bowl",
                    identifiedItems = listOf("Rolled Oats", "Blueberries", "Almonds", "Honey drizzle", "Chia Seeds"),
                    estimatedCalories = 390,
                    estimatedProteinGrams = 14f,
                    estimatedCarbsGrams = 62f,
                    estimatedFatGrams = 11f,
                    dietaryFeedback = "Rich in complex carbohydrates and slow-release energy for a productive morning.",
                    isVegetableRich = false
                )
            }
            else -> {
                MealAnalysisResult(
                    mealTitle = "Balanced Mixed Meal Plate",
                    identifiedItems = listOf("Protein Portion", "Complex Carbohydrates", "Steamed Vegetables"),
                    estimatedCalories = 450,
                    estimatedProteinGrams = 26f,
                    estimatedCarbsGrams = 48f,
                    estimatedFatGrams = 14f,
                    dietaryFeedback = "Balanced macronutrient distribution. Adding an extra portion of leafy greens supports daily fiber goals.",
                    isVegetableRich = true
                )
            }
        }
    }
}
