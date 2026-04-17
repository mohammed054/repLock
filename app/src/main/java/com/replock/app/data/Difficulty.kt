package com.replock.app.data

data class Difficulty(
    val name: String,
    val displayName: String,
    val targetReps: Int,
    val strictness: Float,
    val description: String,
    val tagline: String
) {
    companion object {
        val EASY    = Difficulty("Easy",    "EASY",    10, 0.2f, "Perfect for beginners.", "Build the habit")
        val MEDIUM  = Difficulty("Medium",  "MID",     20, 0.5f, "Intermediate challenge.", "Keep pushing")
        val HARD    = Difficulty("Hard",    "HARD",    25, 0.8f, "Advanced level.", "Form matters")
        val EXTREME = Difficulty("Extreme", "EXTREME", 30, 1.0f, "Elite tier.", "No shortcuts")

        val ALL = listOf(EASY, MEDIUM, HARD, EXTREME)

        fun fromName(name: String): Difficulty =
            ALL.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: MEDIUM
    }
}