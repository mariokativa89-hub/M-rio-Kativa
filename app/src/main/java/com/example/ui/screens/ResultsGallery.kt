package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ClipProject
import com.example.data.model.TranscriptSegment
import com.example.data.model.formatSeconds
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextSecondary

@Composable
fun ResultsGallery(
    clips: List<ClipProject>,
    onReset: () -> Unit,
    onEditClip: (ClipProject) -> Unit,
    onUpdateTranscript: (clipId: String, segmentId: String, newText: String) -> Unit,
    onNotify: (String) -> Unit
) {
    val context = LocalContext.current
    var editingSegmentPair by remember { mutableStateOf<Pair<String, TranscriptSegment>?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.results_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        text = "Toque em qualquer legenda para editar",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                OutlinedButton(
                    onClick = onReset,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("results_new_video_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.results_new_video),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
        }

        itemsIndexed(clips, key = { _, item -> item.id.ifBlank { item.title } }) { index, clip ->
            ClipResultCard(
                clip = clip,
                index = index,
                onEditClip = { onEditClip(clip) },
                onTapSegment = { seg -> editingSegmentPair = clip.id to seg },
                onCopyScript = {
                    copyClipToClipboard(context, clip)
                    onNotify("Roteiro e hashtags copiados!")
                },
                onShareClip = {
                    shareClipSummary(context, clip)
                }
            )
        }

        item { Spacer(Modifier.height(12.dp)) }
    }

    val activeEdit = editingSegmentPair
    if (activeEdit != null) {
        var draftText by remember(activeEdit) { mutableStateOf(activeEdit.second.text) }
        AlertDialog(
            onDismissRequest = { editingSegmentPair = null },
            containerColor = StudioSurface,
            title = {
                Text(
                    text = "Editar legenda (${formatSeconds(activeEdit.second.startSec)} – ${formatSeconds(activeEdit.second.endSec)})",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                OutlinedTextField(
                    value = draftText,
                    onValueChange = { draftText = it },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateTranscript(activeEdit.first, activeEdit.second.id, draftText)
                        editingSegmentPair = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Text("Salvar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSegmentPair = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ClipResultCard(
    clip: ClipProject,
    index: Int,
    onEditClip: () -> Unit,
    onTapSegment: (TranscriptSegment) -> Unit,
    onCopyScript: () -> Unit,
    onShareClip: () -> Unit
) {
    val bgRes = if (clip.themeIndex % 2L == 0L) R.drawable.img_clip_bg_1 else R.drawable.img_clip_bg_2

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = StudioSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("clip_result_card_$index")
    ) {
        Column {
            // Visual Cover Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
            ) {
                Image(
                    painter = painterResource(id = bgRes),
                    contentDescription = clip.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    StudioBackground.copy(alpha = 0.25f),
                                    StudioBackground.copy(alpha = 0.92f)
                                )
                            )
                        )
                )

                // Top Pills: Format + Timestamp + Viral Score
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = StudioBackground.copy(alpha = 0.8f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "${formatSeconds(clip.startTimeSec)} – ${formatSeconds(clip.endTimeSec)} (${clip.durationSec}s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Surface(
                            color = IndigoPrimary.copy(alpha = 0.85f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = clip.aspectRatio,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Surface(
                        color = EmeraldContainer.copy(alpha = 0.95f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${clip.viralScore}/100",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Title & Hook overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                ) {
                    Text(
                        text = clip.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Gancho: \"${clip.hook}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldAccent
                    )
                }
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (clip.viralReason.isNotBlank()) {
                    Surface(
                        color = StudioBackground.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = clip.viralReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Interactive Transcript List
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Legendas (${clip.captionStyle})",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    clip.transcript.forEach { seg ->
                        Surface(
                            color = StudioSurfaceElevated.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onTapSegment(seg) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = formatSeconds(seg.startSec),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IndigoPrimary
                                )
                                Text(
                                    text = seg.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Editar segmento",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Hashtags
                if (clip.hashtags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        clip.hashtags.forEach { tag ->
                            Surface(
                                color = IndigoPrimary.copy(alpha = 0.16f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IndigoPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onEditClip,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("open_editor_button_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.open_mini_editor),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StudioSurfaceElevated,
                        modifier = Modifier.size(48.dp)
                    ) {
                        IconButton(
                            onClick = onCopyScript,
                            modifier = Modifier.testTag("copy_script_button_$index")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = stringResource(R.string.copy_script),
                                tint = Color.White
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StudioSurfaceElevated,
                        modifier = Modifier.size(48.dp)
                    ) {
                        IconButton(
                            onClick = onShareClip,
                            modifier = Modifier.testTag("share_clip_button_$index")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = stringResource(R.string.share_clip),
                                tint = EmeraldAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun copyClipToClipboard(context: Context, clip: ClipProject) {
    val body = buildString {
        appendLine("🎬 ${clip.title}")
        appendLine("⚡ Gancho: ${clip.hook}")
        appendLine("⏱ ${formatSeconds(clip.startTimeSec)} - ${formatSeconds(clip.endTimeSec)} | Viral Score: ${clip.viralScore}/100")
        appendLine()
        clip.transcript.forEach { seg ->
            appendLine("[${formatSeconds(seg.startSec)}] ${seg.text}")
        }
        if (clip.hashtags.isNotEmpty()) {
            appendLine()
            append(clip.hashtags.joinToString(" "))
        }
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(clip.title, body))
}

private fun shareClipSummary(context: Context, clip: ClipProject) {
    val text = buildString {
        appendLine("🎬 ${clip.title}")
        appendLine("⚡ ${clip.hook}")
        appendLine()
        clip.transcript.forEach { seg ->
            appendLine("${formatSeconds(seg.startSec)} - ${seg.text}")
        }
        if (clip.hashtags.isNotEmpty()) {
            appendLine()
            append(clip.hashtags.joinToString(" "))
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, clip.title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_clip)))
}
