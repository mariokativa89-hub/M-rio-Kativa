package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideoLibrary
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ClipProject
import com.example.data.model.UserProfile
import com.example.data.model.formatSeconds
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseError
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ProjectsUiState

@Composable
fun ProjectsTab(
    projectsState: ProjectsUiState,
    onOpenClip: (ClipProject) -> Unit,
    onDeleteClip: (String) -> Unit,
    onGoCreate: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedAspectFilter by remember { mutableStateOf("Todos") }

    when (projectsState) {
        is ProjectsUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IndigoPrimary)
            }
        }
        is ProjectsUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = projectsState.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        is ProjectsUiState.Success -> {
            val allClips = projectsState.clips
            val filteredClips = remember(allClips, searchQuery, selectedAspectFilter) {
                allClips.filter { clip ->
                    val matchesQuery = searchQuery.isBlank() ||
                        clip.title.contains(searchQuery, ignoreCase = true) ||
                        clip.hook.contains(searchQuery, ignoreCase = true) ||
                        clip.sourceValue.contains(searchQuery, ignoreCase = true)
                    val matchesAspect = selectedAspectFilter == "Todos" ||
                        clip.aspectRatio == selectedAspectFilter
                    matchesQuery && matchesAspect
                }
            }

            if (allClips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = StudioSurface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(IndigoPrimary.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VideoLibrary,
                                    contentDescription = null,
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.projects_empty_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.projects_empty_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Button(
                                onClick = onGoCreate,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("projects_empty_create_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCut,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.projects_go_create), color = Color.White)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "${stringResource(R.string.projects_title)} (${allClips.size})",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    stringResource(R.string.projects_search_hint),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("projects_search_input")
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Todos", "9:16", "1:1", "16:9").forEach { filter ->
                                FilterChip(
                                    selected = selectedAspectFilter == filter,
                                    onClick = { selectedAspectFilter = filter },
                                    label = { Text(filter, style = MaterialTheme.typography.labelMedium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IndigoPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    items(filteredClips, key = { it.id }) { clip ->
                        ProjectListCard(
                            clip = clip,
                            onClick = { onOpenClip(clip) },
                            onDelete = { onDeleteClip(clip.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectListCard(
    clip: ClipProject,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val thumbRes = if (clip.themeIndex % 2L == 0L) R.drawable.img_clip_bg_1 else R.drawable.img_clip_bg_2
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = StudioSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("project_card_${clip.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 82.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Image(
                    painter = painterResource(id = thumbRes),
                    contentDescription = clip.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = StudioBackground.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(4.dp)
                ) {
                    Text(
                        text = clip.aspectRatio,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = clip.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = clip.hook,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = EmeraldContainer,
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${clip.viralScore}/100",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldAccent
                            )
                        }
                    }
                    Text(
                        text = "${formatSeconds(clip.startTimeSec)} – ${formatSeconds(clip.endTimeSec)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = IndigoPrimary
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Excluir corte",
                    tint = TextSecondary
                )
            }
        }
    }
}

@Composable
fun ProfileTab(
    userDisplayName: String,
    userEmail: String,
    userPhotoUrl: String?,
    clips: List<ClipProject>,
    userProfile: UserProfile?,
    onSaveDefaults: (
        displayName: String,
        aspectRatio: String,
        tone: String,
        captionStyle: String,
        durationSec: Long
    ) -> Unit,
    onSignOut: () -> Unit
) {
    var displayName by remember(userProfile, userDisplayName) {
        mutableStateOf(userProfile?.displayName?.ifBlank { userDisplayName } ?: userDisplayName)
    }
    var defaultAspectRatio by remember(userProfile) {
        mutableStateOf(userProfile?.defaultAspectRatio ?: "9:16")
    }
    var defaultTone by remember(userProfile) {
        mutableStateOf(userProfile?.defaultTone ?: "Hook Viral")
    }
    var defaultCaptionStyle by remember(userProfile) {
        mutableStateOf(userProfile?.defaultCaptionStyle ?: "Karaokê")
    }
    var defaultDurationSec by remember(userProfile) {
        mutableLongStateOf(userProfile?.defaultDurationSec ?: 45L)
    }

    LaunchedEffect(userProfile) {
        if (userProfile != null) {
            displayName = userProfile.displayName
            defaultAspectRatio = userProfile.defaultAspectRatio
            defaultTone = userProfile.defaultTone
            defaultCaptionStyle = userProfile.defaultCaptionStyle
            defaultDurationSec = userProfile.defaultDurationSec
        }
    }

    val avgScore = if (clips.isNotEmpty()) clips.map { it.viralScore }.average().toInt() else 0
    val topScore = clips.maxOfOrNull { it.viralScore } ?: 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.profile_title),
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )

        // Creator Identity Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = StudioSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!userPhotoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = userPhotoUrl,
                        contentDescription = stringResource(R.string.cd_creator_avatar),
                        placeholder = painterResource(id = R.drawable.img_creator_avatar),
                        error = painterResource(id = R.drawable.img_creator_avatar),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_creator_avatar),
                        contentDescription = stringResource(R.string.cd_creator_avatar),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName.ifBlank { "Criador Webb" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    if (userEmail.isNotBlank()) {
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = EmeraldContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "Sincronizado com Firebase",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatTile(
                value = clips.size.toString(),
                label = stringResource(R.string.profile_stats_clips),
                accent = IndigoPrimary,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = if (clips.isNotEmpty()) "$avgScore" else "--",
                label = stringResource(R.string.profile_stats_avg_score),
                accent = EmeraldAccent,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = if (clips.isNotEmpty()) "$topScore" else "--",
                label = stringResource(R.string.profile_stats_top_score),
                accent = EmeraldAccent,
                modifier = Modifier.weight(1f)
            )
        }

        // Default Preferences Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = StudioSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.profile_defaults_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Nome do Canal / Criador") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Formato padrão", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("9:16", "1:1", "16:9").forEach { ratio ->
                        FilterChip(
                            selected = defaultAspectRatio == ratio,
                            onClick = { defaultAspectRatio = ratio },
                            label = { Text(ratio) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Text("Duração padrão", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30L, 45L, 60L).forEach { sec ->
                        FilterChip(
                            selected = defaultDurationSec == sec,
                            onClick = { defaultDurationSec = sec },
                            label = { Text("${sec}s") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldContainer,
                                selectedLabelColor = EmeraldAccent
                            )
                        )
                    }
                }

                Text("Legenda padrão", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Karaokê", "Minimalista", "Alto Contraste").forEach { style ->
                        FilterChip(
                            selected = defaultCaptionStyle == style,
                            onClick = { defaultCaptionStyle = style },
                            label = { Text(style) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoPrimary.copy(alpha = 0.28f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Button(
                    onClick = {
                        onSaveDefaults(
                            displayName,
                            defaultAspectRatio,
                            defaultTone,
                            defaultCaptionStyle,
                            defaultDurationSec
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("profile_save_prefs_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.profile_save_prefs), color = Color.White)
                }
            }
        }

        OutlinedButton(
            onClick = onSignOut,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, RoseError.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("profile_sign_out_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Logout,
                contentDescription = null,
                tint = RoseError,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.profile_sign_out),
                color = RoseError,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = StudioSurfaceElevated,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
