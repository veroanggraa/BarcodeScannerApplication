package com.veroanggra.barcodescannerapplication.presentation

import android.graphics.Rect
import com.google.mlkit.vision.barcode.common.Barcode

data class CameraUiState(
    val detectedBarcode: Barcode? = null,
    val isFlashEnabled: Boolean = false,
    val showBarcodeResultDialog: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val analyzerError: String? = null
) {
    val detectedBarcodeValue: String?
        get() = detectedBarcode?.rawValue

    val detectedBarcodeType: Rect?
        get() = detectedBarcode?.boundingBox
}