package com.replock.app.ml.pose

data class Joint(
    val x: Float,
    val y: Float,
    val z: Float,
    val inFrameLikelihood: Float
) {

    fun isReliable(threshold: Float = 0.5f): Boolean {
        return inFrameLikelihood >= threshold
    }

    fun distanceTo(other: Joint): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun lerp(to: Joint, alpha: Float): Joint {
        return Joint(
            x = x + alpha * (to.x - x),
            y = y + alpha * (to.y - y),
            z = z + alpha * (to.z - z),
            inFrameLikelihood = to.inFrameLikelihood
        )
    }
}