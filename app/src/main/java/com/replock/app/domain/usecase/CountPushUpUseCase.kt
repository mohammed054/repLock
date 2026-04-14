package com.replock.app.domain.usecase

import com.replock.app.domain.model.RepState
import com.replock.app.ml.pose.PoseDetector
import com.replock.app.ml.pose.PoseLandmarkMapper
import com.replock.app.ml.rep.PushUpCounter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CountPushUpUseCase(
    private val poseDetector : PoseDetector,
    private val counter      : PushUpCounter = PushUpCounter()
) {

    data class Result(
        val repCount : Int,
        val repState : RepState,
        val frame    : LandmarkFrame? = null
    )

    val results: Flow<Result> = poseDetector.poseFlow
        .map { pose ->
            val frame = PoseLandmarkMapper.map(pose)
            val state = counter.process(frame)
            Result(
                repCount = counter.repCount,
                repState = state,
                frame    = frame
            )
        }

    fun reset() = counter.reset()
}