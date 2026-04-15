package com.replock.app.domain.usecase

import com.replock.app.domain.model.Analysis
import com.replock.app.data.Difficulty
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
        val rawFrame = PoseLandmarkMapper.map(
            poseResult.pose,
            poseResult.width,
            poseResult.height
        )

        val normalized = normalizeFrame(
            frame = rawFrame,
            width = poseResult.width,
            height = poseResult.height,
            mirrorX = true
        )

        val processedFrame = processor.process(normalized)
        val analysis = counter.process(processedFrame)

        return Result(
            repCount = counter.repCount,
            analysis = analysis,
            frame = processedFrame,
            frameWidth = poseResult.width,
            frameHeight = poseResult.height
        )
    }

    fun reset() {
        counter.reset()
        processor.reset()
    }

    private fun normalizeFrame(
        frame: LandmarkFrame,
        width: Int,
        height: Int,
        mirrorX: Boolean
    ): LandmarkFrame {
        val safeWidth = if (width == 0) 1f else width.toFloat()
        val safeHeight = if (height == 0) 1f else height.toFloat()

        fun normalize(j: com.replock.app.ml.pose.Joint?): com.replock.app.ml.pose.Joint? {
            j ?: return null
            val nx = j.x / safeWidth
            val ny = j.y / safeHeight
            return j.copy(
                x = if (mirrorX) 1f - nx else nx,
                y = ny
            )
        }

        return LandmarkFrame(
            leftShoulder  = normalize(frame.leftShoulder),
            rightShoulder = normalize(frame.rightShoulder),
            leftElbow     = normalize(frame.leftElbow),
            rightElbow    = normalize(frame.rightElbow),
            leftWrist     = normalize(frame.leftWrist),
            rightWrist    = normalize(frame.rightWrist),
            leftHip       = normalize(frame.leftHip),
            rightHip      = normalize(frame.rightHip),
            leftKnee      = normalize(frame.leftKnee),
            rightKnee     = normalize(frame.rightKnee),
            leftAnkle     = normalize(frame.leftAnkle),
            rightAnkle    = normalize(frame.rightAnkle)
        )
    }
}