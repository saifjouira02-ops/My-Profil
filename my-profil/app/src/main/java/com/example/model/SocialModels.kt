package com.example.model

import androidx.annotation.DrawableRes
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val handle: String = "",
    val bio: String = "",
    val avatarSeed: Int = 0,
    val avatarDataUrl: String? = null,
    val isPrivate: Boolean = false,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val postsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val isDemo: Boolean = false,
)

@IgnoreExtraProperties
data class PrivateAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String? = null,
    val language: String = "ar",
    val darkMode: String = "system",
    val largeText: Boolean = false,
    val highContrast: Boolean = false,
    val showDemoContent: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Post(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorHandle: String = "",
    val authorAvatarSeed: Int = 0,
    val authorAvatarDataUrl: String? = null,
    val content: String = "",
    val imageDataUrl: String? = null,
    val visibility: String = "public",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    // Local/demo-only fields (not written to Firestore)
    val isDemo: Boolean = false,
    @param:DrawableRes val demoDrawableRes: Int? = null,
)

@IgnoreExtraProperties
data class Comment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val postVisibility: String = "public",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarSeed: Int = 0,
    val authorAvatarDataUrl: String? = null,
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val isDemo: Boolean = false,
)

@IgnoreExtraProperties
data class FollowRelation(
    @DocumentId val id: String = "",
    val followerId: String = "",
    val followerName: String = "",
    val followerHandle: String = "",
    val followerAvatarSeed: Int = 0,
    val followingId: String = "",
    val followingName: String = "",
    val followingHandle: String = "",
    val followingAvatarSeed: Int = 0,
    val isPublicTarget: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val actorAvatarSeed: Int = 0,
    val type: String = "like",
    val postId: String? = null,
    val previewText: String? = null,
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class BlockedUser(
    @DocumentId val id: String = "",
    val blockedUserId: String = "",
    val blockedDisplayName: String = "",
    val blockedHandle: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ContentReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "post",
    val targetId: String = "",
    val reason: String = "spam",
    val details: String = "",
    val status: String = "submitted",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

data class UserPreferences(
    val language: String = "ar",
    val darkMode: String = "system",
    val largeText: Boolean = false,
    val highContrast: Boolean = false,
    val showDemoContent: Boolean = true,
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
