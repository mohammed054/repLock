package com.replock.app.domain.model

data class RepState(
    val phase      : RepPhase = RepPhase.UP,
    val confidence : Float    = 0f
) {
    val label: String get() = when (phase) {
        RepPhase.UP         -> "UP"
        RepPhase.DOWN       -> "DOWN"
        RepPhase.TRANSITION -> "TRANSITION"
    }
}