package com.replock.app.domain.usecase

import com.replock.app.domain.model.RepState
import com.replock.app.ml.pose.Joint
import com.replock.app.ml.pose.LandmarkFrame
import com.replock.app.ml.pose.PoseDetector
import com.replock.app.ml.pose.PoseLandmarkMapper
import com.replock.app.ml.rep.PushUpCounter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CountPushUpUseCase(
    private val counter: PushUpCounter = PushUpCounter()
) {

    data class Result(
        val repCount    : Int,
        val repState    : RepState,
        val frame       : LandmarkFrame? = null,
        val isFormValid : Boolean = false,
        val feedback    : String = ""
    )

    fun process(poseResult: PoseDetector.Result): Result {
        val frame = PoseLandmarkMapper.map(poseResult.pose)
        val scaledFrame = scaleFrame(frame, poseResult.width, poseResult.height)
        val analysis = counter.process(scaledFrame)
        return Result(
            repCount    = counter.repCount,
            repState    = analysis.state,
            frame       = scaledFrame,
            isFormValid = analysis.isFormValid,
            feedback    = analysis.feedback
        )
    }

    private fun scaleFrame(frame: LandmarkFrame, width: Int, height: Int): LandmarkFrame {
        fun scale(joint: Joint?): Joint? {
            if (joint == null) return null
            return joint.copy(
                x = (joint.x / width),
                y = (joint.y / height)
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

    fun reset() = counter.reset()
}
