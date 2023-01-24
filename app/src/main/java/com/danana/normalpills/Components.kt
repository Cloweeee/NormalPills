package com.danana.normalpills

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.danana.normalpills.ui.theme.applyOpacity
import com.danana.normalpills.ui.theme.preferenceTitle

@Composable
internal fun PreferenceItemTitle(
    modifier: Modifier = Modifier,
    text: String,
    maxLines: Int = 2,
    style: TextStyle = preferenceTitle,
    enabled: Boolean,
    color: Color = MaterialTheme.colorScheme.onBackground,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    Text(
        modifier = modifier,
        text = text,
        maxLines = maxLines,
        style = style,
        color = color.applyOpacity(enabled),
        overflow = overflow
    )
}

@Composable
internal fun PreferenceItemDescription(
    modifier: Modifier = Modifier,
    text: String,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    enabled: Boolean,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    Text(
        modifier = modifier.padding(top = 2.dp),
        text = text,
        maxLines = maxLines,
        style = style,
        color = color.applyOpacity(enabled),
        overflow = overflow
    )
}

@Composable
fun PreferenceSwitch(
    title: String = "test title".repeat(10),
    description: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isChecked: Boolean = true,
    checkedIcon: ImageVector = Icons.Outlined.Check,
    onClick: (() -> Unit) = {},
) {
    val thumbContent: (@Composable () -> Unit)? = if (isChecked) {
        {
            Icon(
                imageVector = checkedIcon,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        }
    } else {
        null
    }
    Surface(
        modifier = Modifier.toggleable(
            value = isChecked,
            enabled = enabled,
            onValueChange = { onClick() }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp, 16.dp)
                .padding(start = if (icon == null) 12.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 8.dp, end = 16.dp)
                        .size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.applyOpacity(enabled)
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                PreferenceItemTitle(
                    text = title,
                    enabled = enabled
                )
                if (!description.isNullOrEmpty())
                    PreferenceItemDescription(
                        text = description,
                        enabled = enabled
                    )
            }
            Switch(
                checked = isChecked,
                onCheckedChange = null,
                modifier = Modifier.padding(start = 20.dp, end = 6.dp),
                enabled = enabled, thumbContent = thumbContent
            )
        }
    }
}

@Composable
fun SettingItem(title: String, description: String, icon: ImageVector?, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp, 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 8.dp, end = 16.dp)
                        .size(24.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (icon == null) 12.dp else 0.dp)
            ) {
                Text(
                    text = title,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

// Preference elements are used from https://github.com/JunkFood02/Seal/blob/main/app/src/main/java/com/junkfood/seal/ui/component/PreferenceItems.kt