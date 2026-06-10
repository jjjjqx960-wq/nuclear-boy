package com.nuclearboy.ui.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuclearboy.ui.chat.NuclearBoyTheme
import com.nuclearboy.ui.chat.parts.ChatCommandKind
import com.nuclearboy.ui.chat.parts.ChatCommandTemplate
import com.nuclearboy.ui.chat.parts.chatCommandTemplates

@Composable
internal fun CommandShortcutBar(
    isProcessing: Boolean,
    hasMessages: Boolean,
    onCommandSelected: (ChatCommandTemplate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nc = NuclearBoyTheme.colorScheme
    val commands = chatCommandTemplates(
        isProcessing = isProcessing,
        hasMessages = hasMessages,
    )

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        commands.forEach { command ->
            AssistChip(
                onClick = { onCommandSelected(command) },
                label = {
                    Text(
                        text = command.label,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = command.icon(),
                        contentDescription = command.label,
                        modifier = Modifier.size(14.dp),
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = command.containerColor(),
                    labelColor = command.contentColor(),
                    leadingIconContentColor = command.contentColor(),
                ),
                border = BorderStroke(1.dp, command.contentColor().copy(alpha = 0.35f)),
                shape = RoundedCornerShape(7.dp),
                modifier = Modifier.height(32.dp),
            )
        }
        Spacer(
            modifier = Modifier
                .size(width = 2.dp, height = 1.dp)
                .background(Color.Transparent),
        )
    }
}

@Composable
private fun ChatCommandTemplate.contentColor(): Color {
    val nc = NuclearBoyTheme.colorScheme
    return when (kind) {
        ChatCommandKind.Stop -> nc.material.error
        ChatCommandKind.Loop -> nc.warning
        ChatCommandKind.Model -> nc.material.secondary
        else -> nc.material.primary
    }
}

@Composable
private fun ChatCommandTemplate.containerColor(): Color {
    val base = contentColor()
    return base.copy(alpha = if (kind == ChatCommandKind.Stop) 0.13f else 0.08f)
}

private fun ChatCommandTemplate.icon(): ImageVector {
    return when (kind) {
        ChatCommandKind.Goal -> Icons.Default.CenterFocusStrong
        ChatCommandKind.Loop -> Icons.AutoMirrored.Filled.PlaylistPlay
        ChatCommandKind.Compact -> Icons.Default.Storage
        ChatCommandKind.Rewind -> Icons.Default.History
        ChatCommandKind.Model -> Icons.Default.AutoAwesome
        ChatCommandKind.Stop -> Icons.Default.Close
    }
}
