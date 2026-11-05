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
import ar.edu.uade.lensvest.domain.model.Quote
import ar.edu.uade.lensvest.domain.model.RecognizedBrand
import ar.edu.uade.lensvest.domain.usecase.GetQuotesUseCase
import ar.edu.uade.lensvest.domain.usecase.RecognizeBrandUseCase
import ar.edu.uade.lensvest.domain.usecase.RecordScanUseCase
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
    private val recordScanUseCase: RecordScanUseCase,
    private val getQuotesUseCase: GetQuotesUseCase,
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

    /** D1 "Sí, es esa": saves it to the history and shows its price (D2). */
    fun confirmBrand() {
        val brand = (_uiState.value.scan as? ScanState.Recognized)?.brand ?: return
        _uiState.update { it.copy(scan = ScanState.Confirmed(brand)) }
        viewModelScope.launch {
            // The history is a convenience: failing to save it shouldn't block the price.
            val scanId = runCatching { recordScanUseCase(brand).id }.getOrNull()
            _uiState.update { state -> state.copy(scan = state.confirmedOr(brand) { it.copy(scanId = scanId) }) }
        }
        loadQuote(brand)
    }

    fun retryQuote() {
        (_uiState.value.scan as? ScanState.Confirmed)?.let { loadQuote(it.brand) }
    }

    private fun loadQuote(brand: RecognizedBrand) {
        val ticker = brand.ticker ?: return
        viewModelScope.launch {
            _uiState.update { state -> state.copy(scan = state.confirmedOr(brand) { it.copy(quoteError = null) }) }
            val update: (ScanState.Confirmed) -> ScanState.Confirmed = try {
                val quote = getQuotesUseCase(listOf(ticker)).firstOrNull()
                ({ it.copy(quote = quote, quoteError = if (quote == null) "No encontramos el precio de $ticker." else null) })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = e.toUserMessage()
                ({ it.copy(quoteError = message) })
            }
            _uiState.update { state -> state.copy(scan = state.confirmedOr(brand, update)) }
        }
    }

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
    /** D1: the AI's guess, waiting for the user to confirm it. */
    data class Recognized(val brand: RecognizedBrand) : ScanState
    /** D2: confirmed; the price arrives a few seconds later. */
    data class Confirmed(
        val brand: RecognizedBrand,
        val quote: Quote? = null,
        val quoteError: String? = null,
        val scanId: Long? = null,
    ) : ScanState
    data object NotFound : ScanState
    data class Failed(val message: String) : ScanState
}

/** Applies [change] only if the user is still looking at [brand]'s card. */
private fun LensUiState.confirmedOr(brand: RecognizedBrand, change: (ScanState.Confirmed) -> ScanState.Confirmed): ScanState =
    (scan as? ScanState.Confirmed)?.takeIf { it.brand == brand }?.let(change) ?: scan

data class LensUiState(
    val surfaceRequest: SurfaceRequest? = null,
    val hasFlash: Boolean = false,
    val isTorchOn: Boolean = false,
    val scan: ScanState = ScanState.Scanning,
)
