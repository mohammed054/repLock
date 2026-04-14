package com.replock.app.ml.rep

import com.replock.app.domain.model.RepPhase
import com.replock.app.domain.model.RepState
import com.replock.app.ml.pose.Joint
import com.replock.app.ml.pose.LandmarkFrame
import kotlin.math.acos
import kotlin.math.sqrt

class PushUpCounter {

    companion object {
        const val DOWN_ANGLE_DEG = 85f   // More relaxed down angle
        const val UP_ANGLE_DEG  = 160f   // More strict up angle
        const val MIN_STRAIGHT_ANGLE = 155f // Better form validation
    }

    private var currentPhase = RepPhase.UP
    private var lastPhaseChangeTime = 0L
    private val debounceMs = 300L

    var repCount = 0
        private set

    data class Analysis(
        val state: RepState,
        val isFormValid: Boolean,
        val feedback: String
    )

    fun process(frame: LandmarkFrame): Analysis {
        val elbowAngle = bestElbowAngle(frame)
        val isStraight = isBodyStraight(frame)
        
        val isFormValid = elbowAngle != null && isStraight
        val feedback = when {
            elbowAngle == null -> "ADJUST CAMERA"
            !isStraight -> "KEEP BACK STRAIGHT"
            currentPhase == RepPhase.UP && elbowAngle > (UP_ANGLE_DEG - 10f) -> "GO DOWN"
            currentPhase == RepPhase.DOWN && elbowAngle < (DOWN_ANGLE_DEG + 10f) -> "PUSH UP"
            else -> "GOOD FORM"
        }

        if (elbowAngle != null) {
            val newPhase = classifyAngle(elbowAngle)
            val now = System.currentTimeMillis()
            
            if (newPhase != currentPhase && (now - lastPhaseChangeTime) > debounceMs) {
                if (currentPhase == RepPhase.DOWN && newPhase == RepPhase.UP) {
                    repCount++
                }
                currentPhase = newPhase
                lastPhaseChangeTime = now
            }
        }

        return Analysis(
            state = RepState(
                phase = currentPhase,
                confidence = if (elbowAngle != null) computeConfidence(currentPhase, elbowAngle) else 0f
            ),
            isFormValid = isFormValid,
            feedback = feedback
        )
    }

    fun reset() {
        repCount     = 0
        currentPhase = RepPhase.UP
    }

    private fun classifyAngle(angleDeg: Float): RepPhase = when {
        angleDeg <= DOWN_ANGLE_DEG -> RepPhase.DOWN
        angleDeg >= UP_ANGLE_DEG   -> RepPhase.UP
        else                       -> RepPhase.TRANSITION
    }

    private fun computeConfidence(phase: RepPhase, angleDeg: Float): Float = when (phase) {
        RepPhase.DOWN       -> ((DOWN_ANGLE_DEG - angleDeg) / DOWN_ANGLE_DEG).coerceIn(0f, 1f)
        RepPhase.UP         -> ((angleDeg - UP_ANGLE_DEG) / (180f - UP_ANGLE_DEG)).coerceIn(0f, 1f)
        RepPhase.TRANSITION -> 0f
    }

    private fun bestElbowAngle(frame: LandmarkFrame): Float? {
        val leftAngle  = calculateAngle(frame.leftShoulder,  frame.leftElbow,  frame.leftWrist)
        val rightAngle = calculateAngle(frame.rightShoulder, frame.rightElbow, frame.rightWrist)
        
        return when {
            leftAngle != null && rightAngle != null -> (leftAngle + rightAngle) / 2f
            leftAngle  != null                      -> leftAngle
            rightAngle != null                      -> rightAngle
            else                                    -> null
        }
    }

    private fun isBodyStraight(frame: LandmarkFrame): Boolean {
        val leftHipAngle = calculateAngle(frame.leftShoulder, frame.leftHip, frame.leftAnkle) 
            ?: calculateAngle(frame.leftShoulder, frame.leftHip, frame.leftKnee)
        
        val rightHipAngle = calculateAngle(frame.rightShoulder, frame.rightHip, frame.rightAnkle)
            ?: calculateAngle(frame.rightShoulder, frame.rightHip, frame.rightKnee)

        val angle = listOfNotNull(leftHipAngle, rightHipAngle).average()
        // If we can't see the hips/legs at all, we allow it (average of empty is NaN)
        return angle.isNaN() || angle > 150.0
    }

    private fun calculateAngle(first: Joint?, middle: Joint?, last: Joint?): Float? {
        if (first == null || middle == null || last == null) return null

        val v1x = first.x - middle.x
        val v1y = first.y - middle.y
        val v2x = last.x - middle.x
        val v2y = last.y - middle.y

        val mag1 = sqrt(v1x * v1x + v1y * v1y)
        val mag2 = sqrt(v2x * v2x + v2y * v2y)

        if (mag1 < 1e-6f || mag2 < 1e-6f) return null

        val cosAngle = ((v1x * v2x + v1y * v2y) / (mag1 * mag2)).toDouble()
            .coerceIn(-1.0, 1.0)

        return Math.toDegrees(acos(cosAngle)).toFloat()
    }
}