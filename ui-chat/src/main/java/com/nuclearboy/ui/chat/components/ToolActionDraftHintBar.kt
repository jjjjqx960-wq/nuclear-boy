package com.nuclearboy.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
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
import com.nuclearboy.ui.chat.parts.ToolActionDraftHint

@Composable
internal fun ToolActionDraftHintBar(
    hint: ToolActionDraftHint,
    modifier: Modifier = Modifier,
) {
    val nc = NuclearBoyTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(nc.warning.copy(alpha = 0.08f))
            .border(1.dp, nc.warning.copy(alpha = 0.42f), shape)
            .semantics { contentDescription = hint.semantics }
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Default.Build,
            contentDescription = null,
            tint = nc.warning,
            modifier = Modifier.size(15.dp).padding(top = 1.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "${hint.title}：${hint.summary}",
            style = MaterialTheme.typography.labelSmall.copy(
                color = nc.material.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                lineHeight = 15.sp,
                fontSize = 11.sp,
            ),
        )
    }
}
