package com.replock.app.ml.rep

import com.replock.app.domain.model.Analysis
import com.replock.app.ml.pose.LandmarkFrame

interface ExerciseCounter {
    val repCount: Int
    fun process(frame: LandmarkFrame): Analysis
    fun reset()
}