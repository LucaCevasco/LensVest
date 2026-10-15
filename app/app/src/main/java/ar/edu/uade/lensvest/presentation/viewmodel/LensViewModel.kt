package ar.edu.uade.lensvest.presentation.viewmodel

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Camera preview for Lens Mode. Brand recognition will analyze these frames (next phase). */
@HiltViewModel
class LensViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(LensUiState())
    val uiState = _uiState.asStateFlow()

    private var camera: Camera? = null

    private val preview = Preview.Builder().build().apply {
        setSurfaceProvider { request -> _uiState.update { it.copy(surfaceRequest = request) } }
    }

    /** Binds the back camera until the calling coroutine is cancelled (the screen leaves). */
    suspend fun bindToCamera(appContext: Context, lifecycleOwner: LifecycleOwner) {
        val provider = ProcessCameraProvider.awaitInstance(appContext)
        val bound = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
        camera = bound
        _uiState.update { it.copy(hasFlash = bound.cameraInfo.hasFlashUnit()) }
        try {
            awaitCancellation()
        } finally {
            provider.unbind(preview)
            camera = null
            _uiState.update { it.copy(surfaceRequest = null, isTorchOn = false) }
        }
    }

    fun toggleTorch() {
        val on = !_uiState.value.isTorchOn
        camera?.cameraControl?.enableTorch(on)
        _uiState.update { it.copy(isTorchOn = on) }
    }
}

data class LensUiState(
    val surfaceRequest: SurfaceRequest? = null,
    val hasFlash: Boolean = false,
    val isTorchOn: Boolean = false,
)
