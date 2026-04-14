package com.replock.app.domain.usecase

import com.replock.app.domain.model.RepState
import com.replock.app.ml.pose.Joint
import com.replock.app.ml.pose.LandmarkFrame
import com.replock.app.ml.pose.PoseDetector
import com.replock.app.ml.pose.PoseLandmarkMapper
import com.replock.app.ml.rep.PushUpCounter

class CountPushUpUseCase(
    private val counter: PushUpCounter = PushUpCounter()
) {

    data class Result(
        val repCount    : Int,
        val repState    : RepState,
        val frame       : LandmarkFrame? = null,
        val isFormValid : Boolean = false,
        val feedback    : String = "",
        // ── NEW: propagated so SkeletonOverlay can compute the FILL_CENTER crop ──
        val frameWidth  : Int = 1,
        val frameHeight : Int = 1
    )

    fun process(poseResult: PoseDetector.Result): Result {
        val frame = PoseLandmarkMapper.map(poseResult.pose)

        // mirrorX = true because PreviewView automatically mirrors front-camera
        // output for the user, but MLKit processes the raw (non-mirrored) frame.
        // Without flipping x here, every skeleton joint appears on the wrong side.
        val scaledFrame = scaleFrame(
            frame   = frame,
            width   = poseResult.width,
            height  = poseResult.height,
            mirrorX = true
        )

        val analysis = counter.process(scaledFrame)
        return Result(
            repCount    = counter.repCount,
            repState    = analysis.state,
            frame       = scaledFrame,
            isFormValid = analysis.isFormValid,
            feedback    = analysis.feedback,
            frameWidth  = poseResult.width,
            frameHeight = poseResult.height
        )
    }

    fun reset() = counter.reset()

    // ─────────────────────────────────────────────────────────────────────────

    private fun scaleFrame(
        frame: LandmarkFrame,
        width: Int,
        height: Int,
        mirrorX: Boolean
    ): LandmarkFrame {
        fun scale(j: Joint?): Joint? {
            j ?: return null
            return j.copy(
                // Normalize to [0,1].  When mirrorX=true, flip: 0→1, 1→0
                // so that left/right matches what the user sees in the mirrored preview.
                x = if (mirrorX) 1f - (j.x / width) else j.x / width,
                y = j.y / height
            )
        }
        return LandmarkFrame(
            leftShoulder  = scale(frame.leftShoulder),
            rightShoulder = scale(frame.rightShoulder),
            leftElbow     = scale(frame.leftElbow),
            rightElbow    = scale(frame.rightElbow),
            leftWrist     = scale(frame.leftWrist),
            rightWrist    = scale(frame.rightWrist),
            leftHip       = scale(frame.leftHip),
            rightHip      = scale(frame.rightHip),
            leftKnee      = scale(frame.leftKnee),
            rightKnee     = scale(frame.rightKnee),
            leftAnkle     = scale(frame.leftAnkle),
            rightAnkle    = scale(frame.rightAnkle)
        )
    }
}
