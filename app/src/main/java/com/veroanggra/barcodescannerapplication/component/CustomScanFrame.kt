package com.veroanggra.barcodescannerapplication.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun CustomScanFrame(modifier: Modifier) {
    Canvas(
        modifier = modifier
            .padding(horizontal = 10.dp)
            .fillMaxSize()
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val frameSize = 300.dp.toPx() // Define the size of the square frame
        val cornerRadius = 40.dp.toPx() // Radius of the rounded corners
        val strokeWidth = 4.dp.toPx()

        // Calculate the top-left corner of the square frame to center it
        val frameTopLeftX = (canvasWidth - frameSize) / 2
        val frameTopLeftY = (canvasHeight - frameSize) / 2

        // Top-left corner arc
        drawArc(
            color = Color.White,
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(frameTopLeftX, frameTopLeftY),
            size = Size(cornerRadius * 2, cornerRadius * 2), // Size of the bounding box for the arc
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Top-right corner arc
        drawArc(
            color = Color.White,
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(frameTopLeftX + frameSize - cornerRadius * 2, frameTopLeftY),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Bottom-left corner arc
        drawArc(
            color = Color.White,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(frameTopLeftX, frameTopLeftY + frameSize - cornerRadius * 2),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Bottom-right corner arc
        drawArc(
            color = Color.White,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(frameTopLeftX + frameSize - cornerRadius * 2, frameTopLeftY + frameSize - cornerRadius * 2),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}