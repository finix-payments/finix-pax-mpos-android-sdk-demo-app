package com.finix.mpossampleapplication.ui.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.finix.mpossampleapplication.utils.drawOnCanvasAndGetBase64
import com.finix.mpossampleapplication.utils.getHorizontalPath
import com.finix.mpossampleapplication.utils.getPath

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureBottomSheet(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val path = remember { mutableStateListOf<Pair<Float, Float>>() }
    var componentSize by remember { mutableStateOf(IntSize.Zero) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier,
        ) {
            Canvas(
                modifier =
                    Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                        .background(Color.LightGray)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()
                                path.add(
                                    Pair(
                                        down.position.x.coerceIn(0f, size.width.toFloat()),
                                        down.position.y.coerceIn(0f, size.height.toFloat()),
                                    ),
                                )
                                do {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    change.consume()
                                    if (change.pressed) {
                                        path.add(
                                            Pair(
                                                change.position.x.coerceIn(0f, size.width.toFloat()),
                                                change.position.y.coerceIn(0f, size.height.toFloat()),
                                            ),
                                        )
                                    } else {
                                        path.add(Pair(-1f, -1f))
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }.onGloballyPositioned { coordinates ->
                            componentSize = coordinates.size
                        },
            ) {
                // Horizontal line
                drawPath(
                    path = getHorizontalPath(size.width - 16f, size.height),
                    color = Color(0xFF7D90A5),
                    style = Stroke(width = 3f),
                )

                // Signature line
                drawPath(
                    path = path.getPath(),
                    color = Color.Black,
                    style = Stroke(width = 5f),
                )
            }
            Button(
                onClick = {
                    onConfirm(
                        drawOnCanvasAndGetBase64(
                            path,
                            componentSize.width,
                            componentSize.height,
                        ),
                    )
                },
                enabled = path.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Submit",
                )
            }
            Button(
                onClick = { path.clear() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Clear")
            }
        }
    }
}
