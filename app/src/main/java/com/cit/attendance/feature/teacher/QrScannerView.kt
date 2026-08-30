package com.cit.attendance.feature.teacher

import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * The camera preview and barcode pipeline (Section 21).
 *
 * Built for a real handset: it selects the device's own back camera, falls back
 * to the front camera on hardware that has none, and reports a bind failure
 * upward instead of leaving a black rectangle on screen. Nothing here is
 * specific to an emulator or a development machine.
 *
 * Lifecycle matters as much as decoding. The use cases are bound to the
 * lifecycle for preview purposes *and* explicitly unbound when the composable
 * leaves, because this scanner lives inside a tab rather than its own
 * destination: without the explicit unbind the camera would keep running after
 * the teacher finished scanning, draining the battery and holding the camera
 * away from other apps.
 */
@Composable
fun QrScannerView(
    onCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCameraUnavailable: (Throwable) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Kept current so a recomposition cannot leave the analyzer calling into a
    // stale callback captured when the view was first created.
    val currentOnCodeScanned by rememberUpdatedState(onCodeScanned)
    val currentOnCameraUnavailable by rememberUpdatedState(onCameraUnavailable)

    val session = remember { ScannerSession() }

    DisposableEffect(Unit) {
        onDispose { session.release() }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            val previewView = PreviewView(viewContext).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                // COMPATIBLE renders through a TextureView, which behaves on the
                // widest range of handsets at the cost of a little efficiency.
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }

            val providerFuture = ProcessCameraProvider.getInstance(viewContext)
            providerFuture.addListener({
                val provider = runCatching { providerFuture.get() }.getOrElse { error ->
                    currentOnCameraUnavailable(error)
                    return@addListener
                }

                session.start(
                    provider = provider,
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    onCode = { currentOnCodeScanned(it) },
                    onFailure = currentOnCameraUnavailable,
                )
            }, ContextCompat.getMainExecutor(viewContext))

            previewView
        },
    )
}

/**
 * Owns everything the scanner allocates, so teardown happens in one place and
 * in the right order.
 */
private class ScannerSession {

    private var provider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null
    private var scanner: BarcodeScanner? = null
    private var executor: ExecutorService? = null
    private var released = false

    // imageProxy.image is opt-in rather than unsafe: CameraX asks callers to
    // acknowledge that the underlying Image is only valid until the proxy is
    // closed, which the analyzer below respects.
    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    fun start(
        provider: ProcessCameraProvider,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        previewView: PreviewView,
        onCode: (String) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        // The composable can be disposed while the provider future is still
        // resolving; binding after that would leak a running camera.
        if (released) {
            return
        }

        val barcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build(),
        )
        val analysisExecutor = Executors.newSingleThreadExecutor()

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val imageAnalysis = ImageAnalysis.Builder()
            // Drop frames rather than queue them: a slow verification round trip
            // must not build a backlog of stale codes.
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage == null || released) {
                imageProxy.close()
                return@setAnalyzer
            }

            // rotationDegrees comes from the device's own sensor orientation, so
            // the same code reads correctly however the phone is held.
            val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            barcodeScanner.process(input)
                .addOnSuccessListener { barcodes ->
                    if (!released) {
                        barcodes.firstNotNullOfOrNull { it.rawValue }?.let(onCode)
                    }
                }
                // A frame that fails to decode is normal, not an error worth
                // surfacing: the next frame will be along shortly.
                .addOnCompleteListener { imageProxy.close() }
        }

        val selector = selectorFor(provider)
        if (selector == null) {
            barcodeScanner.close()
            analysisExecutor.shutdown()
            onFailure(IllegalStateException("This device has no usable camera"))
            return
        }

        try {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
        } catch (error: Exception) {
            // Another app holding the camera, or a device that cannot satisfy
            // the requested use cases: tell the screen so it can say so.
            barcodeScanner.close()
            analysisExecutor.shutdown()
            onFailure(error)
            return
        }

        this.provider = provider
        this.analysis = imageAnalysis
        this.scanner = barcodeScanner
        this.executor = analysisExecutor
    }

    /** Prefers the back camera; falls back to the front on hardware without one. */
    private fun selectorFor(provider: ProcessCameraProvider): CameraSelector? = when {
        runCatching { provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) }
            .getOrDefault(false) -> CameraSelector.DEFAULT_BACK_CAMERA

        runCatching { provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) }
            .getOrDefault(false) -> CameraSelector.DEFAULT_FRONT_CAMERA

        else -> null
    }

    /**
     * Order matters: stop feeding the analyzer, release the camera, then close
     * the decoder. Closing the decoder while a frame is still in flight throws.
     */
    fun release() {
        released = true
        analysis?.clearAnalyzer()
        runCatching { provider?.unbindAll() }
        runCatching { scanner?.close() }
        executor?.shutdown()

        analysis = null
        provider = null
        scanner = null
        executor = null
    }
}
