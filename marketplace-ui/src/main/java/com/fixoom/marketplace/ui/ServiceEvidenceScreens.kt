package com.fixoom.marketplace.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fixoom.core.data.repository.*
import com.fixoom.core.model.*
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID
import kotlinx.coroutines.*

private data class EvidenceDraft(val id: String, val bytes: ByteArray, val video: Boolean)

private fun InputStream.readEvidenceBytes(): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        require(output.size() + count <= ServiceEvidenceRepository.MAX_BYTES)
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

/** Shared by both participants. No broad storage or camera permissions are requested. */
@Composable
fun ServiceEvidencePanel(
    contextId: String,
    complaint: Boolean = false,
    canUpload: Boolean = true,
    modern: Boolean = false,
) {
    val repository = remember { ServiceEvidenceRepository() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var records by remember(contextId) { mutableStateOf<List<ServiceAttachment>>(emptyList()) }
    var error by remember(contextId) { mutableStateOf<String?>(null) }
    var notice by remember(contextId) { mutableStateOf<String?>(null) }
    var busy by remember(contextId) { mutableStateOf(false) }
    var draft by remember(contextId) { mutableStateOf<EvidenceDraft?>(null) }
    var showing by remember(contextId) { mutableStateOf<ServiceAttachment?>(null) }
    var retry by remember(contextId) { mutableIntStateOf(0) }
    LaunchedEffect(contextId, complaint, retry) {
        busy = true
        try {
            records = withTimeout(15_000) { repository.attachments(contextId, complaint) }
            error = null
        } catch (e: CancellationException) {
            if (e !is TimeoutCancellationException) throw e
            error = "Attachments timed out. Refresh to retry."
        } catch (_: Exception) {
            error = "Attachments could not load. Refresh to retry."
        } finally {
            busy = false
        }
    }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null)
                scope.launch {
                    busy = true
                    error = null
                    notice = null
                    try {
                        draft =
                            withContext(Dispatchers.IO) {
                                val video =
                                    context.contentResolver.getType(uri)?.startsWith("video/") ==
                                        true
                                val bytes =
                                    if (video) {
                                        require(context.contentResolver.getType(uri) == "video/mp4")
                                        val info = MediaMetadataRetriever()
                                        try {
                                            info.setDataSource(context, uri)
                                            val duration =
                                                info
                                                    .extractMetadata(
                                                        MediaMetadataRetriever.METADATA_KEY_DURATION
                                                    )
                                                    ?.toLongOrNull()
                                            require(duration != null && duration in 1..60_000)
                                        } finally {
                                            info.release()
                                        }
                                        context.contentResolver.openInputStream(uri)!!.use {
                                            it.readEvidenceBytes()
                                        }
                                    } else {
                                        // Preserve the complete frame; evidence must not be cropped
                                        // like an avatar.
                                        val image = decodeProviderPhoto(context, uri)
                                        try {
                                            ByteArrayOutputStream().use {
                                                check(
                                                    image.compress(
                                                        Bitmap.CompressFormat.JPEG,
                                                        82,
                                                        it,
                                                    )
                                                )
                                                it.toByteArray()
                                            }
                                        } finally {
                                            image.recycle()
                                        }
                                    }
                                require(ServiceEvidenceRepository.validMedia(bytes, video))
                                EvidenceDraft(UUID.randomUUID().toString(), bytes, video)
                            }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        error =
                            "Choose a photo up to 15 MB, or an MP4 video up to 60 seconds and 25 MB. Try a smaller file if needed."
                    } finally {
                        busy = false
                    }
                }
        }
    BackHandler(busy) { /* Prevent leaving an in-flight upload through the system Back action. */ }
    EvidenceCard(
        modern,
        if (complaint) "Complaint evidence" else "Booking photos & videos",
        if (complaint) "Private to you and the administrator. Add up to five files."
        else
            "Visible to this booking's customer, provider and administrator. Up to five files per person.",
    ) {
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Preparing, uploading or refreshing…")
        }
        if (records.isEmpty() && !busy && error == null) Text("No attachments yet.")
        records.forEachIndexed { index, file ->
            OutlinedButton({ showing = file }, enabled = !busy) {
                Icon(
                    if (file.mediaType == "video/mp4") Icons.Rounded.PlayCircle
                    else Icons.Rounded.Image,
                    null,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${if (file.mediaType == "video/mp4") "Video" else "Photo"} ${index + 1} · ${serviceTime(file.createdAt)}"
                )
            }
        }
        if (draft == null) {
            if (canUpload)
                Button(
                    {
                        picker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageAndVideo
                            )
                        )
                    },
                    enabled = !busy,
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add photo or video")
                }
        } else {
            val pending = draft!!
            Text("${if (pending.video) "Video" else "Photo"} selected · not uploaded yet")
            if (!pending.video) {
                val bitmap =
                    remember(pending.id) {
                        BitmapFactory.decodeByteArray(pending.bytes, 0, pending.bytes.size)
                            .asImageBitmap()
                    }
                Image(
                    bitmap,
                    "Selected evidence preview",
                    Modifier.fillMaxWidth().heightIn(max = 240.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    {
                        scope.launch {
                            busy = true
                            error = null
                            try {
                                withTimeout(120_000) {
                                    repository.upload(
                                        contextId,
                                        complaint,
                                        pending.id,
                                        pending.bytes,
                                        pending.video,
                                    )
                                }
                                draft = null
                                notice = "Attachment saved."
                                retry++
                            } catch (e: CancellationException) {
                                if (e !is TimeoutCancellationException) throw e
                                error =
                                    "Upload timed out. Retry this same file to safely finish saving it."
                            } catch (_: Exception) {
                                error =
                                    "Could not save this attachment. Refresh to check saved files, then retry. Check the five-file limit and account access."
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy,
                ) {
                    Text("Upload attachment")
                }
                TextButton({ draft = null }, enabled = !busy) { Text("Discard") }
            }
        }
        TextButton({ retry++ }, enabled = !busy) { Text("Refresh attachments") }
    }
    showing?.let { file -> EvidenceViewer(file, repository) { showing = null } }
}

@Composable
private fun EvidenceViewer(
    file: ServiceAttachment,
    repository: ServiceEvidenceRepository,
    close: () -> Unit,
) {
    var url by remember(file.id) { mutableStateOf<String?>(null) }
    var error by remember(file.id) { mutableStateOf(false) }
    var image by remember(file.id) { mutableStateOf<Bitmap?>(null) }
    var videoView by remember { mutableStateOf<VideoView?>(null) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(file.id) {
        try {
            withTimeout(30_000) {
                val signed = repository.preview(file.objectPath)
                if (file.mediaType == "image/jpeg")
                    image =
                        withContext(Dispatchers.IO) {
                            val connection =
                                java.net.URI(signed).toURL().openConnection().apply {
                                    connectTimeout = 10_000
                                    readTimeout = 15_000
                                }
                            val bytes = connection.getInputStream().use { it.readEvidenceBytes() }
                            require(bytes.size <= ServiceEvidenceRepository.MAX_BYTES)
                            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                            require(bounds.outWidth in 1..4096 && bounds.outHeight in 1..4096)
                            requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                        }
                else url = signed
            }
        } catch (e: CancellationException) {
            if (e !is TimeoutCancellationException) throw e
            error = true
        } catch (_: Exception) {
            error = true
        }
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) videoView?.pause()
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            videoView?.stopPlayback()
        }
    }
    Dialog(onDismissRequest = close) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (file.mediaType == "video/mp4") "Video evidence" else "Photo evidence",
                    style = MaterialTheme.typography.titleLarge,
                )
                if (error)
                    Text(
                        "This file could not be opened. Close and try again.",
                        color = MaterialTheme.colorScheme.error,
                    )
                else if (image != null)
                    Image(
                        image!!.asImageBitmap(),
                        "Saved evidence",
                        Modifier.fillMaxWidth().heightIn(max = 420.dp),
                        contentScale = ContentScale.Fit,
                    )
                else if (url != null)
                    AndroidView(
                        factory = { context ->
                            VideoView(context).also { view ->
                                videoView = view
                                view.setMediaController(
                                    MediaController(context).also { it.setAnchorView(view) }
                                )
                                view.setVideoURI(Uri.parse(url))
                                view.setOnPreparedListener { view.seekTo(1) }
                                view.setOnErrorListener { _, _, _ ->
                                    error = true
                                    true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(280.dp),
                    )
                else CircularProgressIndicator()
                TextButton(close) { Text("Close") }
            }
        }
    }
}

