package io.github.bugburrito.ezcrop.core

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

internal fun computeTargetOffset(
    currentOffset: Offset,
    minOffset: Offset,
    maxOffset: Offset,
    currentScale: Float,
    targetScale: Float,
    rawImageSize: IntSize,
): Offset {
    var targetOffsetX = currentOffset.x
    var targetOffsetY = currentOffset.y
    if (currentScale != targetScale && currentScale != 0F) {
        val cropCircleCenterX = (minOffset.x + maxOffset.x) / 2f
        val cropCircleCenterY = (minOffset.y + maxOffset.y) / 2f
        val cropCircleCenter =
            Offset(cropCircleCenterX, cropCircleCenterY)
        val imagePointUnderCircleCenterX =
            (cropCircleCenter.x - currentOffset.x) / currentScale
        val imagePointUnderCircleCenterY =
            (cropCircleCenter.y - currentOffset.y) / currentScale
        targetOffsetX =
            cropCircleCenter.x - (imagePointUnderCircleCenterX * targetScale)
        targetOffsetY =
            cropCircleCenter.y - (imagePointUnderCircleCenterY * targetScale)
    }

    val targetScaledImageWidth = rawImageSize.width * targetScale
    val targetScaledImageHeight = rawImageSize.height * targetScale
    if (targetOffsetX > minOffset.x) {
        targetOffsetX = minOffset.x
    } else if (targetOffsetX + targetScaledImageWidth < maxOffset.x) {
        targetOffsetX = maxOffset.x - targetScaledImageWidth
    }
    if (targetOffsetY > minOffset.y) {
        targetOffsetY = minOffset.y
    } else if (targetOffsetY + targetScaledImageHeight < maxOffset.y) {
        targetOffsetY = maxOffset.y - targetScaledImageHeight
    }
    return Offset(targetOffsetX, targetOffsetY)
}

internal fun recalculateOffset(
    rawCentroid: Offset,
    rawImageSize: IntSize,
    offset: Offset,
    prevScale: Float,
    newScale: Float,
): Offset {
    val imageClampedCentroid = clampCentroid(
        offset,
        Size(
            rawImageSize.width * prevScale,
            rawImageSize.height * prevScale
        ),
        rawCentroid
    )
    val centroidInImageX =
        (imageClampedCentroid.x - offset.x) / prevScale
    val centroidInImageY =
        (imageClampedCentroid.y - offset.y) / prevScale
    return Offset(
        imageClampedCentroid.x - (centroidInImageX * newScale),
        imageClampedCentroid.y - (centroidInImageY * newScale)
    )
}

internal fun clampCentroid(
    offset: Offset,
    imageSize: Size,
    centroid: Offset,
): Offset {
    return Offset(
        centroid.x.coerceIn(
            offset.x,
            offset.x + imageSize.width,
        ),
        centroid.y.coerceIn(
            offset.y,
            offset.y + imageSize.height
        )
    )
}

internal fun computePanEffectiveness(
    minOffset: Offset,
    maxOffset: Offset,
    potentialOffset: Offset,
    scaledImageSize: Size,
    tolerance: Float = .35f,
): Float {
    val viewportWidth = maxOffset.x - minOffset.x
    val viewportHeight = maxOffset.y - minOffset.y

    var panEffectiveness = 1f
    if (potentialOffset.x > minOffset.x) {
        val limit = minOffset.x + (viewportWidth * tolerance)
        val fallOffRange = limit - minOffset.x
        val currentFallOff = ((potentialOffset.x - minOffset.x) / fallOffRange).coerceIn(0f, 1f)
        val t = 1 - currentFallOff
        panEffectiveness = min(panEffectiveness, t)
    } else if (potentialOffset.x + scaledImageSize.width < maxOffset.x) {
        val limit = maxOffset.x - (viewportWidth * tolerance)
        val fallOffRange = maxOffset.x - limit
        val currentFallOff =
            ((maxOffset.x - (potentialOffset.x + scaledImageSize.width)) / fallOffRange).coerceIn(0f, 1f)
        val t = 1 - currentFallOff
        panEffectiveness = min(panEffectiveness, t)
    }
    if (potentialOffset.y > minOffset.y) {
        val limit = minOffset.y + (viewportHeight * tolerance)
        val fallOffRange = limit - minOffset.y
        val currentFallOff = ((potentialOffset.y - minOffset.y) / fallOffRange).coerceIn(0f, 1f)
        val t = 1 - currentFallOff
        panEffectiveness = min(panEffectiveness, t)
    } else if (potentialOffset.y + scaledImageSize.height < maxOffset.y) {
        val limit = maxOffset.y - (viewportHeight * tolerance)
        val fallOffRange = maxOffset.y - limit
        val currentFallOff =
            ((maxOffset.y - (potentialOffset.y + scaledImageSize.height)) / fallOffRange).coerceIn(0f, 1f)
        val t = 1 - currentFallOff
        panEffectiveness = min(panEffectiveness, t)
    }
    return panEffectiveness
}

