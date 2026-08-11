package io.github.bugburrito.ezcrop.misc

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun rememberGalleryLauncher(onResult: (uri: Uri?) -> Unit): () -> Unit {
    var launched by remember { mutableStateOf(false) }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = {
            onResult(it)
            launched = false
        }
    )

    return {
        if (!launched) {
            launched = true
            galleryLauncher.launch("image/*")
        }
    }
}
