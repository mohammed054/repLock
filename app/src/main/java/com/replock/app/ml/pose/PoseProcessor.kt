package com.replock.app.ml.pose

class PoseProcessor {
    private val tracker = MotionTracker()
    private val smoother = PoseSmoother(smoothingFactor = 0.4f)

    fun process(frame: LandmarkFrame): LandmarkFrame {
        // Collect updated joints into a map for cleaner processing
        val updatedJoints = frame.associate { (key, joint) ->
            key to tracker.update(key, joint)?.joint
        }

        // Construct the frame using the map
        val rawFrame = LandmarkFrame(
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