package com.replock.app.ml.pose

data class Joint(
    val x                 : Float,
    val y                 : Float,
    val z                 : Float,
    val inFrameLikelihood : Float
)