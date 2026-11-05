package ar.edu.uade.lensvest.presentation.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.lensvest.domain.model.RecognizedBrand
import ar.edu.uade.lensvest.domain.usecase.RecognizeBrandUseCase
import ar.edu.uade.lensvest.mapper.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

/** Longest side of the photo sent for recognition: enough to read a logo, small to upload. */
private const val MAX_PHOTO_SIDE = 1024

/** Lens Mode: live preview, and recognizing the brand in a photo of it. */
@HiltViewModel
class LensViewModel @Inject constructor(
    private val recognizeBrandUseCase: RecognizeBrandUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LensUiState())
    val uiState = _uiState.asStateFlow()

    private var camera: Camera? = null
    // Photo decoding and scaling happen off the main thread (RNF04).
    private val photoExecutor = Executors.newSingleThreadExecutor()

    private val preview = Preview.Builder().build().apply {
        setSurfaceProvider { request -> _uiState.update { it.copy(surfaceRequest = request) } }
    }
    private val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    /** Binds the back camera until the calling coroutine is cancelled (the screen leaves). */
    suspend fun bindToCamera(appContext: Context, lifecycleOwner: LifecycleOwner) {
        val provider = ProcessCameraProvider.awaitInstance(appContext)
        val bound = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
        camera = bound
        _uiState.update { it.copy(hasFlash = bound.cameraInfo.hasFlashUnit()) }
        try {
            awaitCancellation()
        } finally {
            provider.unbind(preview, imageCapture)
            camera = null
            _uiState.update { it.copy(surfaceRequest = null, isTorchOn = false) }
        }
    }

    fun toggleTorch() {
        val on = !_uiState.value.isTorchOn
        camera?.cameraControl?.enableTorch(on)
        _uiState.update { it.copy(isTorchOn = on) }
    }

    /** Takes a photo and asks which brand it shows. The photo is never stored (RNF05). */
    fun recognize() {
        if (_uiState.value.scan is ScanState.Recognizing) return
        viewModelScope.launch {
            _uiState.update { it.copy(scan = ScanState.Recognizing) }
            val scan = try {
                recognizeBrandUseCase(takePhoto())?.let { ScanState.Recognized(it) } ?: ScanState.NotFound
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ScanState.Failed(e.toUserMessage())
            }
            _uiState.update { it.copy(scan = scan) }
        }
    }

    fun scanAgain() = _uiState.update { it.copy(scan = ScanState.Scanning) }

    private suspend fun takePhoto(): ByteArray = suspendCancellableCoroutine { continuation ->
        imageCapture.takePicture(
            photoExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val jpeg = runCatching { image.use { it.toUploadJpeg() } }
                    jpeg.onSuccess(continuation::resume).onFailure(continuation::resumeWithException)
                }

                override fun onError(exception: ImageCaptureException) {
                    continuation.resumeWithException(exception)
                }
            },
        )
    }

    override fun onCleared() {
        photoExecutor.shutdown()
    }
}

/** Upright, downscaled JPEG of the captured frame. */
private fun ImageProxy.toUploadJpeg(): ByteArray {
    val frame = toBitmap()
    val scale = MAX_PHOTO_SIDE.toFloat() / max(frame.width, frame.height)
    val matrix = Matrix().apply {
        postRotate(imageInfo.rotationDegrees.toFloat())
        if (scale < 1f) postScale(scale, scale)
    }
    val upright = Bitmap.createBitmap(frame, 0, 0, frame.width, frame.height, matrix, true)
    return ByteArrayOutputStream().use { out ->
        upright.compress(Bitmap.CompressFormat.JPEG, 85, out)
        out.toByteArray()
    }
}

sealed interface ScanState {
    data object Scanning : ScanState
    data object Recognizing : ScanState
    data class Recognized(val brand: RecognizedBrand) : ScanState
    data object NotFound : ScanState
    data class Failed(val message: String) : ScanState
}

data class LensUiState(
    val surfaceRequest: SurfaceRequest? = null,
    val hasFlash: Boolean = false,
    val isTorchOn: Boolean = false,
    val scan: ScanState = ScanState.Scanning,
)
