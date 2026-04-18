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

    val poseFlow: Flow<Result> = channel.receiveAsFlow()

    val imageAnalysisUseCase: ImageAnalysis =
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(executor, ::analyze)
            }

    fun close() {
        imageAnalysisUseCase.clearAnalyzer()
        detector.close()
        channel.close()
        executor.shutdown()
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun analyze(proxy: ImageProxy) {
        val image = proxy.image ?: return proxy.close()

        val rotation = proxy.imageInfo.rotationDegrees
        val width = if (rotation % 180 == 0) proxy.width else proxy.height
        val height = if (rotation % 180 == 0) proxy.height else proxy.width

        val input = InputImage.fromMediaImage(image, rotation)

        detector.process(input)
            .addOnSuccessListener { pose ->
                val hasBody = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
                    ?.inFrameLikelihood?.let { it >= 0.4f } == true ||
                        pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
                    ?.inFrameLikelihood?.let { it >= 0.4f } == true

                if (hasBody && pose.allPoseLandmarks.isNotEmpty()) {
                    channel.trySend(Result(pose, width, height))
                }
            }
            .addOnCompleteListener {
                proxy.close()
            }
    }
}