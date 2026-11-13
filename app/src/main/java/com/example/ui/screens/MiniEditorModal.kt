package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.ClipProject
import com.example.data.model.formatSeconds
import com.example.ui.theme.AmberHighlight
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseError
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextSecondary

@Composable
fun MiniEditorModal(
    clip: ClipProject,
    isSaving: Boolean,
    onClose: () -> Unit,
    onSave: (ClipProject) -> Unit,
    onDelete: (String) -> Unit
) {
    var title by remember(clip.id) { mutableStateOf(clip.title) }
    var hook by remember(clip.id) { mutableStateOf(clip.hook) }
    var aspectRatio by remember(clip.id) { mutableStateOf(clip.aspectRatio) }
    var captionStyle by remember(clip.id) { mutableStateOf(clip.captionStyle) }
    var captionPosition by remember(clip.id) { mutableStateOf(clip.captionPosition) }
    var transcript by remember(clip.id) { mutableStateOf(clip.transcript) }
    var previewLineIdx by remember(clip.id) { mutableIntStateOf(0) }

    val minBound = (clip.startTimeSec - 30L).coerceAtLeast(0L).toFloat()
    val maxBound = (clip.endTimeSec + 45L).toFloat().coerceAtLeast(minBound + 20f)
    var trimRange by remember(clip.id) {
        mutableStateOf(clip.startTimeSec.toFloat()..clip.endTimeSec.toFloat())
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler(onBack = onClose)

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = StudioBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("editor_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.editor_close),
                                tint = Color.White
                            )
                        }
                        Text(
                            text = stringResource(R.string.editor_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }

                    if (clip.id.isNotBlank()) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("editor_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Excluir corte",
                                tint = RoseError
                            )
                        }
                    }
                }

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Live Aspect Ratio Preview Canvas
                    val previewAspect = when (aspectRatio) {
                        "1:1" -> 1f
                        "16:9" -> 16f / 9f
                        else -> 9f / 14f
                    }
                    val bgRes = if (clip.themeIndex % 2L == 0L) R.drawable.img_clip_bg_1 else R.drawable.img_clip_bg_2
                    val activeLine = transcript.getOrNull(previewLineIdx)?.text ?: hook

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 260.dp)
                                .fillMaxWidth(if (aspectRatio == "16:9") 1f else 0.72f)
                                .aspectRatio(previewAspect)
                                .clip(RoundedCornerShape(20.dp))
                                .background(StudioSurface)
                        ) {
                            Image(
                                painter = painterResource(id = bgRes),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Black.copy(alpha = 0.25f),
                                                Color.Black.copy(alpha = 0.65f)
                                            )
                                        )
                                    )
                            )

                            // Play preview cycle button
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(48.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (transcript.isNotEmpty()) {
                                            previewLineIdx = (previewLineIdx + 1) % transcript.size
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Simular legenda",
                                        tint = Color.White
                                    )
                                }
                            }

                            // Caption Preview Overlay positioned dynamically
                            val captionAlignment = when (captionPosition) {
                                "Topo" -> Alignment.TopCenter
                                "Centro" -> Alignment.Center
                                else -> Alignment.BottomCenter
                            }
                            Box(
                                modifier = Modifier
                                    .align(captionAlignment)
                                    .padding(14.dp)
                            ) {
                                CaptionPreviewBadge(
                                    text = activeLine,
                                    styleName = captionStyle
                                )
                            }
                        }
                    }

                    // Trim Slider
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = StudioSurface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val startSec = trimRange.start.toLong()
                            val endSec = trimRange.endInclusive.toLong().coerceAtLeast(startSec + 5L)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Recorte de Tempo",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "${formatSeconds(startSec)} – ${formatSeconds(endSec)} (${endSec - startSec}s)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldAccent
                                )
                            }
                            RangeSlider(
                                value = trimRange,
                                onValueChange = { range ->
                                    if (range.endInclusive - range.start >= 5f) {
                                        trimRange = range
                                    }
                                },
                                valueRange = minBound..maxBound,
                                colors = SliderDefaults.colors(
                                    thumbColor = EmeraldAccent,
                                    activeTrackColor = IndigoPrimary
                                )
                            )
                        }
                    }

                    // Aspect Ratio, Caption Style & Position
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = StudioSurface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Formato do Vídeo", style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("9:16", "1:1", "16:9").forEach { ratio ->
                                    FilterChip(
                                        selected = aspectRatio == ratio,
                                        onClick = { aspectRatio = ratio },
                                        label = { Text(ratio, style = MaterialTheme.typography.labelMedium) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = IndigoPrimary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }

                            Text("Estilo da Legenda", style = MaterialTheme.typography.titleSmall)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Karaokê", "Minimalista", "Alto Contraste").forEach { style ->
                                    FilterChip(
                                        selected = captionStyle == style,
                                        onClick = { captionStyle = style },
                                        label = { Text(style, style = MaterialTheme.typography.bodySmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldContainer,
                                            selectedLabelColor = EmeraldAccent
                                        )
                                    )
                                }
                            }

                            Text("Posição da Legenda", style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Topo", "Centro", "Base").forEach { pos ->
                                    FilterChip(
                                        selected = captionPosition == pos,
                                        onClick = { captionPosition = pos },
                                        label = { Text(pos, style = MaterialTheme.typography.bodySmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = IndigoPrimary.copy(alpha = 0.28f),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Title & Hook Fields
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título do Corte") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_title_input")
                    )

                    OutlinedTextField(
                        value = hook,
                        onValueChange = { hook = it },
                        label = { Text("Gancho Inicial (3s)") },
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_hook_input")
                    )

                    // Editable Transcript Lines
                    Text(
                        text = "Segmentos da Legenda",
                        style = MaterialTheme.typography.titleSmall
                    )
                    transcript.forEachIndexed { idx, seg ->
                        OutlinedTextField(
                            value = seg.text,
                            onValueChange = { newVal ->
                                transcript = transcript.mapIndexed { i, item ->
                                    if (i == idx) item.copy(text = newVal) else item
                                }
                            },
                            label = { Text("${formatSeconds(seg.startSec)} – ${formatSeconds(seg.endSec)}") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                }

                // Bottom Save Bar
                Surface(
                    color = StudioSurface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onClose,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(stringResource(R.string.editor_close), color = Color.White)
                        }
                        Button(
                            onClick = {
                                val startSec = trimRange.start.toLong().coerceAtLeast(0L)
                                val endSec = trimRange.endInclusive.toLong().coerceAtLeast(startSec + 5L)
                                onSave(
                                    clip.copy(
                                        title = title.trim().ifBlank { clip.title },
                                        hook = hook.trim().ifBlank { clip.hook },
                                        startTimeSec = startSec,
                                        endTimeSec = endSec,
                                        aspectRatio = aspectRatio,
                                        captionStyle = captionStyle,
                                        captionPosition = captionPosition,
                                        transcript = transcript.filter { it.text.isNotBlank() }
                                            .ifEmpty { clip.transcript }
                                    )
                                )
                            },
                            enabled = !isSaving,
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp)
                                .testTag("editor_save_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = StudioBackground,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = StudioBackground,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.editor_save),
                                    color = StudioBackground,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = StudioSurfaceElevated,
            title = { Text("Excluir corte?") },
            text = { Text("Essa ação remove permanentemente \"${clip.title}\" dos seus projetos.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(clip.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                    Text("Excluir", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun CaptionPreviewBadge(
    text: String,
    styleName: String
) {
    when (styleName) {
        "Alto Contraste" -> Surface(
            color = AmberHighlight,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
        "Minimalista" -> Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
        else -> Surface(
            color = StudioBackground.copy(alpha = 0.85f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.7f))
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = EmeraldAccent,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }
    }
}
