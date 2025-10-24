package com.example.shelfsnap.scan

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject as MlDetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.ObjectDetectorOptions
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Represents a detected object with a bounding box and optional label.
 */
/**
 * Represents a detected object with a relative bounding box and optional label.
 *
 * The bounding box coordinates are normalized (0..1) relative to the image
 * dimensions, making it easy to scale to any viewport.
 */
data class DetectedObject(
    val id: Int,
    val boundingBox: RectF,
    val label: String? = null
)

/**
 * ViewModel responsible for handling camera frame analysis and object detection.
 *
 * This is currently a stub and does not perform real detection. Integration with
 * ML Kit Object Detection or a custom YOLOv8 model can be added here.
 */
class ScanViewModel : ViewModel() {
    private val _detections = MutableStateFlow<List<DetectedObject>>(emptyList())
    val detections: StateFlow<List<DetectedObject>> = _detections

    /**
     * Controls whether object detection is currently enabled. When disabled (e.g. during editing),
     * incoming frames will be ignored and immediately closed to allow camera streaming to continue
     * without processing. This prevents continuous updates from overwriting the user's edits.
     */
    private val _detectionEnabled = MutableStateFlow(true)
    val detectionEnabled: StateFlow<Boolean> = _detectionEnabled

    /**
     * Enables or disables object detection. When disabled, [processImageProxy] will skip processing
     * and simply close incoming frames.
     */
    fun setDetectionEnabled(enabled: Boolean) {
        _detectionEnabled.value = enabled
    }

    // Configure ML Kit object detector for streaming mode with optional classification
    private val objectDetector: ObjectDetector

    init {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            // Enable detection of multiple objects (up to five). Feel free to disable if only the most prominent object is required.
            .enableMultipleObjects()
            // Enable coarse classification of objects into broad categories (fashion, food, home goods, places, plants).
            .enableClassification()
            .build()
        objectDetector = ObjectDetection.getClient(options)
    }

    /**
     * Processes an [ImageProxy] from CameraX and updates the detection results.
     *
     * Bounding boxes are converted to normalized coordinates relative to the image
     * dimensions so they can easily be mapped onto any viewport.
     *
     * Note: This method must ensure [imageProxy] is closed to release camera
     * resources. We attach a completion listener to close the image once ML Kit
     * finishes processing.
     */
    fun processImageProxy(imageProxy: ImageProxy) {
        // If detection is disabled (e.g. during editing), skip processing and close the frame.
        if (!_detectionEnabled.value) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)
            objectDetector.process(inputImage)
                .addOnSuccessListener { results ->
                    // Convert results to our DetectedObject model with normalized bounding boxes
                    val imageWidth = inputImage.width.toFloat()
                    val imageHeight = inputImage.height.toFloat()
                    val detectedList = results.map { mlObj: MlDetectedObject ->
                        val box = mlObj.boundingBox
                        // Normalize bounding box coordinates
                        val rectF = RectF(
                            box.left.toFloat() / imageWidth,
                            box.top.toFloat() / imageHeight,
                            box.right.toFloat() / imageWidth,
                            box.bottom.toFloat() / imageHeight
                        )
                        val label = mlObj.labels.firstOrNull()?.text
                        DetectedObject(
                            id = mlObj.trackingId ?: mlObj.hashCode(),
                            boundingBox = rectF,
                            label = label
                        )
                    }
                    _detections.value = detectedList
                }
                .addOnFailureListener {
                    // In case of detection error, clear detections or handle error appropriately
                    _detections.value = emptyList()
                }
                .addOnCompleteListener {
                    // Close the image to allow CameraX to provide the next frame
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}