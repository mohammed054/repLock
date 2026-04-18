package com.replock.app.presentation.exercise

import android.app.Application
import androidx.camera.core.CameraSelector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.replock.app.data.Difficulty
import com.replock.app.data.local.AppDatabase
import com.replock.app.data.repository.ProgressRepository
import com.replock.app.domain.model.SessionState
import com.replock.app.domain.usecase.CountPushUpUseCase
import com.replock.app.ml.pose.LandmarkFrame
import com.replock.app.ml.pose.PoseDetector
import com.replock.app.system.audio.SoundManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── Timing constants ──────────────────────────────────────────────────────────
private const val POSE_HOLD_FOR_READY_MS    = 1_500L   // 1.5 s of valid pose → READY
private const val POSE_LOST_FOR_COMPLETE_MS = 2_000L   // 2 s of no pose in ACTIVE → SET_COMPLETE
private const val SET_COMPLETE_AUTO_RESET_MS = 3_000L  // auto back to STANDBY after 3 s
private const val MONITOR_POLL_MS           =   100L   // how often the pose-monitor ticks

class ExerciseViewModel(
    application: Application,
    private val difficulty: Difficulty = Difficulty.MEDIUM
) : AndroidViewModel(application) {

    // ── Dependencies ──────────────────────────────────────────────────────────
    private val repo: ProgressRepository by lazy {
        val dao = AppDatabase.getInstance(application).userProgressDao()
        ProgressRepository(dao)
    }

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

    /** Controlled from the UI layer via the sound preference. */
    var soundEnabled: Boolean = true

    // ── Exposed state ─────────────────────────────────────────────────────────

    /** The exercise display name — used in the UI header. */
    val exerciseName: String = "Push-Up"
    /** Rep target driven by Difficulty config (Phase 5.4 — no more hardcoded 10). */
    val targetReps: Int = difficulty.targetReps

    private val _sessionState = MutableStateFlow(SessionState.STANDBY)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _repCount = MutableStateFlow(0)
    val repCount: StateFlow<Int> = _repCount.asStateFlow()

    /** Rep count frozen at the moment SET_COMPLETE triggers. */
    private val _completedSetReps = MutableStateFlow(0)
    val completedSetReps: StateFlow<Int> = _completedSetReps.asStateFlow()

    private val _formScore = MutableStateFlow(0)
    val formScore: StateFlow<Int> = _formScore.asStateFlow()

    private val _feedback = MutableStateFlow("GET INTO POSITION")
    val feedback: StateFlow<String> = _feedback.asStateFlow()

    /** Legacy string repState kept for CameraPreview badge compatibility. */
    private val _repState = MutableStateFlow("STANDBY")
    val repState: StateFlow<String> = _repState.asStateFlow()

    private val _currentFrame = MutableStateFlow<LandmarkFrame?>(null)
    val currentFrame: StateFlow<LandmarkFrame?> = _currentFrame.asStateFlow()

    private val _isFormValid = MutableStateFlow(false)
    val isFormValid: StateFlow<Boolean> = _isFormValid.asStateFlow()

    private val _poseDetected = MutableStateFlow(false)
    val poseDetected: StateFlow<Boolean> = _poseDetected.asStateFlow()

    /** Elapsed seconds — only increments while SessionState == ACTIVE. */
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

    /** Cleared once the first real camera frame arrives (Phase 4.4). */
    private val _isCameraReady = MutableStateFlow(false)
    val isCameraReady: StateFlow<Boolean> = _isCameraReady.asStateFlow()

    // ── PB state (Phase 5.3) ──────────────────────────────────────────────────
    /** Current personal best rep count for this exercise. Loaded on session start. */
    private val _personalBest = MutableStateFlow(0)
    val personalBest: StateFlow<Int> = _personalBest.asStateFlow()

    /** True for one SET_COMPLETE cycle if the session set a new PB. */
    private val _isNewPb = MutableStateFlow(false)
    val isNewPb: StateFlow<Boolean> = _isNewPb.asStateFlow()

    // ── READY flash (Phase 5.2) ───────────────────────────────────────────────
    /** Pulses true for ~800 ms when entering READY state. */
    private val _readyFlash = MutableStateFlow(false)
    val readyFlash: StateFlow<Boolean> = _readyFlash.asStateFlow()

    // ── Jobs ──────────────────────────────────────────────────────────────────
    private var collectionJob:  Job? = null
    private var cameraReadyJob: Job? = null
    private var timerJob:       Job? = null
    private var monitorJob:     Job? = null

    val imageAnalysisUseCase: androidx.camera.core.ImageAnalysis
        get() = poseDetector.imageAnalysisUseCase

    // ── Camera controls ───────────────────────────────────────────────────────
    fun flipCamera() {
        _cameraFacing.value = if (_cameraFacing.value == CameraSelector.DEFAULT_FRONT_CAMERA)
            CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
    }

    fun toggleOrientation() { _isLandscape.value = !_isLandscape.value }
    fun toggleDebugMode()    { _isDebugMode.value = !_isDebugMode.value }

    // ── Session control ───────────────────────────────────────────────────────
    fun startSession() {
        countPushUpUseCase.reset()
        _repCount.value         = 0
        _completedSetReps.value = 0
        _formScore.value        = 0
        _feedback.value         = "GET INTO POSITION"
        _repState.value         = "STANDBY"
        _sessionState.value     = SessionState.STANDBY
        _elapsedSecs.value      = 0
        _isCameraReady.value    = false
        _isNewPb.value          = false

        // Load PB before the session starts (Phase 5.3)
        viewModelScope.launch {
            _personalBest.value = repo.getPbForProgram("push_up")
        }

        // Camera-ready signal (Phase 4.4)
        cameraReadyJob?.cancel()
        cameraReadyJob = viewModelScope.launch {
            poseDetector.cameraReadyFlow.collect {
                _isCameraReady.value = true
            }
        }

        // Pose collection + state machine output
        collectionJob?.cancel()
        collectionJob = viewModelScope.launch {
            poseDetector.poseFlow.collect { poseResult ->
                val r = countPushUpUseCase.process(poseResult)

                // Always update frame/tracking data
                _currentFrame.value = r.frame
                _frameWidth.value   = r.frameWidth
                _frameHeight.value  = r.frameHeight
                _poseDetected.value = r.analysis.poseDetected
                _trackingQuality.value = r.frame?.let { frame ->
                    val joints = listOfNotNull(
                        frame.leftShoulder, frame.rightShoulder,
                        frame.leftElbow,    frame.rightElbow,
                        frame.leftWrist,    frame.rightWrist,
                        frame.leftHip,      frame.rightHip,
                        frame.leftKnee,     frame.rightKnee,
                        frame.leftAnkle,    frame.rightAnkle
                    )
                    if (joints.isEmpty()) 0f
                    else joints.map { it.inFrameLikelihood }.average().toFloat()
                } ?: 0f

                // State-gated counting (Phase 5.2)
                when (_sessionState.value) {
                    SessionState.STANDBY -> {
                        _feedback.value = "GET INTO POSITION"
                    }

                    SessionState.READY -> {
                        // Transition to ACTIVE the moment the user starts the first rep motion
                        if (r.analysis.poseDetected && r.analysis.phase == "GOING_DOWN") {
                            transitionToActive()
                        }
                        _feedback.value = "START!"
                    }

                    SessionState.ACTIVE -> {
                        val prevReps = _repCount.value
                        _repCount.value  = r.repCount
                        _formScore.value = r.analysis.formScore
                        _feedback.value  = r.analysis.feedback
                        _isFormValid.value = r.analysis.goodForm

                        if (r.repCount > prevReps && soundEnabled) {
                            soundManager.playRepComplete()
                        }
                    }

                    SessionState.SET_COMPLETE -> { /* frozen — no updates */ }
                }

                // Keep legacy repState string in sync for badge
                _repState.value = _sessionState.value.name
            }
        }

        // Pose-loss / hold-time monitor (Phase 5.2)
        startPoseMonitor()
    }

    fun stopSession() {
        cancelAllJobs()
        soundManager.release()
        _poseDetector?.close()
        _poseDetector        = null
        _isCameraReady.value = false
        _sessionState.value  = SessionState.STANDBY
        _repState.value      = "STANDBY"
    }

    /** Called from SET_COMPLETE banner "Dismiss" or after auto-reset timer. */
    fun resetAfterSet() {
        countPushUpUseCase.reset()
        _repCount.value         = 0
        _completedSetReps.value = 0
        _formScore.value        = 0
        _feedback.value         = "GET INTO POSITION"
        _sessionState.value     = SessionState.STANDBY
        _repState.value         = "STANDBY"
        _isNewPb.value          = false
        _elapsedSecs.value      = 0
        timerJob?.cancel()
        timerJob = null
        startPoseMonitor()
    }

    override fun onCleared() {
        super.onCleared()
        stopSession()
    }

    // ── State transitions (Phase 5.2) ─────────────────────────────────────────

    private fun transitionToReady() {
        // Reset counter so reps start from zero when ACTIVE begins
        countPushUpUseCase.reset()
        _repCount.value     = 0
        _sessionState.value = SessionState.READY
        _repState.value     = "READY"
        _feedback.value     = "START!"
        soundManager.playRepComplete() // audio cue for READY

        // Brief flash
        viewModelScope.launch {
            _readyFlash.value = true
            delay(800)
            _readyFlash.value = false
        }
    }

    private fun transitionToActive() {
        _sessionState.value = SessionState.ACTIVE
        _repState.value     = "ACTIVE"

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                _elapsedSecs.value += 1
            }
        }
    }

    private fun transitionToSetComplete() {
        timerJob?.cancel()
        timerJob = null

        val finalReps = _repCount.value
        _completedSetReps.value = finalReps
        _sessionState.value     = SessionState.SET_COMPLETE
        _repState.value         = "DONE"
        _feedback.value         = "Set complete!"

        // PB check and persist (Phase 5.3)
        viewModelScope.launch {
            val currentPb = _personalBest.value
            val isNew     = finalReps > 0 && finalReps > currentPb
            if (isNew) {
                _personalBest.value = finalReps
                _isNewPb.value      = true
            }

            // Persist session to DB
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            repo.saveDaySession(
                date          = today,
                repsDone      = finalReps,
                programName   = "push_up",
                durationSecs  = _elapsedSecs.value,
                completedGoal = finalReps >= targetReps
            )
        }

        // Auto-reset after 3 s
        viewModelScope.launch {
            delay(SET_COMPLETE_AUTO_RESET_MS)
            if (_sessionState.value == SessionState.SET_COMPLETE) {
                resetAfterSet()
            }
        }
    }

    // ── Pose-monitor coroutine (Phase 5.2) ────────────────────────────────────

    private fun startPoseMonitor() {
        monitorJob?.cancel()
        monitorJob = viewModelScope.launch {
            var poseHeldMs    = 0L
            var poseLostMs    = 0L

            while (true) {
                delay(MONITOR_POLL_MS)
                val posePresent = _poseDetected.value

                when (_sessionState.value) {
                    SessionState.STANDBY -> {
                        if (posePresent) {
                            poseHeldMs += MONITOR_POLL_MS
                            if (poseHeldMs >= POSE_HOLD_FOR_READY_MS) {
                                poseHeldMs = 0L
                                transitionToReady()
                            }
                        } else {
                            poseHeldMs = 0L
                        }
                    }

                    SessionState.READY -> {
                        if (!posePresent) {
                            // Lost pose before first rep — fall back to STANDBY
                            poseHeldMs          = 0L
                            _sessionState.value = SessionState.STANDBY
                            _repState.value     = "STANDBY"
                            _feedback.value     = "GET INTO POSITION"
                        }
                    }

                    SessionState.ACTIVE -> {
                        if (!posePresent) {
                            poseLostMs += MONITOR_POLL_MS
                            if (poseLostMs >= POSE_LOST_FOR_COMPLETE_MS) {
                                poseLostMs = 0L
                                transitionToSetComplete()
                            }
                        } else {
                            poseLostMs = 0L
                        }
                    }

                    SessionState.SET_COMPLETE -> {
                        // Handled by the auto-reset launched in transitionToSetComplete()
                    }
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun cancelAllJobs() {
        collectionJob?.cancel();  collectionJob  = null
        cameraReadyJob?.cancel(); cameraReadyJob = null
        timerJob?.cancel();       timerJob       = null
        monitorJob?.cancel();     monitorJob     = null
    }
}
