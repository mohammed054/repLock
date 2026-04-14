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

class PoseDetector {

    private val mlKitOptions: AccuratePoseDetectorOptions =
        AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()

    private val mlKitDetector: MLKitPoseDetector =
        PoseDetection.getClient(mlKitOptions)

    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private val poseChannel = Channel<Pose>(Channel.CONFLATED)

    val imageAnalysisUseCase: ImageAnalysis =
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(analysisExecutor, ::analyzeImage)
            }

    val poseFlow: Flow<Pose> = poseChannel.receiveAsFlow()

    fun close() {
        imageAnalysisUseCase.clearAnalyzer()
        mlKitDetector.close()
        poseChannel.close()
        analysisExecutor.shutdown()
    }

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
                if (pose.allPoseLandmarks.isNotEmpty()) {
                    poseChannel.trySend(pose)
                }
            }
            .addOnCompleteListener {
                proxy.close()
            }
    }
}