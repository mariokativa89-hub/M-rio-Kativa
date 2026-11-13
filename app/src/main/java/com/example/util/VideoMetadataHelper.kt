package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LocalVideoInfo(
    val displayName: String,
    val durationSec: Long,
    val frames: List<Bitmap>
)

suspend fun extractLocalVideoInfo(
    context: Context,
    uri: Uri
): LocalVideoInfo = withContext(Dispatchers.IO) {
    val displayName = resolveDisplayName(context, uri)
    val retriever = MediaMetadataRetriever()
    val frames = mutableListOf<Bitmap>()
    var durationSec = 45L

    try {
        retriever.setDataSource(context, uri)
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 45_000L
        durationSec = (durationMs / 1000L).coerceAtLeast(5L)

        val sampleFractions = listOf(0.15, 0.50, 0.85)
        for (fraction in sampleFractions) {
            val timeUs = (durationMs * 1000.0 * fraction).toLong()
            val rawFrame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (rawFrame != null) {
                frames.add(scaleDownBitmap(rawFrame, 640))
            }
        }
    } catch (_: Exception) {
        // Return fallback duration and any captured frames gracefully
    } finally {
        try {
            retriever.release()
        } catch (_: Exception) {
        }
    }

    LocalVideoInfo(
        displayName = displayName,
        durationSec = durationSec,
        frames = frames
    )
}

private fun resolveDisplayName(context: Context, uri: Uri): String {
    return try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            } ?: uri.lastPathSegment ?: "video_selecionado.mp4"
    } catch (_: Exception) {
        uri.lastPathSegment ?: "video_selecionado.mp4"
    }
}

private fun scaleDownBitmap(source: Bitmap, maxDim: Int): Bitmap {
    val width = source.width
    val height = source.height
    if (width <= maxDim && height <= maxDim) return source
    val scale = maxDim.toFloat() / maxOf(width, height).toFloat()
    val targetW = (width * scale).toInt().coerceAtLeast(1)
    val targetH = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(source, targetW, targetH, true)
}
