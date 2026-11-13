package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Post
import com.example.model.UiState
import com.example.ui.components.CenteredLoadingState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.PostCard

@Composable
fun FeedScreen(
    feedState: UiState<List<Post>>,
    likedPostIds: Set<String>,
    currentUserId: String,
    languageCode: String,
    followingOnly: Boolean,
    onFilterChange: (Boolean) -> Unit,
    onOpenComposer: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onLikeClick: (Post) -> Unit,
    onCommentClick: (Post) -> Unit,
    onEditPost: (Post) -> Unit,
    onDeletePost: (Post) -> Unit,
    onReportPost: (Post) -> Unit,
    onBlockAuthor: (Post) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !followingOnly,
                    onClick = { onFilterChange(false) },
                    label = { Text(stringResource(R.string.feed_filter_all)) },
                    modifier = Modifier.testTag("feed_filter_all"),
                )
                FilterChip(
                    selected = followingOnly,
                    onClick = { onFilterChange(true) },
                    label = { Text(stringResource(R.string.feed_filter_following)) },
                    modifier = Modifier.testTag("feed_filter_following"),
                )
            }

            when (feedState) {
                is UiState.Loading -> CenteredLoadingState()
                is UiState.Error -> {
                    Box(modifier = Modifier.padding(16.dp)) {
                        ErrorBannerCard(message = stringResource(R.string.error_network_or_permission))
                    }
                }
                is UiState.Success -> {
                    val posts = feedState.data
                    if (posts.isEmpty()) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            EmptyStateCard(
                                icon = Icons.Default.DynamicFeed,
                                title = stringResource(R.string.feed_empty_title),
                                subtitle = stringResource(R.string.feed_empty_subtitle),
                                actionLabel = stringResource(R.string.action_create_post),
                                onAction = onOpenComposer,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(posts, key = { it.id }) { post ->
                                PostCard(
                                    post = post,
                                    isLiked = post.id in likedPostIds,
                                    isOwnPost = post.authorId == currentUserId,
                                    languageCode = languageCode,
                                    onAuthorClick = { onAuthorClick(post.authorId) },
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
}
