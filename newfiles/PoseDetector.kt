package com.replock.app.ml.pose

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector as MLKitPoseDetector
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.Executors

/**
 * Wraps ML Kit's accurate pose detector behind a CameraX [ImageAnalysis] use case
 * and exposes a [poseFlow] of detected [Pose] frames.
 *
 * Usage:
 * 1. Bind [imageAnalysisUseCase] to the CameraX lifecycle provider alongside [Preview].
 * 2. Collect [poseFlow] in a coroutine scope (e.g. [ViewModel.viewModelScope]).
 * 3. Call [close] when done (ViewModel.onCleared).
 */
class PoseDetector {

    // ── ML Kit detector (STREAM_MODE = optimised for live camera; reuses prev frame hints) ──
    private val mlKitOptions: AccuratePoseDetectorOptions =
        AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()

    private val mlKitDetector: MLKitPoseDetector =
        PoseDetection.getClient(mlKitOptions)

    // ── Background thread for CameraX analysis callbacks ────────────────────
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    // ── Internal channel — CONFLATED drops stale frames if collector is slow ─
    private val poseChannel = Channel<Pose>(Channel.CONFLATED)

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * CameraX use case — pass this to [ProcessCameraProvider.bindToLifecycle]
     * alongside your [Preview] use case.
     *
     * The analyzer is wired up immediately so frames start arriving as soon as
     * the camera is bound.
     */
    val imageAnalysisUseCase: ImageAnalysis =
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(analysisExecutor, ::analyzeImage)
            }

    /**
     * Cold flow of [Pose] objects — one per camera frame that the detector
     * successfully processes. Backed by a CONFLATED channel so the collector
     * always sees the latest pose without back-pressure buildup.
     */
    val poseFlow: Flow<Pose> = poseChannel.receiveAsFlow()

    /**
     * Release all resources. Call from [ViewModel.onCleared] or a
     * DisposableEffect in Compose.
     */
    fun close() {
        imageAnalysisUseCase.clearAnalyzer()
        mlKitDetector.close()
        poseChannel.close()
        analysisExecutor.shutdown()
    }

    // ── Private ──────────────────────────────────────────────────────────────

    @SuppressLint("UnsafeOptInUsageError")
    private fun analyzeImage(proxy: ImageProxy) {
        val mediaImage = proxy.image
        if (mediaImage == null) {
            proxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            proxy.imageInfo.rotationDegrees
        )

        mlKitDetector.process(inputImage)
            .addOnSuccessListener { pose ->
                // Only emit if the pose contains at least one landmark
                // (an empty Pose object is returned when nobody is visible)
                if (pose.allPoseLandmarks.isNotEmpty()) {
                    poseChannel.trySend(pose)
                }
            }
            .addOnCompleteListener {
                // Always close the proxy — even on failure — or CameraX stalls
                proxy.close()
            }
    }
}
