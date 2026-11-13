package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Post
import com.example.model.PublicProfile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PostCard
import com.example.ui.components.UserAvatar
import com.example.ui.i18n.LocaleFormatters

@Composable
fun ExploreScreen(
    profiles: List<PublicProfile>,
    posts: List<Post>,
    followingIds: Set<String>,
    likedPostIds: Set<String>,
    currentUserId: String,
    languageCode: String,
    onProfileClick: (String) -> Unit,
    onFollowToggle: (PublicProfile) -> Unit,
    onLikeClick: (Post) -> Unit,
    onCommentClick: (Post) -> Unit,
    onEditPost: (Post) -> Unit,
    onDeletePost: (Post) -> Unit,
    onReportPost: (Post) -> Unit,
    onBlockAuthor: (Post) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    val cleanQuery = query.trim().lowercase()
    val filteredProfiles = remember(profiles, cleanQuery) {
        if (cleanQuery.isEmpty()) profiles
        else profiles.filter {
            it.displayName.lowercase().contains(cleanQuery) ||
                it.handle.lowercase().contains(cleanQuery) ||
                it.bio.lowercase().contains(cleanQuery)
        }
    }
    val filteredPosts = remember(posts, cleanQuery) {
        if (cleanQuery.isEmpty()) posts
        else posts.filter {
            it.content.lowercase().contains(cleanQuery) ||
                it.authorName.lowercase().contains(cleanQuery) ||
                it.authorHandle.lowercase().contains(cleanQuery)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize(),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_input"),
            )

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.search_tab_people)) },
                    modifier = Modifier.testTag("search_tab_people"),
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.search_tab_posts)) },
                    modifier = Modifier.testTag("search_tab_posts"),
                )
            }

            if (selectedTab == 0) {
                if (filteredProfiles.isEmpty()) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        EmptyStateCard(
                            icon = Icons.Default.PersonSearch,
                            title = stringResource(R.string.search_empty_people),
                            subtitle = stringResource(R.string.search_prompt_subtitle),
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filteredProfiles, key = { it.userId }) { profile ->
                            val isSelf = profile.userId == currentUserId
                            val isFollowing = profile.userId in followingIds

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onProfileClick(profile.userId) }
                                    .testTag("explore_user_${profile.userId}"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    UserAvatar(
                                        displayName = profile.displayName,
                                        avatarSeed = profile.avatarSeed,
                                        avatarDataUrl = profile.avatarDataUrl,
                                        size = 48.dp,
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Text(
                                                text = profile.displayName,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
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
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "@${profile.handle} · ${LocaleFormatters.formatNumber(profile.followersCount, languageCode)} ${stringResource(R.string.profile_stats_followers)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        if (profile.bio.isNotBlank()) {
                                            Text(
                                                text = profile.bio,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(top = 4.dp),
                                            )
                                        }
                                    }
                                    if (!isSelf && !profile.isDemo) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        if (isFollowing) {
                                            OutlinedButton(onClick = { onFollowToggle(profile) }) {
                                                Text(stringResource(R.string.profile_following_btn))
                                            }
                                        } else {
                                            Button(onClick = { onFollowToggle(profile) }) {
                                                Text(stringResource(R.string.profile_follow))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (filteredPosts.isEmpty()) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        EmptyStateCard(
                            icon = Icons.Default.Search,
                            title = stringResource(R.string.search_empty_posts),
                            subtitle = stringResource(R.string.search_prompt_subtitle),
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(filteredPosts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                isLiked = post.id in likedPostIds,
                                isOwnPost = post.authorId == currentUserId,
                                languageCode = languageCode,
                                onAuthorClick = { onProfileClick(post.authorId) },
                                onLikeClick = { onLikeClick(post) },
                                onCommentClick = { onCommentClick(post) },
                                onEditClick = { onEditPost(post) },
                                onDeleteClick = { onDeletePost(post) },
                                onReportClick = { onReportPost(post) },
                                onBlockUserClick = { onBlockAuthor(post) },
                            )
                        }
                    }
                }
            }
        }
    }
}