internal fun computeEffectiveZoom(
    currentScale: Float,
    minScale: Float,
    maxScale: Float,
    zoom: Float,
): Float {
    if (currentScale < minScale && zoom < 1.0f) {
        val zeroResistanceMinScaleLimit = minScale / 2.0f
        val fallOffRange = minScale - zeroResistanceMinScaleLimit
        val currentFallOff =
            ((minScale - currentScale) / fallOffRange).coerceIn(0f, 1f)
        val t = 1f - currentFallOff
        val zoomEffectiveness = t * t * t
        return 1.0f - ((1.0f - zoom) * zoomEffectiveness)
    } else if (currentScale > maxScale && zoom > 1.0f) {
        val zeroResistanceMaxScaleLimit = maxScale * 2f
        val fallOffRange = zeroResistanceMaxScaleLimit - maxScale
        val currentFallOff =
            ((currentScale - maxScale) / fallOffRange).coerceIn(0f, 1f)
        val t = 1f - currentFallOff
        val zoomEffectiveness = t * t * t
        return 1.0f + ((zoom - 1.0f) * zoomEffectiveness)
    }
    return zoom
}

internal fun computeSafeImageCropArea(
    imageBitmap: ImageBitmap,
    scale: Float,
    offset: Offset,
    cropArea: CropAreaF,
): CropArea {
    if (scale <= 0.00001f) {
        // prevent division by zero
        return CropArea(0, 0, imageBitmap.width, imageBitmap.height)
    }

    val idealLeftInOriginal = (cropArea.left - offset.x) / scale
    val idealTopInOriginal = (cropArea.top - offset.y) / scale
    val idealWidthInOriginal = cropArea.width / scale
    val idealHeightInOriginal = cropArea.height / scale

    val originalImageWidthF = imageBitmap.width.toFloat()
    val originalImageHeightF = imageBitmap.height.toFloat()

    // Clamp the starting position (top-left)
    val clampedLeftInOriginal = idealLeftInOriginal.coerceIn(0f, originalImageWidthF)
    val clampedTopInOriginal = idealTopInOriginal.coerceIn(0f, originalImageHeightF)

    // Clamp these ideal edges to the image boundaries
    val idealRightInOriginal = idealLeftInOriginal + idealWidthInOriginal
    val idealBottomInOriginal = idealTopInOriginal + idealHeightInOriginal

    val clampedRightInOriginal = idealRightInOriginal.coerceIn(0f, originalImageWidthF)
    val clampedBottomInOriginal = idealBottomInOriginal.coerceIn(0f, originalImageHeightF)

    val finalWidthInOriginal = (clampedRightInOriginal - clampedLeftInOriginal).coerceAtLeast(0f)
    val finalHeightInOriginal = (clampedBottomInOriginal - clampedTopInOriginal).coerceAtLeast(0f)

    return CropArea(
        left = clampedLeftInOriginal.roundToInt(),
        top = clampedTopInOriginal.roundToInt(),
        width = finalWidthInOriginal.roundToInt(),
        height = finalHeightInOriginal.roundToInt()
    )
}

internal data class CropAreaF(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

// Direct copy of commonMain/androidx/compose/foundation/gestures/TransformGestureDetector.kt
// Added onGestureStart callback overload
// Added onGestureEnd callback overload
internal suspend fun PointerInputScope.detectTransformGestures(
    panZoomLock: Boolean = false,
    onGestureEnd: () -> Unit = {},
    onGestureStart: () -> Unit = {},
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit,
) {
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var lockedToPanZoom = false

        awaitFirstDown(requireUnconsumed = false)
        onGestureStart()
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.fastAny { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                    val panMotion = pan.getDistance()

                    if (
                        zoomMotion > touchSlop ||
                        rotationMotion > touchSlop ||
                        panMotion > touchSlop
                    ) {
                        pastTouchSlop = true
                        lockedToPanZoom = panZoomLock && rotationMotion < touchSlop
                    }
                }

                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                    if (effectiveRotation != 0f || zoomChange != 1f || panChange != Offset.Zero) {
                        onGesture(centroid, panChange, zoomChange, effectiveRotation)
                    }
                    event.changes.fastForEach {
                        if (it.positionChanged()) {
                            it.consume()
                        }
                    }
                }
            }
        } while (!canceled && event.changes.fastAny { it.pressed })
        onGestureEnd()
    }
}
