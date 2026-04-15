package com.replock.app.ml.rep

import com.replock.app.domain.model.Analysis
import com.replock.app.domain.model.RepPhase
import com.replock.app.ml.pose.Joint
import com.replock.app.ml.pose.LandmarkFrame
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

class PushUpCounter(private val strictness: Float = 0.6f) : ExerciseCounter {

    private val downThreshold: Float
        get() = lerp(100f, 80f, strictness)

    private val upThreshold: Float
        get() = lerp(150f, 160f, strictness)

    private val alignmentTolerance: Float
        get() = lerp(20f, 10f, strictness)

    private enum class Phase { UP, GOING_DOWN, DOWN, GOING_UP }

    private var phase = Phase.UP
    override var repCount = 0
        private set

    private var minAngleSeen = 180f
    private var formScoreAccum = 0f
    private var formSampleCount = 0

    override fun reset() {
        phase = Phase.UP
        repCount = 0
        minAngleSeen = 180f
        formScoreAccum = 0f
        formSampleCount = 0
    }

    override fun process(frame: LandmarkFrame): Analysis {
        if (!frame.isValid) {
            return Analysis(repCount, 0, "GET IN FRAME", poseDetected = false, goodForm = false)
        }

        val elbowAngle = computeAngle(frame.leftShoulder, frame.leftElbow, frame.leftWrist)
        val alignScore = computeAlignmentScore(frame)
        val formScore = computeFormScore(elbowAngle, alignScore)
        val goodForm = formScore >= 60

        if (elbowAngle < minAngleSeen) minAngleSeen = elbowAngle
        formScoreAccum += formScore
        formSampleCount++

        val cue = updatePhase(elbowAngle, formScore, alignScore)

        return Analysis(repCount, formScore.toInt(), cue, poseDetected = true, goodForm = goodForm)
    }

    private fun updatePhase(elbowAngle: Float, formScore: Float, alignScore: Float): String {
        return when (phase) {
            Phase.UP -> {
                if (elbowAngle < downThreshold + 30f) {
                    phase = Phase.GOING_DOWN
                    minAngleSeen = elbowAngle
                }
                if (alignScore < (1f - alignmentTolerance / 40f)) "STRAIGHTEN BODY" else "LOWER"
            }

            Phase.GOING_DOWN -> {
                if (elbowAngle < minAngleSeen) minAngleSeen = elbowAngle
                if (elbowAngle <= downThreshold) {
                    phase = Phase.DOWN
                    "PUSH!"
                } else if (elbowAngle > upThreshold) {
                    phase = Phase.UP
                    "LOWER!"
                } else {
                    "LOWER"
                }
            }

            Phase.DOWN -> {
                if (elbowAngle > downThreshold + 15f) {
                    phase = Phase.GOING_UP
                }
                "PUSH!"
            }

            Phase.GOING_UP -> {
                if (elbowAngle >= upThreshold) {
                    if (minAngleSeen <= downThreshold && formScore >= 50f) {
                        repCount++
                        minAngleSeen = 180f
                        formScoreAccum = 0f
                        formSampleCount = 0
                    }
                    phase = Phase.UP
                    "GOOD"
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