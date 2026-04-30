package com.replock.app.presentation

import android.app.Application
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.replock.app.data.local.AppDatabase
import com.replock.app.data.local.WorkoutSessionEntity
import com.replock.app.data.repository.UserPreferences
import com.replock.app.data.repository.UserPreferencesRepository
import com.replock.app.data.repository.WorkoutRepository
import com.replock.app.domain.model.ExerciseType
import com.replock.app.domain.usecase.AnalyzeWorkoutUseCase
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

data class SessionHistoryItem(
    val id: Long,
    val exerciseType: ExerciseType,
    val reps: Int,
    val targetReps: Int,
    val durationLabel: String,
    val dayLabel: String,
    val completedGoal: Boolean,
    val formScore: Int
)

data class DailyVolumeItem(
    val dayLabel: String,
    val reps: Int,
    val isToday: Boolean
)

data class DashboardState(
    val selectedExercise: ExerciseType = ExerciseType.PUSH_UP,
    val targetReps: Int = ExerciseType.PUSH_UP.defaultTargetReps,
    val pushUpTargetReps: Int = ExerciseType.PUSH_UP.defaultTargetReps,
    val pullUpTargetReps: Int = ExerciseType.PULL_UP.defaultTargetReps,
    val todayExerciseReps: Int = 0,
    val todayPushUps: Int = 0,
    val todayPullUps: Int = 0,
    val todayTotalReps: Int = 0,
    val totalReps: Int = 0,
    val weeklyReps: Int = 0,
    val weeklySessions: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val personalBestPushUps: Int = 0,
    val personalBestPullUps: Int = 0,
    val recentSessions: List<SessionHistoryItem> = emptyList(),
    val weeklyVolume: List<DailyVolumeItem> = emptyList()
)

data class SettingsState(
    val soundEnabled: Boolean = true,
    val reminderIntervalHours: Int = 3,
    val pushUpTargetReps: Int = ExerciseType.PUSH_UP.defaultTargetReps,
    val pullUpTargetReps: Int = ExerciseType.PULL_UP.defaultTargetReps
)

data class WorkoutUiState(
    val visible: Boolean = false,
    val isActive: Boolean = false,
    val exerciseType: ExerciseType = ExerciseType.PUSH_UP,
    val targetReps: Int = ExerciseType.PUSH_UP.defaultTargetReps,
    val repCount: Int = 0,
    val feedback: String = "Center your body in frame",
    val repState: String = "READY",
    val formScore: Int = 0,
    val isFormValid: Boolean = false,
    val isPoseDetected: Boolean = false,
    val elapsedSecs: Long = 0,
    val currentFrame: LandmarkFrame? = null,
    val frameWidth: Int = 1,
    val frameHeight: Int = 1,
    val trackingQuality: Float = 0f,
    val cameraFacing: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
    val isCameraReady: Boolean = false,
    val personalBest: Int = 0,
    val todayExerciseReps: Int = 0
)

