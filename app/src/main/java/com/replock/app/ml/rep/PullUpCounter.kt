package com.replock.app.ml.rep

import com.replock.app.domain.model.Analysis
import com.replock.app.ml.pose.LandmarkFrame
import com.replock.app.ml.pose.PoseMath

class PullUpCounter(private val strictness: Float = 0.6f) : ExerciseCounter {

    private val topThreshold: Float get() = lerp(102f, 88f, strictness)
    private val hangThreshold: Float get() = lerp(150f, 165f, strictness)
    private val bodyTolerance: Float get() = lerp(50f, 30f, strictness)
    private val transitionFramesRequired = 3

    private enum class Phase { HANG, PULLING, TOP, LOWERING }

    private var phase = Phase.HANG
    override var repCount = 0
        private set

    private var pendingPhase: Phase? = null
    private var pendingFrames = 0
    private var reachedTopInCurrentRep = false

    override fun reset() {
        phase = Phase.HANG
        repCount = 0
        pendingPhase = null
        pendingFrames = 0
        reachedTopInCurrentRep = false
    }

    override fun process(frame: LandmarkFrame): Analysis {
        if (!frame.isValid) {
            clearPending()
            return Analysis(
                repCount = repCount,
                formScore = 0,
                feedback = "Step back and fit the full pull-up line in frame",
                poseDetected = false,
                goodForm = false,
                phase = "STANDBY"
            )
        }

        val side = PoseMath.strongestSide(frame)
        val elbowAngle = PoseMath.angle(side.shoulder, side.elbow, side.wrist)
        val bodyAngle = PoseMath.angle(side.shoulder, side.hip, side.knee ?: side.ankle)
        val bodyLineScore = PoseMath.bodyLineScore(bodyAngle, bodyTolerance)
        val wristLine = PoseMath.averageY(frame.leftWrist, frame.rightWrist) ?: side.wrist?.y
        val noseAboveHands = frame.nose != null && wristLine != null && frame.nose.y <= wristLine + 0.015f

        val pullDepthScore = when {
            elbowAngle <= topThreshold -> 1f
            elbowAngle >= hangThreshold -> 0.2f
            else -> (1f - (elbowAngle - topThreshold) / (hangThreshold - topThreshold)).coerceIn(0f, 1f)
        }
        val topScore = if (noseAboveHands) 1f else 0.35f
        val formScore = ((pullDepthScore * 0.45f) + (bodyLineScore * 0.3f) + (topScore * 0.25f)) * 100f
        val goodForm = formScore >= 62f

        val feedback = updatePhase(
            elbowAngle = elbowAngle,
            bodyLineScore = bodyLineScore,
            noseAboveHands = noseAboveHands
        )

        return Analysis(
            repCount = repCount,
            formScore = formScore.toInt(),
            feedback = feedback,
            poseDetected = true,
            goodForm = goodForm,
            phase = phase.name
        )
    }

    private fun updatePhase(
        elbowAngle: Float,
        bodyLineScore: Float,
        noseAboveHands: Boolean
    ): String {
        return when (phase) {
            Phase.HANG -> {
                if (elbowAngle < hangThreshold - 15f) {
                    tryTransition(Phase.PULLING)
                } else if (pendingPhase != Phase.PULLING) {
                    clearPending()
                }

                if (bodyLineScore < 0.55f) "Stay tall and reduce swing"
                else "Start from a full hang"
            }

            Phase.PULLING -> {
                when {
                    elbowAngle <= topThreshold && noseAboveHands -> {
                        tryTransition(Phase.TOP) { reachedTopInCurrentRep = true }
                        "Chest to the bar"
                    }
                    elbowAngle >= hangThreshold -> {
                        tryTransition(Phase.HANG)
                        "Reset from a full hang"
                    }
                    else -> {
                        if (pendingPhase != Phase.TOP && pendingPhase != Phase.HANG) clearPending()
                        if (bodyLineScore < 0.55f) "Brace your core"
                        else "Pull harder"
                    }
                }
            }

            Phase.TOP -> {
                if (elbowAngle > topThreshold + 12f) {
                    tryTransition(Phase.LOWERING)
                } else if (pendingPhase != Phase.LOWERING) {
                    clearPending()
                }
                "Lower with control"
            }

            Phase.LOWERING -> {
                when {
                    elbowAngle >= hangThreshold && !noseAboveHands -> {
                        tryTransition(Phase.HANG) {
                            if (reachedTopInCurrentRep) repCount++
                            reachedTopInCurrentRep = false
                        }
                        "Full hang"
                    }
                    else -> {
                        if (pendingPhase != Phase.HANG) clearPending()
                        "Full range on the way down"
                    }
                }
            }
        }
    }

    private fun tryTransition(proposedPhase: Phase, onCommit: () -> Unit = {}) {
        if (proposedPhase == pendingPhase) {
            pendingFrames++
            if (pendingFrames >= transitionFramesRequired) {
                phase = proposedPhase
                pendingPhase = null
                pendingFrames = 0
                onCommit()
            }
        } else {
            pendingPhase = proposedPhase
            pendingFrames = 1
        }
    }

    private fun clearPending() {
        pendingPhase = null
        pendingFrames = 0
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
}
