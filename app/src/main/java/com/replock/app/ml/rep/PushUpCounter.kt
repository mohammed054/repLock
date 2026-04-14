package com.replock.app.ml.rep

import com.replock.app.domain.model.RepPhase
import com.replock.app.domain.model.RepState
import com.replock.app.ml.pose.Joint
import com.replock.app.ml.pose.LandmarkFrame
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

class PushUpCounter {

    companion object {
        private const val DOWN_THRESHOLD = 95f
        private const val UP_THRESHOLD   = 145f

        private const val EMA_ALPHA = 0.30f

        private const val CONFIRM_FRAMES = 3

        private const val DEBOUNCE_MS = 200L

        private const val BODY_MIN_ANGLE = 145.0

        private const val BAD_FORM_FRAMES = 9

        private const val SIDE_VIEW_X_SEP = 0.14f
    }

    private enum class CameraOrientation { FRONT_FACE, SIDE_VIEW, UNKNOWN }

    private var phase             = RepPhase.UP
    private var lastExtreme       = RepPhase.UP

    private var pendingPhase      = RepPhase.UP
    private var pendingFrameCount = 0

    private var lastRepTime       = 0L
    private var smoothedAngle: Float? = null

    private var badFormCounter    = 0

    var repCount = 0
        private set

    data class Analysis(
        val state       : RepState,
        val isFormValid : Boolean,
        val feedback    : String
    )

    fun process(frame: LandmarkFrame): Analysis {
        val rawAngle    = bestElbowAngle(frame)
        val orientation = detectOrientation(frame)
        val isStraight  = isBodyStraight(frame, orientation)

        if (rawAngle != null) {
            smoothedAngle = smoothedAngle
                ?.let { prev -> prev + EMA_ALPHA * (rawAngle - prev) }
                ?: rawAngle
        }

        val angle = smoothedAngle
        val now   = System.currentTimeMillis()

        if (angle != null) {
            val target = classifyAngle(angle)

            if (target == phase) {
                pendingPhase      = phase
                pendingFrameCount = 0
            } else if (target == pendingPhase) {
                pendingFrameCount++
                if (pendingFrameCount >= CONFIRM_FRAMES) {
                    if (target == RepPhase.UP
                        && lastExtreme == RepPhase.DOWN
                        && (now - lastRepTime) > DEBOUNCE_MS
                    ) {
                        repCount++
                        lastRepTime = now
                    }
                    if (target != RepPhase.TRANSITION) {
                        lastExtreme = target
                    }
                    phase             = target
                    pendingPhase      = target
                    pendingFrameCount = 0
                }
            } else {
                pendingPhase      = target
                pendingFrameCount = 1
            }
        }

        if (!isStraight) {
            badFormCounter = (badFormCounter + 1).coerceAtMost(BAD_FORM_FRAMES + 5)
        } else {
            badFormCounter = (badFormCounter - 2).coerceAtLeast(0)
        }
        val formBad     = badFormCounter >= BAD_FORM_FRAMES
        val isFormValid = (angle != null) && !formBad
        val feedback    = buildFeedback(angle, formBad, phase, lastExtreme)

        return Analysis(
            state = RepState(
                phase      = phase,
                confidence = if (angle != null) confidence(phase, angle) else 0f
            ),
            isFormValid = isFormValid,
            feedback    = feedback
        )
    }

    fun reset() {
        repCount          = 0
        phase             = RepPhase.UP
        lastExtreme       = RepPhase.UP
        pendingPhase      = RepPhase.UP
        pendingFrameCount = 0
        lastRepTime       = 0L
        smoothedAngle     = null
        badFormCounter    = 0
    }

    private fun classifyAngle(deg: Float): RepPhase = when {
        deg <= DOWN_THRESHOLD -> RepPhase.DOWN
        deg >= UP_THRESHOLD   -> RepPhase.UP
        else                  -> RepPhase.TRANSITION
    }

    private fun buildFeedback(
        angle      : Float?,
        formBad    : Boolean,
        phase      : RepPhase,
        lastExtreme: RepPhase
    ): String = when {
        angle == null                                       -> "ADJUST CAMERA"
        formBad                                             -> "KEEP BACK STRAIGHT"
        phase == RepPhase.UP && lastExtreme == RepPhase.UP  -> "LOWER YOURSELF"
        phase == RepPhase.DOWN                              -> "PUSH UP"
        else                                               -> "KEEP GOING"
    }

