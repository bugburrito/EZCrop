package io.github.bugburrito.ezcrop.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The stroke drawn along the outline of the crop frame in [EZCrop], following its
 * [CropShape].
 *
 * The border is drawn inside the frame, on top of the image. It is purely decorative: the
 * [CropArea] reported by [EZCrop] covers the whole frame, including the pixels hidden under
 * the border.
 *
 * ```
 * EZCrop(
 *     imageBitmap = imageBitmap,
 *     border = CropBorder(width = 1.dp, color = Color.Yellow),
 * )
 * ```
 *
 * @property width Thickness of the stroke. `0.dp` or less disables the border.
 * @property color Color of the stroke.
 */
data class CropBorder(
    val width: Dp = 2.dp,
    val color: Color = Color.White,
)
