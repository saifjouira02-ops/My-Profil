package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.UserPreferences
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SocialRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserInitialized_and_createPost_succeedsForAuthenticatedOwner() = runBlocking {
        val aliceUid = signInTestUser("alice_${UUID.randomUUID()}@test.com")
        val repo = SocialRepository(firestore, auth)

        val profile = withTimeout(TIMEOUT_MS) {
            repo.ensureUserInitialized(
                uid = aliceUid,
                displayName = "Alice",
                email = "alice@test.com",
                initialPrefs = UserPreferences(),
            ).getOrThrow()
        }

        val postId = withTimeout(TIMEOUT_MS) {
            repo.createPost(
                author = profile,
                content = "Hello from Robolectric rule test!",
                visibility = "public",
                imageDataUrl = null,
            ).getOrThrow()
        }
        assertTrue(postId.isNotBlank())

        val posts = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePublicFeed().first { list -> list.any { it.id == postId } }
        }
        assertTrue(posts.any { it.id == postId && it.authorId == aliceUid })
    }

    @Test
    fun toggleLike_and_addComment_succeedsAcrossUsers() = runBlocking {
        val aliceUid = signInTestUser("alice2_${UUID.randomUUID()}@test.com")
        val repo = SocialRepository(firestore, auth)
        val aliceProfile = withTimeout(TIMEOUT_MS) {
            repo.ensureUserInitialized(aliceUid, "Alice Two", null, UserPreferences()).getOrThrow()
        }
        val postId = withTimeout(TIMEOUT_MS) {
            repo.createPost(aliceProfile, "Public post for interaction", "public", null).getOrThrow()
        }
        val post = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePublicFeed().first { list -> list.any { it.id == postId } }.first { it.id == postId }
        }

        val bobUid = signInTestUser("bob2_${UUID.randomUUID()}@test.com")
        val bobProfile = withTimeout(TIMEOUT_MS) {
            repo.ensureUserInitialized(bobUid, "Bob Two", null, UserPreferences()).getOrThrow()
        }
        withTimeout(TIMEOUT_MS) {
            repo.toggleLike(post, currentlyLiked = false, actorProfile = bobProfile).getOrThrow()
            repo.addComment(post, bobProfile, "Nice post!").getOrThrow()
        }

        val updated = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePublicFeed().first { list ->
                list.any { it.id == postId && it.likesCount == 1L && it.commentsCount == 1L }
            }
        }
        assertTrue(updated.any { it.id == postId })
    }

    @Test
    fun crossUser_privatePostAndDelete_rejectedWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice3_${UUID.randomUUID()}@test.com")
        val repo = SocialRepository(firestore, auth)
        val aliceProfile = withTimeout(TIMEOUT_MS) {
            repo.ensureUserInitialized(aliceUid, "Alice Three", null, UserPreferences()).getOrThrow()
        }
        val privatePostId = withTimeout(TIMEOUT_MS) {
            repo.createPost(aliceProfile, "Secret note", "private", null).getOrThrow()
        }

        signInTestUser("bob3_${UUID.randomUUID()}@test.com")
        val deleteResult = withTimeout(TIMEOUT_MS) { repo.deletePost(privatePostId) }
        assertTrue(deleteResult.isFailure)
        val fsErr = deleteResult.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr?.code)
    }

    @Test
    fun observePublicFeed_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = SocialRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observePublicFeed().first()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val fsErr = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr?.code)
        }
    }

    private companion object {
        const val TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
