package com.veroanggra.barcodescannerapplication.repositories

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.IOException


class BarcodeRepositoryImpl : BarcodeRepository {
    companion object {
        private const val TAG = "BarcodeRepositoryImpl"
    }

    private val scannerOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()

    private val barcodeScanner: BarcodeScanner by lazy {
        BarcodeScanning.getClient(scannerOptions)
    }

    override fun getBarcodeScannerInstance(): BarcodeScanner {
        return barcodeScanner
    }

    override fun processBitmapForBarcodes(bitmap: Bitmap): Flow<List<Barcode>> = callbackFlow {
        val image = InputImage.fromBitmap(bitmap, 0)

        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                Log.d(TAG, "Bitmap processing success: ${barcodes.size} barcodes found")
                trySend(barcodes).isSuccess
                close()
            }
            .addOnFailureListener { errors ->
                Log.e(TAG, "Bitmap processing failed", errors)
                close(errors)
            }
        awaitClose {
            Log.d(TAG, "Bitmap processing flow closing/cancelled")
        }
    }

    override fun processUriForBarcodes(context: Context, uri: Uri): Flow<List<Barcode>> =
        callbackFlow {
            try {
                val image = InputImage.fromFilePath(context, uri)
                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        Log.d(TAG, "URI processing success: ${barcodes.size} barcodes found")
                        trySend(barcodes).isSuccess
                        close()
                    }
                    .addOnFailureListener { errors ->
                        Log.e(TAG, "URI processing failed", errors)
                        close(errors)
                    }
            } catch (e: IOException) {
                Log.e(TAG, "Failed to create InputImage from Uri: $uri", e)
                close(e)
            } catch (e: Exception) {
                Log.e(TAG, "An unexpected error occured during URI processing", e)
                close(e)
            }
            awaitClose {
                Log.d(TAG, "URI processing flow closing/cancelled")
            }
        }

    override fun closeScanner() {
        try {
            barcodeScanner.close()
            Log.i(TAG, "ML Kit Barcode Scanner instance closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing barcode scanner", e)
        }
    }
}