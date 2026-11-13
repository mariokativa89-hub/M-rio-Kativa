package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.ai.VideoClipAiService
import com.example.data.model.CreateFlowStage
import com.example.data.model.TabId
import com.example.data.repository.ClipRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CreateScreen
import com.example.ui.screens.MiniEditorModal
import com.example.ui.screens.ProcessingScreen
import com.example.ui.screens.ProfileTab
import com.example.ui.screens.ProjectsTab
import com.example.ui.screens.ResultsGallery
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EncurtadorWebbTheme
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ClipStudioViewModel
import com.example.ui.viewmodel.ProjectsUiState
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase ops
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            EncurtadorWebbTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { /* AuthStateListener updates currentUser automatically */ })
    } else {
        key(user.uid) {
            EncurtadorStudioHost(user = user)
        }
    }
}

@Composable
private fun EncurtadorStudioHost(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: ClipStudioViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ClipStudioViewModel(
                    repository = ClipRepository(db),
                    aiService = VideoClipAiService(),
                    currentUserId = user.uid
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val projectsState by viewModel.projectsState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val allClips = (projectsState as? ProjectsUiState.Success)?.clips.orEmpty()

    LaunchedEffect(uiState.toastMessage) {
        val msg = uiState.toastMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    // Handle back navigation across secondary tabs and sub-stages
    BackHandler(
        enabled = uiState.activeTab != TabId.CREATE || uiState.flowStage != CreateFlowStage.FORM
    ) {
        when {
            uiState.activeTab != TabId.CREATE -> viewModel.selectTab(TabId.CREATE)
            uiState.flowStage != CreateFlowStage.FORM -> viewModel.resetCreateFlow()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(StudioBackground)) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = StudioBackground,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = { StudioTopHeader() },
            bottomBar = {
                if (!useRail) {
                    StudioBottomBar(
                        activeTab = uiState.activeTab,
                        projectsCount = allClips.size,
                        onSelectTab = { viewModel.selectTab(it) }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    StudioNavigationRail(
                        activeTab = uiState.activeTab,
                        projectsCount = allClips.size,
                        onSelectTab = { viewModel.selectTab(it) }
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 680.dp)
                    ) {
                        when (uiState.activeTab) {
                            TabId.CREATE -> {
                                when (uiState.flowStage) {
                                    CreateFlowStage.FORM -> CreateScreen(
                                        userProfile = userProfile,
                                        onGenerate = { prefs -> viewModel.generateViralClips(prefs) }
                                    )
                                    CreateFlowStage.PROCESSING -> ProcessingScreen(
                                        currentStepIndex = uiState.processingStepIndex,
                                        progressPercent = uiState.processingProgress,
                                        sourceLabel = uiState.sourceLabel,
                                        errorMessage = uiState.processingError,
                                        onRetry = { viewModel.retryLastGeneration() },
                                        onBack = { viewModel.resetCreateFlow() }
                                    )
                                    CreateFlowStage.RESULTS -> ResultsGallery(
                                        clips = uiState.currentResults,
                                        onReset = { viewModel.resetCreateFlow() },
                                        onEditClip = { viewModel.openEditor(it) },
                                        onUpdateTranscript = { clipId, segId, newText ->
                                            viewModel.updateTranscriptSegment(clipId, segId, newText)
                                        },
                                        onNotify = { viewModel.showToast(it) }
                                    )
                                }
                            }
                            TabId.PROJECTS -> ProjectsTab(
                                projectsState = projectsState,
                                onOpenClip = { viewModel.openEditor(it) },
                                onDeleteClip = { viewModel.deleteClip(it) },
                                onGoCreate = {
                                    viewModel.selectTab(TabId.CREATE)
                                    viewModel.resetCreateFlow()
                                }
                            )
                            TabId.PROFILE -> ProfileTab(
                                userDisplayName = user.displayName ?: "Criador Webb",
                                userEmail = user.email.orEmpty(),
                                userPhotoUrl = user.photoUrl?.toString(),
                                clips = allClips,
                                userProfile = userProfile,
                                onSaveDefaults = { name, ratio, tone, captionStyle, duration ->
                                    viewModel.saveProfileDefaults(name, ratio, tone, captionStyle, duration)
                                },
                                onSignOut = {
                                    signOutUser(
                                        credentialManager = credentialManager,
                                        scope = scope
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        val editingClip = uiState.editingClip
        if (editingClip != null) {
            MiniEditorModal(
                clip = editingClip,
                isSaving = uiState.isSavingEditor,
                onClose = { viewModel.closeEditor() },
                onSave = { updated -> viewModel.saveEditorChanges(updated) },
                onDelete = { clipId -> viewModel.deleteClip(clipId) }
            )
        }
    }
}

@Composable
private fun StudioTopHeader() {
    Surface(
        color = StudioBackground.copy(alpha = 0.96f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(IndigoPrimary, EmeraldAccent))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCut,
                            contentDescription = stringResource(R.string.cd_brand_logo),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = buildAnnotatedString {
                            append("Encurtador ")
                            withStyle(SpanStyle(color = IndigoPrimary)) {
                                append("Webb")
                            }
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                Surface(
                    color = StudioSurface,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = stringResource(R.string.badge_ai_studio),
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun StudioBottomBar(
    activeTab: TabId,
    projectsCount: Int,
    onSelectTab: (TabId) -> Unit
) {
    NavigationBar(
        containerColor = StudioSurface,
        windowInsets = WindowInsets.navigationBars,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = activeTab == TabId.CREATE,
            onClick = { onSelectTab(TabId.CREATE) },
            icon = {
                Icon(
                    imageVector = if (activeTab == TabId.CREATE) Icons.Rounded.AutoAwesome else Icons.Outlined.AutoAwesome,
                    contentDescription = stringResource(R.string.nav_create)
                )
            },
            label = { Text(stringResource(R.string.nav_create)) },
            modifier = Modifier.testTag("nav_tab_create"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                indicatorColor = IndigoPrimary,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )

        NavigationBarItem(
            selected = activeTab == TabId.PROJECTS,
            onClick = { onSelectTab(TabId.PROJECTS) },
            icon = {
                BadgedBox(
                    badge = {
                        if (projectsCount > 0) {
                            Badge(containerColor = EmeraldAccent, contentColor = StudioBackground) {
                                Text(projectsCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (activeTab == TabId.PROJECTS) Icons.Rounded.Folder else Icons.Outlined.Folder,
                        contentDescription = stringResource(R.string.nav_projects)
                    )
                }
            },
            label = { Text(stringResource(R.string.nav_projects)) },
            modifier = Modifier.testTag("nav_tab_projects"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                indicatorColor = IndigoPrimary,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )

        NavigationBarItem(
            selected = activeTab == TabId.PROFILE,
            onClick = { onSelectTab(TabId.PROFILE) },
            icon = {
                Icon(
                    imageVector = if (activeTab == TabId.PROFILE) Icons.Rounded.Person else Icons.Outlined.Person,
                    contentDescription = stringResource(R.string.nav_profile)
                )
            },
            label = { Text(stringResource(R.string.nav_profile)) },
            modifier = Modifier.testTag("nav_tab_profile"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                indicatorColor = IndigoPrimary,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
    }
}

@Composable
private fun StudioNavigationRail(
    activeTab: TabId,
    projectsCount: Int,
    onSelectTab: (TabId) -> Unit
) {
    NavigationRail(
        containerColor = StudioSurface
    ) {
        Spacer(Modifier.height(12.dp))
        NavigationRailItem(
            selected = activeTab == TabId.CREATE,
            onClick = { onSelectTab(TabId.CREATE) },
            icon = {
                Icon(
                    imageVector = if (activeTab == TabId.CREATE) Icons.Rounded.AutoAwesome else Icons.Outlined.AutoAwesome,
                    contentDescription = stringResource(R.string.nav_create)
                )
            },
            label = { Text(stringResource(R.string.nav_create)) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                indicatorColor = IndigoPrimary,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
        NavigationRailItem(
            selected = activeTab == TabId.PROJECTS,
            onClick = { onSelectTab(TabId.PROJECTS) },
            icon = {
                BadgedBox(
                    badge = {
                        if (projectsCount > 0) {
                            Badge(containerColor = EmeraldAccent, contentColor = StudioBackground) {
                                Text(projectsCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (activeTab == TabId.PROJECTS) Icons.Rounded.Folder else Icons.Outlined.Folder,
                        contentDescription = stringResource(R.string.nav_projects)
                    )
                }
            },
            label = { Text(stringResource(R.string.nav_projects)) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                indicatorColor = IndigoPrimary,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
        NavigationRailItem(
            selected = activeTab == TabId.PROFILE,
            onClick = { onSelectTab(TabId.PROFILE) },
            icon = {
                Icon(
                    imageVector = if (activeTab == TabId.PROFILE) Icons.Rounded.Person else Icons.Outlined.Person,
                    contentDescription = stringResource(R.string.nav_profile)
                )
            },
            label = { Text(stringResource(R.string.nav_profile)) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                indicatorColor = IndigoPrimary,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
    }
}
