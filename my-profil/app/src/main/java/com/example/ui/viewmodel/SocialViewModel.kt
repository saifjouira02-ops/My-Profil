package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DemoCatalog
import com.example.data.preferences.UserPreferencesStore
import com.example.data.repository.SocialRepository
import com.example.data.unwrapFirestoreException
import com.example.model.AppNotification
import com.example.model.BlockedUser
import com.example.model.Comment
import com.example.model.FollowRelation
import com.example.model.Post
import com.example.model.PublicProfile
import com.example.model.UiState
import com.example.model.UserPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SocialViewModel(
    private val repository: SocialRepository,
    private val prefsStore: UserPreferencesStore,
    val currentUserId: String,
    private val currentUserDisplayName: String?,
    private val currentUserEmail: String?,
) : ViewModel() {

    private val _selectedLanguage = MutableStateFlow("ar")
    private val _showDemoContent = MutableStateFlow(true)
    private val _feedFilterFollowingOnly = MutableStateFlow(false)
    val feedFilterFollowingOnly: StateFlow<Boolean> = _feedFilterFollowingOnly.asStateFlow()

    private val _activeCommentsPost = MutableStateFlow<Post?>(null)
    val activeCommentsPost: StateFlow<Post?> = _activeCommentsPost.asStateFlow()

    private val _viewedProfileId = MutableStateFlow(currentUserId)
    val viewedProfileId: StateFlow<String> = _viewedProfileId.asStateFlow()

    private val _demoLikedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _demoExtraComments = MutableStateFlow<Map<String, List<Comment>>>(emptyMap())

    private val _actionMessage = MutableStateFlow<Int?>(null)
    val actionMessage: StateFlow<Int?> = _actionMessage.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    init {
        viewModelScope.launch {
            prefsStore.preferencesFlow.collect { prefs ->
                _selectedLanguage.value = prefs.language
                _showDemoContent.value = prefs.showDemoContent
            }
        }
        viewModelScope.launch {
            val initialPrefs = prefsStore.preferencesFlow.let { flow ->
                var value = UserPreferences()
                runCatching {
                    flow.collect {
                        value = it
                        throw StopCollectException()
                    }
                }
                value
            }
            repository.ensureUserInitialized(
                uid = currentUserId,
                displayName = currentUserDisplayName,
                email = currentUserEmail,
                initialPrefs = initialPrefs,
            ).onFailure { err ->
                Log.w(TAG, "Failed to initialize user profile", err)
            }
        }
    }

    val myProfile: StateFlow<PublicProfile?> = repository.observeProfile(currentUserId)
        .catch { err ->
            handleFlowError("myProfile", err)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val likedPostIds: StateFlow<Set<String>> = combine(
        repository.observeLikedPostIds(currentUserId).catch { err ->
            handleFlowError("likedPostIds", err)
            emit(emptySet())
        },
        _demoLikedIds,
    ) { realLikes, demoLikes -> realLikes + demoLikes }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptySet())

    val blockedUsers: StateFlow<List<BlockedUser>> = repository.observeBlockedUsers(currentUserId)
        .catch { err ->
            handleFlowError("blockedUsers", err)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val followingRelations: StateFlow<List<FollowRelation>> = repository.observeFollowing(currentUserId)
        .catch { err ->
            handleFlowError("following", err)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val publicProfiles: StateFlow<List<PublicProfile>> = combine(
        repository.observePublicProfiles().catch { err ->
            handleFlowError("publicProfiles", err)
            emit(emptyList())
        },
        blockedUsers,
        _selectedLanguage,
        _showDemoContent,
    ) { realProfiles, blocked, lang, showDemo ->
        val blockedIds = blocked.map { it.blockedUserId }.toSet()
        val filteredReal = realProfiles.filter { it.userId !in blockedIds }
        if (showDemo) {
            filteredReal + DemoCatalog.sampleProfiles(lang).filter { it.userId !in blockedIds }
        } else {
            filteredReal
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val feedState: StateFlow<UiState<List<Post>>> = combine(
        repository.observePublicFeed()
            .map<List<Post>, UiState<List<Post>>> { UiState.Success(it) }
            .catch { error ->
                val fsErr = error.unwrapFirestoreException()
                if (fsErr == null && error is CancellationException) throw error
                val root = fsErr ?: error
                Log.w(TAG, "Error observing public feed", root)
                emit(UiState.Error(root.message ?: "Failed to load feed"))
            },
        blockedUsers,
        followingRelations,
        _feedFilterFollowingOnly,
        combine(_selectedLanguage, _showDemoContent) { l, d -> l to d },
    ) { realState, blocked, following, followingOnly, (lang, showDemo) ->
        when (realState) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> realState
            is UiState.Success -> {
                val blockedIds = blocked.map { it.blockedUserId }.toSet()
                val followingIds = following.map { it.followingId }.toSet() + currentUserId
                val demoPosts = if (showDemo) DemoCatalog.samplePosts(lang) else emptyList()
                val merged = (realState.data + demoPosts)
                    .filter { it.authorId !in blockedIds }
                    .filter { !followingOnly || it.authorId in followingIds }
                UiState.Success(merged)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val viewedProfile: StateFlow<PublicProfile?> = combine(
        _viewedProfileId,
        _selectedLanguage,
    ) { targetId, lang -> targetId to lang }
        .flatMapLatest { (targetId, lang) ->
            if (targetId.startsWith("demo_")) {
                flowOf(DemoCatalog.sampleProfiles(lang).firstOrNull { it.userId == targetId })
            } else {
                repository.observeProfile(targetId).catch { err ->
                    handleFlowError("viewedProfile", err)
                    emit(null)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val viewedProfilePosts: StateFlow<List<Post>> = combine(
        _viewedProfileId,
        _selectedLanguage,
    ) { targetId, lang -> targetId to lang }
        .flatMapLatest { (targetId, lang) ->
            if (targetId.startsWith("demo_")) {
                flowOf(DemoCatalog.samplePosts(lang).filter { it.authorId == targetId })
            } else {
                repository.observeUserPosts(targetId, currentUserId).catch { err ->
                    handleFlowError("viewedProfilePosts", err)
                    emit(emptyList())
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val viewedProfileFollowers: StateFlow<List<FollowRelation>> = _viewedProfileId
        .flatMapLatest { targetId ->
            if (targetId.startsWith("demo_")) flowOf(emptyList())
            else repository.observeFollowers(targetId, currentUserId).catch { err ->
                handleFlowError("viewedProfileFollowers", err)
                emit(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val viewedProfileFollowing: StateFlow<List<FollowRelation>> = _viewedProfileId
        .flatMapLatest { targetId ->
            if (targetId.startsWith("demo_")) flowOf(emptyList())
            else repository.observeFollowing(targetId).catch { err ->
                handleFlowError("viewedProfileFollowing", err)
                emit(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val commentsForActivePost: StateFlow<List<Comment>> = combine(
        _activeCommentsPost,
        _selectedLanguage,
        _demoExtraComments,
        blockedUsers,
    ) { post, lang, demoExtra, blocked ->
        Triple(post, lang to demoExtra, blocked.map { it.blockedUserId }.toSet())
    }.flatMapLatest { (post, langAndExtra, blockedIds) ->
        val (lang, demoExtra) = langAndExtra
        when {
            post == null -> flowOf(emptyList())
            post.isDemo -> {
                val base = DemoCatalog.sampleComments(post.id, lang)
                val added = demoExtra[post.id].orEmpty()
                flowOf((base + added).filter { it.authorId !in blockedIds })
            }
            else -> repository.observeComments(post.id, post.visibility, currentUserId)
                .map { list -> list.filter { it.authorId !in blockedIds } }
                .catch { err ->
                    handleFlowError("comments", err)
                    emit(emptyList())
                }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val notifications: StateFlow<List<AppNotification>> = repository.observeNotifications(currentUserId)
        .catch { err ->
            handleFlowError("notifications", err)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = notifications
        .map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

    // --- Actions ---

    fun setFeedFilterFollowingOnly(followingOnly: Boolean) {
        _feedFilterFollowingOnly.value = followingOnly
    }

    fun selectProfile(userId: String) {
        _viewedProfileId.value = userId
    }

    fun openComments(post: Post) {
        _activeCommentsPost.value = post
    }

    fun closeComments() {
        _activeCommentsPost.value = null
    }

    fun clearMessages() {
        _actionMessage.value = null
        _actionError.value = null
    }

    fun createPost(content: String, visibility: String, imageDataUrl: String?, successMsgRes: Int) {
        val author = myProfile.value ?: PublicProfile(
            id = currentUserId,
            userId = currentUserId,
            displayName = currentUserDisplayName ?: "Member",
            handle = "member",
        )
        viewModelScope.launch {
            _isBusy.value = true
            repository.createPost(author, content, visibility, imageDataUrl)
                .onSuccess { _actionMessage.value = successMsgRes }
                .onFailure { _actionError.value = it.localizedMessage }
            _isBusy.value = false
        }
    }

    fun updatePost(postId: String, content: String, visibility: String, imageDataUrl: String?, successMsgRes: Int) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.updatePost(postId, content, visibility, imageDataUrl)
                .onSuccess { _actionMessage.value = successMsgRes }
                .onFailure { _actionError.value = it.localizedMessage }
            _isBusy.value = false
        }
    }

    fun deletePost(postId: String, successMsgRes: Int) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.deletePost(postId)
                .onSuccess { _actionMessage.value = successMsgRes }
                .onFailure { _actionError.value = it.localizedMessage }
            _isBusy.value = false
        }
    }

    fun toggleLike(post: Post) {
        val isLiked = post.id in likedPostIds.value
        if (post.isDemo) {
            _demoLikedIds.value = if (isLiked) _demoLikedIds.value - post.id else _demoLikedIds.value + post.id
            return
        }
        val actor = myProfile.value ?: return
        viewModelScope.launch {
            repository.toggleLike(post, isLiked, actor)
                .onFailure { _actionError.value = it.localizedMessage }
        }
    }

    fun addComment(post: Post, text: String) {
        if (post.isDemo) {
            val actor = myProfile.value
            val localComment = Comment(
                id = "local_${System.currentTimeMillis()}",
                postId = post.id,
                authorId = currentUserId,
                authorName = actor?.displayName ?: (currentUserDisplayName ?: "You"),
                authorAvatarSeed = actor?.avatarSeed ?: 0,
                text = text.trim(),
                isDemo = true,
            )
            val current = _demoExtraComments.value[post.id].orEmpty()
            _demoExtraComments.value = _demoExtraComments.value + (post.id to (current + localComment))
            return
        }
        val actor = myProfile.value ?: return
        viewModelScope.launch {
            repository.addComment(post, actor, text)
                .onFailure { _actionError.value = it.localizedMessage }
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        if (postId.startsWith("demo_")) {
            val current = _demoExtraComments.value[postId].orEmpty()
            _demoExtraComments.value = _demoExtraComments.value + (postId to current.filterNot { it.id == commentId })
            return
        }
        viewModelScope.launch {
            repository.deleteComment(postId, commentId)
                .onFailure { _actionError.value = it.localizedMessage }
        }
    }

    fun updateProfile(
        displayName: String,
        handle: String,
        bio: String,
        isPrivate: Boolean,
        avatarSeed: Int,
        avatarDataUrl: String?,
        successMsgRes: Int,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.updateProfile(displayName, handle, bio, isPrivate, avatarSeed, avatarDataUrl)
                .onSuccess { _actionMessage.value = successMsgRes }
                .onFailure { _actionError.value = it.localizedMessage }
            _isBusy.value = false
        }
    }

    fun togglePrivateProfile(isPrivate: Boolean) {
        val current = myProfile.value ?: return
        viewModelScope.launch {
            repository.updateProfile(
                displayName = current.displayName,
                handle = current.handle,
                bio = current.bio,
                isPrivate = isPrivate,
                avatarSeed = current.avatarSeed,
                avatarDataUrl = current.avatarDataUrl,
            ).onFailure { _actionError.value = it.localizedMessage }
        }
    }

    fun toggleFollow(target: PublicProfile) {
        if (target.isDemo) return
        val me = myProfile.value ?: return
        val isFollowing = followingRelations.value.any { it.followingId == target.userId }
        viewModelScope.launch {
            repository.toggleFollow(me, target, isFollowing)
                .onFailure { _actionError.value = it.localizedMessage }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notificationId)
        }
    }

    fun markAllNotificationsRead() {
        val unreadIds = notifications.value.filter { !it.isRead }.map { it.id }
        if (unreadIds.isEmpty()) return
        viewModelScope.launch {
            repository.markAllNotificationsRead(unreadIds)
                .onFailure { _actionError.value = it.localizedMessage }
        }
    }

    fun blockUser(userId: String, displayName: String, handle: String, successMsgRes: Int) {
        viewModelScope.launch {
            repository.blockUser(userId, displayName, handle)
                .onSuccess { _actionMessage.value = successMsgRes }
                .onFailure { _actionError.value = it.localizedMessage }
        }
    }

    fun unblockUser(userId: String, successMsgRes: Int) {
        viewModelScope.launch {
            repository.unblockUser(userId)
                .onSuccess { _actionMessage.value = successMsgRes }
                .onFailure { _actionError.value = it.localizedMessage }
        }
    }

    fun submitReport(
        targetType: String,
        targetId: String,
        reason: String,
        details: String,
        successMsgRes: Int,
    ) {
        viewModelScope.launch {
            repository.submitReport(targetType, targetId, reason, details)
                .onSuccess { _actionMessage.value = successMsgRes }
                .onFailure { _actionError.value = it.localizedMessage }
        }
    }

    fun syncRemotePreferences(prefs: UserPreferences) {
        viewModelScope.launch {
            repository.syncPreferences(prefs)
        }
    }

    private fun handleFlowError(source: String, err: Throwable) {
        val fsErr = err.unwrapFirestoreException()
        if (fsErr == null && err is CancellationException) throw err
        Log.w(TAG, "Error in $source flow", fsErr ?: err)
    }

    private class StopCollectException : CancellationException("Initial preference read complete")

    private companion object {
        const val TAG = "SocialVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
