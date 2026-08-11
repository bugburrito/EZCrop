package io.github.bugburrito.ezcrop.core

/**
 * The region of an image selected in [EZCrop], reported through its `onCropAreaChanged`
 * callback.
 *
 * All values are in pixels of the `ImageBitmap` that was passed to [EZCrop], with the origin
 * at the image's top-left corner. The area is always clamped to the image, so it can be handed
 * straight to `Bitmap.createBitmap`:
 *
 * ```
 * val cropped = Bitmap.createBitmap(
 *     imageBitmap.asAndroidBitmap(),
 *     cropArea.left,
 *     cropArea.top,
 *     cropArea.width,
 *     cropArea.height,
 * )
 * ```
 *
 * **Note:** The area is always the rectangular bounding box of the crop frame, also for
 * [CropShape.Circle].
 *
 * If [EZCrop] was given a downsampled preview of a larger image, the values refer to the
 * preview. Multiply all four by the sample size that was used to map the area onto the
 * full-resolution image.
 *
 * @property left Distance of the area's left edge from the image's left edge, in pixels.
 * @property top Distance of the area's top edge from the image's top edge, in pixels.
 * @property width Width of the area in pixels. `0` only if [EZCrop] was laid out with no
 * space for the crop frame in that direction; check for it before creating a bitmap.
 * @property height Height of the area in pixels. `0` under the same condition as [width].
 */
data class CropArea(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)
