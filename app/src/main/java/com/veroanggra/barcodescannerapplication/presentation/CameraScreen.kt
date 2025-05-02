package com.veroanggra.barcodescannerapplication.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.common.Barcode
import com.veroanggra.barcodescannerapplication.component.QrCodeHighlightDrawable
import com.veroanggra.barcodescannerapplication.R
import com.veroanggra.barcodescannerapplication.component.CircleButton
import com.veroanggra.barcodescannerapplication.component.CustomScanFrame
import com.veroanggra.barcodescannerapplication.utils.setClipboard
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(viewModel: CameraViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val galleryLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                viewModel.onGalleryImageSelected(result.data?.data)
            } else {
                Log.d("CameraScreen", "Gallery selection cencelled or failed")
            }
        }
    LaunchedEffect(cameraPermissionState.status) {
        if (!cameraPermissionState.status.isGranted && !cameraPermissionState.status.shouldShowRationale) {
            Log.d("CameraScreen", "Requesting camera permission")
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        val snackBarHostState = remember { SnackbarHostState() }
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.selectedGalleryImageUri == null) {
                if (cameraPermissionState.status.isGranted) {
                    CameraContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        lifecycleOwner = lifecycleOwner,
                        onGalleryClick = {
                            val galleryIntent = Intent(
                                Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            ).apply { type = "image/*" }
                            galleryLauncher.launch(galleryIntent)
                        },
                        onFlashClick = { viewModel.toggleFlash(context) }
                    )
                } else {
                    PermissionDeniedContent(cameraPermissionState)
                }
            } else {
                GalleryImageConfirmation(
                    imageUri = uiState.selectedGalleryImageUri!!,
                    onConfirm = { viewModel.confirmGalleryImageSelected(context) },
                    onCancel = { viewModel.cancelGalleryImageSelection() }
                )
            }
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            SnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }

        if (uiState.showBarcodeResultDialog && uiState.detectedBarcode != null) {
            BarcodeResultDialog(
                barcode = uiState.detectedBarcode,
                onDismiss = { viewModel.dismissBarcodeDialog() },
                onOpenLink = { url ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("CameraScreen", "Failed to Open URL: $url", e)
                        viewModel.showSnackbarMessage("Could not open link")
                    }
                },
                onCopyText = { text ->
                    context.setClipboard("Barcode", text)
                    viewModel.showSnackbarMessage("Copied to clipboard")
                }
            )
        }
        uiState.errorMessage?.let { message ->
            LaunchedEffect(message) {
                snackBarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearErrorMessage()
            }
        }
        uiState.analyzerError?.let { message ->
            LaunchedEffect(message) {
                snackBarHostState.showSnackbar(
                    message = "Analyzer: $message",
                    duration = SnackbarDuration.Indefinite
                )
            }
        }
    }
}

