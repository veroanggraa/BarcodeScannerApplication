package com.veroanggra.barcodescannerapplication

import android.app.Application
import com.veroanggra.barcodescannerapplication.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class ScanBarcodeApp: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.INFO)
            androidContext(this@ScanBarcodeApp)
            modules(appModule)
        }
    }
}