package io.github.bugburrito.ezcrop.core

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape

/**
 * The shape and proportions of the crop frame drawn by [EZCrop].
 *
 * The shape decides the outline of the frame (its clip, border and shadow) and, through
 * [aspectRatio], its proportions. It does not change what is reported: the [CropArea] is
 * always the rectangular bounding box of the frame.
 *
 * ```
 * CropShape.Circle()                  // round avatar crop, given a square EZCrop
 * CropShape.Circle(aspectRatio = 1f)  // always a true circle
 * CropShape.Rectangle(16f / 9f)       // fixed 16:9 crop
 * CropShape.Rectangle()               // rectangle filling the whole EZCrop
 * ```
 */
sealed interface CropShape {

    /**
     * Width-to-height ratio of the crop frame, e.g. `4f / 3f` for a landscape frame or
     * `3f / 4f` for a portrait one. Must be a positive, finite number.
     *
     * With a ratio, the frame is the largest one of that ratio that fits into [EZCrop],
     * centered. When `null`, the frame fills [EZCrop] completely and takes on whatever
     * proportions [EZCrop] is laid out with.
     */
    val aspectRatio: Float?

    /**
     * A round crop frame.
     *
     * The frame is a true circle only when it is square, i.e. with an [aspectRatio] of `1f`,
     * or with `null` when [EZCrop] itself is square. For any other proportions it is a
     * stadium (straight sides with fully rounded ends), not an ellipse.
     *
     * The reported [CropArea] is the bounding rectangle of the frame; the corners outside the
     * round outline are not masked out.
     *
     * @property aspectRatio Width-to-height ratio of the frame, or `null` to fill [EZCrop].
     */
    data class Circle(override val aspectRatio: Float? = null) : CropShape

    /**
     * A rectangular crop frame with square corners.
     *
     * @property aspectRatio Width-to-height ratio of the frame, or `null` to fill [EZCrop].
     */
    data class Rectangle(override val aspectRatio: Float? = null) : CropShape
}

/**
 * The Compose [Shape] that traces this crop shape's outline, as used by [EZCrop] for the
 * frame's clip, border and shadow.
 *
 * Useful to draw your own overlays or previews with exactly the same outline as the crop
 * frame, e.g. `Modifier.clip(cropShape.toComposeShape())` on a preview of the cropped result.
 *
 * The returned shape carries no size: it adapts to whatever bounds it is applied to, so
 * applying [CropShape.aspectRatio] is up to the caller. [CropShape.Circle] maps to
 * [CircleShape], which is a circle in square bounds and a stadium in any other bounds;
 * [CropShape.Rectangle] maps to [RectangleShape].
 */
fun CropShape.toComposeShape(): Shape = when (this) {
    is CropShape.Circle -> CircleShape
    is CropShape.Rectangle -> RectangleShape
}
