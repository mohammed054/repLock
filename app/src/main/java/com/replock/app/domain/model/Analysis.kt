package com.replock.app.domain.model

data class Analysis(
    val repCount: Int,
    val formScore: Int,
    val feedback: String,
    val poseDetected: Boolean,
    val goodForm: Boolean,
    val phase: String = "WAITING"
) {
    val formLabel: String get() = when {
        formScore >= 80 -> "PERFECT"
        formScore >= 60 -> "OK"
        else            -> "BAD FORM"
    }
}