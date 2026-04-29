package com.replock.app.ml.pose

class PoseProcessor {
    private val tracker = MotionTracker()
    private val smoother = PoseSmoother(smoothingFactor = 0.4f)

    fun process(frame: LandmarkFrame): LandmarkFrame {
        val updatedJoints = mutableMapOf<String, Joint?>()
        frame.forEach { key, joint ->
            updatedJoints[key] = tracker.update(key, joint)?.joint
        }

        val rawFrame = LandmarkFrame(
            nose = updatedJoints["nos"],
            leftShoulder = updatedJoints["lsh"],
            rightShoulder = updatedJoints["rsh"],
            leftElbow = updatedJoints["lel"],
            rightElbow = updatedJoints["rel"],
            leftWrist = updatedJoints["lwr"],
            rightWrist = updatedJoints["rwr"],
            leftHip = updatedJoints["lhi"],
            rightHip = updatedJoints["rhi"],
            leftKnee = updatedJoints["lkn"],
            rightKnee = updatedJoints["rkn"],
            leftAnkle = updatedJoints["lan"],
            rightAnkle = updatedJoints["ran"],
            timestamp = frame.timestamp
        )

        return smoother.smooth(rawFrame)
    }

    fun reset() {
        tracker.reset()
        smoother.reset()
    }
}
