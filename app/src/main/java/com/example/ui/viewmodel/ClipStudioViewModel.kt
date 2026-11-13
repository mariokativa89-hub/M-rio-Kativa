package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.VideoClipAiService
import com.example.data.model.ClipProject
import com.example.data.model.CreateFlowStage
import com.example.data.model.GenerationPreferences
import com.example.data.model.TabId
import com.example.data.model.UserProfile
import com.example.data.repository.ClipRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ProjectsUiState {
    data object Loading : ProjectsUiState
    data class Success(val clips: List<ClipProject>) : ProjectsUiState
    data class Error(val message: String) : ProjectsUiState
}

data class StudioUiState(
    val activeTab: TabId = TabId.CREATE,
    val flowStage: CreateFlowStage = CreateFlowStage.FORM,
    val processingStepIndex: Int = 0,
    val processingProgress: Int = 0,
    val sourceLabel: String = "",
    val processingError: String? = null,
    val currentResults: List<ClipProject> = emptyList(),
    val editingClip: ClipProject? = null,
    val toastMessage: String? = null,
    val isSavingEditor: Boolean = false
)

class ClipStudioViewModel(
    private val repository: ClipRepository,
    private val aiService: VideoClipAiService,
    private val currentUserId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    val projectsState: StateFlow<ProjectsUiState> = repository.observeClips(currentUserId)
        .map<List<ClipProject>, ProjectsUiState> { ProjectsUiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing clips", rootError)
            emit(ProjectsUiState.Error(rootError.localizedMessage ?: "Erro ao carregar projetos."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ProjectsUiState.Loading
        )

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing profile", firestoreError ?: error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    private var lastPreferences: GenerationPreferences? = null

    fun selectTab(tab: TabId) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
    }

    fun showToast(message: String) {
        _uiState.value = _uiState.value.copy(toastMessage = message)
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    fun resetCreateFlow() {
        _uiState.value = _uiState.value.copy(
            flowStage = CreateFlowStage.FORM,
            processingStepIndex = 0,
            processingProgress = 0,
            processingError = null
        )
    }

    fun openEditor(clip: ClipProject) {
        _uiState.value = _uiState.value.copy(editingClip = clip)
    }

    fun closeEditor() {
        _uiState.value = _uiState.value.copy(editingClip = null, isSavingEditor = false)
    }

    fun retryLastGeneration() {
        val prefs = lastPreferences ?: return
        generateViralClips(prefs)
    }

    fun generateViralClips(prefs: GenerationPreferences) {
        lastPreferences = prefs
        _uiState.value = _uiState.value.copy(
            activeTab = TabId.CREATE,
            flowStage = CreateFlowStage.PROCESSING,
            processingStepIndex = 0,
            processingProgress = 12,
            sourceLabel = prefs.sourceValue,
            processingError = null
        )

        viewModelScope.launch {
            val progressJob = launch {
                val steps = listOf(18 to 0, 42 to 1, 68 to 2, 88 to 3)
                for ((pct, idx) in steps) {
                    delay(1100L)
                    if (_uiState.value.flowStage != CreateFlowStage.PROCESSING ||
                        _uiState.value.processingError != null
                    ) break
                    _uiState.value = _uiState.value.copy(
                        processingStepIndex = idx,
                        processingProgress = pct
                    )
                }
            }

            try {
                val aiClips = aiService.generateViralClips(currentUserId, prefs)
                progressJob.cancel()

                _uiState.value = _uiState.value.copy(
                    processingStepIndex = 3,
                    processingProgress = 96
                )

                val saveResult = repository.saveGeneratedClips(aiClips)
                if (saveResult.isSuccess) {
                    val savedClips = saveResult.getOrThrow()
                    _uiState.value = _uiState.value.copy(
                        flowStage = CreateFlowStage.RESULTS,
                        processingStepIndex = 3,
                        processingProgress = 100,
                        currentResults = savedClips,
                        toastMessage = "${savedClips.size} cortes virais prontos para publicar!"
                    )
                } else {
                    val err = saveResult.exceptionOrNull()
                    _uiState.value = _uiState.value.copy(
                        processingError = err?.localizedMessage
                            ?: "Cortes gerados, mas falhou ao salvar na nuvem."
                    )
                }
            } catch (e: CancellationException) {
                progressJob.cancel()
                throw e
            } catch (e: Exception) {
                progressJob.cancel()
                Log.e(TAG, "Generation pipeline failed", e)
                _uiState.value = _uiState.value.copy(
                    processingError = e.localizedMessage ?: "Falha inesperada ao gerar cortes com IA."
                )
            }
        }
    }

    fun updateTranscriptSegment(clipId: String, segmentId: String, newText: String) {
        val trimmed = newText.trim()
        if (trimmed.isEmpty()) return

        val currentClip = _uiState.value.currentResults.find { it.id == clipId }
            ?: (projectsState.value as? ProjectsUiState.Success)?.clips?.find { it.id == clipId }
            ?: return

        val updatedClip = currentClip.copy(
            transcript = currentClip.transcript.map { seg ->
                if (seg.id == segmentId) seg.copy(text = trimmed) else seg
            }
        )

        _uiState.value = _uiState.value.copy(
            currentResults = _uiState.value.currentResults.map {
                if (it.id == clipId) updatedClip else it
            }
        )

        viewModelScope.launch {
            val result = repository.updateClip(updatedClip)
            if (result.isSuccess) {
                showToast("Legenda atualizada.")
            } else {
                showToast("Erro ao salvar legenda: ${result.exceptionOrNull()?.localizedMessage ?: ""}")
            }
        }
    }

    fun saveEditorChanges(updatedClip: ClipProject) {
        _uiState.value = _uiState.value.copy(isSavingEditor = true)
        viewModelScope.launch {
            val result = repository.updateClip(updatedClip)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    currentResults = _uiState.value.currentResults.map {
                        if (it.id == updatedClip.id) updatedClip else it
                    },
                    editingClip = null,
                    isSavingEditor = false,
                    toastMessage = "Ajustes do corte salvos na nuvem."
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isSavingEditor = false,
                    toastMessage = "Não foi possível salvar: ${result.exceptionOrNull()?.localizedMessage ?: "erro"}"
                )
            }
        }
    }

    fun deleteClip(clipId: String) {
        viewModelScope.launch {
            val result = repository.deleteClip(clipId)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    currentResults = _uiState.value.currentResults.filterNot { it.id == clipId },
                    editingClip = if (_uiState.value.editingClip?.id == clipId) null else _uiState.value.editingClip,
                    toastMessage = "Corte removido."
                )
            } else {
                showToast("Erro ao excluir: ${result.exceptionOrNull()?.localizedMessage ?: ""}")
            }
        }
    }

    fun saveProfileDefaults(
        displayName: String,
        aspectRatio: String,
        tone: String,
        captionStyle: String,
        durationSec: Long
    ) {
        viewModelScope.launch {
            val result = repository.saveUserProfile(
                displayName = displayName,
                defaultAspectRatio = aspectRatio,
                defaultTone = tone,
                defaultCaptionStyle = captionStyle,
                defaultDurationSec = durationSec
            )
            if (result.isSuccess) {
                showToast("Preferências padrão salvas!")
            } else {
                showToast("Erro ao salvar perfil: ${result.exceptionOrNull()?.localizedMessage ?: ""}")
            }
        }
    }

    private companion object {
        const val TAG = "ClipStudioVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
