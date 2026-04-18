package com.replock.app.domain.model

/**
 * Minimal exercise configuration (Phase 5.4).
 * Replaces all hardcoded rep targets.  Extend this list as more exercises
 * are added to the app.
 */
data class ExerciseConfig(
    val id          : String,
    val name        : String,
    val targetReps  : Int,
    val targetSets  : Int
) {
    companion object {
        val PUSH_UP = ExerciseConfig(
            id         = "push_up",
            name       = "Push-Up",
            targetReps = 20,
            targetSets = 3
        )

        val SQUAT = ExerciseConfig(
            id         = "squat",
            name       = "Squat",
            targetReps = 15,
            targetSets = 3
        )

        val SIT_UP = ExerciseConfig(
            id         = "sit_up",
            name       = "Sit-Up",
            targetReps = 20,
            targetSets = 3
        )

        val ALL = listOf(PUSH_UP, SQUAT, SIT_UP)

        fun default(): ExerciseConfig = PUSH_UP

        /** Map an arbitrary program-name string to an ExerciseConfig id so that
         *  ProgressRepository queries (keyed by [id]) are stable. */
        fun idFromProgramName(programName: String): String =
            ALL.firstOrNull {
                it.name.equals(programName, ignoreCase = true) ||
                it.id.equals(programName, ignoreCase = true)
            }?.id ?: "push_up"
    }
}