    private fun confidence(phase: RepPhase, deg: Float): Float = when (phase) {
        RepPhase.DOWN       -> ((DOWN_THRESHOLD - deg) / DOWN_THRESHOLD).coerceIn(0f, 1f)
        RepPhase.UP         -> ((deg - UP_THRESHOLD)   / (180f - UP_THRESHOLD)).coerceIn(0f, 1f)
        RepPhase.TRANSITION -> {
            val mid = (DOWN_THRESHOLD + UP_THRESHOLD) / 2f
            (1f - abs(deg - mid) / (mid - DOWN_THRESHOLD)).coerceIn(0f, 1f)
        }
    }

    private fun detectOrientation(frame: LandmarkFrame): CameraOrientation {
        val ls = frame.leftShoulder  ?: return CameraOrientation.UNKNOWN
        val rs = frame.rightShoulder ?: return CameraOrientation.UNKNOWN
        return if (abs(ls.x - rs.x) < SIDE_VIEW_X_SEP)
            CameraOrientation.SIDE_VIEW
        else
            CameraOrientation.FRONT_FACE
    }

    private fun bestElbowAngle(frame: LandmarkFrame): Float? {
        val left  = angle2D(frame.leftShoulder,  frame.leftElbow,  frame.leftWrist)
        val right = angle2D(frame.rightShoulder, frame.rightElbow, frame.rightWrist)
        return when {
            left != null && right != null -> (left + right) / 2f
            left  != null                 -> left
            right != null                 -> right
            else                          -> null
        }
    }

    private fun isBodyStraight(frame: LandmarkFrame, orientation: CameraOrientation): Boolean =
        when (orientation) {
            CameraOrientation.SIDE_VIEW -> {
                val left  = angle2D(frame.leftShoulder,  frame.leftHip,  frame.leftAnkle)
                         ?: angle2D(frame.leftShoulder,  frame.leftHip,  frame.leftKnee)
                val right = angle2D(frame.rightShoulder, frame.rightHip, frame.rightAnkle)
                         ?: angle2D(frame.rightShoulder, frame.rightHip, frame.rightKnee)
                val samples = listOfNotNull(left, right)
                samples.isEmpty() || samples.average() >= BODY_MIN_ANGLE
            }
            CameraOrientation.FRONT_FACE -> {
                val left  = angle3D(frame.leftShoulder,  frame.leftHip,  frame.leftAnkle)
                         ?: angle3D(frame.leftShoulder,  frame.leftHip,  frame.leftKnee)
                val right = angle3D(frame.rightShoulder, frame.rightHip, frame.rightAnkle)
                         ?: angle3D(frame.rightShoulder, frame.rightHip, frame.rightKnee)
                val samples = listOfNotNull(left, right)
                samples.isEmpty() || samples.average() >= (BODY_MIN_ANGLE - 8.0)
            }
            CameraOrientation.UNKNOWN -> true
        }

    private fun angle2D(a: Joint?, b: Joint?, c: Joint?): Float? {
        if (a == null || b == null || c == null) return null
        val v1x = a.x - b.x;  val v1y = a.y - b.y
        val v2x = c.x - b.x;  val v2y = c.y - b.y
        val m1  = sqrt(v1x * v1x + v1y * v1y)
        val m2  = sqrt(v2x * v2x + v2y * v2y)
        if (m1 < 1e-6f || m2 < 1e-6f) return null
        val cos = ((v1x * v2x + v1y * v2y) / (m1 * m2)).toDouble().coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cos)).toFloat()
    }

    private fun angle3D(a: Joint?, b: Joint?, c: Joint?): Float? {
        if (a == null || b == null || c == null) return null
        val v1x = a.x - b.x;  val v1y = a.y - b.y;  val v1z = a.z - b.z
        val v2x = c.x - b.x;  val v2y = c.y - b.y;  val v2z = c.z - b.z
        val m1  = sqrt(v1x * v1x + v1y * v1y + v1z * v1z)
        val m2  = sqrt(v2x * v2x + v2y * v2y + v2z * v2z)
        if (m1 < 1e-6f || m2 < 1e-6f) return null
        val dot = v1x * v2x + v1y * v2y + v1z * v2z
        val cos = (dot / (m1 * m2)).toDouble().coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cos)).toFloat()
    }
}