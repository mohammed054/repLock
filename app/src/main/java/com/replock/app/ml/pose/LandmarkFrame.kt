package com.replock.app.ml.pose

data class LandmarkFrame(
    val leftShoulder: Joint?,
    val rightShoulder: Joint?,
    val leftElbow: Joint?,
    val rightElbow: Joint?,
    val leftWrist: Joint?,
    val rightWrist: Joint?,
    val leftHip: Joint?,
    val rightHip: Joint?,
    val leftKnee: Joint?,
    val rightKnee: Joint?,
    val leftAnkle: Joint?,
    val rightAnkle: Joint?,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isValid: Boolean
        get() = listOfNotNull(
            leftShoulder, leftElbow, leftWrist, leftHip, leftAnkle
        ).all { it.inFrameLikelihood >= MIN_LIKELIHOOD }

    companion object {
        const val MIN_LIKELIHOOD = 0.5f
    }

    fun forEach(action: (String, Joint?) -> Unit) {
        action("lsh", leftShoulder)
        action("rsh", rightShoulder)
        action("lel", leftElbow)
        action("rel", rightElbow)
        action("lwr", leftWrist)
        action("rwr", rightWrist)
        action("lhi", leftHip)
        action("rhi", rightHip)
        action("lkn", leftKnee)
        action("rkn", rightKnee)
        action("lan", leftAnkle)
        action("ran", rightAnkle)
    }
}