package com.replock.app.domain.model

/**
 * Supported exercise types.
 *
 * Each type maps to its own rep-counting algorithm:
 * - [PUSH_UP] → elbow-angle descent (DOWN < 80°, UP > 155°)
 * - [PULL_UP] → same elbow-angle logic; chin above bar is detected via
 *               shoulder-vs-wrist height relationship (future Phase 3 work)
 */
enum class ExerciseType(val displayName: String) {
    PUSH_UP("Push-Up"),
    PULL_UP("Pull-Up")
}
