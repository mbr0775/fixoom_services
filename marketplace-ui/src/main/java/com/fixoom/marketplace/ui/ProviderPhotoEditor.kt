package com.fixoom.marketplace.ui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderPhotoEditor(
    path: String?,
    name: String,
    busy: Boolean,
    onSave: (ByteArray?, () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf<ByteArray?>(null) }
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var draftSource by remember { mutableStateOf<Bitmap?>(null) }
    var reading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var options by rememberSaveable { mutableStateOf(false) }
    var viewing by rememberSaveable { mutableStateOf(false) }
    var removing by rememberSaveable { mutableStateOf(false) }
    var cameraName by rememberSaveable { mutableStateOf<String?>(null) }
    val enabled = !busy && !reading
    val readPhoto: (android.net.Uri) -> Unit = { uri ->
        scope.launch {
            reading = true
            error = null
            try {
                source = withContext(Dispatchers.IO) { decodeProviderPhoto(context, uri) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = "This image could not be opened. Choose a photo smaller than 15 MB."
            } finally {
                reading = false
            }
        }
    }
    val gallery =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) readPhoto(uri)
        }
    val camera =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
            // The generated basename is the only value persisted across activity recreation.
            val file =
                cameraName
                    ?.takeIf { it.matches(Regex("[0-9a-f-]{36}[.]jpg")) }
                    ?.let { File(File(context.cacheDir, "profile-camera"), it) }
            cameraName = null
            if (captured && file != null) {
                scope.launch {
                    reading = true
                    error = null
                    try {
                        source =
                            withContext(Dispatchers.IO) {
                                val uri =
                                    FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.profilephotos",
                                        file,
                                    )
                                decodeProviderPhoto(context, uri)
                            }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        error =
                            "The camera photo could not be opened. Try again or choose from gallery."
                    } finally {
                        file.delete()
                        reading = false
                    }
                }
            } else file?.delete()
        }
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(132.dp)
                    .semantics { contentDescription = "Edit profile photo" }
                    .clickable(enabled = enabled, role = Role.Button) { options = true },
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    Modifier.size(128.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Box(Modifier.padding(5.dp), contentAlignment = Alignment.Center) {
                        ProviderPhoto(path, name, Modifier.size(114.dp), draft)
                    }
                }
                Surface(
                    Modifier.align(Alignment.BottomEnd).size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(3.dp, MaterialTheme.colorScheme.surface),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.PhotoCamera, null, Modifier.size(20.dp))
                    }
                }
                if (busy || reading)
                    CircularProgressIndicator(Modifier.size(132.dp), strokeWidth = 3.dp)
            }
            Text(
                name.ifBlank { "Your profile photo" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton({ options = true }, enabled = enabled) {
                Text(if (path == null && draft == null) "Add photo" else "Edit photo")
            }
            Text(
                "Choose a photo, then crop and adjust it before saving.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            if (draft != null) {
                Text(
                    "Photo preview · not saved yet",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                OutlinedButton({ source = draftSource }, enabled = enabled && draftSource != null) {
                    Icon(Icons.Rounded.Crop, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Adjust crop")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        {
                            onSave(draft) {
                                draft = null
                                draftSource = null
                            }
                        },
                        enabled = enabled,
                    ) {
                        Text(if (busy) "Saving…" else "Save photo")
                    }
                    TextButton(
                        {
                            draft = null
                            draftSource = null
                        },
                        enabled = enabled,
                    ) {
                        Text("Discard")
                    }
                }
            } else if (busy || reading)
                Text(
                    if (reading) "Preparing photo…" else "Saving changes…",
                    style = MaterialTheme.typography.bodySmall,
                )
            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    if (options)
        ModalBottomSheet(
            onDismissRequest = { options = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Profile photo",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                PhotoAction("Choose from gallery", Icons.Rounded.PhotoLibrary, enabled) {
                    options = false
                    try {
                        gallery.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    } catch (_: Exception) {
                        error = "The photo picker is unavailable. Try again."
                    }
                }
                PhotoAction("Take photo", Icons.Rounded.PhotoCamera, enabled) {
                    options = false
                    try {
                        val directory =
                            File(context.cacheDir, "profile-camera").apply {
                                check(exists() || mkdirs())
                            }
                        val file = File(directory, "${UUID.randomUUID()}.jpg")
                        cameraName = file.name
                        camera.launch(
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.profilephotos",
                                file,
                            )
                        )
                    } catch (_: Exception) {
                        cameraName?.let {
                            File(File(context.cacheDir, "profile-camera"), it).delete()
                        }
                        cameraName = null
                        error =
                            "No camera app could be opened. Choose a photo from gallery instead."
                    }
                }
                if (path != null || draft != null)
                    PhotoAction("View photo", Icons.Rounded.Visibility, enabled) {
                        options = false
                        viewing = true
                    }
                if (path != null)
                    PhotoAction(
                        "Remove photo",
                        Icons.Rounded.DeleteOutline,
                        enabled,
                        danger = true,
                    ) {
                        options = false
                        removing = true
                    }
                TextButton({ options = false }, Modifier.fillMaxWidth()) { Text("Cancel") }
                Spacer(Modifier.height(16.dp))
            }
        }
    source?.let { bitmap ->
        ProviderPhotoCropper(bitmap, { source = null }) { bytes ->
            draft = bytes
            draftSource = bitmap
            source = null
        }
    }
    if (viewing)
        Dialog(onDismissRequest = { viewing = false }) {
            Surface(shape = RoundedCornerShape(28.dp)) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Text("Profile photo", style = MaterialTheme.typography.titleLarge)
                    ProviderPhoto(
                        path,
                        name,
                        Modifier.sizeIn(maxWidth = 280.dp, maxHeight = 280.dp)
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        draft,
                    )
                    TextButton({ viewing = false }) { Text("Close") }
                }
            }
        }
    if (removing)
        AlertDialog(
            onDismissRequest = { if (!busy) removing = false },
            title = { Text("Remove profile photo?") },
            text = { Text("Your initials will appear until you add another photo.") },
            confirmButton = {
                TextButton(
                    {
                        removing = false
                        onSave(null) {
                            draft = null
                            draftSource = null
                        }
                    },
                    enabled = !busy,
                ) {
                    Text("Remove photo", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton({ removing = false }, enabled = !busy) { Text("Cancel") }
            },
        )
}

@Composable
private fun PhotoAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            icon,
            null,
            tint =
                if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
        Text(
            label,
            color =
                if (danger) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface,
        )
    }
}
