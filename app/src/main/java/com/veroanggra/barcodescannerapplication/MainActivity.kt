package com.veroanggra.barcodescannerapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.veroanggra.barcodescannerapplication.ui.theme.BarcodeScannerApplicationTheme
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var cameraController: LifecycleCameraController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BarcodeScannerApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CameraPreview()
                }
            }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun CameraPreview() {
        // check camera permission
        val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
        if (!cameraPermissionState.status.isGranted) {
            LaunchedEffect(Unit) {
                cameraPermissionState.launchPermissionRequest()
            }
        } else {
            var enableFlash by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { getCameraPreview() })
                CircleButton(
                    modifier = Modifier.align(alignment = Alignment.BottomCenter),
                    enableFlash = enableFlash,
                    onClick = {
                        enableFlash = !enableFlash
                        cameraController.cameraControl?.enableTorch(enableFlash)
                    })
            }
        }
    }

    @Composable
    fun CircleButton(enableFlash: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
        val vectorAsset =
            if (enableFlash) R.drawable.baseline_flash_on else R.drawable.baseline_flash_off
        Icon(
            painterResource(id = vectorAsset),
            contentDescription = "Flash",
            modifier = modifier
                .padding(bottom = 50.dp)
                .size(64.dp)
                .clickable {
                    onClick()
                })
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun getCameraPreview(): PreviewView {
        val options =
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        val barcodeScanner = BarcodeScanning.getClient(options)
        cameraController = LifecycleCameraController(this)
        val previewView = PreviewView(this)
        cameraController.cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        cameraController.setImageAnalysisAnalyzer(
            Executors.newSingleThreadExecutor(),
            MlKitAnalyzer(
                listOf(barcodeScanner),
                COORDINATE_SYSTEM_VIEW_REFERENCED,
                ContextCompat.getMainExecutor(this)
            ) { result ->
                val barcodeResults = result?.getValue(barcodeScanner)
                if (barcodeResults?.firstOrNull() == null) {
                    previewView.overlay.clear()
                    previewView.setOnTouchListener { _, _ ->
                        false
                    }
                    return@MlKitAnalyzer
                }
                val firstResult = barcodeResults.first()
                val drawable = firstResult.boundingBox?.let {
                    QrCodeHighlightDrawable(it)
                }
                previewView.overlay.clear()
                drawable?.let {
                    previewView.overlay.add(
                        it
                    )
                }
                previewView.setOnTouchListener { _, event ->
                    if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                        if (firstResult.boundingBox!!.contains(event.x.toInt(), event.y.toInt())) {
                            if (firstResult.valueType == Barcode.TYPE_URL) {
                                val browserIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(firstResult.rawValue)
                                )
                                startActivity(browserIntent)
                            }
                        }
                    }
                    true
                }
            })
        cameraController.bindToLifecycle(this)
        previewView.controller = cameraController
        return previewView
    }
}

