package com.replock.app.ml.pose

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

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
            leftWrist    = landmark(PoseLandmark.LEFT_WRIST),
            rightWrist   = landmark(PoseLandmark.RIGHT_WRIST),
            leftHip     = landmark(PoseLandmark.LEFT_HIP),
            rightHip    = landmark(PoseLandmark.RIGHT_HIP),
            leftKnee    = landmark(PoseLandmark.LEFT_KNEE),
            rightKnee   = landmark(PoseLandmark.RIGHT_KNEE),
            leftAnkle   = landmark(PoseLandmark.LEFT_ANKLE),
            rightAnkle  = landmark(PoseLandmark.RIGHT_ANKLE)
        )
    }
}