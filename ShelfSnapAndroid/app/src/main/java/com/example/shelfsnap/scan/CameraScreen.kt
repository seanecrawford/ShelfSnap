package com.example.shelfsnap.scan

import android.Manifest
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.example.shelfsnap.scan.ScanViewModel

/**
 * Displays a camera preview and allows capturing a frame for analysis.
 * This is a simplified example using CameraX. In a production app, handle
 * permissions, lifecycle, and error cases appropriately.
 */
@Composable
fun CameraScreen(scanViewModel: ScanViewModel) {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                val provider = cameraProviderFuture.get()
                val preview = androidx.camera.core.Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val selector = CameraSelector.DEFAULT_BACK_CAMERA
                val analyzer = ImageAnalysis.Builder()
                    .setTargetResolution(Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                    // Delegate analysis to the view model
                    scanViewModel.processImageProxy(imageProxy)
                }
                provider.unbindAll()
                provider.bindToLifecycle(
                    ctx as androidx.lifecycle.LifecycleOwner,
                    selector,
                    preview,
                    analyzer
                )
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        // Capture button could be added here to trigger a frame capture or detection.
    }
}