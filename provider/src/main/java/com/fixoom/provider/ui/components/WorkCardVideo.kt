package com.fixoom.provider.ui.components

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.viewinterop.AndroidView
import com.fixoom.provider.R

/** Packaged, silent decoration. The poster remains beneath preparation and playback failures. */
@Composable
internal fun ActiveJobsVideoBackground(playing: Boolean, modifier: Modifier = Modifier) {
    var failed by remember { mutableStateOf(false) }
    Box(modifier.clearAndSetSemantics {}) {
        Image(
            painterResource(R.drawable.active_jobs_poster),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().testTag("active_jobs_poster"),
            contentScale = ContentScale.Crop,
        )
        if (playing && !failed) {
            AndroidView(
                factory = { context ->
                    WorkCardVideoView(context) { failed = true }
                },
                modifier = Modifier.fillMaxSize().testTag("active_jobs_video"),
                onRelease = { it.releasePlayback() },
            )
        }
    }
}

/**
 * TextureView respects Compose clipping/transforms and leaves the parent card in charge of taps.
 */
internal class WorkCardVideoView(context: Context, private val onFailure: () -> Unit) :
    TextureView(context), TextureView.SurfaceTextureListener {
    private var player: MediaPlayer? = null
    private var output: Surface? = null

    init {
        alpha = 0f // Reveal only once decoded video is available; never flash black.
        isOpaque = false
        isClickable = false
        isFocusable = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        surfaceTextureListener = this
    }

    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        releasePlayback()
        cropToFill(width, height)
        val media = MediaPlayer()
        player = media
        try {
            output = Surface(texture)
            media.setSurface(output)
            resources.openRawResourceFd(R.raw.active_jobs_background).use {
                media.setDataSource(it.fileDescriptor, it.startOffset, it.length)
            }
            media.setVolume(0f, 0f)
            media.isLooping = true
            media.setOnPreparedListener { prepared ->
                if (player === prepared) prepared.start()
            }
            media.setOnInfoListener { current, what, _ ->
                if (player === current && what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    alpha = 1f
                }
                false
            }
            media.setOnErrorListener { _, _, _ ->
                releasePlayback()
                onFailure()
                true
            }
            media.prepareAsync()
        } catch (_: Exception) {
            releasePlayback()
            onFailure()
        }
    }

    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
        cropToFill(width, height)
    }

    private fun cropToFill(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val side = maxOf(width, height).toFloat()
        setTransform(
            Matrix().apply {
                setScale(side / width, side / height, width / 2f, height / 2f)
            }
        )
    }

    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit

    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
        releasePlayback()
        return true
    }

    override fun onDetachedFromWindow() {
        releasePlayback()
        super.onDetachedFromWindow()
    }

    fun releasePlayback() {
        alpha = 0f
        player?.apply {
            setOnPreparedListener(null)
            setOnInfoListener(null)
            setOnErrorListener(null)
            release()
        }
        player = null
        output?.release()
        output = null
    }
}