data class AppUiState(
    val dashboard: DashboardState = DashboardState(),
    val history: List<SessionHistoryItem> = emptyList(),
    val settings: SettingsState = SettingsState(),
    val workout: WorkoutUiState = WorkoutUiState()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val workoutRepository = WorkoutRepository(database.sessionDao())
    private val preferencesRepository = UserPreferencesRepository(application)
    private val soundManager = SoundManager()

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var preferences = preferencesRepository.load()
    private var sessions: List<WorkoutSessionEntity> = emptyList()

    private var poseDetector: PoseDetector? = null
    private var analyzer: AnalyzeWorkoutUseCase? = null
    private var sessionCollectorJob: Job? = null
    private var cameraReadyJob: Job? = null
    private var timerJob: Job? = null
    private var sessionStartedAt = 0L
    private var formScoreTotal = 0
    private var formScoreSamples = 0

    val imageAnalysisUseCase: ImageAnalysis?
        get() = poseDetector?.imageAnalysisUseCase

    init {
        publishState()
        viewModelScope.launch {
            workoutRepository.observeSessions().collect { savedSessions ->
                sessions = savedSessions
                publishState()
            }
        }
    }

    fun selectExercise(exerciseType: ExerciseType) {
        preferencesRepository.setSelectedExercise(exerciseType)
        preferences = preferences.copy(selectedExercise = exerciseType)
        publishState()
    }

    fun setTargetReps(exerciseType: ExerciseType, reps: Int) {
        val clamped = when (exerciseType) {
            ExerciseType.PUSH_UP -> reps.coerceIn(6, 80)
            ExerciseType.PULL_UP -> reps.coerceIn(3, 40)
        }
        preferencesRepository.setTargetReps(exerciseType, clamped)
        preferences = when (exerciseType) {
            ExerciseType.PUSH_UP -> preferences.copy(pushUpTargetReps = clamped)
            ExerciseType.PULL_UP -> preferences.copy(pullUpTargetReps = clamped)
        }
        publishState()
    }

    fun setSoundEnabled(enabled: Boolean) {
        preferencesRepository.setSoundEnabled(enabled)
        preferences = preferences.copy(soundEnabled = enabled)
        publishState()
    }

    fun setReminderInterval(hours: Int) {
        preferencesRepository.setReminderIntervalHours(hours)
        preferences = preferences.copy(reminderIntervalHours = hours)
        publishState()
    }

    fun startWorkout() {
        stopDetectorOnly()

        val exerciseType = preferences.selectedExercise
        val targetReps = preferences.targetFor(exerciseType)
        val defaultCamera = if (exerciseType == ExerciseType.PULL_UP) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }

        analyzer = AnalyzeWorkoutUseCase(exerciseType)
        val detector = PoseDetector().also { poseDetector = it }
        sessionStartedAt = System.currentTimeMillis()
        formScoreTotal = 0
        formScoreSamples = 0

        _uiState.value = _uiState.value.copy(
            workout = WorkoutUiState(
                visible = true,
                isActive = true,
                exerciseType = exerciseType,
                targetReps = targetReps,
                feedback = exerciseType.setupHint,
                repState = "READY",
                cameraFacing = defaultCamera,
                personalBest = personalBestFor(exerciseType),
                todayExerciseReps = todayRepsFor(exerciseType)
            )
        )

        cameraReadyJob?.cancel()
        cameraReadyJob = viewModelScope.launch {
            detector.cameraReadyFlow.collect {
                _uiState.value = _uiState.value.copy(
                    workout = _uiState.value.workout.copy(isCameraReady = true)
                )
            }
        }

        sessionCollectorJob?.cancel()
        sessionCollectorJob = viewModelScope.launch {
            detector.poseFlow.collect { poseResult ->
                val result = analyzer?.process(poseResult) ?: return@collect
                if (result.analysis.poseDetected) {
                    formScoreTotal += result.analysis.formScore
                    formScoreSamples++
                }

                val previousCount = _uiState.value.workout.repCount
                val newCount = result.repCount
                if (newCount > previousCount && preferences.soundEnabled) {
                    soundManager.playRepComplete()
                }

                _uiState.value = _uiState.value.copy(
                    workout = _uiState.value.workout.copy(
                        repCount = newCount,
                        feedback = result.analysis.feedback,
                        repState = result.analysis.phase,
                        formScore = result.analysis.formScore,
                        isFormValid = result.analysis.goodForm,
                        isPoseDetected = result.analysis.poseDetected,
                        currentFrame = result.frame,
                        frameWidth = result.frameWidth,
                        frameHeight = result.frameHeight,
                        trackingQuality = trackingQualityFor(result.frame)
                    )
                )
            }
        }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                val workoutState = _uiState.value.workout
                if (!workoutState.isActive) break
                _uiState.value = _uiState.value.copy(
                    workout = workoutState.copy(elapsedSecs = workoutState.elapsedSecs + 1)
                )
            }
        }
    }

    fun finishWorkout() {
        val workout = _uiState.value.workout
        val shouldSave = workout.repCount > 0 || workout.elapsedSecs >= 20
        val exerciseType = workout.exerciseType
        val targetReps = workout.targetReps
        val reps = workout.repCount
        val durationSecs = workout.elapsedSecs
        val averageFormScore = if (formScoreSamples == 0) workout.formScore else {
            (formScoreTotal.toFloat() / formScoreSamples).toInt()
        }

        stopDetectorOnly()

        _uiState.value = _uiState.value.copy(
            workout = WorkoutUiState(
                exerciseType = preferences.selectedExercise,
                targetReps = preferences.targetFor(preferences.selectedExercise),
                cameraFacing = workout.cameraFacing,
                personalBest = personalBestFor(preferences.selectedExercise),
                todayExerciseReps = todayRepsFor(preferences.selectedExercise)
            )
        )

        if (shouldSave) {
            viewModelScope.launch {
                workoutRepository.saveSession(
                    exerciseType = exerciseType,
                    reps = reps,
                    targetReps = targetReps,
                    durationSecs = durationSecs,
                    averageFormScore = averageFormScore
                )
            }
        }
    }

    fun flipCamera() {
        val current = _uiState.value.workout.cameraFacing
        val next = if (current == CameraSelector.DEFAULT_FRONT_CAMERA) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        _uiState.value = _uiState.value.copy(
            workout = _uiState.value.workout.copy(cameraFacing = next)
        )
    }

    private fun publishState() {
        val selectedExercise = preferences.selectedExercise
        val todayKey = WorkoutRepository.dayKey(System.currentTimeMillis())
        val recentSessions = sessions.take(20).map(::toHistoryItem)
        val weeklyVolume = buildWeeklyVolume(todayKey)
        val historyDayKeys = sessions.map { it.dayKey }

        _uiState.value = _uiState.value.copy(
            dashboard = DashboardState(
                selectedExercise = selectedExercise,
                targetReps = preferences.targetFor(selectedExercise),
                pushUpTargetReps = preferences.pushUpTargetReps,
                pullUpTargetReps = preferences.pullUpTargetReps,
                todayExerciseReps = todayRepsFor(selectedExercise),
                todayPushUps = todayRepsFor(ExerciseType.PUSH_UP),
                todayPullUps = todayRepsFor(ExerciseType.PULL_UP),
                todayTotalReps = sessions.filter { it.dayKey == todayKey }.sumOf { it.reps },
                totalReps = sessions.sumOf { it.reps },
                weeklyReps = weeklyVolume.sumOf { it.reps },
                weeklySessions = sessions.count { it.dayKey >= WorkoutRepository.shiftDay(todayKey, -6) },
                currentStreak = WorkoutRepository.currentStreak(historyDayKeys),
                longestStreak = WorkoutRepository.longestStreak(historyDayKeys),
                personalBestPushUps = personalBestFor(ExerciseType.PUSH_UP),
                personalBestPullUps = personalBestFor(ExerciseType.PULL_UP),
                recentSessions = recentSessions.take(4),
                weeklyVolume = weeklyVolume
            ),
            history = recentSessions,
            settings = SettingsState(
                soundEnabled = preferences.soundEnabled,
                reminderIntervalHours = preferences.reminderIntervalHours,
                pushUpTargetReps = preferences.pushUpTargetReps,
                pullUpTargetReps = preferences.pullUpTargetReps
            ),
            workout = _uiState.value.workout.copy(
                exerciseType = if (_uiState.value.workout.visible) {
                    _uiState.value.workout.exerciseType
                } else {
                    selectedExercise
                },
                targetReps = if (_uiState.value.workout.visible) {
                    _uiState.value.workout.targetReps
                } else {
                    preferences.targetFor(selectedExercise)
                },
                personalBest = personalBestFor(
                    if (_uiState.value.workout.visible) {
                        _uiState.value.workout.exerciseType
                    } else {
                        selectedExercise
                    }
                ),
                todayExerciseReps = todayRepsFor(
                    if (_uiState.value.workout.visible) {
                        _uiState.value.workout.exerciseType
                    } else {
                        selectedExercise
                    }
                )
            )
        )
    }

    private fun buildWeeklyVolume(todayKey: String): List<DailyVolumeItem> {
        return (-6..0).map { offset ->
            val day = WorkoutRepository.shiftDay(todayKey, offset)
            DailyVolumeItem(
                dayLabel = WorkoutRepository.formatWeekday(day),
                reps = sessions.filter { it.dayKey == day }.sumOf { it.reps },
                isToday = day == todayKey
            )
        }
    }

    private fun personalBestFor(exerciseType: ExerciseType): Int {
        return sessions
            .filter { it.exerciseType == exerciseType.name }
            .maxOfOrNull { it.reps } ?: 0
    }

    private fun todayRepsFor(exerciseType: ExerciseType): Int {
        val todayKey = WorkoutRepository.dayKey(System.currentTimeMillis())
        return sessions
            .filter { it.dayKey == todayKey && it.exerciseType == exerciseType.name }
            .sumOf { it.reps }
    }

    private fun toHistoryItem(entity: WorkoutSessionEntity): SessionHistoryItem {
        return SessionHistoryItem(
            id = entity.id,
            exerciseType = ExerciseType.fromName(entity.exerciseType),
            reps = entity.reps,
            targetReps = entity.targetReps,
            durationLabel = formatDuration(entity.durationSecs),
            dayLabel = WorkoutRepository.formatDayLabel(entity.dayKey),
            completedGoal = entity.completedGoal,
            formScore = entity.averageFormScore
        )
    }

    private fun trackingQualityFor(frame: LandmarkFrame?): Float {
        frame ?: return 0f
        val joints = listOfNotNull(
            frame.nose,
            frame.leftShoulder,
            frame.rightShoulder,
            frame.leftElbow,
            frame.rightElbow,
            frame.leftWrist,
            frame.rightWrist,
            frame.leftHip,
            frame.rightHip,
            frame.leftKnee,
            frame.rightKnee,
            frame.leftAnkle,
            frame.rightAnkle
        )
        if (joints.isEmpty()) return 0f
        return joints.map { it.inFrameLikelihood }.average().toFloat()
    }

    private fun stopDetectorOnly() {
        sessionCollectorJob?.cancel()
        sessionCollectorJob = null
        cameraReadyJob?.cancel()
        cameraReadyJob = null
        timerJob?.cancel()
        timerJob = null
        soundManager.release()
        analyzer?.reset()
        analyzer = null
        poseDetector?.close()
        poseDetector = null
        formScoreTotal = 0
        formScoreSamples = 0
    }

    override fun onCleared() {
        stopDetectorOnly()
        super.onCleared()
    }

    companion object {
        fun formatDuration(secs: Long): String {
            val minutes = secs / 60
            val seconds = secs % 60
            return "%02d:%02d".format(minutes, seconds)
        }
    }
}