@SuppressLint("ClickableViewAccessibility") // For PreviewView touch listener
@Composable
fun CameraContent(
    uiState: CameraUiState,
    viewModel: CameraViewModel,
    lifecycleOwner: LifecycleOwner,
    onGalleryClick: () -> Unit,
    onFlashClick: () -> Unit
) {
    val context = LocalContext.current
    val cameraController = remember { LifecycleCameraController(context) }
    val previewView = remember {
        PreviewView(context).apply {
            this.scaleType = PreviewView.ScaleType.FILL_CENTER
            this.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(lifecycleOwner, cameraController) {
        Log.d("CameraContent", "Binding CameraX use cases.")
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val analyzer = viewModel.getAnalyzer(context)

        try {
            cameraController.unbind()
            cameraController.bindToLifecycle(lifecycleOwner)
            cameraController.cameraSelector = cameraSelector
            cameraController.setImageAnalysisAnalyzer(
                ContextCompat.getMainExecutor(context), // Use main executor for analyzer thread pool
                analyzer
            )
            previewView.controller = cameraController
            viewModel.setCameraController(cameraController)
            Log.d("CameraContent", "CameraX binding successful.")

        } catch (exc: Exception) {
            Log.e("CameraContent", "Use case binding failed", exc)
            viewModel.showSnackbarMessage("Failed to start camera: ${exc.localizedMessage}")
        }
    }


    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        val (cameraPreview, buttonsRow, txtInstruction, scanFrame, txtResult) = createRefs()

        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .constrainAs(cameraPreview) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            update = { view ->
                view.overlay.clear()
                if (uiState.selectedGalleryImageUri == null) {
                    uiState.detectedBarcodeBoundingBox?.let { bounds ->
                        if (!bounds.isEmpty) { // Avoid drawing empty rects
                            val drawable = QrCodeHighlightDrawable(bounds) // Use your drawable
                            view.overlay.add(drawable)
                        }
                    }
                }
            }
        )

        Text(
            text = "Scan barcode",
            fontSize = 24.sp,
            color = Color.White, // Ensure contrast
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .constrainAs(txtInstruction) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                }
                .padding(top = 60.dp, start = 16.dp, end = 16.dp)
                .systemBarsPadding()
        )

        if (uiState.detectedBarcodeValue != null && uiState.selectedGalleryImageUri == null) {
            Text(
                text = "Result: ${uiState.detectedBarcodeValue}",
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .constrainAs(txtResult) {
                        top.linkTo(txtInstruction.bottom, margin = 8.dp)
                        start.linkTo(parent.start, margin = 16.dp)
                        end.linkTo(parent.end, margin = 16.dp)
                        width = Dimension.fillToConstraints
                    }
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        if (uiState.selectedGalleryImageUri == null) {
            CustomScanFrame(modifier = Modifier.constrainAs(scanFrame) {
                val topMargin = if (uiState.detectedBarcodeValue != null && uiState.selectedGalleryImageUri == null) 20.dp else 30.dp
                val topAnchor =
                    if (uiState.detectedBarcodeValue != null) txtResult.bottom else txtInstruction.bottom

                top.linkTo(topAnchor, margin = topMargin)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(buttonsRow.top, 30.dp)
                width = Dimension.percent(0.7f)
                height = Dimension.ratio("1:1")
            })
        }

        if (uiState.selectedGalleryImageUri == null) {
            Row(
                modifier = Modifier
                    .constrainAs(buttonsRow) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                    }
                    .fillMaxWidth()
                    .padding(bottom = 50.dp, start = 20.dp, end = 20.dp)
                    .systemBarsPadding(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleButton(
                    size = 70,
                    icon = R.drawable.icon_gallery,
                    onClick = onGalleryClick,
                    contentDescription = "Open Gallery"
                )

                CircleButton(
                    size = 70,
                    icon = if (uiState.isFlashEnabled) R.drawable.icon_flash_off else R.drawable.icon_flash_on,
                    onClick = onFlashClick,
                    contentDescription = if (uiState.isFlashEnabled) "Turn Flash Off" else "Turn Flash On"
                )
            }
        }
    }
}

@Composable
fun GalleryImageConfirmation(imageUri: Uri, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Image(
            painter = rememberAsyncImagePainter(imageUri),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp),
            contentScale = ContentScale.Fit
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 30.dp, start = 20.dp, end = 20.dp)
                .systemBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel")
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Cancel")
            }
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Confirm")
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Confirm")
            }
        }
    }


}

@Composable
fun BarcodeResultDialog(
    barcode: Barcode?,
    onDismiss: () -> Unit,
    onOpenLink: (String) -> Unit,
    onCopyText: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_dialog_info),
                contentDescription = "Barcode"
            )
        },
        title = { Text("Barcode Detected") },
        text = {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    "Type: ${getBarcodeTypeName(barcode?.valueType!!)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Data:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = barcode?.rawValue ?: "N/A",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (barcode?.valueType == Barcode.TYPE_URL && barcode.rawValue != null) {
                    TextButton(onClick = {
                        onOpenLink(barcode.rawValue!!)
                        onDismiss()
                    }) {
                        Text("Open Link")
                    }
                }
                barcode?.rawValue?.let {
                    TextButton(onClick = {
                        onCopyText(it)
                        onDismiss()
                    }) {
                        Text("Copy")
                    }
                }
            }
        }
    )
}

fun getBarcodeTypeName(type: Int): String {
    return when (type) {
        Barcode.TYPE_URL -> "URL"
        Barcode.TYPE_CONTACT_INFO -> "Contact Info"
        Barcode.TYPE_EMAIL -> "Email"
        Barcode.TYPE_ISBN -> "ISBN"
        Barcode.TYPE_PHONE -> "Phone"
        Barcode.TYPE_PRODUCT -> "Product"
        Barcode.TYPE_SMS -> "SMS"
        Barcode.TYPE_TEXT -> "Text"
        Barcode.TYPE_WIFI -> "WiFi"
        Barcode.TYPE_GEO -> "Geo Point"
        Barcode.TYPE_CALENDAR_EVENT -> "Calendar Event"
        Barcode.TYPE_DRIVER_LICENSE -> "Driver License"
        else -> "Unknown"
    }
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionDeniedContent(
    cameraPermissionState: PermissionState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val textToShow = if (cameraPermissionState.status.shouldShowRationale) {
            "Scanning barcodes requires camera access. Please grant the permission to continue."
        } else {
            "Camera permission is required for scanning. Please enable it in your device settings to use this feature."
        }
        Icon(
            painter = painterResource(id = android.R.drawable.ic_menu_report_image), // Add a relevant icon
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(textToShow, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
            Text("Grant Permission")
        }
    }
}