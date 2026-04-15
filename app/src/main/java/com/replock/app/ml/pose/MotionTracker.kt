package com.replock.app.ml.pose

class MotionTracker(
    private val maxMissingFrames: Int = 10,
    private val velocityDamping: Float = 0.9f
) {

    data class MotionJoint(
        val joint: Joint,
        val vx: Float,
        val vy: Float,
        val vz: Float,
        val missingFrames: Int = 0
    )

    private val tracked = HashMap<String, MotionJoint>()

    fun update(key: String, incoming: Joint?): MotionJoint? {
        val prev = tracked[key]

        val result = when {
            incoming != null -> {
                val vx = if (prev != null) (incoming.x - prev.joint.x) * velocityDamping else 0f
                val vy = if (prev != null) (incoming.y - prev.joint.y) * velocityDamping else 0f
                val vz = if (prev != null) (incoming.z - prev.joint.z) * velocityDamping else 0f

                MotionJoint(incoming, vx, vy, vz, 0)
            }

            prev != null && prev.missingFrames < maxMissingFrames -> {
                val predicted = Joint(
                    x = prev.joint.x + prev.vx,
                    y = prev.joint.y + prev.vy,
                    z = prev.joint.z + prev.vz,
                    inFrameLikelihood = prev.joint.inFrameLikelihood * 0.75f
                )

                MotionJoint(
                    joint = predicted,
                    vx = prev.vx * velocityDamping,
                    vy = prev.vy * velocityDamping,
                    vz = prev.vz * velocityDamping,
                    missingFrames = prev.missingFrames + 1
                )
            }

            else -> null
        }

        if (result != null) tracked[key] = result else tracked.remove(key)
        return result
    }

    fun reset() = tracked.clear()
}