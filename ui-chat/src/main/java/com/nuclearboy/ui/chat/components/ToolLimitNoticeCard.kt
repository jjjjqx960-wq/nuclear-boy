package com.nuclearboy.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuclearboy.ui.chat.NuclearBoyTheme
import com.nuclearboy.ui.chat.parts.ToolLimitNotice

@Composable
internal fun ToolLimitNoticeCard(
    notice: ToolLimitNotice,
    modifier: Modifier = Modifier,
) {
    WarningNoticeCard(
        title = notice.title,
        summary = notice.summary,
        actions = notice.actions,
        semantics = notice.semantics,
        modifier = modifier,
    )
}

@Composable
internal fun WarningNoticeCard(
    title: String,
    summary: String,
    actions: List<String>,
    semantics: String,
    modifier: Modifier = Modifier,
) {
    val nc = NuclearBoyTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(nc.warning.copy(alpha = 0.10f))
            .border(1.dp, nc.warning.copy(alpha = 0.55f), shape)
            .semantics { contentDescription = semantics }
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = nc.warning,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = nc.warning,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                ),
            )
        }
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall.copy(
                color = nc.material.onSurface,
                lineHeight = 17.sp,
                fontSize = 12.sp,
            ),
            modifier = Modifier.padding(top = 5.dp),
        )
        actions.forEach { action ->
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = nc.warning,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = nc.material.onSurfaceVariant,
                        lineHeight = 16.sp,
                        fontSize = 11.sp,
                    ),
                )
            }
        }
    }
}
