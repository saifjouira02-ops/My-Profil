package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BlockedUser
import com.example.model.UserPreferences
import com.example.ui.i18n.AppLanguages

@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    isProfilePrivate: Boolean,
    blockedUsers: List<BlockedUser>,
    userEmailOrName: String,
    onBack: () -> Unit,
    onLanguageChange: (String) -> Unit,
    onDarkModeChange: (String) -> Unit,
    onLargeTextChange: (Boolean) -> Unit,
    onHighContrastChange: (Boolean) -> Unit,
    onPrivateProfileChange: (Boolean) -> Unit,
    onShowDemoChange: (Boolean) -> Unit,
    onUnblockUser: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var showBlockedDialog by remember { mutableStateOf(false) }

    if (showBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedDialog = false },
            title = { Text(stringResource(R.string.blocked_users_title)) },
            text = {
                if (blockedUsers.isEmpty()) {
                    Text(stringResource(R.string.blocked_users_empty))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(blockedUsers, key = { it.blockedUserId }) { blocked ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(blocked.blockedDisplayName, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "@${blocked.blockedHandle}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(onClick = { onUnblockUser(blocked.blockedUserId) }) {
                                    Text(stringResource(R.string.action_unblock_user))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBlockedDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            },
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            // Language
            SettingsSectionCard(
                icon = Icons.Default.Language,
                title = stringResource(R.string.settings_section_language),
            ) {
                Text(
                    text = stringResource(R.string.settings_language_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                AppLanguages.supported.forEach { lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onLanguageChange(lang.code) }
                            .padding(vertical = 4.dp)
                            .testTag("lang_option_${lang.code}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = preferences.language == lang.code,
                            onClick = { onLanguageChange(lang.code) },
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(lang.nativeLabelRes))
                    }
                }
                Text(
                    text = stringResource(R.string.settings_language_extensible_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Appearance
            SettingsSectionCard(
                icon = Icons.Default.DarkMode,
                title = stringResource(R.string.settings_section_appearance),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "system" to R.string.settings_theme_system,
                        "light" to R.string.settings_theme_light,
                        "dark" to R.string.settings_theme_dark,
                    ).forEach { (mode, labelRes) ->
                        FilterChip(
                            selected = preferences.darkMode == mode,
                            onClick = { onDarkModeChange(mode) },
                            label = { Text(stringResource(labelRes)) },
                            modifier = Modifier.testTag("theme_chip_$mode"),
                        )
                    }
                }
            }

            // Accessibility
            SettingsSectionCard(
                icon = Icons.Default.AccessibilityNew,
                title = stringResource(R.string.settings_section_accessibility),
            ) {
                ToggleRow(
                    title = stringResource(R.string.settings_large_text),
                    subtitle = stringResource(R.string.settings_large_text_desc),
                    checked = preferences.largeText,
                    onCheckedChange = onLargeTextChange,
                    testTag = "switch_large_text",
                )
                HorizontalDivider()
                ToggleRow(
                    title = stringResource(R.string.settings_high_contrast),
                    subtitle = stringResource(R.string.settings_high_contrast_desc),
                    checked = preferences.highContrast,
                    onCheckedChange = onHighContrastChange,
                    testTag = "switch_high_contrast",
                )
            }

            // Privacy & Safety
            SettingsSectionCard(
                icon = Icons.Default.PrivacyTip,
                title = stringResource(R.string.settings_section_privacy),
            ) {
                ToggleRow(
                    title = stringResource(R.string.settings_private_profile),
                    subtitle = stringResource(R.string.settings_private_profile_desc),
                    checked = isProfilePrivate,
                    onCheckedChange = onPrivateProfileChange,
                    testTag = "switch_private_profile",
                )
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable { showBlockedDialog = true }
                        .padding(vertical = 8.dp)
                        .testTag("manage_blocked_button"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Block, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${stringResource(R.string.settings_blocked_accounts)} (${blockedUsers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Sample data
            SettingsSectionCard(
                icon = Icons.Default.Language,
                title = stringResource(R.string.settings_section_demo),
            ) {
                ToggleRow(
                    title = stringResource(R.string.settings_show_demo),
                    subtitle = stringResource(R.string.settings_show_demo_desc),
                    checked = preferences.showDemoContent,
                    onCheckedChange = onShowDemoChange,
                    testTag = "switch_demo_content",
                )
            }

            // Account
            SettingsSectionCard(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = stringResource(R.string.settings_section_account),
            ) {
                Text(
                    text = stringResource(R.string.settings_signed_in_as, userEmailOrName),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.settings_console_note),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onSignOut,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign_out_button"),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_sign_out))
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(text = title, style = MaterialTheme.typography.titleLarge)
            }
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
        )
    }
}
