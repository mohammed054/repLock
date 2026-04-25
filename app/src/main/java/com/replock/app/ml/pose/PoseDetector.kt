package com.replock.app.ml.pose

import android.annotation.SuppressLint
import android.util.Log
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
    private val channel = Channel<Result>(Channel.CONFLATED)
    private val cameraReadyChannel = Channel<Unit>(Channel.CONFLATED)
    
    // Ensures we only notify that the camera is "ready" once
    private val isCameraReadyEmitted = AtomicBoolean(false)

    val poseFlow: Flow<Result> = channel.receiveAsFlow()
    val cameraReadyFlow: Flow<Unit> = cameraReadyChannel.receiveAsFlow()

    val imageAnalysisUseCase: ImageAnalysis =
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(executor, ::analyze)
            }

    fun close() {
        // Stop accepting new frames first
        imageAnalysisUseCase.clearAnalyzer()
        detector.close()
        channel.close()
        cameraReadyChannel.close()
        executor.shutdown()
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun analyze(proxy: ImageProxy) {
        val image = proxy.image 
        if (image == null) {
            proxy.close()
            return
        }

        val rotation = proxy.imageInfo.rotationDegrees
        val width = if (rotation % 180 == 0) proxy.width else proxy.height
        val height = if (rotation % 180 == 0) proxy.height else proxy.width

        val input = InputImage.fromMediaImage(image, rotation)

        detector.process(input)
            .addOnSuccessListener { pose ->
                // Emit camera ready ONLY once
                if (isCameraReadyEmitted.compareAndSet(false, true)) {
                    cameraReadyChannel.trySend(Unit)
                }
                
                // Logic check for body presence
                val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
                val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
                
                val hasBody = (leftHip?.inFrameLikelihood ?: 0f) >= 0.4f ||
                              (rightHip?.inFrameLikelihood ?: 0f) >= 0.4f

                if (hasBody && pose.allPoseLandmarks.isNotEmpty()) {
                    channel.trySend(Result(pose, width, height))
                }
            }
            .addOnFailureListener { e ->
                Log.e("PoseDetector", "ML Kit processing failed", e)
            }
            .addOnCompleteListener {
                // Always close the proxy to allow the camera to get the next frame
                proxy.close()
            }
    }
}