package com.veroanggra.barcodescannerapplication.presentation

import android.graphics.Rect
import android.net.Uri
import com.google.mlkit.vision.barcode.common.Barcode

data class CameraUiState(
    val detectedBarcode: Barcode? = null,
    val isFlashEnabled: Boolean = false,
    val showBarcodeResultDialog: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val analyzerError: String? = null,
    val selectedGalleryImageUri: Uri? = null
) {
    val detectedBarcodeValue: String?
        get() = detectedBarcode?.rawValue

    val detectedBarcodeBoundingBox: Rect?
        get() = detectedBarcode?.boundingBox
}