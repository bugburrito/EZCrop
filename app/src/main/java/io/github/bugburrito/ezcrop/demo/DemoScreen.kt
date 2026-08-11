package io.github.bugburrito.ezcrop.demo

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.bugburrito.ezcrop.core.CropArea
import io.github.bugburrito.ezcrop.core.CropBorder
import io.github.bugburrito.ezcrop.core.CropShadow
import io.github.bugburrito.ezcrop.core.CropShape
import io.github.bugburrito.ezcrop.core.EZCrop
import io.github.bugburrito.ezcrop.misc.ImageSourcePicker
import io.github.bugburrito.ezcrop.misc.rememberCameraLauncherWithPermission
import io.github.bugburrito.ezcrop.misc.rememberGalleryLauncher
import kotlin.math.roundToInt

@Composable
fun DemoApp() {
    val viewModel: DemoViewModel = viewModel()
    val context = LocalContext.current
    val uiState = viewModel.uiState

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            val result = uiState.cropResult
            if (result != null) {
                ResultScreen(
                    originalImage = result.originalImage,
                    cropArea = result.cropArea,
                    croppedImage = result.croppedImage,
                    onBack = viewModel::backToDemo
                )
            } else {
                DemoScreen(
                    uiState = uiState,
                    onPickImage = { uri -> viewModel.pickImage(context, uri) },
                    onCancelImage = viewModel::cancelImage,
                    onCropAreaChanged = viewModel::onCropAreaChanged,
                    onRectangleModeChanged = viewModel::setRectangleMode,
                    onBorderEnabledChanged = viewModel::setBorderEnabled,
                    onShadowEnabledChanged = viewModel::setShadowEnabled,
                    onWidthWeightChanged = viewModel::setWidthWeight,
                    onHeightWeightChanged = viewModel::setHeightWeight,
                    onSubmit = { viewModel.submit(context) }
                )
            }
        }
    }
}

@Composable
fun ResultScreen(
    originalImage: ImageBitmap,
    cropArea: CropArea,
    croppedImage: ImageBitmap,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        Text(
            text = "Crop area",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val originalAspectRatio = originalImage.width.toFloat() / originalImage.height.toFloat()
            val fittedHeight = maxWidth / originalAspectRatio
            val (displayWidth, displayHeight) = if (fittedHeight <= maxHeight) {
                maxWidth to fittedHeight
            } else {
                maxHeight * originalAspectRatio to maxHeight
            }
            Canvas(
                modifier = Modifier
                    .size(displayWidth, displayHeight)
                    .border(1.dp, Color.Gray)
            ) {
                val scale = size.width / originalImage.width.toFloat()

                drawImage(
                    image = originalImage,
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                )
                drawRect(
                    color = Color.Yellow,
                    topLeft = Offset(cropArea.left * scale, cropArea.top * scale),
                    size = androidx.compose.ui.geometry.Size(
                        cropArea.width * scale,
                        cropArea.height * scale
                    ),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Text(
            text = "Cropped original image",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val imageAspectRatio = croppedImage.width.toFloat() / croppedImage.height.toFloat()
            val fittedHeight = maxWidth / imageAspectRatio
            val (displayWidth, displayHeight) = if (fittedHeight <= maxHeight) {
                maxWidth to fittedHeight
            } else {
                maxHeight * imageAspectRatio to maxHeight
            }
            Image(
                bitmap = croppedImage,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(displayWidth, displayHeight)
                    .border(1.dp, Color.Gray)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoScreen(
    uiState: DemoUiState,
    onPickImage: (Uri) -> Unit,
    onCancelImage: () -> Unit,
    onCropAreaChanged: (CropArea) -> Unit,
    onRectangleModeChanged: (Boolean) -> Unit,
    onBorderEnabledChanged: (Boolean) -> Unit,
    onShadowEnabledChanged: (Boolean) -> Unit,
    onWidthWeightChanged: (Float) -> Unit,
    onHeightWeightChanged: (Float) -> Unit,
    onSubmit: () -> Unit,
) {
    var showSourceChooser by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    fun pickImage(uri: Uri) {
        showSourceChooser = false
        onPickImage(uri)
    }

    val galleryLauncher = rememberGalleryLauncher { uri -> uri?.let(::pickImage) }
    val cameraLauncher = rememberCameraLauncherWithPermission { uri -> uri?.let(::pickImage) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "EZCrop V2 Demo", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !uiState.isRectangleMode,
                onClick = { onRectangleModeChanged(false) },
                label = { Text("Circle") }
            )
            FilterChip(
                selected = uiState.isRectangleMode,
                onClick = { onRectangleModeChanged(true) },
                label = { Text("Rectangle") }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = uiState.isBorderEnabled,
                onClick = { onBorderEnabledChanged(!uiState.isBorderEnabled) },
                label = { Text("Border") }
            )
            FilterChip(
                selected = uiState.isShadowEnabled,
                onClick = { onShadowEnabledChanged(!uiState.isShadowEnabled) },
                label = { Text("Shadow") }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Aspect ratio ${"%.2f".format(uiState.widthWeight / uiState.heightWeight)}",
            style = MaterialTheme.typography.bodySmall
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Width", modifier = Modifier.width(56.dp))
            Slider(
                value = uiState.widthWeight,
                onValueChange = onWidthWeightChanged,
                valueRange = 1f..4f,
                modifier = Modifier.weight(1f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Height", modifier = Modifier.width(56.dp))
            Slider(
                value = uiState.heightWeight,
                onValueChange = onHeightWeightChanged,
                valueRange = 1f..4f,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        EZCrop(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            imageBitmap = uiState.loadedImage?.image,
            cropShape = if (uiState.isRectangleMode) {
                CropShape.Rectangle(uiState.widthWeight / uiState.heightWeight)
            } else {
                CropShape.Circle(uiState.widthWeight / uiState.heightWeight)
            },
            border = if (uiState.isBorderEnabled) CropBorder() else CropBorder(width = 0.dp),
            shadow = if (uiState.isShadowEnabled) CropShadow() else CropShadow(elevation = 0.dp),
            onCropAreaChanged = onCropAreaChanged
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { showSourceChooser = true }, enabled = !uiState.isLoading) {
                Text(
                    when {
                        uiState.isLoading -> "Loading…"
                        uiState.loadedImage == null -> "Load image"
                        else -> "Change image"
                    }
                )
            }
            if (uiState.loadedImage != null) {
                OutlinedIconButton(onClick = onCancelImage) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onSubmit,
                enabled = uiState.loadedImage != null && !uiState.isSubmitting
            ) {
                if (uiState.isSubmitting) {
                    Text("Submitting…")
                } else {
                    Text("Submit")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showSourceChooser) {
        ModalBottomSheet(
            onDismissRequest = { showSourceChooser = false },
            sheetState = sheetState
        ) {
            ImageSourcePicker(
                onCameraSelected = cameraLauncher,
                onGallerySelected = galleryLauncher
            )
        }
    }
}
