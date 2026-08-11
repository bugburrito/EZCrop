package io.github.bugburrito.ezcrop.core

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Drop-in image cropping for Compose: the frame stays put while [imageBitmap] glides and
 * pinch-zooms behind it, edges stretch like a rubber band and snap back into place, and
 * whatever ends up inside the frame is the crop.
 *
 * `EZCrop` lets the user select an area. It never creates or modifies a bitmap: the
 * selection is reported through [onCropAreaChanged] as a [CropArea] in pixels of the
 * [imageBitmap], and cropping the image with it is up to the caller. For that reason it is
 * best to pass a downscaled preview rather than the original image. The bitmap is redrawn on
 * every frame of a gesture and stays in memory for as long as `EZCrop` is shown, so a
 * full-resolution photo costs smooth panning and a lot of memory for detail the screen cannot
 * show anyway. Decode the image at roughly the on-screen size of `EZCrop` (e.g. with
 * `ImageDecoder.setTargetSampleSize` or `BitmapFactory.Options.inSampleSize`), then multiply
 * the reported [CropArea] by the sample size and crop the original once the user confirms.
 *
 * ```
 * var cropArea by remember { mutableStateOf<CropArea?>(null) }
 *
 * EZCrop(
 *     modifier = Modifier
 *         .fillMaxWidth()
 *         .aspectRatio(1f),
 *     imageBitmap = imageBitmap,
 *     cropShape = CropShape.Rectangle(aspectRatio = 16f / 9f),
 *     onCropAreaChanged = { cropArea = it },
 * )
 *
 * // Later, e.g. when the user confirms:
 * val cropped = cropArea?.let {
 *     Bitmap.createBitmap(imageBitmap.asAndroidBitmap(), it.left, it.top, it.width, it.height)
 * }
 * ```
 *
 * ### Layout
 *
 * [modifier] must give `EZCrop` a bounded size, e.g. `fillMaxWidth().aspectRatio(1f)` or
 * `size(300.dp)`; it has no intrinsic size and must not be measured with unbounded
 * constraints, such as directly inside a scrolling container. The crop frame is placed
 * centered in that space: it fills it completely when [CropShape.aspectRatio] is `null`,
 * otherwise it is the largest frame of that ratio that fits. Nothing is drawn outside the
 * frame except its shadow.
 *
 * ### Gestures
 *
 * The image starts centered and scaled to just cover the frame. Dragging pans it, pinching
 * zooms it around the pinch center, up to five times the initial scale. Gestures are picked
 * up anywhere inside `EZCrop`, not only inside the frame. The image can be dragged or zoomed
 * slightly past its limits with increasing resistance and animates back when released, so at
 * rest the frame is always completely filled by the image. Rotation is not supported yet.
 *
 * ### State
 *
 * Pan aind zoom are kept nside `EZCrop` and survive recomposition, but not Activity
 * recreation (e.g. a configuration change). When [cropShape], the available size or
 * [imageBitmap] changes, the current pan and zoom are kept and only adjusted as far as needed
 * to keep the frame filled. To start over with the initial framing for a new image, wrap the
 * call in `key(imageBitmap) { ... }`.
 *
 * @param modifier Modifier for the whole composable. Must result in a bounded size; see
 * "Layout" above.
 * @param imageBitmap The image to crop. With `null`, only the empty frame with its
 * [background], [border] and [shadow] is shown, gestures are ignored and [onCropAreaChanged]
 * is not invoked. Ideally a downscaled preview rather than the original, as described above.
 * @param cropShape Outline and proportions of the crop frame.
 * @param background Fill of the crop frame behind the image. Visible while there is no image,
 * through transparent parts of the image, and while the image is dragged past its limits.
 * @param border Stroke drawn along the outline of the crop frame. Pass
 * `CropBorder(width = 0.dp)` for none.
 * @param shadow Elevation shadow cast by the crop frame. Pass `CropShadow(elevation = 0.dp)`
 * for none.
 * @param onCropAreaChanged Invoked with the currently selected area, in pixels of
 * [imageBitmap]. It is invoked once when an image is first shown, again whenever
 * [imageBitmap], [cropShape] or the available size changes, and at the end of every pan or
 * zoom gesture with the area the image settles on. It is not invoked continuously while a
 * gesture is in progress. Hold on to the latest value and use it once the user confirms.
 */
@Composable
fun EZCrop(
    modifier: Modifier = Modifier,
    imageBitmap: ImageBitmap? = null,
    cropShape: CropShape = CropShape.Circle(),
    background: Brush = SolidColor(Color.Black),
    border: CropBorder = CropBorder(),
    shadow: CropShadow = CropShadow(),
    onCropAreaChanged: (crop: CropArea) -> Unit = {},
) {
    BoxWithConstraints(modifier = modifier) {
        val availableSpace = Size(
            constraints.maxWidth.toFloat(),
            constraints.maxHeight.toFloat()
        )

        val ratio = cropShape.aspectRatio
        val (cropWidth, cropHeight) = if (ratio == null) {
            availableSpace.width to availableSpace.height
        } else {
            var w = availableSpace.width
            var h = w / ratio
            if (h > availableSpace.height) {
                h = availableSpace.height
                w = h * ratio
            }
            w to h
        }
        val centerX = availableSpace.width / 2f
        val centerY = availableSpace.height / 2f
        val cropRect = Rect(
            centerX - cropWidth / 2f,
            centerY - cropHeight / 2f,
            centerX + cropWidth / 2f,
            centerY + cropHeight / 2f
        )

        val shape = cropShape.toComposeShape()
        val density = LocalDensity.current

        // The single clip/shadow/background/border chain shared by everything drawn inside the
        // frame - the image below, and any future slot content.
        val frameModifier = Modifier
            .offset { IntOffset(cropRect.left.roundToInt(), cropRect.top.roundToInt()) }
            .size(
                width = with(density) { cropRect.width.toDp() },
                height = with(density) { cropRect.height.toDp() }
            )
            .shadow(elevation = shadow.elevation, shape = shape, clip = false)
            .clip(shape)
            .background(background)
            .then(
                if (border.width > 0.dp) {
                    Modifier.border(border.width, border.color, shape)
                } else {
                    Modifier
                }
            )

        if (imageBitmap != null) {
            InteractiveImage(
                imageBitmap = imageBitmap,
                cropRect = cropRect,
                modifier = frameModifier,
                onCropAreaChanged = onCropAreaChanged,
            )
        } else {
            Box(modifier = frameModifier)
        }
    }
}

