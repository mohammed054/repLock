package com.replock.app.ml.rep

import com.replock.app.domain.model.Analysis
import com.replock.app.ml.pose.LandmarkFrame
import com.replock.app.ml.pose.PoseMath

class PushUpCounter(private val strictness: Float = 0.5f) : ExerciseCounter {

    private val downThreshold: Float get() = lerp(108f, 92f, strictness)
    private val upThreshold: Float get() = lerp(145f, 160f, strictness)
    private val alignmentTolerance: Float get() = lerp(42f, 24f, strictness)
    private val minRepCompletionAngle: Float get() = lerp(145f, 160f, strictness)

    private val minimumVisibleKeypoints = 5
    private val transitionFramesRequired = 3

    private enum class Phase { READY, DESCENT, BOTTOM, ASCENT }

    private var phase = Phase.READY
    override var repCount = 0
        private set

    private var pendingPhase: Phase? = null
    private var pendingFrames = 0

    private var minAngleSeen = 180f
    private var formScoreAccum = 0f
    private var formSampleCount = 0

    override fun reset() {
        phase = Phase.READY
        repCount = 0
        minAngleSeen = 180f
        formScoreAccum = 0f
        formSampleCount = 0
        pendingPhase = null
        pendingFrames = 0
    }

    override fun process(frame: LandmarkFrame): Analysis {
        if (!frame.isValid) {
            pendingPhase = null
            pendingFrames = 0
            return Analysis(
                repCount = repCount,
                formScore = 0,
                feedback = "Get your full body in frame",
                poseDetected = false,
                goodForm = false,
                phase = "STANDBY"
            )
        }

        val side = PoseMath.strongestSide(frame)
        val confidentCount = listOfNotNull(
            side.shoulder,
            side.elbow,
            side.wrist,
            side.hip,
            side.knee,
            side.ankle
        ).count { it.inFrameLikelihood >= LandmarkFrame.MIN_LIKELIHOOD }
        if (confidentCount < minimumVisibleKeypoints) {
            pendingPhase = null
            pendingFrames = 0
            return Analysis(
                repCount = repCount,
                formScore = 0,
                feedback = "Get your full body in frame",
                poseDetected = false,
                goodForm = false,
                phase = "STANDBY"
            )
        }

        val elbowAngle = PoseMath.angle(side.shoulder, side.elbow, side.wrist)
        val bodyAngle = PoseMath.angle(side.shoulder, side.hip, side.ankle)
        val alignScore = PoseMath.bodyLineScore(bodyAngle, alignmentTolerance)
        val formScore = computeFormScore(elbowAngle, alignScore)
        val goodForm = formScore >= 60f

        if (elbowAngle < minAngleSeen) minAngleSeen = elbowAngle
        formScoreAccum += formScore
        formSampleCount++

        val feedback = updatePhase(elbowAngle, alignScore)

        return Analysis(
            repCount = repCount,
            formScore = formScore.toInt(),
            feedback = feedback,
            poseDetected = true,
            goodForm = goodForm,
            phase = phase.name
        )
    }

    private fun tryTransition(proposedPhase: Phase, onCommit: () -> Unit = {}): Boolean {
        return if (proposedPhase == pendingPhase) {
            pendingFrames++
            if (pendingFrames >= transitionFramesRequired) {
                phase = proposedPhase
                pendingPhase = null
                pendingFrames = 0
                onCommit()
                true
            } else false
        } else {
            pendingPhase = proposedPhase
            pendingFrames = 1
            false
        }
    }

    private fun updatePhase(elbowAngle: Float, alignScore: Float): String {
        return when (phase) {
            Phase.READY -> {
                if (elbowAngle < upThreshold - 18f) {
                    tryTransition(Phase.DESCENT) { minAngleSeen = elbowAngle }
                } else {
                    if (pendingPhase != Phase.DESCENT) clearPending()
                }
                if (alignScore < 0.62f) "Keep hips in line"
                else "Lower with control"
            }

            Phase.DESCENT -> {
                if (elbowAngle < minAngleSeen) minAngleSeen = elbowAngle
                when {
                    elbowAngle <= downThreshold -> {
                        tryTransition(Phase.BOTTOM)
                        "Drive up"
                    }
                    elbowAngle > upThreshold + 10f -> {
                        tryTransition(Phase.READY)
                        "Lower with control"
                    }
                    else -> {
                        if (pendingPhase != Phase.BOTTOM && pendingPhase != Phase.READY) clearPending()
                        if (alignScore < 0.62f) "Brace your core"
                        else "Chest a little lower"
                    }
                }
            }

            Phase.BOTTOM -> {
                if (elbowAngle > downThreshold + 20f) {
                    tryTransition(Phase.ASCENT)
                } else {
                    if (pendingPhase != Phase.ASCENT) clearPending()
                }
                "Drive up"
            }

            Phase.ASCENT -> {
                when {
                    elbowAngle >= minRepCompletionAngle -> {
                        tryTransition(Phase.READY) {
                            if (minAngleSeen <= downThreshold + 5f) {
                                repCount++
                                formScoreAccum = 0f
                                formSampleCount = 0
                            }
                            minAngleSeen = 180f
                        }
                        "Full lockout"
                    }
                    elbowAngle > upThreshold + 15f -> {
                        tryTransition(Phase.READY)
                        "Reset and go again"
                    }
                    else -> {
                        if (pendingPhase != Phase.READY) clearPending()
                        if (alignScore < 0.62f) "Keep the plank tight"
                        else "Press through the floor"
                    }
                }
            }
        }
    }

    private fun clearPending() {
        pendingPhase = null
        pendingFrames = 0
    }

    private fun computeFormScore(elbowAngle: Float, alignScore: Float): Float {
        val romScore = if (elbowAngle <= downThreshold) 1f
            else (1f - (elbowAngle - downThreshold) / 70f).coerceIn(0f, 1f)
        return (romScore * 0.7f + alignScore * 0.3f) * 100f
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
}
