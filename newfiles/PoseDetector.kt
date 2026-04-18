package com.replock.app.ml.pose

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector as MLKitPoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class PoseDetector {

    data class Result(
        val pose: Pose,
        val width: Int,
        val height: Int
    )

    private val detector: MLKitPoseDetector =
        PoseDetection.getClient(
            AccuratePoseDetectorOptions.Builder()
                .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
                .build()
        )

    private val executor = Executors.newSingleThreadExecutor()

    // ── Pose results (body detected) ──────────────────────────────────────────
    private val channel = Channel<Result>(Channel.CONFLATED)
    val poseFlow: Flow<Result> = channel.receiveAsFlow()

    // ── Camera-ready signal (fires once on the very first raw frame) ──────────
    // This is independent of body / pose detection so the placeholder can be
    // hidden as soon as real camera pixels arrive, not when a person appears.
    private val _cameraReadySent = AtomicBoolean(false)
    private val _cameraReadyChannel = Channel<Unit>(Channel.CONFLATED)
    val cameraReadyFlow: Flow<Unit> = _cameraReadyChannel.receiveAsFlow()

    val imageAnalysisUseCase: ImageAnalysis =
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(executor, ::analyze) }

    fun close() {
        imageAnalysisUseCase.clearAnalyzer()
        detector.close()
        channel.close()
        _cameraReadyChannel.close()
        executor.shutdown()
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun analyze(proxy: ImageProxy) {
        val image = proxy.image ?: return proxy.close()

        // Fire the one-shot camera-ready signal on the very first raw frame —
        // regardless of whether a human body is present.
        if (_cameraReadySent.compareAndSet(false, true)) {
            _cameraReadyChannel.trySend(Unit)
        }

        val rotation = proxy.imageInfo.rotationDegrees
        val width    = if (rotation % 180 == 0) proxy.width else proxy.height
        val height   = if (rotation % 180 == 0) proxy.height else proxy.width

        val input = InputImage.fromMediaImage(image, rotation)

        detector.process(input)
            .addOnSuccessListener { pose ->
                val hasBody =
                    pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
                        ?.inFrameLikelihood?.let { it >= 0.4f } == true ||
                    pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
                        ?.inFrameLikelihood?.let { it >= 0.4f } == true

                if (hasBody && pose.allPoseLandmarks.isNotEmpty()) {
                    channel.trySend(Result(pose, width, height))
                }
            }
            .addOnCompleteListener { proxy.close() }
    }
}
