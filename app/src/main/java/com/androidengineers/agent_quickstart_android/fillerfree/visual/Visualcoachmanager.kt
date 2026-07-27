package com.androidengineers.agent_quickstart_android.fillerfree.visual

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.AttentionSignal
import com.androidengineers.agent_quickstart_android.fillerfree.domain.usecase.DetectAttentionSignalUseCase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Point 4 (visual coaching), tier 1: on-device camera + face detection for
 * an eye-contact/attention read, with NO video ever streamed anywhere.
 *
 * SCOPE, DELIBERATELY: this binds a front-camera preview + ML Kit Face
 * Detection through CameraX's ImageAnalysis use case, entirely local to the
 * device. It does NOT publish a video track to Agora, does NOT send frames
 * to any server, and does NOT save images. If you want a coach or recorded
 * review to actually see video, or a vision-capable agent to comment on
 * posture, that's tier 3 from the original plan — a materially bigger
 * change (Agora video channel + a multimodal-capable agent pipeline) that
 * this class intentionally does not attempt.
 *
 * Threading: ML Kit's face detector callback runs on the ImageAnalysis
 * executor thread, not the main thread. [attentionSignal] is safe to
 * collect from Compose/main because MutableStateFlow handles the
 * cross-thread publish; do not touch UI directly from [analyzeFrame].
 */
class VisualCoachManager(context: Context) {

    private val appContext = context.applicationContext
    private val detectAttentionSignal = DetectAttentionSignalUseCase()
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
    )

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null

    private val _attentionSignal = MutableStateFlow<AttentionSignal?>(null)
    val attentionSignal: StateFlow<AttentionSignal?> = _attentionSignal.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /**
     * Binds the front camera to [lifecycleOwner] and starts analyzing
     * frames. Caller is responsible for having already obtained the
     * CAMERA permission — this does not request it. Safe to call again;
     * rebinds cleanly.
     */
    fun start(lifecycleOwner: LifecycleOwner) {
        detectAttentionSignal.reset()
        _lastError.value = null

        val providerFuture = ProcessCameraProvider.getInstance(appContext)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                cameraProvider = provider
                provider.unbindAll()

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(analysisExecutor, ::analyzeFrame) }
                imageAnalysis = analysis

                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    analysis,
                )
                _isActive.value = true
            } catch (error: Exception) {
                Log.w(TAG, "Failed to start visual coaching camera", error)
                _lastError.value = error.message ?: "Could not start the camera."
                _isActive.value = false
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    fun stop() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        imageAnalysis = null
        _isActive.value = false
        _attentionSignal.value = null
        detectAttentionSignal.reset()
    }

    fun release() {
        stop()
        faceDetector.close()
        analysisExecutor.shutdown()
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun analyzeFrame(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                val yaw = faces.firstOrNull()?.headEulerAngleY
                _attentionSignal.value = detectAttentionSignal(
                    headYawDegrees = yaw,
                    timestampMs = System.currentTimeMillis(),
                )
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Face detection failed on a frame", error)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    companion object {
        private const val TAG = "VisualCoachManager"
    }
}