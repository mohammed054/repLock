package com.replock.app.ml.pose

class PoseProcessor {
    private val tracker = MotionTracker()

    fun process(frame: LandmarkFrame): LandmarkFrame {
        var lsh: Joint? = null
        var rsh: Joint? = null
        var lel: Joint? = null
        var rel: Joint? = null
        var lwr: Joint? = null
        var rwr: Joint? = null
        var lhi: Joint? = null
        var rhi: Joint? = null
        var lkn: Joint? = null
        var rkn: Joint? = null
        var lan: Joint? = null
        var ran: Joint? = null

        frame.forEach { key, joint ->
            val updated = tracker.update(key, joint)?.joint
            when (key) {
                "lsh" -> lsh = updated
                "rsh" -> rsh = updated
                "lel" -> lel = updated
                "rel" -> rel = updated
                "lwr" -> lwr = updated
                "rwr" -> rwr = updated
                "lhi" -> lhi = updated
                "rhi" -> rhi = updated
                "lkn" -> lkn = updated
                "rkn" -> rkn = updated
                "lan" -> lan = updated
                "ran" -> ran = updated
            }
        }

        return LandmarkFrame(
            leftShoulder = lsh,
            rightShoulder = rsh,
            leftElbow = lel,
            rightElbow = rel,
            leftWrist = lwr,
            rightWrist = rwr,
            leftHip = lhi,
            rightHip = rhi,
            leftKnee = lkn,
            rightKnee = rkn,
            leftAnkle = lan,
            rightAnkle = ran,
            timestamp = frame.timestamp
        )
    }

    fun reset() {
        tracker.reset()
    }
}
