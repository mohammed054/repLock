package com.replock.app.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.replock.app.data.Difficulty
import com.replock.app.presentation.exercise.ExerciseScreen
import com.replock.app.presentation.exercise.ExerciseViewModel
import com.replock.app.presentation.onboarding.OnboardingScreen
import com.replock.app.presentation.onboarding.getSavedDifficulty
import com.replock.app.presentation.settings.SettingsScreen
import com.replock.app.presentation.settings.isSoundEnabled
import com.replock.app.system.notification.ReminderScheduler

private enum class Screen { EXERCISE, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent { RepLockApp() }
    }
}

@Composable
private fun RepLockApp() {
    val context = LocalContext.current
    var currentDifficulty by remember { mutableStateOf(context.getSavedDifficulty()) }
    var currentScreen     by remember { mutableStateOf(Screen.EXERCISE) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07070B))
    ) {
        if (currentDifficulty == null) {
            OnboardingScreen(
                onProgramSelected = { difficulty -> currentDifficulty = difficulty }
            )
        } else {
            when (currentScreen) {
                Screen.EXERCISE -> WorkoutRoot(
                    difficulty     = currentDifficulty!!,
                    onOpenSettings = { currentScreen = Screen.SETTINGS }
                )
                Screen.SETTINGS -> SettingsScreen(
                    onBack          = { currentScreen = Screen.EXERCISE },
                    onChangeProgram = {
                        currentDifficulty = null
                        currentScreen     = Screen.EXERCISE
                    }
                )
            }
        }
    }
}

@Composable
private fun WorkoutRoot(
    difficulty     : Difficulty,
    onOpenSettings : () -> Unit
) {
    val context   = LocalContext.current
    val viewModel : ExerciseViewModel = viewModel()

    // ── Collect all ViewModel state ──────────────────────────────────────────
    val repCount         by viewModel.repCount.collectAsStateWithLifecycle()
    val completedSetReps by viewModel.completedSetReps.collectAsStateWithLifecycle()
    val sessionState     by viewModel.sessionState.collectAsStateWithLifecycle()
    val repState         by viewModel.repState.collectAsStateWithLifecycle()
    val elapsedSecs      by viewModel.elapsedSecs.collectAsStateWithLifecycle()
    val currentFrame     by viewModel.currentFrame.collectAsStateWithLifecycle()
    val isFormValid      by viewModel.isFormValid.collectAsStateWithLifecycle()
    val feedback         by viewModel.feedback.collectAsStateWithLifecycle()
    val frameWidth       by viewModel.frameWidth.collectAsStateWithLifecycle()
    val frameHeight      by viewModel.frameHeight.collectAsStateWithLifecycle()
    val isDebugMode      by viewModel.isDebugMode.collectAsStateWithLifecycle()
    val trackingQuality  by viewModel.trackingQuality.collectAsStateWithLifecycle()
    val cameraFacing     by viewModel.cameraFacing.collectAsStateWithLifecycle()
    val isCameraReady    by viewModel.isCameraReady.collectAsStateWithLifecycle()

    // Phase 5.3 — PB state
    val personalBest     by viewModel.personalBest.collectAsStateWithLifecycle()
    val isNewPb          by viewModel.isNewPb.collectAsStateWithLifecycle()

    // Phase 5.2 — READY flash
    val readyFlash       by viewModel.readyFlash.collectAsStateWithLifecycle()

    // Propagate sound preference
    val soundEnabled by remember { derivedStateOf { context.isSoundEnabled() } }
    LaunchedEffect(soundEnabled) { viewModel.soundEnabled = soundEnabled }

    // Cancel today's reminders when goal is met
    LaunchedEffect(repCount) {
        if (repCount >= difficulty.targetReps && difficulty.targetReps > 0) {
            ReminderScheduler.scheduleFromTomorrow(context)
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    var isActive by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) { viewModel.startSession(); isActive = true }
    }

    ExerciseScreen(
        // Rep state — Phase 5.4: targetReps from difficulty (config-driven)
        repCount             = repCount,
        targetReps           = difficulty.targetReps,
        completedSetReps     = completedSetReps,
        sessionState         = sessionState,
        isActive             = isActive && hasCameraPermission,
        isCameraReady        = isCameraReady,
        repState             = repState,
        elapsedSecs          = elapsedSecs,
        // PB — Phase 5.3
        personalBest         = personalBest,
        isNewPb              = isNewPb,
        // READY flash — Phase 5.2
        readyFlash           = readyFlash,
        // Camera
        currentFrame         = currentFrame,
        isFormValid          = isFormValid,
        feedback             = feedback,
        frameWidth           = frameWidth,
        frameHeight          = frameHeight,
        isDebugMode          = isDebugMode,
        trackingQuality      = trackingQuality,
        cameraFacing         = cameraFacing,
        onFlipCamera         = { viewModel.flipCamera() },
        onToggleOrientation  = { viewModel.toggleOrientation() },
        onOpenSettings       = onOpenSettings,
        imageAnalysisUseCase = if (isActive && hasCameraPermission)
                                   viewModel.imageAnalysisUseCase else null,
        onDismissSet         = { viewModel.resetAfterSet() },
        onStop = {
            if (isActive) { viewModel.stopSession(); isActive = false }
            else {
                if (hasCameraPermission) { viewModel.startSession(); isActive = true }
                else launcher.launch(Manifest.permission.CAMERA)
            }
        },
        onBack = {
            viewModel.stopSession()
            isActive = false
        }
    )
}
