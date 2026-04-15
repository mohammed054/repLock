package com.replock.app.ml.rep

import com.replock.app.domain.model.Analysis
import com.replock.app.domain.model.RepPhase
import com.replock.app.ml.pose.Joint
import com.replock.app.ml.pose.LandmarkFrame
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

class PushUpCounter(private val strictness: Float = 0.3f) : ExerciseCounter {

    private val downThreshold: Float
        get() = lerp(110f, 90f, strictness)

    private val upThreshold: Float
        get() = lerp(120f, 140f, strictness)

    private val alignmentTolerance: Float
        get() = lerp(30f, 15f, strictness)

    private val minRepCompletionAngle: Float
        get() = 135f

    private enum class Phase { UP, GOING_DOWN, DOWN, GOING_UP }

    private var phase = Phase.UP
    override var repCount = 0
        private set

    private var minAngleSeen = 180f
    private var formScoreAccum = 0f
    private var formSampleCount = 0
    private var warningCount = 0
    private var lastFeedback = ""

    override fun reset() {
        phase = Phase.UP
        repCount = 0
        minAngleSeen = 180f
        formScoreAccum = 0f
        formSampleCount = 0
        warningCount = 0
        lastFeedback = ""
    }

    override fun process(frame: LandmarkFrame): Analysis {
        if (!frame.isValid) {
            return Analysis(repCount, 0, "GET IN FRAME", poseDetected = false, goodForm = false, phase = "STANDBY")
        }

        val elbowAngle = computeAngle(frame.leftShoulder, frame.leftElbow, frame.leftWrist)
        val alignScore = computeAlignmentScore(frame)
        val formScore = computeFormScore(elbowAngle, alignScore)
        val goodForm = formScore >= 40

        if (elbowAngle < minAngleSeen) minAngleSeen = elbowAngle
        formScoreAccum += formScore
        formSampleCount++

        val feedback = updatePhase(elbowAngle, formScore, alignScore)

        return Analysis(
            repCount = repCount,
            formScore = formScore.toInt(),
            feedback = feedback,
            poseDetected = true,
            goodForm = goodForm,
            phase = phase.name
        )
    }

    private fun updatePhase(elbowAngle: Float, formScore: Float, alignScore: Float): String {
        val wasInPhase = phase.name
        return when (phase) {
            Phase.UP -> {
                if (elbowAngle < upThreshold - 20f) {
                    phase = Phase.GOING_DOWN
                    minAngleSeen = elbowAngle
                }
                if (alignScore < (1f - alignmentTolerance / 50f)) "STRAIGHTEN BODY" else "LOWER"
            }

            Phase.GOING_DOWN -> {
                if (elbowAngle < minAngleSeen) minAngleSeen = elbowAngle
                if (elbowAngle <= downThreshold) {
                    phase = Phase.DOWN
                    "PUSH!"
                } else if (elbowAngle > upThreshold + 10f) {
                    phase = Phase.UP
                    "LOWER!"
                } else {
                    "LOWER"
                }
            }

            Phase.DOWN -> {
                if (elbowAngle > downThreshold + 20f) {
                    phase = Phase.GOING_UP
                }
                "PUSH!"
            }

            Phase.GOING_UP -> {
                if (elbowAngle >= minRepCompletionAngle) {
                    if (minAngleSeen <= downThreshold + 5f) {
                        repCount++
                        minAngleSeen = 180f
                        formScoreAccum = 0f
                        formSampleCount = 0
                    }
                    phase = Phase.UP
                    "GOOD"
                } else if (elbowAngle > upThreshold + 15f) {
                    phase = Phase.UP
                    "LOWER!"
                } else {
                    "PUSH!"
                }
            }
        }
    }

    private fun computeAlignmentScore(frame: LandmarkFrame): Float {
        val deviation = abs(
            computeAngle(frame.leftShoulder, frame.leftHip, frame.leftAnkle) - 180f
        )
        return (1f - (deviation / 40f)).coerceIn(0f, 1f)
    }

    private fun computeFormScore(elbowAngle: Float, alignScore: Float): Float {
        val romScore = if (elbowAngle <= downThreshold) 1f
                      else (1f - (elbowAngle - downThreshold) / 80f).coerceIn(0f, 1f)
        return ((romScore * 0.6f + alignScore * 0.4f) * 100f)
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