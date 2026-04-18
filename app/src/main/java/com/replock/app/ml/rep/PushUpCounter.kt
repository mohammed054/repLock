package com.replock.app.ml.rep

import com.replock.app.domain.model.Analysis
import com.replock.app.ml.pose.Joint
import com.replock.app.ml.pose.LandmarkFrame
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

class PushUpCounter(private val strictness: Float = 0.3f) : ExerciseCounter {

    private val downThreshold: Float get() = lerp(110f, 90f, strictness)
    private val upThreshold: Float get() = lerp(120f, 140f, strictness)
    private val alignmentTolerance: Float get() = lerp(30f, 15f, strictness)
    private val minRepCompletionAngle: Float get() = 135f

    private val MIN_VISIBLE_KEYPOINTS = 6
    private val CONFIDENCE_THRESHOLD = 0.5f
    private val TRANSITION_FRAMES_REQUIRED = 3

    private enum class Phase { UP, GOING_DOWN, DOWN, GOING_UP }

    private var phase = Phase.UP
    override var repCount = 0
        private set

    private var pendingPhase: Phase? = null
    private var pendingFrames = 0

    private var minAngleSeen = 180f
    private var formScoreAccum = 0f
    private var formSampleCount = 0

    override fun reset() {
        phase = Phase.UP
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
            return Analysis(repCount, 0, "GET IN FRAME", poseDetected = false, goodForm = false, phase = "STANDBY")
        }

        val confidentCount = countConfidentJoints(frame)
        if (confidentCount < MIN_VISIBLE_KEYPOINTS) {
            pendingPhase = null
            pendingFrames = 0
            return Analysis(repCount, 0, "GET IN FRAME", poseDetected = false, goodForm = false, phase = "STANDBY")
        }

        val elbowAngle = computeAngle(frame.leftShoulder, frame.leftElbow, frame.leftWrist)
        val alignScore = computeAlignmentScore(frame)
        val formScore = computeFormScore(elbowAngle, alignScore)
        val goodForm = formScore >= 40f

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

    private fun countConfidentJoints(frame: LandmarkFrame): Int =
        listOfNotNull(
            frame.leftShoulder, frame.rightShoulder,
            frame.leftElbow, frame.rightElbow,
            frame.leftWrist, frame.rightWrist,
            frame.leftHip, frame.rightHip,
            frame.leftKnee, frame.rightKnee,
            frame.leftAnkle, frame.rightAnkle
        ).count { it.inFrameLikelihood >= CONFIDENCE_THRESHOLD }

    private fun tryTransition(proposedPhase: Phase, onCommit: () -> Unit = {}): Boolean {
        return if (proposedPhase == pendingPhase) {
            pendingFrames++
            if (pendingFrames >= TRANSITION_FRAMES_REQUIRED) {
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
            Phase.UP -> {
                if (elbowAngle < upThreshold - 20f) {
                    tryTransition(Phase.GOING_DOWN) { minAngleSeen = elbowAngle }
                } else {
                    if (pendingPhase != Phase.GOING_DOWN) clearPending()
                }
                if (alignScore < (1f - alignmentTolerance / 50f)) "STRAIGHTEN BODY" else "LOWER"
            }

            Phase.GOING_DOWN -> {
                if (elbowAngle < minAngleSeen) minAngleSeen = elbowAngle
                when {
                    elbowAngle <= downThreshold -> {
                        tryTransition(Phase.DOWN)
                        "PUSH!"
                    }
                    elbowAngle > upThreshold + 10f -> {
                        tryTransition(Phase.UP)
                        "LOWER!"
                    }
                    else -> {
                        if (pendingPhase != Phase.DOWN && pendingPhase != Phase.UP) clearPending()
                        "LOWER"
                    }
                }
            }

            Phase.DOWN -> {
                if (elbowAngle > downThreshold + 20f) {
                    tryTransition(Phase.GOING_UP)
                } else {
                    if (pendingPhase != Phase.GOING_UP) clearPending()
                }
                "PUSH!"
            }

            Phase.GOING_UP -> {
                when {
                    elbowAngle >= minRepCompletionAngle -> {
                        tryTransition(Phase.UP) {
                            if (minAngleSeen <= downThreshold + 5f) {
                                repCount++
                                formScoreAccum = 0f
                                formSampleCount = 0
                            }
                            minAngleSeen = 180f
                        }
                        "GOOD"
                    }
                    elbowAngle > upThreshold + 15f -> {
                        tryTransition(Phase.UP)
                        "LOWER!"
                    }
                    else -> {
                        if (pendingPhase != Phase.UP) clearPending()
                        "PUSH!"
                    }
                }
            }
        }
    }

    private fun clearPending() {
        pendingPhase = null
        pendingFrames = 0
    }

    private fun computeAlignmentScore(frame: LandmarkFrame): Float {
        val deviation = abs(computeAngle(frame.leftShoulder, frame.leftHip, frame.leftAnkle) - 180f)
        return (1f - (deviation / 40f)).coerceIn(0f, 1f)
    }

    private fun computeFormScore(elbowAngle: Float, alignScore: Float): Float {
        val romScore = if (elbowAngle <= downThreshold) 1f
                      else (1f - (elbowAngle - downThreshold) / 80f).coerceIn(0f, 1f)
        return (romScore * 0.6f + alignScore * 0.4f) * 100f
    }

    private fun computeAngle(a: Joint?, b: Joint?, c: Joint?): Float {
        if (a == null || b == null || c == null) return 180f
        val abX = a.x - b.x
        val abY = a.y - b.y
        val cbX = c.x - b.x
        val cbY = c.y - b.y
        val dot = abX * cbX + abY * cbY
        val magA = sqrt((abX * abX + abY * abY).toDouble())
        val magC = sqrt((cbX * cbX + cbY * cbY).toDouble())
        if (magA < 1e-6 || magC < 1e-6) return 180f
        return Math.toDegrees(acos((dot / (magA * magC)).coerceIn(-1.0, 1.0))).toFloat()
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
}