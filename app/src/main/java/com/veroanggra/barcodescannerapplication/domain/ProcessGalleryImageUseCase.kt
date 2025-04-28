package com.veroanggra.barcodescannerapplication.domain

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.common.Barcode
import com.veroanggra.barcodescannerapplication.repositories.BarcodeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProcessGalleryImageUseCase(private val barcodeRepository: BarcodeRepository) {
    fun execute(context: Context, uri: Uri): Flow<Barcode?> {
        return barcodeRepository.processUriForBarcodes(context, uri)
            .map { barcodes ->
                barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
            }
    }
}