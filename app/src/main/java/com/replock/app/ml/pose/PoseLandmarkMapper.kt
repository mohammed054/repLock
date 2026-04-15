package com.replock.app.ml.pose

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

object PoseLandmarkMapper {

    private const val MIN_CONFIDENCE = 0.5f

    private fun mapLandmark(pose: Pose, type: Int): Joint? {
        val lm = pose.getPoseLandmark(type) ?: return null
        if (lm.inFrameLikelihood < MIN_CONFIDENCE) return null

        return Joint(
            x = lm.position3D.x,
            y = lm.position3D.y,
            z = lm.position3D.z,
            inFrameLikelihood = lm.inFrameLikelihood
        )
    }

    fun map(pose: Pose): LandmarkFrame {
        return LandmarkFrame(
            leftShoulder  = mapLandmark(pose, PoseLandmark.LEFT_SHOULDER),
            rightShoulder = mapLandmark(pose, PoseLandmark.RIGHT_SHOULDER),
            leftElbow     = mapLandmark(pose, PoseLandmark.LEFT_ELBOW),
            rightElbow    = mapLandmark(pose, PoseLandmark.RIGHT_ELBOW),
            leftWrist     = mapLandmark(pose, PoseLandmark.LEFT_WRIST),
            rightWrist    = mapLandmark(pose, PoseLandmark.RIGHT_WRIST),
            leftHip       = mapLandmark(pose, PoseLandmark.LEFT_HIP),
            rightHip      = mapLandmark(pose, PoseLandmark.RIGHT_HIP),
            leftKnee      = mapLandmark(pose, PoseLandmark.LEFT_KNEE),
            rightKnee     = mapLandmark(pose, PoseLandmark.RIGHT_KNEE),
            leftAnkle     = mapLandmark(pose, PoseLandmark.LEFT_ANKLE),
            rightAnkle    = mapLandmark(pose, PoseLandmark.RIGHT_ANKLE)
        )
    }
}