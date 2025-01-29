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
        val width = size.width
        val height = 1000f
        val cornerRadius = 40.dp.toPx() // Changed to 40dp
        val strokeWidth = 4.dp.toPx()
        val horizontalPadding = (width - (width - 300f)) / 2
        val verticalPadding = 100f

        // Top-left
        drawArc(
            color = Color.White,
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(horizontalPadding, verticalPadding),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Top-right
        drawArc(
            color = Color.White,
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(width - horizontalPadding - cornerRadius * 2, verticalPadding),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Bottom-left
        drawArc(
            color = Color.White,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(horizontalPadding, verticalPadding + height - cornerRadius * 2),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Bottom-right
        drawArc(
            color = Color.White,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(width - horizontalPadding - cornerRadius * 2, verticalPadding + height - cornerRadius * 2),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}