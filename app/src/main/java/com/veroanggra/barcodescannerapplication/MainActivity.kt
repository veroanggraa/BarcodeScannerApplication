package com.veroanggra.barcodescannerapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.veroanggra.barcodescannerapplication.component.CircleButton
import com.veroanggra.barcodescannerapplication.component.CustomQrFrame
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
        var enableFlash by remember { mutableStateOf(false) }
        var isShowPopup by remember { mutableStateOf(false) }
        var isShowGallery by remember { mutableStateOf(false) }
        var barcodeData by remember { mutableStateOf("") }
        val context = LocalContext.current
        val barcodeScanner = remember { BarcodeScanning.getClient() }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    val bitmap = if (Build.VERSION.SDK_INT < 28) {
                        MediaStore.Images.Media.getBitmap(
                            context.contentResolver,
                            uri
                        )
                    } else {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source)
                    }
                    val image = InputImage.fromBitmap(bitmap, 0)
                    barcodeScanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            if (barcodes.isNotEmpty()) {
                                val firstResult = barcodes.first()
                                barcodeData = firstResult.rawValue ?: ""
                                isShowPopup = true
                            }
                        }
                }
            }
        }

        // check camera permission
        val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
        if (!cameraPermissionState.status.isGranted) {
            LaunchedEffect(Unit) {
                cameraPermissionState.launchPermissionRequest()
            }
        } else {
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
            ) {
                val (cameraPreview, galleryButton, flashButton, txtInstruction, scanFrame) = createRefs()
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .constrainAs(cameraPreview) {
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        },
                    factory = {
                        getCameraPreview(onBarcodeData = { data ->
                            barcodeData = data
                            isShowPopup = true
                        })
                    })

                Text(
                    text = "Scan barcode",
                    fontSize = 24.sp,
                    color = Color.White,
                    modifier = Modifier.constrainAs(txtInstruction) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top, margin = 100.dp)
                        end.linkTo(parent.end)
                    })

                CustomQrFrame(modifier = Modifier.constrainAs(scanFrame) {
                    top.linkTo(txtInstruction.bottom, 30.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                })

                CircleButton(modifier = Modifier.constrainAs(galleryButton) {
                    start.linkTo(parent.start, margin = 120.dp)
                    bottom.linkTo(parent.bottom, margin = 70.dp)
                },
                    size = 70, icon = R.drawable.icon_gallery, onClick = {
                        isShowGallery = true
                    })


                CircleButton(
                    modifier = Modifier
                        .constrainAs(flashButton) {
                            end.linkTo(parent.end, margin = 120.dp)
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
        if (isShowPopup) {
            AlertDialog(
                onDismissRequest = { isShowPopup = false },
                title = { Text("Barcode Data") },
                text = { Text(barcodeData) },
                confirmButton = {
                    Button(onClick = { isShowPopup = false }) {
                        Text("OK")
                    }
                }
            )
        }
        if (isShowGallery) {
            LaunchedEffect(Unit) {
                val galleryIntent = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
                launcher.launch(galleryIntent)
                isShowGallery = false
            }
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun getCameraPreview(onBarcodeData: (String) -> Unit): PreviewView {
        val options =
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
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
                if (firstResult.valueType == Barcode.TYPE_URL) {
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(firstResult.rawValue)
                    )
                    startActivity(browserIntent)
                } else {
                    onBarcodeData(firstResult.rawValue ?: "")

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

