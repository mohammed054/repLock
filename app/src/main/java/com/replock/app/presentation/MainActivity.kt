package com.replock.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.replock.app.presentation.exercise.ExerciseViewModel
import com.replock.app.presentation.lock.LockScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ExerciseViewModel = viewModel()
            val repCount by viewModel.repCount.collectAsStateWithLifecycle()
            val repState by viewModel.repState.collectAsStateWithLifecycle()
            val elapsedSecs by viewModel.elapsedSecs.collectAsStateWithLifecycle()
            
            val context = LocalContext.current
            var hasCameraPermission by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }
            
            var isActive by remember { mutableStateOf(false) }

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                hasCameraPermission = isGranted
                if (isGranted) {
                    viewModel.startSession()
                    isActive = true
                }
            }

            val targetReps = 20

            LockScreen(
                repCount             = repCount,
                targetReps         = targetReps,
                isActive           = isActive && hasCameraPermission,
                isUnlocked         = repCount >= targetReps,
                repState           = repState,
                imageAnalysisUseCase = if (isActive && hasCameraPermission) viewModel.imageAnalysisUseCase else null,
                onStartStop = {
                    if (isActive) {
                        viewModel.stopSession()
                        isActive = false
                    } else {
                        if (hasCameraPermission) {
                            viewModel.startSession()
                            isActive = true
                        } else {
                            launcher.launch(Manifest.permission.CAMERA)
                        }
                    }
                },
                onUnlock   = { /* OverlayController.dismiss() */ }
            )
        }
    }
}
