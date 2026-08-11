package io.github.bugburrito.ezcrop.core

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The elevation shadow cast by the crop frame in [EZCrop], following its [CropShape].
 *
 * This is the platform elevation shadow, the same one `Card` and `Surface` use. It is drawn
 * outside the frame, so it is only visible where there is room around the frame: leave some
 * padding around [EZCrop], and make sure no parent clips its children.
 *
 * ```
 * EZCrop(
 *     imageBitmap = imageBitmap,
 *     shadow = CropShadow(elevation = 12.dp),
 * )
 * ```
 *
 * @property elevation Elevation of the frame, which controls the size and softness of the
 * shadow. `0.dp` disables the shadow.
 */
data class CropShadow(
    val elevation: Dp = 6.dp,
)
