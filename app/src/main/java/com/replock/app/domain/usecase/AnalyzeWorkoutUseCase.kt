package com.replock.app.domain.usecase

import com.replock.app.domain.model.Analysis
import com.replock.app.domain.model.ExerciseType
import com.replock.app.ml.pose.LandmarkFrame
import com.replock.app.ml.pose.PoseDetector
import com.replock.app.ml.pose.PoseLandmarkMapper
import com.replock.app.ml.pose.PoseProcessor
import com.replock.app.ml.rep.ExerciseCounter
import com.replock.app.ml.rep.PullUpCounter
import com.replock.app.ml.rep.PushUpCounter

class AnalyzeWorkoutUseCase(
    private val exerciseType: ExerciseType,
    private val strictness: Float = exerciseType.strictness,
    private val processor: PoseProcessor = PoseProcessor()
) {

    data class Result(
        val repCount: Int,
        val analysis: Analysis,
        val frame: LandmarkFrame?,
        val frameWidth: Int,
        val frameHeight: Int
    )

    private val counter: ExerciseCounter = when (exerciseType) {
        ExerciseType.PUSH_UP -> PushUpCounter(strictness)
        ExerciseType.PULL_UP -> PullUpCounter(strictness)
    }

    fun process(poseResult: PoseDetector.Result): Result {
        val processedFrame = processor.process(
            PoseLandmarkMapper.map(
                pose = poseResult.pose,
                imageWidth = poseResult.width,
                imageHeight = poseResult.height
            )
        )
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
}