@Composable
fun ConnectedSupport(vm: MarketplaceViewModel, bookingId: String? = null, onBack: () -> Unit) {
    val account by vm.state.collectAsStateWithLifecycle()
    val repository = remember { ServiceEvidenceRepository() }
    val scope = rememberCoroutineScope()
    var records by remember(bookingId) { mutableStateOf<List<ServiceComplaint>>(emptyList()) }
    var selected by remember(bookingId) { mutableStateOf<ServiceComplaint?>(null) }
    var subject by rememberSaveable(bookingId) { mutableStateOf("") }
    var description by rememberSaveable(bookingId) { mutableStateOf("") }
    var requestId by rememberSaveable(bookingId) { mutableStateOf(UUID.randomUUID().toString()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(account.data.profile?.id, bookingId, retry) {
        if (account.data.profile == null) {
            records = emptyList()
            selected = null
            loading = false
            return@LaunchedEffect
        }
        loading = true
        try {
            records = withTimeout(15_000) { repository.complaints(bookingId) }
            selected = selected?.let { old -> records.firstOrNull { it.id == old.id } }
            error = null
        } catch (e: CancellationException) {
            if (e !is TimeoutCancellationException) throw e
            error = "Complaint refresh timed out. Try again."
        } catch (_: Exception) {
            error = "Complaints could not load. Check account access and refresh."
        } finally {
            loading = false
        }
    }
    val back = {
        if (!saving) {
            if (selected != null) selected = null else onBack()
        }
    }
    BackHandler { back() }
    MarketplacePage(
        "Help & complaints",
        account.copy(loading = loading, saving = saving, error = error, notice = null),
        { retry++ },
        back,
    ) {
        if (account.data.profile == null)
            item {
                InfoCard("Sign in required", "Sign in to view or submit your private complaints.")
            }
        else if (selected != null) {
            val complaint = selected!!
            item {
                InfoCard(complaint.subject, complaint.description) {
                    Text("Status: ${serviceStatus(complaint.status)}")
                    Text(serviceTime(complaint.createdAt))
                }
            }
            item {
                InfoCard(
                    "Administrator reply",
                    complaint.adminResponse.ifBlank {
                        "No reply yet. Refresh to check for an update."
                    },
                )
            }
            item {
                ServiceEvidencePanel(
                    complaint.id,
                    complaint = true,
                    canUpload = complaint.status != "resolved",
                )
            }
        } else {
            item {
                InfoCard(
                    "Private support",
                    "Describe your concern. Only you and the administrator can read this complaint. Submit it first, then add photos or videos. For urgent safety issues, contact your local emergency service.",
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        subject,
                        { if (it.length <= 120) subject = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("Complaint subject") },
                        supportingText = { Text("5–120 characters") },
                        enabled = !saving,
                        singleLine = true,
                    )
                    OutlinedTextField(
                        description,
                        { if (it.length <= 4000) description = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("What happened?") },
                        supportingText = { Text("10–4000 characters") },
                        minLines = 4,
                        enabled = !saving,
                    )
                    Button(
                        {
                            scope.launch {
                                saving = true
                                error = null
                                try {
                                    selected =
                                        withTimeout(20_000) {
                                            repository.submit(
                                                requestId,
                                                bookingId,
                                                subject,
                                                description,
                                            )
                                        }
                                    requestId = UUID.randomUUID().toString()
                                    subject = ""
                                    description = ""
                                    retry++
                                } catch (e: CancellationException) {
                                    if (e !is TimeoutCancellationException) throw e
                                    error =
                                        "Submission timed out. Retry to check and finish this same complaint."
                                } catch (_: Exception) {
                                    error =
                                        "Complaint could not be saved. Check account access and retry."
                                } finally {
                                    saving = false
                                }
                            }
                        },
                        enabled =
                            !saving &&
                                subject.trim().length >= 5 &&
                                description.trim().length >= 10,
                    ) {
                        Text("Submit private complaint")
                    }
                }
            }
            item { Text("Your complaints", style = MaterialTheme.typography.titleLarge) }
            if (!loading && records.isEmpty()) item { Text("No complaints submitted yet.") }
            items(records, key = { it.id }) { record ->
                InfoCard(
                    record.subject,
                    "${serviceStatus(record.status)} · ${serviceTime(record.createdAt)}",
                ) {
                    TextButton({ selected = record }) { Text("View complaint & evidence") }
                }
            }
        }
    }
}
