package io.github.bugburrito.ezcrop.misc

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun rememberCameraLauncherWithPermission(
    onResult: (uri: Uri?) -> Unit,
): () -> Unit {
    var launched by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var tempImageUriHolder by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = {
            if (it) {
                onResult(tempImageUriHolder)
            }
            tempImageUriHolder = null
            launched = false
        }
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                val newImageFileUri = createCacheImageFile(context)
                tempImageUriHolder = newImageFileUri
                cameraLauncher.launch(newImageFileUri)
            } else {
                launched = false
            }
        }
    )

    val launchCamera = {
        if (!launched) {
            launched = true
            when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                PackageManager.PERMISSION_GRANTED -> {
                    val newImageFileUri = createCacheImageFile(context)
                    tempImageUriHolder = newImageFileUri
                    cameraLauncher.launch(newImageFileUri)
                }
                else -> {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }
    return launchCamera
}
