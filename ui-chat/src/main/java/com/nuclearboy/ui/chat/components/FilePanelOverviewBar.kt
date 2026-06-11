package com.nuclearboy.ui.chat.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuclearboy.ui.chat.NuclearBoyTheme
import com.nuclearboy.ui.chat.parts.FilePanelOverview
import com.nuclearboy.ui.chat.parts.filePanelOverviewPillDescription

@Composable
internal fun FilePanelOverviewBar(
    overview: FilePanelOverview,
    totalSizeLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilePanelOverviewPill(
            icon = Icons.Default.Folder,
            label = "目录",
            value = overview.directoryCount.toString(),
            emphasized = overview.directoryCount > 0,
        )
        FilePanelOverviewPill(
            icon = Icons.Default.Description,
            label = "文件",
            value = overview.fileCount.toString(),
            emphasized = overview.fileCount > 0,
        )
        FilePanelOverviewPill(
            icon = Icons.Default.Storage,
            label = "大小",
            value = totalSizeLabel,
            emphasized = overview.totalFileSizeBytes > 0L,
        )
        overview.typeCounts.forEach { typeCount ->
            FilePanelOverviewPill(
                icon = Icons.AutoMirrored.Filled.Label,
                label = typeCount.label,
                value = typeCount.count.toString(),
                emphasized = false,
            )
        }
        Spacer(Modifier.width(2.dp))
    }
}

@Composable
private fun FilePanelOverviewPill(
    icon: ImageVector,
    label: String,
    value: String,
    emphasized: Boolean,
) {
    val nc = NuclearBoyTheme.colorScheme
    val contentColor = if (emphasized) nc.material.primary else nc.material.onSurfaceVariant
    Surface(
        modifier = Modifier.height(28.dp),
        shape = RoundedCornerShape(7.dp),
        color = contentColor.copy(alpha = if (emphasized) 0.10f else 0.06f),
        border = BorderStroke(1.dp, contentColor.copy(alpha = if (emphasized) 0.32f else 0.18f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = filePanelOverviewPillDescription(label, value),
                modifier = Modifier.size(13.dp),
                tint = contentColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = contentColor,
                ),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = contentColor,
                ),
            )
        }
    }
}
