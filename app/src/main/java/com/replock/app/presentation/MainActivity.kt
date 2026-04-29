package com.replock.app.presentation

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.replock.app.presentation.components.RepLockColors
import com.replock.app.presentation.dashboard.DashboardScreen
import com.replock.app.presentation.exercise.ExerciseScreen
import com.replock.app.presentation.history.HistoryScreen
import com.replock.app.presentation.settings.SettingsScreen

private enum class RootTab { DASHBOARD, HISTORY, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { RepLockApp() }
    }
}

@Composable
private fun RepLockApp() {
    val viewModel: MainViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf(RootTab.DASHBOARD) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) viewModel.startWorkout()
    }

    val activity = context as? Activity
    DisposableEffect(uiState.workout.visible) {
        if (uiState.workout.visible) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    if (uiState.workout.visible) {
        ExerciseScreen(
            exerciseType = uiState.workout.exerciseType,
            repCount = uiState.workout.repCount,
            targetReps = uiState.workout.targetReps,
            repState = uiState.workout.repState,
            elapsedSecs = uiState.workout.elapsedSecs,
            feedback = uiState.workout.feedback,
            formScore = uiState.workout.formScore,
            isFormValid = uiState.workout.isFormValid,
            isPoseDetected = uiState.workout.isPoseDetected,
            isCameraReady = uiState.workout.isCameraReady,
            imageAnalysisUseCase = viewModel.imageAnalysisUseCase,
            currentFrame = uiState.workout.currentFrame,
            frameWidth = uiState.workout.frameWidth,
            frameHeight = uiState.workout.frameHeight,
            trackingQuality = uiState.workout.trackingQuality,
            cameraFacing = uiState.workout.cameraFacing,
            personalBest = uiState.workout.personalBest,
            todayExerciseReps = uiState.workout.todayExerciseReps,
            onFlipCamera = viewModel::flipCamera,
            onFinish = viewModel::finishWorkout
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = RepLockColors.Background,
        bottomBar = {
            NavigationBar(
                containerColor = RepLockColors.Surface,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = selectedTab == RootTab.DASHBOARD,
                    onClick = { selectedTab = RootTab.DASHBOARD },
                    icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) },
                    label = { Text("Train") }
                )
                NavigationBarItem(
                    selected = selectedTab == RootTab.HISTORY,
                    onClick = { selectedTab = RootTab.HISTORY },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("History") }
                )
                NavigationBarItem(
                    selected = selectedTab == RootTab.SETTINGS,
                    onClick = { selectedTab = RootTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            RootTab.DASHBOARD -> DashboardScreen(
                state = uiState.dashboard,
                onSelectExercise = viewModel::selectExercise,
                onStartWorkout = {
                    if (hasCameraPermission) {
                        viewModel.startWorkout()
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )

            RootTab.HISTORY -> HistoryScreen(
                dashboardState = uiState.dashboard,
                history = uiState.history,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )

            RootTab.SETTINGS -> SettingsScreen(
                state = uiState.settings,
                selectedExercise = uiState.dashboard.selectedExercise,
                onSelectExercise = viewModel::selectExercise,
                onTargetRepsChange = viewModel::setTargetReps,
                onSoundEnabledChange = viewModel::setSoundEnabled,
                onReminderIntervalChange = viewModel::setReminderInterval,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}
