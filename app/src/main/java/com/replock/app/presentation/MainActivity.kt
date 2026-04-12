package com.replock.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.replock.app.presentation.lock.LockScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Demo state — wire to LockViewModel / ExerciseViewModel when ready
            var repCount by remember { mutableStateOf(0) }
            var isActive by remember { mutableStateOf(false) }
            val targetReps = 20

            LockScreen(
                repCount   = repCount,
                targetReps = targetReps,
                isActive   = isActive,
                isUnlocked = repCount >= targetReps,
                repState   = if (isActive) "DETECTING" else "WAITING",
                onStartStop = { isActive = !isActive },
                onUnlock   = { /* OverlayController.dismiss() */ }
            )
        }
    }
}
