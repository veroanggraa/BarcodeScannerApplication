package com.veroanggra.barcodescannerapplication.repositories

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.flow.Flow

interface BarcodeRepository {
    fun getBarcodeScannerInstance(): BarcodeScanner
    fun processBitmapForBarcodes(bitmap: Bitmap) : Flow<List<Barcode>>
    fun processUriForBarcodes(context: Context, uri: Uri): Flow<List<Barcode>>
    fun closeScanner()
}