/**
 * The pannable/zoomable image content of [EZCrop] - the gesture handling and the image
 * [Canvas] drawn inside [modifier]'s single clip.
 */
@Composable
private fun InteractiveImage(
    imageBitmap: ImageBitmap,
    cropRect: Rect,
    modifier: Modifier,
    onCropAreaChanged: (crop: CropArea) -> Unit,
) {
    val minOffset = cropRect.topLeft
    val maxOffset = cropRect.bottomRight
    val minScale = maxOf(
        cropRect.width / imageBitmap.width,
        cropRect.height / imageBitmap.height
    )
    val maxScale = minScale * 5

    val viewportCenterX = minOffset.x + (maxOffset.x - minOffset.x) / 2f
    val viewportCenterY = minOffset.y + (maxOffset.y - minOffset.y) / 2f
    val initialOffset = Offset(
        x = viewportCenterX - (imageBitmap.width * minScale) / 2f,
        y = viewportCenterY - (imageBitmap.height * minScale) / 2f
    )

    val animatableOffset = remember { Animatable(initialOffset, Offset.VectorConverter) }
    val animatableScale = remember { Animatable(minScale) }

    fun reportCropArea(offset: Offset, scale: Float) {
        if (cropRect.width <= 0f || cropRect.height <= 0f) {
            onCropAreaChanged(CropArea(0, 0, imageBitmap.width, imageBitmap.height))
            return
        }
        onCropAreaChanged(
            computeSafeImageCropArea(
                imageBitmap,
                scale,
                offset,
                CropAreaF(cropRect.left, cropRect.top, cropRect.width, cropRect.height)
            )
        )
    }

    // Re-clamp the current pan/zoom into the (possibly new) bounds whenever the crop
    // viewport or image changes, e.g. an aspect-ratio slider - instead of resetting to
    // the initial framing and losing whatever the user had panned/zoomed to.
    LaunchedEffect(cropRect, imageBitmap) {
        val currentScale = animatableScale.value
        val targetScale = currentScale.coerceIn(minScale, maxScale)
        val targetOffset = computeTargetOffset(
            animatableOffset.value,
            minOffset,
            maxOffset,
            currentScale,
            targetScale,
            IntSize(imageBitmap.width, imageBitmap.height)
        )
        animatableScale.snapTo(targetScale)
        animatableOffset.snapTo(targetOffset)
        reportCropArea(targetOffset, targetScale)
    }

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(cropRect, imageBitmap) {
                detectTransformGestures(
                    onGesture = { centroid, pan, zoom, _ ->
                        val currentScale = animatableScale.value
                        val targetScale = currentScale * computeEffectiveZoom(
                            currentScale,
                            minScale,
                            maxScale,
                            zoom
                        )
                        val recalculatedOffset = recalculateOffset(
                            centroid,
                            IntSize(imageBitmap.width, imageBitmap.height),
                            animatableOffset.value,
                            currentScale,
                            targetScale
                        )

                        val potentialTargetOffset = Offset(
                            recalculatedOffset.x + pan.x,
                            recalculatedOffset.y + pan.y
                        )

                        val panEffectiveness = computePanEffectiveness(
                            minOffset,
                            maxOffset,
                            potentialTargetOffset,
                            Size(
                                imageBitmap.width * targetScale,
                                imageBitmap.height * targetScale
                            )
                        )

                        val targetOffset = Offset(
                            recalculatedOffset.x + pan.x * panEffectiveness,
                            recalculatedOffset.y + pan.y * panEffectiveness
                        )

                        coroutineScope.launch {
                            animatableScale.snapTo(targetScale)
                            animatableOffset.snapTo(targetOffset)
                        }
                    },
                    onGestureEnd = {
                        coroutineScope.launch {
                            val currentScale = animatableScale.value
                            val currentOffset = animatableOffset.value
                            val targetScale = currentScale.coerceIn(minScale, maxScale)
                            val targetOffset = computeTargetOffset(
                                currentOffset,
                                minOffset,
                                maxOffset,
                                currentScale,
                                targetScale,
                                IntSize(imageBitmap.width, imageBitmap.height)
                            )

                            reportCropArea(targetOffset, targetScale)

                            if (currentScale != targetScale) {
                                launch {
                                    animatableScale.animateTo(targetScale, tween(350))
                                }
                            }
                            if (currentOffset != targetOffset) {
                                launch {
                                    animatableOffset.animateTo(targetOffset, tween(350))
                                }
                            }
                        }
                    }
                )
            }
    ) {
        Box(modifier = modifier) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawImage(
                    image = imageBitmap,
                    dstOffset = IntOffset(
                        (animatableOffset.value.x - cropRect.left).toInt(),
                        (animatableOffset.value.y - cropRect.top).toInt()
                    ),
                    dstSize = IntSize(
                        (imageBitmap.width * animatableScale.value).toInt(),
                        (imageBitmap.height * animatableScale.value).toInt()
                    )
                )
            }
        }
    }
}
