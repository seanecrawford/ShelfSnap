package com.example.shelfsnap.scan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import android.graphics.RectF

/**
 * Represents an editable object with a normalized bounding box and optional label.
 */
data class EditableObject(
    val id: Int,
    val label: String?,
    var box: RectF
)

/**
 * Hosts the camera preview and provides the ability to freeze detections and edit them interactively.
 *
 * When detection is active, it overlays red bounding boxes for each detected object.
 * When frozen, the bounding boxes turn green and can be dragged around the screen. Dragging updates
 * the normalized bounding box coordinates for each object. Tapping "Freeze & Edit" will copy the
 * current detection results into editable objects. Tapping "Resume Detection" will resume live
 * detection and clear any edits.
 */
@Composable
fun ScanScreen(scanViewModel: ScanViewModel) {
    val detections by scanViewModel.detections.collectAsState()
    // Whether detection is active or frozen for editing
    val detectionFrozen = remember { mutableStateOf(false) }
    // List of editable objects derived from detections when frozen
    val editableObjects = remember { mutableStateOf(listOf<EditableObject>()) }
    // Index of the currently selected object during a drag
    val selectedIndex = remember { mutableStateOf(-1) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val canvasWidthPx = with(density) { maxWidth.toPx() }
        val canvasHeightPx = with(density) { maxHeight.toPx() }

        // Camera preview always visible
        CameraScreen(scanViewModel)

        // Overlay canvas for drawing and dragging bounding boxes
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(detectionFrozen.value, editableObjects.value) {
                    // Only handle drag gestures when editing mode is active
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (detectionFrozen.value) {
                                val x = offset.x
                                val y = offset.y
                                // Determine which object is being dragged by checking if the touch point lies within a box
                                editableObjects.value.forEachIndexed { index, obj ->
                                    val left = obj.box.left * canvasWidthPx
                                    val top = obj.box.top * canvasHeightPx
                                    val right = obj.box.right * canvasWidthPx
                                    val bottom = obj.box.bottom * canvasHeightPx
                                    if (x >= left && x <= right && y >= top && y <= bottom) {
                                        selectedIndex.value = index
                                        return@forEachIndexed
                                    }
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (detectionFrozen.value && selectedIndex.value >= 0) {
                                change.consume()
                                // Convert drag amount from pixels to normalized units
                                val dxRel = dragAmount.x / canvasWidthPx
                                val dyRel = dragAmount.y / canvasHeightPx
                                val idx = selectedIndex.value
                                // Update the box for the selected object, clamping within [0,1]
                                val updated = editableObjects.value.mapIndexed { index, obj ->
                                    if (index == idx) {
                                        val width = obj.box.width()
                                        val height = obj.box.height()
                                        val newLeft = (obj.box.left + dxRel).coerceIn(0f, 1f - width)
                                        val newTop = (obj.box.top + dyRel).coerceIn(0f, 1f - height)
                                        val newRect = RectF(
                                            newLeft,
                                            newTop,
                                            newLeft + width,
                                            newTop + height
                                        )
                                        obj.copy(box = newRect)
                                    } else obj
                                }
                                editableObjects.value = updated
                            }
                        },
                        onDragEnd = {
                            selectedIndex.value = -1
                        },
                        onDragCancel = {
                            selectedIndex.value = -1
                        }
                    )
                }
        ) {
            if (!detectionFrozen.value) {
                // Draw red bounding boxes for live detections
                detections.forEach { detected ->
                    val left = detected.boundingBox.left * size.width
                    val top = detected.boundingBox.top * size.height
                    val right = detected.boundingBox.right * size.width
                    val bottom = detected.boundingBox.bottom * size.height
                    drawRect(
                        color = Color.Red,
                        topLeft = Offset(left, top),
                        size = Size(right - left, bottom - top),
                        style = Stroke(width = 3f)
                    )
                }
            } else {
                // Draw green bounding boxes for editable objects
                editableObjects.value.forEach { obj ->
                    val left = obj.box.left * size.width
                    val top = obj.box.top * size.height
                    val right = obj.box.right * size.width
                    val bottom = obj.box.bottom * size.height
                    drawRect(
                        color = Color.Green,
                        topLeft = Offset(left, top),
                        size = Size(right - left, bottom - top),
                        style = Stroke(width = 3f)
                    )
                }
            }
        }

        // Freeze/resume button at bottom center
        if (!detectionFrozen.value) {
            Button(
                onClick = {
                    // Freeze detection and copy current detections into editable objects
                    detectionFrozen.value = true
                    // Disable detection in the view model to prevent updates
                    scanViewModel.setDetectionEnabled(false)
                    editableObjects.value = detections.map { det ->
                        EditableObject(
                            id = det.id,
                            label = det.label,
                            box = RectF(
                                det.boundingBox.left,
                                det.boundingBox.top,
                                det.boundingBox.right,
                                det.boundingBox.bottom
                            )
                        )
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Text(text = "Freeze & Edit")
            }
        } else {
            Button(
                onClick = {
                    // Resume detection and clear editable objects
                    detectionFrozen.value = false
                    // Re-enable detection in the view model
                    scanViewModel.setDetectionEnabled(true)
                    editableObjects.value = emptyList()
                    selectedIndex.value = -1
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Text(text = "Resume Detection")
            }
        }
    }
}