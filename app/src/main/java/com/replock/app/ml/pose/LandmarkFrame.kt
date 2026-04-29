package com.replock.app.ml.pose

data class LandmarkFrame(
    val nose: Joint? = null,
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
        get() = hasReliableLeftSide() || hasReliableRightSide()

    companion object {
        const val MIN_LIKELIHOOD = 0.5f
    }

    fun forEach(action: (String, Joint?) -> Unit) {
        action("nos", nose)
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

    private fun hasReliableLeftSide(): Boolean =
        listOfNotNull(leftShoulder, leftElbow, leftWrist, leftHip, leftKnee, leftAnkle)
            .count { it.inFrameLikelihood >= MIN_LIKELIHOOD } >= 4

    private fun hasReliableRightSide(): Boolean =
        listOfNotNull(rightShoulder, rightElbow, rightWrist, rightHip, rightKnee, rightAnkle)
            .count { it.inFrameLikelihood >= MIN_LIKELIHOOD } >= 4
}
