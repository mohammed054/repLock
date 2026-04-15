package com.replock.app.ml.pose

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

object PoseLandmarkMapper {

    private const val MIN_CONFIDENCE = 0.5f

    fun map(pose: Pose, imageWidth: Int = 1, imageHeight: Int = 1): LandmarkFrame {
        val w = imageWidth.toFloat().coerceAtLeast(1f)
        val h = imageHeight.toFloat().coerceAtLeast(1f)

        fun mapLandmark(type: Int): Joint? {
            val lm = pose.getPoseLandmark(type) ?: return null
            if (lm.inFrameLikelihood < MIN_CONFIDENCE) return null

            return Joint(
                x = lm.position.x / w,
                y = lm.position.y / h,
                z = lm.position3D.z,
                inFrameLikelihood = lm.inFrameLikelihood
            )
        }

        return LandmarkFrame(
            leftShoulder  = mapLandmark(PoseLandmark.LEFT_SHOULDER),
            rightShoulder = mapLandmark(PoseLandmark.RIGHT_SHOULDER),
            leftElbow     = mapLandmark(PoseLandmark.LEFT_ELBOW),
            rightElbow    = mapLandmark(PoseLandmark.RIGHT_ELBOW),
            leftWrist     = mapLandmark(PoseLandmark.LEFT_WRIST),
            rightWrist    = mapLandmark(PoseLandmark.RIGHT_WRIST),
            leftHip       = mapLandmark(PoseLandmark.LEFT_HIP),
            rightHip      = mapLandmark(PoseLandmark.RIGHT_HIP),
            leftKnee      = mapLandmark(PoseLandmark.LEFT_KNEE),
            rightKnee     = mapLandmark(PoseLandmark.RIGHT_KNEE),
            leftAnkle     = mapLandmark(PoseLandmark.LEFT_ANKLE),
            rightAnkle    = mapLandmark(PoseLandmark.RIGHT_ANKLE)
        )
    }
}