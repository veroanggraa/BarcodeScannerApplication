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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.veroanggra.barcodescannerapplication.component.CircleButton
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
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
            ) {
                val (cameraPreview, galleryButton, cameraButton, flashButton) = createRefs()
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .constrainAs(cameraPreview) {
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        },
                    factory = { getCameraPreview() })

                CircleButton(modifier = Modifier.constrainAs(galleryButton) {
                    bottom.linkTo(parent.bottom, margin = 70.dp)
                    end.linkTo(cameraButton.start, margin = 30.dp)
                },
                    size = 70, icon = R.drawable.icon_gallery, onClick = {})

                CircleButton(modifier = Modifier.constrainAs(cameraButton) {
                    bottom.linkTo(parent.bottom, margin = 100.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }, size = 80, icon = R.drawable.icon_camera, onClick = {})

                CircleButton(
                    modifier = Modifier
                        .constrainAs(flashButton) {
                            start.linkTo(cameraButton.end, margin = 30.dp)
                            bottom.linkTo(parent.bottom, margin = 70.dp)
                        },
                    size = 70,
                    icon = if (enableFlash) R.drawable.icon_flash_off else R.drawable.icon_flash_on,
                    onClick = {
                        enableFlash = !enableFlash
                        cameraController.cameraControl?.enableTorch(enableFlash)
                    }
                )
            }
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun getCameraPreview(): PreviewView {
        val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        val barcodeScanner = BarcodeScanning.getClient(options)
        cameraController = LifecycleCameraController(this)
        val previewView = PreviewView(this)
        cameraController.cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
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

