package com.replock.app.presentation.exercise

import androidx.camera.core.CameraSelector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replock.app.data.Difficulty
import com.replock.app.domain.usecase.CountPushUpUseCase
import com.replock.app.ml.pose.LandmarkFrame
import com.replock.app.ml.pose.PoseDetector
import com.replock.app.system.audio.SoundManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExerciseViewModel(
    private val difficulty: Difficulty = Difficulty.MEDIUM
) : ViewModel() {

    private var _poseDetector: PoseDetector? = null
    private val poseDetector: PoseDetector
        get() {
            if (_poseDetector == null) _poseDetector = PoseDetector()
            return _poseDetector!!
        }

    private val countPushUpUseCase by lazy {
        CountPushUpUseCase(difficulty = difficulty)
    }

    private val soundManager = SoundManager()

    /** Set externally from the UI layer via the sound preference. */
    var soundEnabled: Boolean = true

    private val _repCount = MutableStateFlow(0)
    val repCount: StateFlow<Int> = _repCount.asStateFlow()

    private val _formScore = MutableStateFlow(0)
    val formScore: StateFlow<Int> = _formScore.asStateFlow()

    private val _feedback = MutableStateFlow("")
    val feedback: StateFlow<String> = _feedback.asStateFlow()

    private val _repState = MutableStateFlow("WAITING")
    val repState: StateFlow<String> = _repState.asStateFlow()

    private val _currentFrame = MutableStateFlow<LandmarkFrame?>(null)
    val currentFrame: StateFlow<LandmarkFrame?> = _currentFrame.asStateFlow()

    private val _isFormValid = MutableStateFlow(false)
    val isFormValid: StateFlow<Boolean> = _isFormValid.asStateFlow()

    private val _poseDetected = MutableStateFlow(false)
    val poseDetected: StateFlow<Boolean> = _poseDetected.asStateFlow()

    private val _elapsedSecs = MutableStateFlow(0L)
    val elapsedSecs: StateFlow<Long> = _elapsedSecs.asStateFlow()

    private val _frameWidth = MutableStateFlow(1)
    val frameWidth: StateFlow<Int> = _frameWidth.asStateFlow()

    private val _frameHeight = MutableStateFlow(1)
    val frameHeight: StateFlow<Int> = _frameHeight.asStateFlow()

    private val _isDebugMode = MutableStateFlow(false)
    val isDebugMode: StateFlow<Boolean> = _isDebugMode.asStateFlow()

    private val _trackingQuality = MutableStateFlow(0f)
    val trackingQuality: StateFlow<Float> = _trackingQuality.asStateFlow()

    private val _cameraFacing = MutableStateFlow(CameraSelector.DEFAULT_FRONT_CAMERA)
    val cameraFacing: StateFlow<CameraSelector> = _cameraFacing.asStateFlow()

    private val _isLandscape = MutableStateFlow(false)
    val isLandscape: StateFlow<Boolean> = _isLandscape.asStateFlow()

    /**
     * True once the very first raw camera frame arrives after [startSession].
     * The UI uses this to drop the "TAP START TO ACTIVATE" placeholder (Phase 4.4).
     */
    private val _isCameraReady = MutableStateFlow(false)
    val isCameraReady: StateFlow<Boolean> = _isCameraReady.asStateFlow()

    private var collectionJob: Job? = null
    private var cameraReadyJob: Job? = null
    private var timerJob: Job? = null

    val imageAnalysisUseCase: androidx.camera.core.ImageAnalysis
        get() = poseDetector.imageAnalysisUseCase

    fun flipCamera() {
        _cameraFacing.value = if (_cameraFacing.value == CameraSelector.DEFAULT_FRONT_CAMERA)
            CameraSelector.DEFAULT_BACK_CAMERA
        else
            CameraSelector.DEFAULT_FRONT_CAMERA
    }

    fun toggleOrientation() {
        _isLandscape.value = !_isLandscape.value
    }

    fun toggleDebugMode() {
        _isDebugMode.value = !_isDebugMode.value
    }

fun startSession() {
        countPushUpUseCase.reset()
        _repCount.value    = 0
        _formScore.value   = 0
        _feedback.value    = ""
        _repState.value    = "WAITING"
        _elapsedSecs.value = 0
        _isCameraReady.value = false   // hide placeholder until first real frame

        // Subscribe to the one-shot camera-ready signal (Phase 4.4).
        cameraReadyJob?.cancel()
        cameraReadyJob = viewModelScope.launch {
            poseDetector.cameraReadyFlow.collect {
                _isCameraReady.value = true
            }
        }

        collectionJob?.cancel()
        collectionJob = viewModelScope.launch {
            poseDetector.poseFlow.collect { poseResult ->
                val r = countPushUpUseCase.process(poseResult)

                val previousRepCount = _repCount.value
                _repCount.value = r.repCount

                if (r.repCount > previousRepCount && soundEnabled) {
                    soundManager.playRepComplete()
                }

                _formScore.value = r.analysis.formScore
                _feedback.value = r.analysis.feedback
                _repState.value = r.analysis.phase
                _currentFrame.value = r.frame
                _isFormValid.value = r.analysis.goodForm
                _poseDetected.value = r.analysis.poseDetected
                _frameWidth.value = r.frameWidth
                _frameHeight.value = r.frameHeight

                _trackingQuality.value = r.frame?.let { frame ->
                    val joints = listOfNotNull(
                        frame.leftShoulder, frame.rightShoulder,
                        frame.leftElbow, frame.rightElbow,
                        frame.leftWrist, frame.rightWrist,
                        frame.leftHip, frame.rightHip,
                        frame.leftKnee, frame.rightKnee,
                        frame.leftAnkle, frame.rightAnkle
                    )
                    if (joints.isEmpty()) 0f
                    else joints.map { it.inFrameLikelihood }.average().toFloat()
                } ?: 0f
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
        cameraReadyJob?.cancel()
        timerJob?.cancel()
        collectionJob  = null
        cameraReadyJob = null
        timerJob       = null
        soundManager.release()
        _poseDetector?.close()
        _poseDetector  = null
        _isCameraReady.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopSession()
    }
}
