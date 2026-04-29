package com.replock.app.domain.model

enum class ExerciseType(
    val displayName: String,
    val shortLabel: String,
    val defaultTargetReps: Int,
    val setupHint: String,
    val coachingHint: String,
    val strictness: Float
) {
    PUSH_UP(
        displayName = "Push-Ups",
        shortLabel = "Push",
        defaultTargetReps = 24,
        setupHint = "Place the phone side-on and keep your full body in frame.",
        coachingHint = "Chest low, hips steady, full lockout at the top.",
        strictness = 0.52f
    ),
    PULL_UP(
        displayName = "Pull-Ups",
        shortLabel = "Pull",
        defaultTargetReps = 8,
        setupHint = "Use the back camera and capture the bar, head, and hips.",
        coachingHint = "Start from a dead hang, drive the chest up, then return under control.",
        strictness = 0.6f
    );

    companion object {
        fun fromName(name: String?): ExerciseType {
            return entries.firstOrNull { type ->
                type.name.equals(name, ignoreCase = true) ||
                    type.displayName.equals(name, ignoreCase = true)
            } ?: PUSH_UP
        }
    }
}
