package com.replock.app.ml.pose

class FrameSmoother(
    private val alpha: Float = 0.40f,
    private val holdFrames: Int = 7
) {

    private data class Tracked(
        val joint: Joint,
        val heldFor: Int = 0
    )

    private val tracked = HashMap<String, Tracked>(24)

    fun smooth(frame: LandmarkFrame): LandmarkFrame {
        val updated = HashMap<String, Tracked>(24)

        fun process(key: String, incoming: Joint?): Joint? {
            val prev = tracked[key]
            return when {
                incoming != null -> {
                    val blended = if (prev != null) {
                        Joint(
                            x                 = lerp(prev.joint.x, incoming.x, alpha),
                            y                 = lerp(prev.joint.y, incoming.y, alpha),
                            z                 = lerp(prev.joint.z, incoming.z, alpha),
                            inFrameLikelihood = incoming.inFrameLikelihood
                        )
                    } else {
                        incoming
                    }
                    updated[key] = Tracked(blended, 0)
                    blended
                }

                prev != null && prev.heldFor < holdFrames -> {
                    val nextHeld = prev.heldFor + 1
                    val decayedConfidence = prev.joint.inFrameLikelihood *
                            (1f - nextHeld.toFloat() / holdFrames)
                    val held = prev.joint.copy(inFrameLikelihood = decayedConfidence)
                    updated[key] = Tracked(held, nextHeld)
                    if (nextHeld <= holdFrames / 2) held else null
                }

                else -> null
            }
        }

        val result = LandmarkFrame(
            leftShoulder  = process("lsh", frame.leftShoulder),
            rightShoulder = process("rsh", frame.rightShoulder),
            leftElbow     = process("lel", frame.leftElbow),
            rightElbow    = process("rel", frame.rightElbow),
            leftWrist     = process("lwr", frame.leftWrist),
            rightWrist    = process("rwr", frame.rightWrist),
            leftHip       = process("lhi", frame.leftHip),
            rightHip      = process("rhi", frame.rightHip),
            leftKnee      = process("lkn", frame.leftKnee),
            rightKnee     = process("rkn", frame.rightKnee),
            leftAnkle     = process("lan", frame.leftAnkle),
            rightAnkle    = process("ran", frame.rightAnkle)
        )

        tracked.clear()
        tracked.putAll(updated)
        return result
    }

    fun reset() = tracked.clear()

    private fun lerp(a: Float, b: Float, t: Float) = a + t * (b - a)
}