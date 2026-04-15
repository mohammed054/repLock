package com.replock.app.domain.model

data class Analysis(
    val repCount: Int,
    val formScore: Int,
    val cue: String,
    val poseDetected: Boolean,
    val goodForm: Boolean
) {
    val formLabel: String get() = when {
        formScore >= 80 -> "PERFECT"
        formScore >= 60 -> "OK"
        else            -> "BAD FORM"
    }
}