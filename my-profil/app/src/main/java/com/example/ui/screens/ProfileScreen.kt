package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.FollowRelation
import com.example.model.Post
import com.example.model.PublicProfile
import com.example.ui.components.CenteredLoadingState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FollowListDialog
import com.example.ui.components.PostCard
import com.example.ui.components.UserAvatar
import com.example.ui.i18n.LocaleFormatters

@Composable
fun ProfileScreen(
    profile: PublicProfile?,
    posts: List<Post>,
    followers: List<FollowRelation>,
    following: List<FollowRelation>,
    likedPostIds: Set<String>,
    currentUserId: String,
    isFollowingViewed: Boolean,
    isOtherUserProfile: Boolean,
    languageCode: String,
    onBackToMyProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onToggleFollow: (PublicProfile) -> Unit,
    onBlockUser: (PublicProfile) -> Unit,
    onReportUser: (PublicProfile) -> Unit,
    onSelectUserProfile: (String) -> Unit,
    onLikeClick: (Post) -> Unit,
    onCommentClick: (Post) -> Unit,
    onEditPost: (Post) -> Unit,
    onDeletePost: (Post) -> Unit,
    onReportPost: (Post) -> Unit,
    onBlockPostAuthor: (Post) -> Unit,
) {
    if (isOtherUserProfile) {
        BackHandler(onBack = onBackToMyProfile)
    }

    var showFollowersDialog by remember { mutableStateOf(false) }
    var showFollowingDialog by remember { mutableStateOf(false) }

    if (profile == null) {
        CenteredLoadingState()
        return
    }

    if (showFollowersDialog) {
        FollowListDialog(
            title = stringResource(R.string.profile_followers_title),
            relations = followers,
            showFollowers = true,
            onDismiss = { showFollowersDialog = false },
            onUserClick = onSelectUserProfile,
        )
    }
    if (showFollowingDialog) {
        FollowListDialog(
            title = stringResource(R.string.profile_following_title),
            relations = following,
            showFollowers = false,
            onDismiss = { showFollowingDialog = false },
            onUserClick = onSelectUserProfile,
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(104.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary,
                                        )
                                    )
                                )
                        ) {
                            if (isOtherUserProfile) {
                                IconButton(
                                    onClick = onBackToMyProfile,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                        .testTag("profile_back_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.action_back),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = onOpenSettings,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .testTag("open_settings_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = stringResource(R.string.action_settings),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.offset(y = (-36).dp)) {
                                    UserAvatar(
                                        displayName = profile.displayName,
                                        avatarSeed = profile.avatarSeed,
                                        avatarDataUrl = profile.avatarDataUrl,
                                        size = 76.dp,
                                    )
                                }

                                if (!isOtherUserProfile) {
                                    OutlinedButton(
                                        onClick = onEditProfile,
                                        modifier = Modifier.testTag("edit_profile_button"),
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.profile_edit_button))
                                    }
                                } else if (!profile.isDemo) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (isFollowingViewed) {
                                            OutlinedButton(
                                                onClick = { onToggleFollow(profile) },
                                                modifier = Modifier.testTag("follow_toggle_button"),
                                            ) {
                                                Text(stringResource(R.string.profile_following_btn))
                                            }
                                        } else {
                                            Button(
                                                onClick = { onToggleFollow(profile) },
                                                modifier = Modifier.testTag("follow_toggle_button"),
                                            ) {
                                                Text(stringResource(R.string.profile_follow))
                                            }
                                        }
                                        IconButton(onClick = { onReportUser(profile) }) {
                                            Icon(Icons.Default.Flag, contentDescription = stringResource(R.string.action_report_content))
                                        }
                                        IconButton(onClick = { onBlockUser(profile) }) {
                                            Icon(Icons.Default.NoAccounts, contentDescription = stringResource(R.string.action_block_user))
                                        }
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = profile.displayName,
                                    style = MaterialTheme.typography.headlineSmall,
                                )
                                if (profile.isDemo) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.demo_badge),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                                if (profile.isPrivate) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Text(
                                                text = stringResource(R.string.profile_private_notice),
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "@${profile.handle}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            if (profile.bio.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = profile.bio,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                StatPill(
                                    count = LocaleFormatters.formatNumber(posts.size.toLong().coerceAtLeast(profile.postsCount), languageCode),
                                    label = stringResource(R.string.profile_stats_posts),
                                    onClick = null,
                                )
                                StatPill(
                                    count = LocaleFormatters.formatNumber(profile.followersCount, languageCode),
                                    label = stringResource(R.string.profile_stats_followers),
                                    onClick = if (profile.isDemo) null else ({ showFollowersDialog = true }),
                                )
                                StatPill(
                                    count = LocaleFormatters.formatNumber(profile.followingCount, languageCode),
                                    label = stringResource(R.string.profile_stats_following),
                                    onClick = if (profile.isDemo) null else ({ showFollowingDialog = true }),
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.profile_posts_heading),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }

            if (posts.isEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        EmptyStateCard(
                            icon = Icons.Default.PostAdd,
                            title = stringResource(R.string.profile_posts_empty),
                            subtitle = stringResource(R.string.feed_empty_subtitle),
                        )
                    }
                }
            } else {
                items(posts, key = { it.id }) { post ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        PostCard(
                            post = post,
                            isLiked = post.id in likedPostIds,
                            isOwnPost = post.authorId == currentUserId,
                            languageCode = languageCode,
                            onAuthorClick = {},
                            onLikeClick = { onLikeClick(post) },
                            onCommentClick = { onCommentClick(post) },
                            onEditClick = { onEditPost(post) },
                            onDeleteClick = { onDeletePost(post) },
                            onReportClick = { onReportPost(post) },
                            onBlockUserClick = { onBlockPostAuthor(post) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    count: String,
    label: String,
    onClick: (() -> Unit)?,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
