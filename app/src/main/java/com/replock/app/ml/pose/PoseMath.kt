package com.replock.app.ml.pose

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

object PoseMath {

    data class BodySide(
        val shoulder: Joint?,
        val elbow: Joint?,
        val wrist: Joint?,
        val hip: Joint?,
        val knee: Joint?,
        val ankle: Joint?
    ) {
        val confidence: Float
            get() = averageLikelihood(shoulder, elbow, wrist, hip, knee, ankle)
    }

    fun strongestSide(frame: LandmarkFrame): BodySide {
        val left = BodySide(
            shoulder = frame.leftShoulder,
            elbow = frame.leftElbow,
            wrist = frame.leftWrist,
            hip = frame.leftHip,
            knee = frame.leftKnee,
            ankle = frame.leftAnkle
        )
        val right = BodySide(
            shoulder = frame.rightShoulder,
            elbow = frame.rightElbow,
            wrist = frame.rightWrist,
            hip = frame.rightHip,
            knee = frame.rightKnee,
            ankle = frame.rightAnkle
        )
        return if (left.confidence >= right.confidence) left else right
    }

    fun averageLikelihood(vararg joints: Joint?): Float {
        val available = joints.filterNotNull()
        if (available.isEmpty()) return 0f
        return available.map { it.inFrameLikelihood }.average().toFloat()
    }

    fun averageY(vararg joints: Joint?): Float? {
        val available = joints.filterNotNull()
        if (available.isEmpty()) return null
        return available.map { it.y }.average().toFloat()
    }

    fun angle(a: Joint?, b: Joint?, c: Joint?): Float {
        if (a == null || b == null || c == null) return 180f

        val abX = a.x - b.x
        val abY = a.y - b.y
        val cbX = c.x - b.x
        val cbY = c.y - b.y
        val dot = abX * cbX + abY * cbY
        val magA = sqrt((abX * abX + abY * abY).toDouble())
        val magC = sqrt((cbX * cbX + cbY * cbY).toDouble())
        if (magA < 1e-6 || magC < 1e-6) return 180f

        return Math.toDegrees(
            acos((dot / (magA * magC)).coerceIn(-1.0, 1.0))
        ).toFloat()
    }

    fun bodyLineScore(bodyAngle: Float, tolerance: Float): Float {
        val deviation = abs(bodyAngle - 180f)
        return (1f - deviation / tolerance).coerceIn(0f, 1f)
    }
}
