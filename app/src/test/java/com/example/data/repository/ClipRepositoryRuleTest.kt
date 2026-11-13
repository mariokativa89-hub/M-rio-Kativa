package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ClipProject
import com.example.data.model.TranscriptSegment
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ClipRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun sampleClip(uid: String) = ClipProject(
        userId = uid,
        sourceType = "topic",
        sourceValue = "Como criar cortes virais no celular",
        title = "O gancho dos 3 primeiros segundos",
        hook = "Se você perde visualizações no início, veja isso!",
        viralReason = "Ataque direto a uma dor comum de criadores.",
        viralScore = 94L,
        startTimeSec = 10L,
        endTimeSec = 55L,
        aspectRatio = "9:16",
        tone = "Hook Viral",
        captionStyle = "Karaokê",
        captionPosition = "Base",
        themeIndex = 0L,
        transcript = listOf(
            TranscriptSegment("seg_1", 10L, 25L, "Se você perde visualizações no início, veja isso!"),
            TranscriptSegment("seg_2", 25L, 55L, "Troque sua abertura por uma pergunta direta.")
        ),
        hashtags = listOf("#cortes", "#reels", "#criadores")
    )

    @Test
    fun saveGeneratedClips_authenticatedOwner_persistsAndObservesClips() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = ClipRepository(firestore)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveGeneratedClips(listOf(sampleClip(uid)))
        }
        assertTrue("Expected saveGeneratedClips to succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)
        val savedClip = saveResult.getOrThrow().first()
        assertTrue(savedClip.id.isNotBlank())

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeClips(uid).first { list -> list.any { it.id == savedClip.id } }
        }
        assertTrue(observed.any { it.id == savedClip.id && it.title == savedClip.title })
    }

    @Test
    fun updateAndDeleteClip_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = ClipRepository(firestore)

        val saved = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveGeneratedClips(listOf(sampleClip(uid))).getOrThrow().first()
        }

        val updated = saved.copy(title = "Título Atualizado no Editor", captionPosition = "Centro")
        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateClip(updated)
        }
        assertTrue("Expected updateClip to succeed: ${updateResult.exceptionOrNull()}", updateResult.isSuccess)

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getClip(saved.id).getOrThrow()
        }
        assertNotNull(fetched)
        assertEquals("Título Atualizado no Editor", fetched?.title)
        assertEquals("Centro", fetched?.captionPosition)

        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteClip(saved.id)
        }
        assertTrue(deleteResult.isSuccess)
    }

    @Test
    fun getClip_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = ClipRepository(firestore)
        val saved = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveGeneratedClips(listOf(sampleClip(aliceUid))).getOrThrow().first()
        }

        signInTestUser(BOB_EMAIL)
        // Bob tries to read Alice's clip document directly
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("clips").document(saved.id)
                    .get()
                    .await()
            }
            fail("Expected PERMISSION_DENIED when Bob reads Alice's clip")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun observeClips_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = ClipRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeClips("alice_unauth_check").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated observeClips")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun saveUserProfile_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = ClipRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserProfile(
                displayName = "Alice Studio",
                defaultAspectRatio = "9:16",
                defaultTone = "Hook Viral",
                defaultCaptionStyle = "Karaokê",
                defaultDurationSec = 45L
            )
        }
        assertTrue("Expected saveUserProfile to succeed: ${result.exceptionOrNull()}", result.isSuccess)

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProfile(uid).first { it != null }
        }
        assertEquals("Alice Studio", profile?.displayName)
        assertEquals(45L, profile?.defaultDurationSec)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@encurtador.test"
        const val BOB_EMAIL = "bob@encurtador.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
