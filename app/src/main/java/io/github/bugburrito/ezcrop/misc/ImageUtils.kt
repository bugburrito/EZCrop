package io.github.bugburrito.ezcrop.misc

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import io.github.bugburrito.ezcrop.core.CropArea
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LoadedImage(val image: ImageBitmap, val sampleSize: Int)

private const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".ezcrop.fileprovider"


fun createCacheImageFile(context: Context): Uri {
    val tempFile = File.createTempFile(
        createRandomFileName(prefix = "temp_"),
        ".jpg",
        context.cacheDir
    )
    return FileProvider.getUriForFile(
        context,
        context.packageName + FILE_PROVIDER_AUTHORITY_SUFFIX,
        tempFile
    )
}

internal fun createRandomFileName(prefix: String = "", suffix: String = ""): String {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.getDefault())
        .format(Date())
    return "${prefix}${timeStamp}${suffix}"
}

suspend fun loadImage(
    context: Context,
    uri: Uri,
    minResolution: Size = Size(Int.MAX_VALUE, Int.MAX_VALUE),
): LoadedImage {
    return withContext(Dispatchers.IO) {
        return@withContext if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            var sampleSize = 1
            LoadedImage(ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                sampleSize = computeSampleSize(info.size, minResolution)
                decoder.isMutableRequired = false
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.allocator
                decoder.setTargetSampleSize(sampleSize)
            }.asImageBitmap(), sampleSize)
        } else {
            // Fallback for older devices
            loadScaledBitmapFromUri(context, uri, minResolution)
        }
    }
}

fun cropImageBitmap(
    imageBitmap: ImageBitmap,
    cropArea: CropArea,
): ImageBitmap {
    val androidBitmap = imageBitmap.asAndroidBitmap()
    val croppedAndroidBitmap = Bitmap.createBitmap(
        androidBitmap,
        cropArea.left,
        cropArea.top,
        cropArea.width,
        cropArea.height
    )
    return croppedAndroidBitmap.asImageBitmap()
}

suspend fun cropOriginalImage(
    context: Context,
    uri: Uri,
    previewCropArea: CropArea,
    previewSampleSize: Int,
): ImageBitmap {
    val original = loadImage(context, uri)
    val fullResArea = getUnscaledCropArea(previewCropArea, previewSampleSize)
    return cropImageBitmap(original.image, fullResArea)
}

internal fun getUnscaledCropArea(cropArea: CropArea, sampleSize: Int): CropArea {
    return CropArea(
        left = cropArea.left * sampleSize,
        top = cropArea.top * sampleSize,
        width = cropArea.width * sampleSize,
        height = cropArea.height * sampleSize
    )
}

internal fun loadScaledBitmapFromUri(context: Context, uri: Uri, minSize: Size): LoadedImage {
    val sampleSize = context.contentResolver.openInputStream(uri).use {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(it, null, options)
        computeSampleSize(Size(options.outWidth, options.outHeight), minSize)
    }
    return context.contentResolver.openInputStream(uri).use {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        LoadedImage(BitmapFactory.decodeStream(it, null, options)?.asImageBitmap()
            ?: throw IllegalStateException(), sampleSize)
    }
}

internal fun computeSampleSize(imageSize: Size, minSize: Size): Int {
    var sampleSize = 1
    if (imageSize.width > minSize.width || imageSize.height > minSize.height) {
        val halfWidth = imageSize.width / 2
        val halfHeight = imageSize.height / 2
        while (halfWidth / sampleSize >= minSize.width && halfHeight / sampleSize >= minSize.height) {
            sampleSize *= 2
        }
    }
    return sampleSize
}
