package com.replock.app.presentation.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replock.app.domain.usecase.CountPushUpUseCase
import com.replock.app.ml.pose.FrameSmoother
import com.replock.app.ml.pose.LandmarkFrame
import com.replock.app.ml.pose.PoseDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExerciseViewModel : ViewModel() {

    private var _poseDetector: PoseDetector? = null
    private val poseDetector: PoseDetector
        get() {
            if (_poseDetector == null) _poseDetector = PoseDetector()
            return _poseDetector!!
        }

    private val countPushUpUseCase by lazy { CountPushUpUseCase() }

    private val frameSmoother = FrameSmoother(alpha = 0.40f, holdFrames = 7)

    private val _repCount     = MutableStateFlow(0)
    val repCount: StateFlow<Int> = _repCount.asStateFlow()

    private val _repState     = MutableStateFlow("WAITING")
    val repState: StateFlow<String> = _repState.asStateFlow()

    private val _currentFrame = MutableStateFlow<LandmarkFrame?>(null)
    val currentFrame: StateFlow<LandmarkFrame?> = _currentFrame.asStateFlow()

    private val _isFormValid  = MutableStateFlow(false)
    val isFormValid: StateFlow<Boolean> = _isFormValid.asStateFlow()

    private val _feedback     = MutableStateFlow("")
    val feedback: StateFlow<String> = _feedback.asStateFlow()

    private val _elapsedSecs  = MutableStateFlow(0L)
    val elapsedSecs: StateFlow<Long> = _elapsedSecs.asStateFlow()

    private val _frameWidth   = MutableStateFlow(1)
    val frameWidth: StateFlow<Int> = _frameWidth.asStateFlow()

    private val _frameHeight  = MutableStateFlow(1)
    val frameHeight: StateFlow<Int> = _frameHeight.asStateFlow()

    private var collectionJob: Job? = null
    private var timerJob:      Job? = null

    val imageAnalysisUseCase: androidx.camera.core.ImageAnalysis
        get() = poseDetector.imageAnalysisUseCase

    fun startSession() {
        countPushUpUseCase.reset()
        frameSmoother.reset()
        _repCount.value    = 0
        _repState.value    = "WAITING"
        _elapsedSecs.value = 0

        collectionJob?.cancel()
        collectionJob = viewModelScope.launch {
            poseDetector.poseFlow.collect { poseResult ->
                val r = countPushUpUseCase.process(poseResult)

                val smoothed = r.frame?.let { frameSmoother.smooth(it) }

                _repCount.value     = r.repCount
                _repState.value     = r.repState.label
                _currentFrame.value = smoothed
                _isFormValid.value  = r.isFormValid
                _feedback.value     = r.feedback
                _frameWidth.value   = r.frameWidth
                _frameHeight.value  = r.frameHeight
            }
        }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                _elapsedSecs.value += 1
            }
        }
    }

    fun stopSession() {
        collectionJob?.cancel()
        timerJob?.cancel()
        collectionJob = null
        timerJob      = null
        _poseDetector?.close()
        _poseDetector = null
        frameSmoother.reset()
    }

    override fun onCleared() {
        super.onCleared()
        stopSession()
    }
}