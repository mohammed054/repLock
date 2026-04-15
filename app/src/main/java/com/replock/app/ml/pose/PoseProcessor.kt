package com.replock.app.ml.pose

class PoseProcessor {

    private val motionTracker = MotionTracker()
    private val smoother = PoseSmoother()

    fun process(frame: LandmarkFrame): LandmarkFrame {

        fun handle(key: String, joint: Joint?): Joint? {
            val motion = motionTracker.update(key, joint)
            return smoother.smooth(key, motion)
        }

        return LandmarkFrame(
            leftShoulder  = handle("lsh", frame.leftShoulder),
            rightShoulder = handle("rsh", frame.rightShoulder),
            leftElbow     = handle("lel", frame.leftElbow),
            rightElbow    = handle("rel", frame.rightElbow),
            leftWrist     = handle("lwr", frame.leftWrist),
            rightWrist    = handle("rwr", frame.rightWrist),
            leftHip       = handle("lhi", frame.leftHip),
            rightHip      = handle("rhi", frame.rightHip),
            leftKnee      = handle("lkn", frame.leftKnee),
            rightKnee     = handle("rkn", frame.rightKnee),
            leftAnkle     = handle("lan", frame.leftAnkle),
            rightAnkle    = handle("ran", frame.rightAnkle)
        )
    }

    fun reset() {
        motionTracker.reset()
        smoother.reset()
    }
}