package com.nuclearboy.ui.chat.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuclearboy.ui.chat.NuclearBoyTheme
import com.nuclearboy.ui.chat.parts.FilePanelSortMode

@Composable
internal fun FilePanelSortBar(
    selectedMode: FilePanelSortMode,
    onModeSelected: (FilePanelSortMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilePanelSortMode.entries.forEach { mode ->
            FilePanelSortChip(
                mode = mode,
                selected = mode == selectedMode,
                onClick = { onModeSelected(mode) },
            )
        }
        Spacer(Modifier.width(2.dp))
    }
}

@Composable
private fun FilePanelSortChip(
    mode: FilePanelSortMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val nc = NuclearBoyTheme.colorScheme
    val contentColor = if (selected) nc.material.primary else nc.material.onSurfaceVariant
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = mode.label,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = mode.icon(),
                contentDescription = mode.label,
                modifier = Modifier.size(13.dp),
            )
        },
        modifier = Modifier.height(30.dp),
        shape = RoundedCornerShape(7.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = contentColor.copy(alpha = if (selected) 0.10f else 0.04f),
            labelColor = contentColor,
            iconColor = contentColor,
            selectedContainerColor = nc.material.primary.copy(alpha = 0.14f),
            selectedLabelColor = nc.material.primary,
            selectedLeadingIconColor = nc.material.primary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = contentColor.copy(alpha = 0.22f),
            selectedBorderColor = nc.material.primary.copy(alpha = 0.42f),
        ),
    )
}

private fun FilePanelSortMode.icon(): ImageVector {
    return when (this) {
        FilePanelSortMode.Name -> Icons.Default.SortByAlpha
        FilePanelSortMode.Type -> Icons.Default.Category
        FilePanelSortMode.Size -> Icons.Default.Storage
        FilePanelSortMode.Recent -> Icons.Default.Schedule
    }
}
