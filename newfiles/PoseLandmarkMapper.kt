package com.replock.app.ml.pose

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

// ─── Domain types ────────────────────────────────────────────────────────────

/**
 * A single 3-D joint position with a detection-confidence score.
 *
 * Coordinates are in image-pixel space as returned by ML Kit.
 * [inFrameLikelihood] is ML Kit's own 0..1 probability that this
 * landmark is visible in the frame.
 */
data class Joint(
    val x                 : Float,
    val y                 : Float,
    val z                 : Float,
    val inFrameLikelihood : Float
)

/**
 * The eight landmarks we care about, extracted from a single pose frame.
 * Any field is `null` if the landmark was not visible or below the
 * minimum-confidence threshold.
 */
data class LandmarkFrame(
    val leftShoulder  : Joint?,
    val rightShoulder : Joint?,
    val leftElbow     : Joint?,
    val rightElbow    : Joint?,
    val leftWrist     : Joint?,
    val rightWrist    : Joint?,
    val leftHip       : Joint?,
    val rightHip      : Joint?
)

// ─── Mapper ──────────────────────────────────────────────────────────────────

/**
 * Converts an ML Kit [Pose] result into a [LandmarkFrame].
 *
 * A landmark is included only if its `inFrameLikelihood` exceeds [minConfidence].
 * Lower thresholds catch more landmarks but introduce more noise; the default 0.5
 * is a good starting point for stream-mode pose detection.
 */
object PoseLandmarkMapper {

    private const val DEFAULT_MIN_CONFIDENCE = 0.5f

    fun map(pose: Pose, minConfidence: Float = DEFAULT_MIN_CONFIDENCE): LandmarkFrame {
        fun landmark(type: Int): Joint? {
            val lm = pose.getPoseLandmark(type) ?: return null
            if (lm.inFrameLikelihood < minConfidence) return null
            return Joint(
                x                 = lm.position3D.x,
                y                 = lm.position3D.y,
                z                 = lm.position3D.z,
                inFrameLikelihood = lm.inFrameLikelihood
            )
        }

        return LandmarkFrame(
            leftShoulder  = landmark(PoseLandmark.LEFT_SHOULDER),
            rightShoulder = landmark(PoseLandmark.RIGHT_SHOULDER),
            leftElbow     = landmark(PoseLandmark.LEFT_ELBOW),
            rightElbow    = landmark(PoseLandmark.RIGHT_ELBOW),
            leftWrist     = landmark(PoseLandmark.LEFT_WRIST),
            rightWrist    = landmark(PoseLandmark.RIGHT_WRIST),
            leftHip       = landmark(PoseLandmark.LEFT_HIP),
            rightHip      = landmark(PoseLandmark.RIGHT_HIP)
        )
    }
}
