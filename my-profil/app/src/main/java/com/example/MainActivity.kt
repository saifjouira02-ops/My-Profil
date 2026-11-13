package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.preferences.UserPreferencesStore
import com.example.data.repository.SocialRepository
import com.example.model.Post
import com.example.model.UiState
import com.example.model.UserPreferences
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.components.BlockUserConfirmDialog
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.DeletePostConfirmDialog
import com.example.ui.components.EditProfileDialog
import com.example.ui.components.PostComposerDialog
import com.example.ui.components.ReportContentDialog
import com.example.ui.i18n.LocalizedAppWrapper
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyProfilTheme
import com.example.ui.viewmodel.PreferencesViewModel
import com.example.ui.viewmodel.SocialViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class AppTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    FEED("feed", R.string.nav_feed, Icons.Filled.Home, Icons.Outlined.Home),
    EXPLORE("explore", R.string.nav_explore, Icons.Filled.Explore, Icons.Outlined.Explore),
    NOTIFICATIONS("notifications", R.string.nav_notifications, Icons.Filled.Notifications, Icons.Outlined.Notifications),
    PROFILE("profile", R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefsViewModel: PreferencesViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        PreferencesViewModel(UserPreferencesStore(app))
                    }
                }
            )
            val preferences by prefsViewModel.preferences.collectAsStateWithLifecycle()

            LocalizedAppWrapper(languageCode = preferences.language) {
                MyProfilTheme(
                    darkThemeMode = preferences.darkMode,
                    largeText = preferences.largeText,
                    highContrast = preferences.highContrast,
                ) {
                    AppNavigationRoot(
                        preferences = preferences,
                        onLanguageChange = prefsViewModel::setLanguage,
                        onDarkModeChange = prefsViewModel::setDarkMode,
                        onLargeTextChange = prefsViewModel::setLargeText,
                        onHighContrastChange = prefsViewModel::setHighContrast,
                        onShowDemoChange = prefsViewModel::setShowDemoContent,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppNavigationRoot(
    preferences: UserPreferences,
    onLanguageChange: (String) -> Unit,
    onDarkModeChange: (String) -> Unit,
    onLargeTextChange: (Boolean) -> Unit,
    onHighContrastChange: (Boolean) -> Unit,
    onShowDemoChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val currentUser by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                scope = scope,
                onAuthSuccess = {},
                onUnauthenticated = {},
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            currentLanguage = preferences.language,
            onLanguageChange = onLanguageChange,
            onAuthSuccess = {},
        )
    } else {
        key(user.uid) {
            MainSocialApp(
                currentUserId = user.uid,
                currentUserDisplayName = user.displayName,
                currentUserEmail = user.email,
                preferences = preferences,
                onLanguageChange = onLanguageChange,
                onDarkModeChange = onDarkModeChange,
                onLargeTextChange = onLargeTextChange,
                onHighContrastChange = onHighContrastChange,
                onShowDemoChange = onShowDemoChange,
                onSignOut = {
                    signOutUser(credentialManager = credentialManager, scope = scope)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainSocialApp(
    currentUserId: String,
    currentUserDisplayName: String?,
    currentUserEmail: String?,
    preferences: UserPreferences,
    onLanguageChange: (String) -> Unit,
    onDarkModeChange: (String) -> Unit,
    onLargeTextChange: (Boolean) -> Unit,
    onHighContrastChange: (Boolean) -> Unit,
    onShowDemoChange: (Boolean) -> Unit,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: SocialViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                SocialViewModel(
                    repository = SocialRepository(db),
                    prefsStore = UserPreferencesStore(app),
                    currentUserId = currentUserId,
                    currentUserDisplayName = currentUserDisplayName,
                    currentUserEmail = currentUserEmail,
                )
            }
        }
    )

    val feedState by viewModel.feedState.collectAsStateWithLifecycle()
    val myProfile by viewModel.myProfile.collectAsStateWithLifecycle()
    val likedPostIds by viewModel.likedPostIds.collectAsStateWithLifecycle()
    val followingRelations by viewModel.followingRelations.collectAsStateWithLifecycle()
    val publicProfiles by viewModel.publicProfiles.collectAsStateWithLifecycle()
    val viewedProfileId by viewModel.viewedProfileId.collectAsStateWithLifecycle()
    val viewedProfile by viewModel.viewedProfile.collectAsStateWithLifecycle()
    val viewedProfilePosts by viewModel.viewedProfilePosts.collectAsStateWithLifecycle()
    val viewedFollowers by viewModel.viewedProfileFollowers.collectAsStateWithLifecycle()
    val viewedFollowing by viewModel.viewedProfileFollowing.collectAsStateWithLifecycle()
    val activeCommentsPost by viewModel.activeCommentsPost.collectAsStateWithLifecycle()
    val activeComments by viewModel.commentsForActivePost.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    val blockedUsers by viewModel.blockedUsers.collectAsStateWithLifecycle()
    val followingOnly by viewModel.feedFilterFollowingOnly.collectAsStateWithLifecycle()
    val actionMessageRes by viewModel.actionMessage.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(AppTab.FEED) }
    var showSettings by remember { mutableStateOf(false) }
    var showComposer by remember { mutableStateOf(false) }
    var editingPost by remember { mutableStateOf<Post?>(null) }
    var deletingPost by remember { mutableStateOf<Post?>(null) }
    var showEditProfile by remember { mutableStateOf(false) }
    var reportTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var blockTarget by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val resolvedActionMsg = actionMessageRes?.let { stringResource(it) }
    LaunchedEffect(resolvedActionMsg, actionError) {
        val msg = resolvedActionMsg ?: actionError
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    // Dialogs & Sheets
    if (showComposer || editingPost != null) {
        PostComposerDialog(
            editingPost = editingPost,
            onDismiss = {
                showComposer = false
                editingPost = null
            },
            onSubmit = { content, visibility, imageDataUrl ->
                val target = editingPost
                if (target == null) {
                    viewModel.createPost(content, visibility, imageDataUrl, R.string.post_saved_toast)
                } else {
                    viewModel.updatePost(target.id, content, visibility, imageDataUrl, R.string.post_updated_toast)
                }
                showComposer = false
                editingPost = null
            },
        )
    }

    val commentsPost = activeCommentsPost
    if (commentsPost != null) {
        CommentsBottomSheet(
            post = commentsPost,
            comments = activeComments,
            currentUserId = currentUserId,
            languageCode = preferences.language,
            onDismiss = { viewModel.closeComments() },
            onSendComment = { text -> viewModel.addComment(commentsPost, text) },
            onDeleteComment = { commentId -> viewModel.deleteComment(commentsPost.id, commentId) },
        )
    }

    if (showEditProfile && myProfile != null) {
        EditProfileDialog(
            profile = myProfile!!,
            onDismiss = { showEditProfile = false },
            onSave = { name, handle, bio, isPrivate, seed, avatarDataUrl ->
                viewModel.updateProfile(
                    displayName = name,
                    handle = handle,
                    bio = bio,
                    isPrivate = isPrivate,
                    avatarSeed = seed,
                    avatarDataUrl = avatarDataUrl,
                    successMsgRes = R.string.profile_saved_toast,
                )
                showEditProfile = false
            },
        )
    }

    val pendingReport = reportTarget
    if (pendingReport != null) {
        ReportContentDialog(
            onDismiss = { reportTarget = null },
            onSubmit = { reason, details ->
                viewModel.submitReport(
                    targetType = pendingReport.first,
                    targetId = pendingReport.second,
                    reason = reason,
                    details = details,
                    successMsgRes = R.string.report_submitted_toast,
                )
                reportTarget = null
            },
        )
    }

    val pendingBlock = blockTarget
    if (pendingBlock != null) {
        BlockUserConfirmDialog(
            displayName = pendingBlock.second,
            onDismiss = { blockTarget = null },
            onConfirm = {
                viewModel.blockUser(
                    userId = pendingBlock.first,
                    displayName = pendingBlock.second,
                    handle = pendingBlock.third,
                    successMsgRes = R.string.user_blocked_toast,
                )
                blockTarget = null
                if (viewedProfileId == pendingBlock.first) {
                    viewModel.selectProfile(currentUserId)
                }
            },
        )
    }

    val pendingDelete = deletingPost
    if (pendingDelete != null) {
        DeletePostConfirmDialog(
            onDismiss = { deletingPost = null },
            onConfirm = {
                viewModel.deletePost(pendingDelete.id, R.string.post_deleted_toast)
                deletingPost = null
            },
        )
    }

    val followingIds = remember(followingRelations) {
        followingRelations.map { it.followingId }.toSet()
    }
    val allFeedPosts = (feedState as? UiState.Success)?.data.orEmpty()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (!showSettings) {
                    val titleText = when (currentTab) {
                        AppTab.FEED -> stringResource(R.string.feed_title)
                        AppTab.EXPLORE -> stringResource(R.string.explore_title)
                        AppTab.NOTIFICATIONS -> stringResource(R.string.notifications_title)
                        AppTab.PROFILE -> stringResource(R.string.profile_title)
                    }
                    TopAppBar(
                        title = {
                            Text(
                                text = titleText,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                        ),
                    )
                }
            },
            floatingActionButton = {
                if (!showSettings && (currentTab == AppTab.FEED || (currentTab == AppTab.PROFILE && viewedProfileId == currentUserId))) {
                    FloatingActionButton(
                        onClick = { showComposer = true },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.testTag("create_post_fab"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.action_create_post),
                        )
                    }
                }
            },
            bottomBar = {
                if (!useRail && !showSettings) {
                    NavigationBar {
                        AppTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (tab == AppTab.PROFILE) {
                                        viewModel.selectProfile(currentUserId)
                                    }
                                    currentTab = tab
                                },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        unreadCount = unreadCount,
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag("nav_${tab.route}"),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail && !showSettings) {
                    NavigationRail {
                        AppTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (tab == AppTab.PROFILE) {
                                        viewModel.selectProfile(currentUserId)
                                    }
                                    currentTab = tab
                                },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        unreadCount = unreadCount,
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag("rail_${tab.route}"),
                            )
                        }
                    }
                }

                if (showSettings) {
                    SettingsScreen(
                        preferences = preferences,
                        isProfilePrivate = myProfile?.isPrivate ?: false,
                        blockedUsers = blockedUsers,
                        userEmailOrName = currentUserEmail ?: (myProfile?.displayName ?: currentUserId),
                        onBack = { showSettings = false },
                        onLanguageChange = { lang ->
                            onLanguageChange(lang)
                            viewModel.syncRemotePreferences(preferences.copy(language = lang))
                        },
                        onDarkModeChange = { mode ->
                            onDarkModeChange(mode)
                            viewModel.syncRemotePreferences(preferences.copy(darkMode = mode))
                        },
                        onLargeTextChange = { enabled ->
                            onLargeTextChange(enabled)
                            viewModel.syncRemotePreferences(preferences.copy(largeText = enabled))
                        },
                        onHighContrastChange = { enabled ->
                            onHighContrastChange(enabled)
                            viewModel.syncRemotePreferences(preferences.copy(highContrast = enabled))
                        },
                        onPrivateProfileChange = viewModel::togglePrivateProfile,
                        onShowDemoChange = { enabled ->
                            onShowDemoChange(enabled)
                            viewModel.syncRemotePreferences(preferences.copy(showDemoContent = enabled))
                        },
                        onUnblockUser = { uid ->
                            viewModel.unblockUser(uid, R.string.user_unblocked_toast)
                        },
                        onSignOut = onSignOut,
                    )
                } else {
                    when (currentTab) {
                        AppTab.FEED -> FeedScreen(
                            feedState = feedState,
                            likedPostIds = likedPostIds,
                            currentUserId = currentUserId,
                            languageCode = preferences.language,
                            followingOnly = followingOnly,
                            onFilterChange = viewModel::setFeedFilterFollowingOnly,
                            onOpenComposer = { showComposer = true },
                            onAuthorClick = { authorId ->
                                viewModel.selectProfile(authorId)
                                currentTab = AppTab.PROFILE
                            },
                            onLikeClick = viewModel::toggleLike,
                            onCommentClick = viewModel::openComments,
                            onEditPost = { editingPost = it },
                            onDeletePost = { deletingPost = it },
                            onReportPost = { reportTarget = "post" to it.id },
                            onBlockAuthor = {
                                blockTarget = Triple(it.authorId, it.authorName, it.authorHandle)
                            },
                        )

                        AppTab.EXPLORE -> ExploreScreen(
                            profiles = publicProfiles,
                            posts = allFeedPosts,
                            followingIds = followingIds,
                            likedPostIds = likedPostIds,
                            currentUserId = currentUserId,
                            languageCode = preferences.language,
                            onProfileClick = { uid ->
                                viewModel.selectProfile(uid)
                                currentTab = AppTab.PROFILE
                            },
                            onFollowToggle = viewModel::toggleFollow,
                            onLikeClick = viewModel::toggleLike,
                            onCommentClick = viewModel::openComments,
                            onEditPost = { editingPost = it },
                            onDeletePost = { deletingPost = it },
                            onReportPost = { reportTarget = "post" to it.id },
                            onBlockAuthor = {
                                blockTarget = Triple(it.authorId, it.authorName, it.authorHandle)
                            },
                        )

                        AppTab.NOTIFICATIONS -> NotificationsScreen(
                            notifications = notifications,
                            languageCode = preferences.language,
                            onNotificationClick = { notif ->
                                if (!notif.isRead) viewModel.markNotificationRead(notif.id)
                                viewModel.selectProfile(notif.actorId)
                                currentTab = AppTab.PROFILE
                            },
                            onMarkAllRead = viewModel::markAllNotificationsRead,
                        )

                        AppTab.PROFILE -> ProfileScreen(
                            profile = viewedProfile,
                            posts = viewedProfilePosts,
                            followers = viewedFollowers,
                            following = viewedFollowing,
                            likedPostIds = likedPostIds,
                            currentUserId = currentUserId,
                            isFollowingViewed = viewedProfileId in followingIds,
                            isOtherUserProfile = viewedProfileId != currentUserId,
                            languageCode = preferences.language,
                            onBackToMyProfile = { viewModel.selectProfile(currentUserId) },
                            onOpenSettings = { showSettings = true },
                            onEditProfile = { showEditProfile = true },
                            onToggleFollow = viewModel::toggleFollow,
                            onBlockUser = {
                                blockTarget = Triple(it.userId, it.displayName, it.handle)
                            },
                            onReportUser = { reportTarget = "user" to it.userId },
                            onSelectUserProfile = { uid -> viewModel.selectProfile(uid) },
                            onLikeClick = viewModel::toggleLike,
                            onCommentClick = viewModel::openComments,
                            onEditPost = { editingPost = it },
                            onDeletePost = { deletingPost = it },
                            onReportPost = { reportTarget = "post" to it.id },
                            onBlockPostAuthor = {
                                blockTarget = Triple(it.authorId, it.authorName, it.authorHandle)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIconWithBadge(
    tab: AppTab,
    selected: Boolean,
    unreadCount: Int,
) {
    val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
    if (tab == AppTab.NOTIFICATIONS && unreadCount > 0) {
        BadgedBox(badge = { Badge { Text(unreadCount.coerceAtMost(99).toString()) } }) {
            Icon(imageVector = icon, contentDescription = null)
        }
    } else {
        Icon(imageVector = icon, contentDescription = null)
    }
}
