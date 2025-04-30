package com.veroanggra.barcodescannerapplication.domain

import android.content.Context
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import com.veroanggra.barcodescannerapplication.repositories.BarcodeRepository

class ScanBarcodeUseCase(private val barcodeRepository: BarcodeRepository) {
    fun execute(
        context: Context,
        targetCoordinateSystem: Int,
        onResult: (List<Barcode>?) -> Unit
    ): MlKitAnalyzer {
        val barcodeScanner: BarcodeScanner = barcodeRepository.getBarcodeScannerInstance()
        return MlKitAnalyzer(
            listOf(barcodeScanner),
            targetCoordinateSystem,
            ContextCompat.getMainExecutor(context)
        ) { resultValue ->
            val barcodes: List<Barcode>? = resultValue.getValue(barcodeScanner)
            onResult(barcodes)
        }
    }
}