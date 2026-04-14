package com.replock.app.ml.pose

data class LandmarkFrame(
    val leftShoulder  : Joint?,
    val rightShoulder : Joint?,
    val leftElbow     : Joint?,
    val rightElbow    : Joint?,
    val leftWrist     : Joint?,
    val rightWrist    : Joint?,
    val leftHip       : Joint?,
    val rightHip      : Joint?,
    val leftKnee      : Joint?,
    val rightKnee     : Joint?,
    val leftAnkle     : Joint?,
    val rightAnkle    : Joint?
)
