package io.github.bugburrito.ezcrop.demo

import android.content.Context
import android.net.Uri
import android.util.Size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.bugburrito.ezcrop.core.CropArea
import io.github.bugburrito.ezcrop.misc.LoadedImage
import io.github.bugburrito.ezcrop.misc.cropOriginalImage
import io.github.bugburrito.ezcrop.misc.loadImage
import kotlinx.coroutines.launch

data class CropResult(
    val originalImage: ImageBitmap,
    val cropArea: CropArea,
    val croppedImage: ImageBitmap,
)

data class DemoUiState(
    val pickedUri: Uri? = null,
    val loadedImage: LoadedImage? = null,
    val cropArea: CropArea? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val isRectangleMode: Boolean = false,
    val isBorderEnabled: Boolean = true,
    val isShadowEnabled: Boolean = true,
    val widthWeight: Float = 1f,
    val heightWeight: Float = 1f,
    val cropResult: CropResult? = null,
)

class DemoViewModel : ViewModel() {
    var uiState by mutableStateOf(DemoUiState())
        private set

    fun pickImage(context: Context, uri: Uri) {
        uiState = uiState.copy(pickedUri = uri)
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            val loaded = loadImage(context, uri, Size(1024, 1024))
            uiState = uiState.copy(loadedImage = loaded, isLoading = false)
        }
    }

    fun cancelImage() {
        uiState = uiState.copy(pickedUri = null, loadedImage = null, cropArea = null)
    }

    fun onCropAreaChanged(area: CropArea) {
        uiState = uiState.copy(cropArea = area)
    }

    fun setRectangleMode(value: Boolean) {
        uiState = uiState.copy(isRectangleMode = value)
    }

    fun setBorderEnabled(value: Boolean) {
        uiState = uiState.copy(isBorderEnabled = value)
    }

    fun setShadowEnabled(value: Boolean) {
        uiState = uiState.copy(isShadowEnabled = value)
    }

    fun setWidthWeight(value: Float) {
        uiState = uiState.copy(widthWeight = value)
    }

    fun setHeightWeight(value: Float) {
        uiState = uiState.copy(heightWeight = value)
    }

    fun submit(context: Context) {
        val uri = uiState.pickedUri
        val preview = uiState.loadedImage
        val area = uiState.cropArea
        if (uri == null || preview == null || area == null) return
        viewModelScope.launch {
            uiState = uiState.copy(isSubmitting = true)
            val cropped = cropOriginalImage(context, uri, area, preview.sampleSize)
            uiState = uiState.copy(
                isSubmitting = false,
                cropResult = CropResult(preview.image, area, cropped)
            )
        }
    }

    fun backToDemo() {
        uiState = uiState.copy(cropResult = null)
    }
}
