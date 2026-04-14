package com.replock.app.domain.model

/**
 * The phase the user's body is currently in during a rep.
 *
 * - [DOWN] – elbow angle below ~80° (bottom of push-up / top of pull-up)
 * - [UP]   – elbow angle above ~155° (arms extended)
 * - [TRANSITION] – moving between extremes; angle in the mid-range
 */
enum class RepPhase { UP, DOWN, TRANSITION }

/**
 * Snapshot of the current detection state.
 *
 * @param phase       Which phase the pose detector has classified.
 * @param confidence  0..1 — how strongly the angle sits at the phase's extreme.
 *                    0 = boundary / TRANSITION; 1 = deep in that phase.
 */
data class RepState(
    val phase      : RepPhase = RepPhase.UP,
    val confidence : Float    = 0f
) {
    /** Human-readable label for the UI badge (matches the old String-based API). */
    val label: String get() = when (phase) {
        RepPhase.UP         -> "UP"
        RepPhase.DOWN       -> "DOWN"
        RepPhase.TRANSITION -> "TRANSITION"
    }
}
