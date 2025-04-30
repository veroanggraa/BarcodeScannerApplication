package com.veroanggra.barcodescannerapplication.di

import com.veroanggra.barcodescannerapplication.domain.ProcessGalleryImageUseCase
import com.veroanggra.barcodescannerapplication.domain.ScanBarcodeUseCase
import com.veroanggra.barcodescannerapplication.presentation.CameraViewModel
import com.veroanggra.barcodescannerapplication.repositories.BarcodeRepository
import com.veroanggra.barcodescannerapplication.repositories.BarcodeRepositoryImpl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koin.dsl.onClose

val appModule = module {
    single<BarcodeRepository> {
        BarcodeRepositoryImpl()
    } onClose { repository ->
        (repository as? BarcodeRepositoryImpl)?.closeScanner()
            ?: println("Warning: Could not close scanner for repository$repository")
    }
    factory { ScanBarcodeUseCase(get())}
    factory { ProcessGalleryImageUseCase(get()) }
    viewModel { CameraViewModel(get(), get()) }
}
