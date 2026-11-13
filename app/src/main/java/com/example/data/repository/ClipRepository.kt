package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.ClipProject
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ClipRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Usuário precisa estar autenticado com o Google.")
    }

    fun observeClips(userId: String): Flow<List<ClipProject>> = flow {
        val path = "users/$userId/clips"
        emitAll(
            db.collection("users").document(userId).collection("clips")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ClipProject::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveUserProfile(
        displayName: String,
        defaultAspectRatio: String,
        defaultTone: String,
        defaultCaptionStyle: String,
        defaultDurationSec: Long
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val existing = docRef.get().await()
            val safeName = displayName.trim().ifBlank { "Criador" }.take(80)
            val safeDuration = defaultDurationSec.coerceIn(15L, 90L)
            if (existing.exists()) {
                val updates = mapOf(
                    "displayName" to safeName,
                    "defaultAspectRatio" to defaultAspectRatio,
                    "defaultTone" to defaultTone,
                    "defaultCaptionStyle" to defaultCaptionStyle,
                    "defaultDurationSec" to safeDuration,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "defaultAspectRatio" to defaultAspectRatio,
                    "defaultTone" to defaultTone,
                    "defaultCaptionStyle" to defaultCaptionStyle,
                    "defaultDurationSec" to safeDuration,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveGeneratedClips(clips: List<ClipProject>): Result<List<ClipProject>> {
        val uid = requireUserId()
        val clipsCollection = db.collection("users").document(uid).collection("clips")
        return try {
            val batch = db.batch()
            val savedModels = mutableListOf<ClipProject>()
            for (clip in clips) {
                val docRef = clipsCollection.document()
                val safeEnd = maxOf(clip.endTimeSec, clip.startTimeSec + 5L).coerceAtMost(86400L)
                val safeStart = clip.startTimeSec.coerceIn(0L, safeEnd - 1L)
                val safeTranscript = clip.transcript.take(30).map { it.toMap() }
                val safeHashtags = clip.hashtags.take(10)
                val payload = mapOf(
                    "userId" to uid,
                    "sourceType" to clip.sourceType,
                    "sourceValue" to clip.sourceValue.take(500).ifBlank { "Vídeo" },
                    "title" to clip.title.take(140).ifBlank { "Corte Viral" },
                    "hook" to clip.hook.take(240).ifBlank { "Assista até o final!" },
                    "viralReason" to clip.viralReason.take(500),
                    "viralScore" to clip.viralScore.coerceIn(0L, 100L),
                    "startTimeSec" to safeStart,
                    "endTimeSec" to safeEnd,
                    "aspectRatio" to clip.aspectRatio,
                    "tone" to clip.tone,
                    "captionStyle" to clip.captionStyle,
                    "captionPosition" to clip.captionPosition,
                    "themeIndex" to clip.themeIndex.coerceIn(0L, 2L),
                    "transcript" to safeTranscript,
                    "hashtags" to safeHashtags,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, payload)
                savedModels.add(
                    clip.copy(
                        id = docRef.id,
                        userId = uid,
                        startTimeSec = safeStart,
                        endTimeSec = safeEnd
                    )
                )
            }
            batch.commit().await()
            Result.success(savedModels)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, clipsCollection.path)
            Result.failure(e)
        }
    }

    suspend fun getClip(clipId: String): Result<ClipProject?> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("clips").document(clipId)
        return try {
            val snap = docRef.get().await()
            val clip = snap.toObject(
                ClipProject::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(clip)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateClip(clip: ClipProject): Result<Unit> {
        val uid = requireUserId()
        require(clip.id.isNotBlank()) { "ID do corte inválido para atualização." }
        val docRef = db.collection("users").document(uid).collection("clips").document(clip.id)
        val safeEnd = maxOf(clip.endTimeSec, clip.startTimeSec + 5L).coerceAtMost(86400L)
        val safeStart = clip.startTimeSec.coerceIn(0L, safeEnd - 1L)
        val updates = mapOf(
            "title" to clip.title.take(140).ifBlank { "Corte Viral" },
            "hook" to clip.hook.take(240).ifBlank { "Assista até o final!" },
            "startTimeSec" to safeStart,
            "endTimeSec" to safeEnd,
            "aspectRatio" to clip.aspectRatio,
            "captionStyle" to clip.captionStyle,
            "captionPosition" to clip.captionPosition,
            "transcript" to clip.transcript.take(30).map { it.toMap() },
            "hashtags" to clip.hashtags.take(10),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteClip(clipId: String): Result<Unit> {
        val uid = requireUserId()
        require(clipId.isNotBlank()) { "ID do corte inválido para exclusão." }
        val docRef = db.collection("users").document(uid).collection("clips").document(clipId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
