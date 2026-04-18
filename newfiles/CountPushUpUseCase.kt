package com.replock.app.domain.usecase

import com.replock.app.domain.model.Analysis
import com.replock.app.data.Difficulty
import com.replock.app.ml.pose.Joint
import com.replock.app.ml.pose.LandmarkFrame
import com.replock.app.ml.pose.PoseDetector
import com.replock.app.ml.pose.PoseLandmarkMapper
import com.replock.app.ml.pose.PoseProcessor
import com.replock.app.ml.rep.PushUpCounter

class CountPushUpUseCase(
    private val difficulty: Difficulty = Difficulty.MEDIUM,
    private val counter: PushUpCounter = PushUpCounter(difficulty.strictness),
    private val processor: PoseProcessor = PoseProcessor()
) {

    data class Result(
        val repCount: Int,
        val analysis: Analysis,
        val frame: LandmarkFrame?,
        val frameWidth: Int,
        val frameHeight: Int
    )

    fun process(poseResult: PoseDetector.Result): Result {
        // PoseLandmarkMapper.map() already normalises pixel coords → [0,1].
        // Do NOT divide by width/height again here (that was the previous bug).
        val rawFrame = PoseLandmarkMapper.map(
            poseResult.pose,
            poseResult.width,
            poseResult.height
        )

        // Only apply the mirror-X flip for front-camera orientation.
        val mirrored = mirrorFrame(rawFrame, mirrorX = true)

        val processedFrame = processor.process(mirrored)
        val analysis       = counter.process(processedFrame)

        return Result(
            repCount    = counter.repCount,
            analysis    = analysis,
            frame       = processedFrame,
            frameWidth  = poseResult.width,
            frameHeight = poseResult.height
        )
    }

    fun reset() {
        counter.reset()
        processor.reset()
    }

    /**
     * Applies only the horizontal mirror needed for the front camera.
     * Coordinates are already in [0,1]; we must NOT re-divide by frame dimensions.
     */
    private fun mirrorFrame(frame: LandmarkFrame, mirrorX: Boolean): LandmarkFrame {
        fun flip(j: Joint?): Joint? {
            j ?: return null
            return if (mirrorX) j.copy(x = 1f - j.x) else j
        }
        return LandmarkFrame(
            leftShoulder  = flip(frame.leftShoulder),
            rightShoulder = flip(frame.rightShoulder),
            leftElbow     = flip(frame.leftElbow),
            rightElbow    = flip(frame.rightElbow),
            leftWrist     = flip(frame.leftWrist),
            rightWrist    = flip(frame.rightWrist),
            leftHip       = flip(frame.leftHip),
            rightHip      = flip(frame.rightHip),
            leftKnee      = flip(frame.leftKnee),
            rightKnee     = flip(frame.rightKnee),
            leftAnkle     = flip(frame.leftAnkle),
            rightAnkle    = flip(frame.rightAnkle)
        )
    }
}
