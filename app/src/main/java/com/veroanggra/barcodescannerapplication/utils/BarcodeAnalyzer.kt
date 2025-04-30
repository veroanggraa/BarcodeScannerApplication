package com.veroanggra.barcodescannerapplication.utils

import android.media.Image
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer {
    private val scanner: BarcodeScanner = BarcodeScanning.getClient()

    fun analyze(image: Image): String? {
        val inputImage = InputImage.fromMediaImage(image, 0)
        var result: String? = null
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    result = barcode.rawValue
                }
            }
            .addOnFailureListener {
            }
        return result
    }
}