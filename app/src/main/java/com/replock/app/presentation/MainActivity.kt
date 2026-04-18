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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            RepLockApp()
        }
    }
}

@Composable
private fun RepLockApp() {
    val context = LocalContext.current
    var currentDifficulty by remember { mutableStateOf(context.getSavedDifficulty()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07070B))
    ) {
        if (currentDifficulty == null) {
            OnboardingScreen(
                onProgramSelected = { difficulty ->
                    currentDifficulty = difficulty
                }
            )
        } else {
            WorkoutRoot(difficulty = currentDifficulty!!)
        }
    }
}

@Composable
private fun WorkoutRoot(difficulty: Difficulty) {
    val viewModel: ExerciseViewModel = viewModel()

    val repCount        by viewModel.repCount.collectAsStateWithLifecycle()
    val repState        by viewModel.repState.collectAsStateWithLifecycle()
    val elapsedSecs     by viewModel.elapsedSecs.collectAsStateWithLifecycle()
    val currentFrame    by viewModel.currentFrame.collectAsStateWithLifecycle()
    val isFormValid     by viewModel.isFormValid.collectAsStateWithLifecycle()
    val feedback        by viewModel.feedback.collectAsStateWithLifecycle()
    val frameWidth      by viewModel.frameWidth.collectAsStateWithLifecycle()
    val frameHeight     by viewModel.frameHeight.collectAsStateWithLifecycle()
    val isDebugMode     by viewModel.isDebugMode.collectAsStateWithLifecycle()
    val trackingQuality by viewModel.trackingQuality.collectAsStateWithLifecycle()
    val cameraFacing    by viewModel.cameraFacing.collectAsStateWithLifecycle()

    val context = LocalContext.current
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
        repCount             = repCount,
        targetReps           = difficulty.targetReps,
        isActive             = isActive && hasCameraPermission,
        repState             = repState,
        elapsedSecs          = elapsedSecs,
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
        imageAnalysisUseCase = if (isActive && hasCameraPermission)
                                   viewModel.imageAnalysisUseCase else null,
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