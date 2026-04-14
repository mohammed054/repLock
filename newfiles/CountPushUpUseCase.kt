package com.replock.app.domain.usecase

import com.replock.app.domain.model.RepState
import com.replock.app.ml.pose.PoseDetector
import com.replock.app.ml.pose.PoseLandmarkMapper
import com.replock.app.ml.rep.PushUpCounter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Domain use-case that wires the full push-up detection pipeline:
 *
 *   [PoseDetector] (camera frames → ML Kit Pose)
 *       ↓
 *   [PoseLandmarkMapper] (Pose → LandmarkFrame)
 *       ↓
 *   [PushUpCounter] (LandmarkFrame → rep count + RepState)
 *
 * Collect [results] from a ViewModel scope; each emission carries the
 * cumulative [Result.repCount] and the instantaneous [Result.repState].
 */
class CountPushUpUseCase(
    private val poseDetector : PoseDetector,
    private val counter      : PushUpCounter = PushUpCounter()
) {

    data class Result(
        val repCount : Int,
        val repState : RepState
    )

    /**
     * Hot flow of detection results, one per camera frame that yields a
     * detected pose.  Backed by the CONFLATED channel inside [PoseDetector],
     * so slow collectors always see the latest result without back-pressure.
     */
    val results: Flow<Result> = poseDetector.poseFlow
        .map { pose ->
            val frame = PoseLandmarkMapper.map(pose)
            val state = counter.process(frame)
            Result(
                repCount = counter.repCount,
                repState = state
            )
        }

    /**
     * Resets the rep count and phase state.
     * Call before starting a new session — the [PoseDetector] channel stays
     * open; only the counter is zeroed.
     */
    fun reset() = counter.reset()
}
