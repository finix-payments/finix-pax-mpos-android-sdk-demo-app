package com.finix.mpossampleapplication.utils

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream
import kotlin.apply
import kotlin.collections.forEachIndexed
import kotlin.collections.maxBy
import kotlin.math.roundToInt

fun drawOnCanvasAndGetBase64(
    path: List<Pair<Float, Float>>,
    width: Int?,
    height: Int?,
): String {
    if (path.isEmpty()) return ""
    val bitmapWidth = width ?: path.maxBy { it.first }.first.roundToInt()
    val bitmapHeight = height ?: path.maxBy { it.second }.second.roundToInt()
    if (bitmapWidth <= 0 || bitmapHeight <= 0) return ""
    val bitmap = drawToBitmap(path, bitmapWidth, bitmapHeight)
    return convertBitmapToBase64(bitmap)
}

fun convertBitmapToBase64(bitmap: Bitmap): String {
    // Convert the bitmap into a PNG format
    val byteArrayOutputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)

    // Encode the PNG image as Base64
    val byteArray = byteArrayOutputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.NO_WRAP)
}

fun drawToBitmap(
    path: List<Pair<Float, Float>>,
    width: Int,
    height: Int,
): Bitmap {
    val bitmap = ImageBitmap(width, height)
    val canvas = Canvas(bitmap)

    val signaturePaint =
        Paint().apply {
            isAntiAlias = true
            color = Color.Black
            strokeWidth = 5f
            style = PaintingStyle.Stroke
        }
    val guideLinePaint =
        Paint().apply {
            isAntiAlias = true
            color = Color.LightGray
            strokeWidth = 3f
            style = PaintingStyle.Stroke
        }

    canvas.drawPath(
        path.getPath(),
        signaturePaint,
    )

    canvas.drawPath(
        getHorizontalPath(width.toFloat(), height.toFloat()),
        guideLinePaint,
    )

    return bitmap.asAndroidBitmap()
}

fun List<Pair<Float, Float>>.getPath(): Path {
    val pathLine = Path()
    forEachIndexed { index, point ->
        if (index == 0) {
            pathLine.moveTo(point.first, point.second)
        } else {
            val prevPoint = this[index - 1]
            if (prevPoint.first == -1f) {
                pathLine.moveTo(point.first, point.second)
            } else {
                if (point.first != -1f) {
                    val x = point.first
                    val y = point.second
                    val prevX = prevPoint.first
                    val prevY = prevPoint.second

                    // Draw a Bézier curve segment
                    pathLine.quadraticTo(prevX, prevY, (x + prevX) / 2, (y + prevY) / 2)
                }
            }
        }
    }
    return pathLine
}

fun getHorizontalPath(
    width: Float,
    height: Float,
): Path {
    val pathLine = Path()
    val startY = (height / 4f) * 3f
    val startX = 0f
    val diffX = 10f
    pathLine.moveTo(startX + diffX, startY)
    pathLine.lineTo(width - diffX, startY)
    return pathLine
}
