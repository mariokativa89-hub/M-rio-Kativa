package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.GenerationPreferences
import com.example.data.model.UserProfile
import com.example.data.model.formatSeconds
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextSecondary
import com.example.util.LocalVideoInfo
import com.example.util.extractLocalVideoInfo
import kotlinx.coroutines.launch

@Composable
fun CreateScreen(
    userProfile: UserProfile?,
    onGenerate: (GenerationPreferences) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceMode by remember { mutableStateOf("url") } // "url" or "video"
    var sourceInput by remember { mutableStateOf("") }
    var aspectRatio by remember { mutableStateOf(userProfile?.defaultAspectRatio ?: "9:16") }
    var durationSec by remember { mutableLongStateOf(userProfile?.defaultDurationSec ?: 45L) }
    var tone by remember { mutableStateOf(userProfile?.defaultTone ?: "Hook Viral") }
    var captionStyle by remember { mutableStateOf(userProfile?.defaultCaptionStyle ?: "Karaokê") }

    var selectedVideoInfo by remember { mutableStateOf<LocalVideoInfo?>(null) }
    var selectedVideoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isReadingVideo by remember { mutableStateOf(false) }

    LaunchedEffect(userProfile) {
        if (userProfile != null) {
            aspectRatio = userProfile.defaultAspectRatio
            durationSec = userProfile.defaultDurationSec
            tone = userProfile.defaultTone
            captionStyle = userProfile.defaultCaptionStyle
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedVideoUri = uri
            isReadingVideo = true
            scope.launch {
                val info = extractLocalVideoInfo(context, uri)
                selectedVideoInfo = info
                isReadingVideo = false
            }
        }
    }

    val aspectOptions = listOf("9:16", "1:1", "16:9")
    val durationOptions = listOf(30L, 45L, 60L)
    val toneOptions = listOf("Hook Viral", "Educacional", "Debate", "Storytelling")
    val captionOptions = listOf("Karaokê", "Minimalista", "Alto Contraste")
    val quickIdeas = listOf(
        "Podcast sobre IA e produtividade para criadores",
        "3 erros que derrubam a retenção no Reels",
        "Bastidores de como gravar vídeos com o celular"
    )

    val canSubmit = if (sourceMode == "video") {
        selectedVideoUri != null && !isReadingVideo
    } else {
        sourceInput.trim().length >= 4
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Compact Studio Hero Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = StudioSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_studio_hero),
                    contentDescription = stringResource(R.string.cd_hero_banner),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    StudioBackground.copy(alpha = 0.94f),
                                    StudioBackground.copy(alpha = 0.60f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.create_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.create_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Source Mode Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(StudioSurface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SourceModeTab(
                label = stringResource(R.string.source_tab_url),
                icon = Icons.Rounded.Link,
                selected = sourceMode == "url",
                modifier = Modifier
                    .weight(1f)
                    .testTag("source_tab_url"),
                onClick = { sourceMode = "url" }
            )
            SourceModeTab(
                label = stringResource(R.string.source_tab_video),
                icon = Icons.Rounded.VideoLibrary,
                selected = sourceMode == "video",
                modifier = Modifier
                    .weight(1f)
                    .testTag("source_tab_video"),
                onClick = { sourceMode = "video" }
            )
        }

        // Source Input Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = StudioSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (sourceMode == "url") {
                    Text(
                        text = stringResource(R.string.source_input_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = sourceInput,
                        onValueChange = { sourceInput = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.source_input_placeholder),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = StudioBackground.copy(alpha = 0.6f),
                            unfocusedContainerColor = StudioBackground.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("source_input_field")
                    )

                    Text(
                        text = stringResource(R.string.quick_ideas_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickIdeas.forEachIndexed { idx, idea ->
                            FilterChip(
                                selected = sourceInput == idea,
                                onClick = { sourceInput = idea },
                                label = {
                                    Text(
                                        text = idea,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                modifier = Modifier.testTag("quick_idea_chip_$idx"),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = StudioSurfaceElevated,
                                    selectedContainerColor = IndigoPrimary.copy(alpha = 0.25f),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                } else {
                    val info = selectedVideoInfo
                    if (isReadingVideo) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = EmeraldAccent,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Extraindo quadros do vídeo…",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else if (info != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val previewFrame = info.frames.firstOrNull()
                            if (previewFrame != null) {
                                Image(
                                    bitmap = previewFrame.asImageBitmap(),
                                    contentDescription = info.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(StudioSurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Movie,
                                        contentDescription = null,
                                        tint = EmeraldAccent
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = info.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "Duração: ${formatSeconds(info.durationSec)} • ${info.frames.size} quadros para análise IA",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pick_video_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VideoLibrary,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (selectedVideoInfo == null) {
                                stringResource(R.string.pick_video_button)
                            } else {
                                stringResource(R.string.change_video_button)
                            },
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Format & Duration Section
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = StudioSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.format_label),
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    aspectOptions.forEach { ratio ->
                        val selected = aspectRatio == ratio
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) IndigoPrimary.copy(alpha = 0.22f) else StudioBackground.copy(alpha = 0.6f),
                            border = BorderStroke(
                                1.dp,
                                if (selected) IndigoPrimary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { aspectRatio = ratio }
                                .testTag("aspect_option_$ratio")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val boxW = when (ratio) {
                                    "9:16" -> 14.dp
                                    "1:1" -> 20.dp
                                    else -> 26.dp
                                }
                                val boxH = when (ratio) {
                                    "9:16" -> 24.dp
                                    "1:1" -> 20.dp
                                    else -> 15.dp
                                }
                                Box(
                                    modifier = Modifier
                                        .size(width = boxW, height = boxH)
                                        .border(
                                            2.dp,
                                            if (selected) EmeraldAccent else TextSecondary,
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                                Text(
                                    text = ratio,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = EmeraldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.duration_label),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    durationOptions.forEach { sec ->
                        val selected = durationSec == sec
                        FilterChip(
                            selected = selected,
                            onClick = { durationSec = sec },
                            label = {
                                Text(
                                    text = "${sec}s",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("duration_option_$sec"),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = StudioBackground.copy(alpha = 0.6f),
                                selectedContainerColor = EmeraldContainer,
                                selectedLabelColor = EmeraldAccent
                            )
                        )
                    }
                }
            }
        }

        // Tone & Caption Style Section
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = StudioSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.tone_label),
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    toneOptions.forEach { option ->
                        FilterChip(
                            selected = tone == option,
                            onClick = { tone = option },
                            label = { Text(option, style = MaterialTheme.typography.bodySmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = StudioBackground.copy(alpha = 0.6f),
                                selectedContainerColor = IndigoPrimary.copy(alpha = 0.25f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ClosedCaption,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.caption_style_label),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    captionOptions.forEach { style ->
                        FilterChip(
                            selected = captionStyle == style,
                            onClick = { captionStyle = style },
                            label = { Text(style, style = MaterialTheme.typography.bodySmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = StudioBackground.copy(alpha = 0.6f),
                                selectedContainerColor = EmeraldContainer,
                                selectedLabelColor = EmeraldAccent
                            )
                        )
                    }
                }
            }
        }

        // Primary Generate CTA
        Button(
            onClick = {
                val trimmed = sourceInput.trim()
                val isUrl = trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)
                val prefs = if (sourceMode == "video" && selectedVideoInfo != null) {
                    GenerationPreferences(
                        sourceType = "video",
                        sourceValue = selectedVideoInfo!!.displayName,
                        aspectRatio = aspectRatio,
                        durationSec = durationSec,
                        tone = tone,
                        captionStyle = captionStyle,
                        localVideoUri = selectedVideoUri,
                        localVideoDurationSec = selectedVideoInfo!!.durationSec,
                        videoFrames = selectedVideoInfo!!.frames
                    )
                } else {
                    GenerationPreferences(
                        sourceType = if (isUrl) "url" else "topic",
                        sourceValue = trimmed,
                        aspectRatio = aspectRatio,
                        durationSec = durationSec,
                        tone = tone,
                        captionStyle = captionStyle
                    )
                }
                onGenerate(prefs)
            },
            enabled = canSubmit,
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = StudioSurfaceElevated
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("generate_clips_button")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (canSubmit) {
                            Brush.horizontalGradient(listOf(IndigoPrimary, EmeraldAccent))
                        } else {
                            Brush.horizontalGradient(listOf(StudioSurfaceElevated, StudioSurfaceElevated))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = if (canSubmit) Color.White else TextSecondary
                    )
                    Text(
                        text = stringResource(R.string.generate_cta),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (canSubmit) Color.White else TextSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SourceModeTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) IndigoPrimary else Color.Transparent,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else TextSecondary
            )
        }
    }
}
