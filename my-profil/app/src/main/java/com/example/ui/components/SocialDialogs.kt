package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.media.ImagePickerHelper
import com.example.data.media.ImageProcessResult
import com.example.data.media.ImageValidationError
import com.example.model.Comment
import com.example.model.FollowRelation
import com.example.model.Post
import com.example.model.PublicProfile
import com.example.ui.i18n.LocaleFormatters
import kotlinx.coroutines.launch

@Composable
fun PostComposerDialog(
    editingPost: Post?,
    onDismiss: () -> Unit,
    onSubmit: (content: String, visibility: String, imageDataUrl: String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var content by remember(editingPost) { mutableStateOf(editingPost?.content.orEmpty()) }
    var visibility by remember(editingPost) { mutableStateOf(editingPost?.visibility ?: "public") }
    var imageDataUrl by remember(editingPost) { mutableStateOf(editingPost?.imageDataUrl) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val errEmpty = stringResource(R.string.post_validation_empty)
    val errTooLong = stringResource(R.string.post_validation_too_long)
    val errType = stringResource(R.string.image_error_type)
    val errSize = stringResource(R.string.image_error_size)
    val errDecode = stringResource(R.string.image_error_decode)

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isProcessingImage = true
            validationError = null
            when (val res = ImagePickerHelper.processPickedImage(context, uri)) {
                is ImageProcessResult.Success -> imageDataUrl = res.dataUrl
                is ImageProcessResult.Error -> {
                    validationError = when (res.error) {
                        ImageValidationError.UNSUPPORTED_TYPE -> errType
                        ImageValidationError.TOO_LARGE -> errSize
                        ImageValidationError.DECODE_FAILED -> errDecode
                    }
                }
            }
            isProcessingImage = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (editingPost == null) R.string.post_composer_title_new else R.string.post_composer_title_edit
                ),
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = content,
                    onValueChange = {
                        content = it
                        validationError = null
                    },
                    label = { Text(stringResource(R.string.post_content_hint)) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_content_input"),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = visibility == "public",
                        onClick = { visibility = "public" },
                        label = { Text(stringResource(R.string.visibility_public)) },
                        leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    )
                    FilterChip(
                        selected = visibility == "private",
                        onClick = { visibility = "private" },
                        label = { Text(stringResource(R.string.visibility_private)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    )
                }

                val previewBytes = remember(imageDataUrl) {
                    ImagePickerHelper.decodeDataUrlToBytes(imageDataUrl)
                }
                if (previewBytes != null) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(previewBytes).build(),
                            contentDescription = stringResource(R.string.post_image_cd),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 3f)
                                .clip(MaterialTheme.shapes.medium),
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                        ) {
                            IconButton(
                                onClick = { imageDataUrl = null },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.action_remove_image),
                                )
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isProcessingImage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_post_photo_button"),
                    ) {
                        if (isProcessingImage) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.post_add_photo))
                        }
                    }
                    Text(
                        text = stringResource(R.string.post_photo_limit_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = content.trim()
                    when {
                        trimmed.isEmpty() -> validationError = errEmpty
                        trimmed.length > 1000 -> validationError = errTooLong
                        else -> onSubmit(trimmed, visibility, imageDataUrl)
                    }
                },
                enabled = !isProcessingImage,
                modifier = Modifier.testTag("publish_post_button"),
            ) {
                Text(
                    stringResource(if (editingPost == null) R.string.post_publish else R.string.post_update)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: Post,
    comments: List<Comment>,
    currentUserId: String,
    languageCode: String,
    onDismiss: () -> Unit,
    onSendComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var commentText by remember { mutableStateOf("") }
    var commentError by remember { mutableStateOf<String?>(null) }
    val emptyError = stringResource(R.string.comment_validation_empty)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.comments_title),
                style = MaterialTheme.typography.titleLarge,
            )

            if (post.isDemo) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.demo_readonly_note),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }

            if (comments.isEmpty()) {
                Text(
                    text = stringResource(R.string.comments_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(comments, key = { it.id }) { comment ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            UserAvatar(
                                displayName = comment.authorName,
                                avatarSeed = comment.authorAvatarSeed,
                                avatarDataUrl = comment.authorAvatarDataUrl,
                                size = 34.dp,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = comment.authorName,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                    Text(
                                        text = LocaleFormatters.formatTimestamp(
                                            comment.createdAt,
                                            languageCode,
                                            stringResource(R.string.time_just_now),
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = comment.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            if (comment.authorId == currentUserId) {
                                IconButton(onClick = { onDeleteComment(comment.id) }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = stringResource(R.string.comment_delete_cd),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = {
                        commentText = it
                        commentError = null
                    },
                    placeholder = { Text(stringResource(R.string.comment_hint)) },
                    isError = commentError != null,
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input"),
                )
                IconButton(
                    onClick = {
                        val trimmed = commentText.trim()
                        if (trimmed.isEmpty()) {
                            commentError = emptyError
                        } else {
                            onSendComment(trimmed)
                            commentText = ""
                        }
                    },
                    modifier = Modifier.testTag("send_comment_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.comment_send_cd),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (commentError != null) {
                Text(
                    text = commentError!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
fun EditProfileDialog(
    profile: PublicProfile,
    onDismiss: () -> Unit,
    onSave: (displayName: String, handle: String, bio: String, isPrivate: Boolean, avatarSeed: Int, avatarDataUrl: String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var displayName by remember(profile) { mutableStateOf(profile.displayName) }
    var handle by remember(profile) { mutableStateOf(profile.handle) }
    var bio by remember(profile) { mutableStateOf(profile.bio) }
    var isPrivate by remember(profile) { mutableStateOf(profile.isPrivate) }
    var avatarDataUrl by remember(profile) { mutableStateOf(profile.avatarDataUrl) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val errName = stringResource(R.string.profile_validation_name)
    val errHandle = stringResource(R.string.profile_validation_handle)
    val errType = stringResource(R.string.image_error_type)
    val errSize = stringResource(R.string.image_error_size)
    val errDecode = stringResource(R.string.image_error_decode)

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isProcessing = true
            errorMsg = null
            when (val res = ImagePickerHelper.processPickedImage(context, uri, maxDimensionPx = 360, maxEncodedChars = 240_000)) {
                is ImageProcessResult.Success -> avatarDataUrl = res.dataUrl
                is ImageProcessResult.Error -> {
                    errorMsg = when (res.error) {
                        ImageValidationError.UNSUPPORTED_TYPE -> errType
                        ImageValidationError.TOO_LARGE -> errSize
                        ImageValidationError.DECODE_FAILED -> errDecode
                    }
                }
            }
            isProcessing = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit_button)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    UserAvatar(
                        displayName = displayName.ifBlank { profile.displayName },
                        avatarSeed = profile.avatarSeed,
                        avatarDataUrl = avatarDataUrl,
                        size = 56.dp,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(
                            onClick = {
                                avatarPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !isProcessing,
                        ) {
                            Text(stringResource(R.string.profile_change_photo))
                        }
                        if (avatarDataUrl != null) {
                            TextButton(onClick = { avatarDataUrl = null }) {
                                Text(stringResource(R.string.action_remove_image))
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it; errorMsg = null },
                    label = { Text(stringResource(R.string.profile_display_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_name"),
                )

                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it; errorMsg = null },
                    label = { Text(stringResource(R.string.profile_handle)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_handle"),
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 240) bio = it },
                    label = { Text(stringResource(R.string.profile_bio)) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_bio"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.profile_privacy_private_label),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.profile_privacy_private_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it },
                    )
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = displayName.trim()
                    val cleanHandle = handle.trim().lowercase()
                    when {
                        cleanName.length !in 2..50 -> errorMsg = errName
                        !cleanHandle.matches(Regex("^[a-z0-9_.]{2,30}$")) -> errorMsg = errHandle
                        else -> onSave(cleanName, cleanHandle, bio.trim(), isPrivate, profile.avatarSeed, avatarDataUrl)
                    }
                },
                enabled = !isProcessing,
                modifier = Modifier.testTag("save_profile_button"),
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
fun ReportContentDialog(
    onDismiss: () -> Unit,
    onSubmit: (reason: String, details: String) -> Unit,
) {
    val reasons = listOf(
        "spam" to R.string.report_reason_spam,
        "harassment" to R.string.report_reason_harassment,
        "inappropriate" to R.string.report_reason_inappropriate,
        "misinformation" to R.string.report_reason_misinformation,
        "other" to R.string.report_reason_other,
    )
    var selectedReason by remember { mutableStateOf("spam") }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.report_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                reasons.forEach { (key, labelRes) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { selectedReason = key }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedReason == key,
                            onClick = { selectedReason = key },
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(labelRes), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = details,
                    onValueChange = { if (it.length <= 300) details = it },
                    label = { Text(stringResource(R.string.report_details_hint)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(selectedReason, details) }) {
                Text(stringResource(R.string.report_submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
fun DeletePostConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.post_delete_confirm_title)) },
        text = { Text(stringResource(R.string.post_delete_confirm_body)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
fun BlockUserConfirmDialog(
    displayName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.block_confirm_title, displayName)) },
        text = { Text(stringResource(R.string.block_confirm_body)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
fun FollowListDialog(
    title: String,
    relations: List<FollowRelation>,
    showFollowers: Boolean,
    onDismiss: () -> Unit,
    onUserClick: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (relations.isEmpty()) {
                Text(
                    text = stringResource(R.string.profile_list_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(relations, key = { it.id.ifBlank { "${it.followerId}_${it.followingId}" } }) { rel ->
                        val uid = if (showFollowers) rel.followerId else rel.followingId
                        val name = if (showFollowers) rel.followerName else rel.followingName
                        val handle = if (showFollowers) rel.followerHandle else rel.followingHandle
                        val seed = if (showFollowers) rel.followerAvatarSeed else rel.followingAvatarSeed

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    onDismiss()
                                    onUserClick(uid)
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            UserAvatar(displayName = name, avatarSeed = seed, avatarDataUrl = null, size = 40.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "@$handle",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}
