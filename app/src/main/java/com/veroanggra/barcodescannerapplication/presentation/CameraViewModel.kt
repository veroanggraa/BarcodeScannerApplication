package com.veroanggra.barcodescannerapplication.presentation

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.common.Barcode
import com.veroanggra.barcodescannerapplication.domain.ProcessGalleryImageUseCase
import com.veroanggra.barcodescannerapplication.domain.ScanBarcodeUseCase
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CameraViewModel(
    private val scanBarcodeUseCase: ScanBarcodeUseCase,
    private val processGalleryImageUseCase: ProcessGalleryImageUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var cameraController: LifecycleCameraController? = null
    private var mlKitAnalyzer: MlKitAnalyzer? = null

    companion object {
        private const val TAG = "CameraViewModel"
    }

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Coroutine Exception: ${throwable.localizedMessage}", throwable)
        _uiState.update {
            it.copy(
                isLoading = false,
                selectedGalleryImageUri = null,
                errorMessage = "An unexpected error occurred: ${throwable.localizedMessage}"
            )
        }
    }

    fun setCameraController(controller: LifecycleCameraController) {
        cameraController = controller
        try {
            controller.enableTorch(uiState.value.isFlashEnabled)
        } catch (e: Exception) {
            Log.w(TAG, "Could not set initial flash state in setCameraController: ${e.message}")
        }
    }

    fun getAnalyzer(context: Context): MlKitAnalyzer {
        Log.d(TAG, "Requesting MLKit Analyzer")
        mlKitAnalyzer?.let { return it }
        val analyzer = scanBarcodeUseCase.execute(
            context = context,
            targetCoordinateSystem = ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
            onResult = { barcodes -> handleAnalyzerResult(barcodes) }
        )
        mlKitAnalyzer = analyzer
        return analyzer
    }

    private fun handleAnalyzerResult(barcodes: List<Barcode>?) {

        if (uiState.value.selectedGalleryImageUri != null || uiState.value.isLoading) return

        if (barcodes == null) {
            Log.w(TAG, "Analyzer returned null result for a frame")
            return
        }
        val firstValidBarcode = barcodes.firstOrNull {
            !it.rawValue.isNullOrBlank()
        }
        if (uiState.value.detectedBarcode?.rawValue != firstValidBarcode?.rawValue ||
            uiState.value.detectedBarcode?.boundingBox != firstValidBarcode?.boundingBox
        ) {
            Log.d(TAG, "Analyzer detected: ${firstValidBarcode?.rawValue}")
            updateStateWithBarcode(firstValidBarcode, showDialog = false)
        } else if (firstValidBarcode == null && uiState.value.detectedBarcode != null) {
            Log.d(TAG, "Analyzer: Barcode disappeared")
            updateStateWithBarcode(null, showDialog = false)
        }
    }

    private fun updateStateWithBarcode(barcode: Barcode?, showDialog: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                detectedBarcode = barcode,
                showBarcodeResultDialog = if (showDialog) barcode != null else currentState.showBarcodeResultDialog
            )
        }
    }

    fun toggleFlash(context: Context) {
        val controller = cameraController ?: run {
            Log.e(TAG, "Camera Controller not set, cannot toggle flash")
            _uiState.update {
                it.copy(errorMessage = "Camera not ready")
            }
            return
        }
        val targetState = !uiState.value.isFlashEnabled
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                controller.enableTorch(targetState).addListener({
                    _uiState.update {
                        it.copy(
                            isFlashEnabled = targetState, errorMessage = null
                        )
                    }
                    Log.d(TAG, "Flash toggled successfully: $targetState")
                }, ContextCompat.getMainExecutor(context))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle flash", e)
                _uiState.update { it.copy(errorMessage = "Failed to control flash") }
            }
        }
    }

    fun onGalleryImageSelected(uri: Uri?) {
        if (uri == null) {
            _uiState.update { it.copy(errorMessage = "Failed to get image form gallery") }
            return
        }
        Log.d(TAG, "Gallery image selected: $uri")
        _uiState.update {
            it.copy(
                selectedGalleryImageUri = uri,
                isLoading = false,
                errorMessage = null,
                detectedBarcode = null,
                showBarcodeResultDialog = false
            )
        }
    }

    fun confirmGalleryImageSelected(context: Context) {
        val uriToProcess = uiState.value.selectedGalleryImageUri ?: run {
            Log.e(TAG, "Confirm called but no image URI selected in state")
            _uiState.update { it.copy(errorMessage = "No image selected to confirm") }
            return
        }
        Log.d(TAG, "Confirming gallery image: $uriToProcess")
        viewModelScope.launch(coroutineExceptionHandler) {
            processGalleryImageUseCase.execute(context, uriToProcess)
                .onStart {
                    Log.d(TAG, "Starting gallery image processing after confirmation")
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                }
                .catch { e ->
                    Log.e(TAG, "Error processing confirmed gallery image flow", e)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedGalleryImageUri = null,
                            errorMessage = "Error processing image: ${e.localizedMessage}"
                        )
                    }
                }
                .collect { barcodeResult ->
                    Log.d(
                        TAG,
                        "Confirmed gallery image processed. Result: ${barcodeResult?.rawValue}"
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
//                            selectedGalleryImageUri = null,
                            detectedBarcode = barcodeResult,
                            showBarcodeResultDialog = barcodeResult != null,
                            errorMessage = if (barcodeResult == null) "No barcode found .in image" else null
                        )
                    }

                }
        }
    }

    fun cancelGalleryImageSelection() {
        Log.d(TAG, "Cancelling gallery image selection")
        _uiState.update {
            it.copy(
                selectedGalleryImageUri = null,
                detectedBarcode = null,
                showBarcodeResultDialog = false,
                isLoading = false,
                errorMessage = null
            )
        }
    }

    fun processGalleryUri(context: Context, uri: Uri?) {
        if (uri == null) {
            _uiState.update {
                it.copy(errorMessage = "Failed to get image from gallery")
            }
            return
        }
        viewModelScope.launch(coroutineExceptionHandler) {
            processGalleryImageUseCase.execute(context, uri)
                .onStart {
                    Log.d(TAG, "Starting gallery image processing")
                    _uiState.update {
                        it.copy(isLoading = true, errorMessage = null)
                    }
                }
                .catch { e ->
                    Log.e(TAG, "Error processing gallery image flow", e)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Error processing image: ${e.localizedMessage}"
                        )
                    }
                }
                .collect { barcodeResult ->
                    Log.d(TAG, "Gallery image processed. Result: ${barcodeResult?.rawValue}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            detectedBarcode = barcodeResult,
                            showBarcodeResultDialog = barcodeResult != null,
                            errorMessage = if (barcodeResult == null) "No barcode found in image" else null
                        )
                    }
                }
        }
    }

    fun dismissBarcodeDialog() {
        val wasShowingGalleryResult = uiState.value.selectedGalleryImageUri != null
        _uiState.update {
            it.copy(
                showBarcodeResultDialog = false,
                selectedGalleryImageUri = if (wasShowingGalleryResult) null else it.selectedGalleryImageUri,
                detectedBarcode = if (wasShowingGalleryResult) null else it.detectedBarcode
            )
        }
        if (wasShowingGalleryResult) {
            Log.d(TAG, "Dialog dismissed, returning camera from gallery confirmation")
        }
    }

    fun clearErrorMessage() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }

    fun showSnackbarMessage(message: String) {
        _uiState.update {
            it.copy(errorMessage = message)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "Viewmodel cleared")
    }
}