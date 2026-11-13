package com.example.data.model

import android.graphics.Bitmap
import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TabId {
    CREATE,
    PROJECTS,
    PROFILE
}

enum class CreateFlowStage {
    FORM,
    PROCESSING,
    RESULTS
}

@IgnoreExtraProperties
data class TranscriptSegment(
    val id: String = "",
    val startSec: Long = 0L,
    val endSec: Long = 0L,
    val text: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "startSec" to startSec,
        "endSec" to endSec,
        "text" to text
    )
}

@IgnoreExtraProperties
data class ClipProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sourceType: String = "topic",
    val sourceValue: String = "",
    val title: String = "",
    val hook: String = "",
    val viralReason: String = "",
    val viralScore: Long = 85L,
    val startTimeSec: Long = 0L,
    val endTimeSec: Long = 45L,
    val aspectRatio: String = "9:16",
    val tone: String = "Hook Viral",
    val captionStyle: String = "Karaokê",
    val captionPosition: String = "Base",
    val themeIndex: Long = 0L,
    val transcript: List<TranscriptSegment> = emptyList(),
    val hashtags: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val durationSec: Long
        get() = (endTimeSec - startTimeSec).coerceAtLeast(1L)
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val defaultAspectRatio: String = "9:16",
    val defaultTone: String = "Hook Viral",
    val defaultCaptionStyle: String = "Karaokê",
    val defaultDurationSec: Long = 45L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class GenerationPreferences(
    val sourceType: String = "url", // "url", "topic", "video"
    val sourceValue: String = "",
    val aspectRatio: String = "9:16",
    val durationSec: Long = 45L,
    val tone: String = "Hook Viral",
    val captionStyle: String = "Karaokê",
    val localVideoUri: Uri? = null,
    val localVideoDurationSec: Long? = null,
    val videoFrames: List<Bitmap> = emptyList()
)

fun formatSeconds(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0L)
    val mins = safe / 60L
    val secs = safe % 60L
    return "%02d:%02d".format(mins, secs)
}
