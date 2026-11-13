package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.unwrapFirestoreException
import com.example.model.AppNotification
import com.example.model.BlockedUser
import com.example.model.Comment
import com.example.model.FollowRelation
import com.example.model.Post
import com.example.model.PrivateAccount
import com.example.model.PublicProfile
import com.example.model.UserPreferences
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SocialRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth,
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- Profile & Account ---

    suspend fun ensureUserInitialized(
        uid: String,
        displayName: String?,
        email: String?,
        initialPrefs: UserPreferences,
    ): Result<PublicProfile> = runCatching {
        requireUserId()
        val profileRef = db.collection("public_profiles").document(uid)
        val privateRef = db.collection("users").document(uid).collection("private").document("account")

        val profileSnap = profileRef.get().await()
        val profile = if (profileSnap.exists()) {
            profileSnap.toObject(
                PublicProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: buildDefaultProfile(uid, displayName)
        } else {
            val safeName = sanitizeDisplayName(displayName)
            val safeHandle = buildHandle(safeName, uid)
            val seed = abs(uid.hashCode()) % 6
            val payload = mapOf(
                "userId" to uid,
                "displayName" to safeName,
                "handle" to safeHandle,
                "bio" to "",
                "avatarSeed" to seed,
                "isPrivate" to false,
                "followersCount" to 0L,
                "followingCount" to 0L,
                "postsCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            profileRef.set(payload).await()
            PublicProfile(
                id = uid,
                userId = uid,
                displayName = safeName,
                handle = safeHandle,
                bio = "",
                avatarSeed = seed,
                isPrivate = false,
            )
        }

        val privateSnap = privateRef.get().await()
        if (!privateSnap.exists()) {
            val privatePayload = mutableMapOf<String, Any>(
                "userId" to uid,
                "language" to initialPrefs.language,
                "darkMode" to initialPrefs.darkMode,
                "largeText" to initialPrefs.largeText,
                "highContrast" to initialPrefs.highContrast,
                "showDemoContent" to initialPrefs.showDemoContent,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (!email.isNullOrBlank()) {
                privatePayload["email"] = email.take(120)
            }
            privateRef.set(privatePayload).await()
        }
        profile
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "public_profiles/$uid")
    }

    fun observeProfile(userId: String): Flow<PublicProfile?> = flow {
        val path = "public_profiles/$userId"
        emitAll(
            db.collection("public_profiles").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(
                        PublicProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsErr = error.unwrapFirestoreException()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observePublicProfiles(): Flow<List<PublicProfile>> = flow {
        val path = "public_profiles"
        emitAll(
            db.collection("public_profiles")
                .whereEqualTo("isPrivate", false)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PublicProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.followersCount }
                }
                .catch { error ->
                    val fsErr = error.unwrapFirestoreException()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePrivateAccount(userId: String): Flow<PrivateAccount?> = flow {
        val path = "users/$userId/private/account"
        emitAll(
            db.collection("users").document(userId).collection("private").document("account")
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) null
                    else snap.toObject(
                        PrivateAccount::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsErr = error.unwrapFirestoreException()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun updateProfile(
        displayName: String,
        handle: String,
        bio: String,
        isPrivate: Boolean,
        avatarSeed: Int,
        avatarDataUrl: String?,
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val trimmedName = displayName.trim()
        val trimmedHandle = handle.trim().lowercase()
        val trimmedBio = bio.trim()
        require(trimmedName.length in 2..50) { "Display name must be 2..50 chars" }
        require(trimmedHandle.length in 2..30) { "Handle must be 2..30 chars" }
        require(trimmedBio.length <= 240) { "Bio max 240 chars" }

        val docRef = db.collection("public_profiles").document(uid)
        val payload = mutableMapOf<String, Any>(
            "displayName" to trimmedName,
            "handle" to trimmedHandle,
            "bio" to trimmedBio,
            "isPrivate" to isPrivate,
            "avatarSeed" to avatarSeed.coerceIn(0, 5),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (!avatarDataUrl.isNullOrBlank()) {
            require(avatarDataUrl.length <= 280_000) { "Avatar too large" }
            payload["avatarDataUrl"] = avatarDataUrl
        } else {
            payload["avatarDataUrl"] = FieldValue.delete()
        }
        docRef.update(payload).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "public_profiles/${auth.currentUser?.uid}")
    }

    suspend fun syncPreferences(prefs: UserPreferences): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("private").document("account")
        val snap = docRef.get().await()
        val payload = mapOf(
            "userId" to uid,
            "language" to prefs.language,
            "darkMode" to prefs.darkMode,
            "largeText" to prefs.largeText,
            "highContrast" to prefs.highContrast,
            "showDemoContent" to prefs.showDemoContent,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (snap.exists()) {
            docRef.update(
                mapOf(
                    "language" to prefs.language,
                    "darkMode" to prefs.darkMode,
                    "largeText" to prefs.largeText,
                    "highContrast" to prefs.highContrast,
                    "showDemoContent" to prefs.showDemoContent,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } else {
            docRef.set(payload).await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/private/account")
    }

    // --- Posts ---

    fun observePublicFeed(): Flow<List<Post>> = flow {
        val path = "posts"
        emitAll(
            db.collection("posts")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Post::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = error.unwrapFirestoreException()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeUserPosts(targetUserId: String, viewerId: String): Flow<List<Post>> = flow {
        val path = "posts"
        val query = if (targetUserId == viewerId) {
            db.collection("posts").whereEqualTo("authorId", targetUserId)
        } else {
            db.collection("posts")
                .whereEqualTo("authorId", targetUserId)
                .whereEqualTo("visibility", "public")
        }
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Post::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = error.unwrapFirestoreException()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createPost(
        author: PublicProfile,
        content: String,
        visibility: String,
        imageDataUrl: String?,
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val trimmed = content.trim()
        require(trimmed.isNotEmpty() && trimmed.length <= 1000) { "Post content invalid" }
        val safeVisibility = if (visibility == "private") "private" else "public"
        val docRef = db.collection("posts").document()
        val profileRef = db.collection("public_profiles").document(uid)

        val payload = mutableMapOf<String, Any>(
            "authorId" to uid,
            "authorName" to author.displayName.ifBlank { "Member" }.take(50),
            "authorHandle" to author.handle.ifBlank { "member" }.take(30),
            "authorAvatarSeed" to author.avatarSeed.coerceIn(0, 5),
            "content" to trimmed,
            "visibility" to safeVisibility,
            "likesCount" to 0L,
            "commentsCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (!author.avatarDataUrl.isNullOrBlank()) {
            payload["authorAvatarDataUrl"] = author.avatarDataUrl
        }
        if (!imageDataUrl.isNullOrBlank()) {
            require(imageDataUrl.length <= 350_000) { "Image payload exceeds safe size" }
            payload["imageDataUrl"] = imageDataUrl
        }

        val profileSnap = profileRef.get().await()
        val batch = db.batch()
        batch.set(docRef, payload)
        if (profileSnap.exists()) {
            batch.update(profileRef, "postsCount", FieldValue.increment(1L))
        }
        batch.commit().await()
        docRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "posts")
    }

    suspend fun updatePost(
        postId: String,
        content: String,
        visibility: String,
        imageDataUrl: String?,
    ): Result<Unit> = runCatching {
        requireUserId()
        val trimmed = content.trim()
        require(postId.isNotBlank()) { "Post ID blank" }
        require(trimmed.isNotEmpty() && trimmed.length <= 1000) { "Post content invalid" }
        val safeVisibility = if (visibility == "private") "private" else "public"
        val docRef = db.collection("posts").document(postId)
        val payload = mutableMapOf<String, Any>(
            "content" to trimmed,
            "visibility" to safeVisibility,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (!imageDataUrl.isNullOrBlank()) {
            require(imageDataUrl.length <= 350_000) { "Image payload exceeds safe size" }
            payload["imageDataUrl"] = imageDataUrl
        } else {
            payload["imageDataUrl"] = FieldValue.delete()
        }
        docRef.update(payload).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "posts/$postId")
    }

    suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(postId.isNotBlank()) { "Post ID blank" }
        val docRef = db.collection("posts").document(postId)
        val profileRef = db.collection("public_profiles").document(uid)
        val profileSnap = profileRef.get().await()
        val currentPostsCount = profileSnap.getLong("postsCount") ?: 0L

        val batch = db.batch()
        batch.delete(docRef)
        if (profileSnap.exists() && currentPostsCount > 0L) {
            batch.update(profileRef, "postsCount", FieldValue.increment(-1L))
        }
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "posts/$postId")
    }

    // --- Likes ---

    fun observeLikedPostIds(userId: String): Flow<Set<String>> = flow {
        val path = "users/$userId/liked_posts"
        emitAll(
            db.collection("users").document(userId).collection("liked_posts")
                .snapshots()
                .map { snapshot -> snapshot.documents.map { it.id }.toSet() }
                .catch { error ->
                    val fsErr = error.unwrapFirestoreException()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun toggleLike(
        post: Post,
        currentlyLiked: Boolean,
        actorProfile: PublicProfile,
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val postRef = db.collection("posts").document(post.id)
        val likeRef = postRef.collection("likes").document(uid)
        val userLikedRef = db.collection("users").document(uid).collection("liked_posts").document(post.id)

        val batch = db.batch()
        if (currentlyLiked) {
            batch.delete(likeRef)
            batch.delete(userLikedRef)
            batch.update(postRef, "likesCount", FieldValue.increment(-1L))
        } else {
            batch.set(
                likeRef,
                mapOf(
                    "userId" to uid,
                    "postId" to post.id,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.set(
                userLikedRef,
                mapOf(
                    "postId" to post.id,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.update(postRef, "likesCount", FieldValue.increment(1L))
        }
        batch.commit().await()

        if (!currentlyLiked && post.authorId != uid) {
            sendNotificationSafe(
                recipientId = post.authorId,
                actor = actorProfile,
                type = "like",
                postId = post.id,
                previewText = post.content.take(100),
            )
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "posts/${post.id}/likes")
    }

    // --- Comments ---

    fun observeComments(postId: String, postVisibility: String, viewerId: String): Flow<List<Comment>> = flow {
        val path = "posts/$postId/comments"
        val query = if (postVisibility == "public") {
            db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("postVisibility", "public")
        } else {
            db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("authorId", viewerId)
        }
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Comment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = error.unwrapFirestoreException()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addComment(
        post: Post,
        author: PublicProfile,
        text: String,
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val trimmed = text.trim()
        require(trimmed.isNotEmpty() && trimmed.length <= 500) { "Comment invalid" }

        val postRef = db.collection("posts").document(post.id)
        val commentRef = postRef.collection("comments").document()
        val payload = mutableMapOf<String, Any>(
            "postId" to post.id,
            "postVisibility" to (if (post.visibility == "private") "private" else "public"),
            "authorId" to uid,
            "authorName" to author.displayName.ifBlank { "Member" }.take(50),
            "authorAvatarSeed" to author.avatarSeed.coerceIn(0, 5),
            "text" to trimmed,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (!author.avatarDataUrl.isNullOrBlank()) {
            payload["authorAvatarDataUrl"] = author.avatarDataUrl
        }

        val batch = db.batch()
        batch.set(commentRef, payload)
        batch.update(postRef, "commentsCount", FieldValue.increment(1L))
        batch.commit().await()

        if (post.authorId != uid) {
            sendNotificationSafe(
                recipientId = post.authorId,
                actor = author,
                type = "comment",
                postId = post.id,
                previewText = trimmed.take(100),
            )
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "posts/${post.id}/comments")
    }

    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> = runCatching {
        requireUserId()
        val postRef = db.collection("posts").document(postId)
        val commentRef = postRef.collection("comments").document(commentId)
        val postSnap = postRef.get().await()
        val currentCommentsCount = postSnap.getLong("commentsCount") ?: 0L

        val batch = db.batch()
        batch.delete(commentRef)
        if (postSnap.exists() && currentCommentsCount > 0L) {
            batch.update(postRef, "commentsCount", FieldValue.increment(-1L))
        }
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "posts/$postId/comments/$commentId")
    }

    // --- Follows ---

    fun observeFollowing(userId: String): Flow<List<FollowRelation>> = flow {
        val path = "follows"
        emitAll(
            db.collection("follows")
                .whereEqualTo("followerId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FollowRelation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsErr = error.unwrapFirestoreException()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeFollowers(userId: String, viewerId: String): Flow<List<FollowRelation>> = flow {
        val path = "follows"
        val query = if (userId == viewerId) {
            db.collection("follows").whereEqualTo("followingId", userId)
        } else {
            db.collection("follows")
                .whereEqualTo("followingId", userId)
                .whereEqualTo("isPublicTarget", true)
        }
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FollowRelation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsErr = error.unwrapFirestoreException()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun toggleFollow(
        follower: PublicProfile,
        target: PublicProfile,
        currentlyFollowing: Boolean,
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(target.userId != uid) { "Cannot follow yourself" }
        val followId = "${uid}_${target.userId}"
        val followRef = db.collection("follows").document(followId)
        val followerRef = db.collection("public_profiles").document(uid)
        val targetRef = db.collection("public_profiles").document(target.userId)

        val batch = db.batch()
        if (currentlyFollowing) {
            batch.delete(followRef)
            batch.update(followerRef, "followingCount", FieldValue.increment(-1L))
            batch.update(targetRef, "followersCount", FieldValue.increment(-1L))
        } else {
            batch.set(
                followRef,
                mapOf(
                    "followerId" to uid,
                    "followerName" to follower.displayName.ifBlank { "Member" }.take(50),
                    "followerHandle" to follower.handle.ifBlank { "member" }.take(30),
                    "followerAvatarSeed" to follower.avatarSeed.coerceIn(0, 5),
                    "followingId" to target.userId,
                    "followingName" to target.displayName.ifBlank { "Member" }.take(50),
                    "followingHandle" to target.handle.ifBlank { "member" }.take(30),
                    "followingAvatarSeed" to target.avatarSeed.coerceIn(0, 5),
                    "isPublicTarget" to !target.isPrivate,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.update(followerRef, "followingCount", FieldValue.increment(1L))
            batch.update(targetRef, "followersCount", FieldValue.increment(1L))
        }
        batch.commit().await()

        if (!currentlyFollowing) {
            sendNotificationSafe(
                recipientId = target.userId,
                actor = follower,
                type = "follow",
                postId = null,
                previewText = null,
            )
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "follows/${auth.currentUser?.uid}_${target.userId}")
    }

    // --- Notifications ---

    fun observeNotifications(userId: String): Flow<List<AppNotification>> = flow {
        val path = "users/$userId/notifications"
        emitAll(
            db.collection("users").document(userId).collection("notifications")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AppNotification::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = error.unwrapFirestoreException()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        db.collection("users").document(uid).collection("notifications")
            .document(notificationId)
            .update("isRead", true)
            .await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/notifications/$notificationId")
    }

    suspend fun markAllNotificationsRead(unreadIds: List<String>): Result<Unit> = runCatching {
        val uid = requireUserId()
        if (unreadIds.isEmpty()) return@runCatching
        val batch = db.batch()
        val col = db.collection("users").document(uid).collection("notifications")
        unreadIds.forEach { id ->
            batch.update(col.document(id), "isRead", true)
        }
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/notifications")
    }

    private suspend fun sendNotificationSafe(
        recipientId: String,
        actor: PublicProfile,
        type: String,
        postId: String?,
        previewText: String?,
    ) {
        try {
            val uid = auth.currentUser?.uid ?: return
            if (recipientId == uid || recipientId.startsWith("demo_")) return
            val docRef = db.collection("users").document(recipientId).collection("notifications").document()
            val payload = mutableMapOf<String, Any>(
                "recipientId" to recipientId,
                "actorId" to uid,
                "actorName" to actor.displayName.ifBlank { "Member" }.take(50),
                "actorAvatarSeed" to actor.avatarSeed.coerceIn(0, 5),
                "type" to type,
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp(),
            )
            if (!postId.isNullOrBlank()) payload["postId"] = postId
            if (!previewText.isNullOrBlank()) payload["previewText"] = previewText.take(140)
            docRef.set(payload).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.CREATE, "users/$recipientId/notifications")
        }
    }

    // --- Safety: Block & Report ---

    fun observeBlockedUsers(userId: String): Flow<List<BlockedUser>> = flow {
        val path = "users/$userId/blocked"
        emitAll(
            db.collection("users").document(userId).collection("blocked")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        BlockedUser::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsErr = error.unwrapFirestoreException()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun blockUser(
        blockedUserId: String,
        displayName: String,
        handle: String,
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(blockedUserId != uid) { "Cannot block yourself" }
        val ref = db.collection("users").document(uid).collection("blocked").document(blockedUserId)
        ref.set(
            mapOf(
                "blockedUserId" to blockedUserId,
                "blockedDisplayName" to displayName.ifBlank { "User" }.take(50),
                "blockedHandle" to handle.ifBlank { "user" }.take(30),
                "createdAt" to FieldValue.serverTimestamp(),
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "users/${auth.currentUser?.uid}/blocked/$blockedUserId")
    }

    suspend fun unblockUser(blockedUserId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        db.collection("users").document(uid).collection("blocked").document(blockedUserId)
            .delete()
            .await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "users/${auth.currentUser?.uid}/blocked/$blockedUserId")
    }

    suspend fun submitReport(
        targetType: String,
        targetId: String,
        reason: String,
        details: String,
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val ref = db.collection("reports").document()
        ref.set(
            mapOf(
                "reporterId" to uid,
                "targetType" to targetType,
                "targetId" to targetId.take(128),
                "reason" to reason,
                "details" to details.trim().take(300),
                "status" to "submitted",
                "createdAt" to FieldValue.serverTimestamp(),
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "reports")
    }

    private fun buildDefaultProfile(uid: String, displayName: String?): PublicProfile {
        val safeName = sanitizeDisplayName(displayName)
        return PublicProfile(
            id = uid,
            userId = uid,
            displayName = safeName,
            handle = buildHandle(safeName, uid),
            bio = "",
            avatarSeed = abs(uid.hashCode()) % 6,
        )
    }

    private fun sanitizeDisplayName(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        return if (trimmed.length >= 2) trimmed.take(50) else "Member"
    }

    private fun buildHandle(name: String, uid: String): String {
        val ascii = name.lowercase()
            .replace(Regex("[^a-z0-9_.]"), "")
            .take(18)
        val prefix = if (ascii.length >= 2) ascii else "member"
        val suffix = uid.take(5).lowercase().replace(Regex("[^a-z0-9]"), "0")
        return "${prefix}_$suffix".take(30)
    }
}
