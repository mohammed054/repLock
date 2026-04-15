package com.replock.app.ml.pose

class PoseSmoother(private val smoothingFactor: Float = 0.3f) {

    private val previousPositions = HashMap<String, Joint>()

    fun smooth(frame: LandmarkFrame): LandmarkFrame {
        fun smoothJoint(key: String, joint: Joint?): Joint? {
            if (joint == null) return null
            val prev = previousPositions[key]
            return if (prev != null) {
                val smoothed = Joint(
                    x = prev.x + smoothingFactor * (joint.x - prev.x),
                    y = prev.y + smoothingFactor * (joint.y - prev.y),
                    z = prev.z + smoothingFactor * (joint.z - prev.z),
                    inFrameLikelihood = joint.inFrameLikelihood
                )
                previousPositions[key] = smoothed
                smoothed
            } else {
                previousPositions[key] = joint
                joint
            }
        }

        return LandmarkFrame(
            leftShoulder = smoothJoint("lsh", frame.leftShoulder),
            rightShoulder = smoothJoint("rsh", frame.rightShoulder),
            leftElbow = smoothJoint("lel", frame.leftElbow),
            rightElbow = smoothJoint("rel", frame.rightElbow),
            leftWrist = smoothJoint("lwr", frame.leftWrist),
            rightWrist = smoothJoint("rwr", frame.rightWrist),
            leftHip = smoothJoint("lhi", frame.leftHip),
            rightHip = smoothJoint("rhi", frame.rightHip),
            leftKnee = smoothJoint("lkn", frame.leftKnee),
            rightKnee = smoothJoint("rkn", frame.rightKnee),
            leftAnkle = smoothJoint("lan", frame.leftAnkle),
            rightAnkle = smoothJoint("ran", frame.rightAnkle),
            timestamp = frame.timestamp
        )
    }

    fun reset() {
        previousPositions.clear()
    }
}