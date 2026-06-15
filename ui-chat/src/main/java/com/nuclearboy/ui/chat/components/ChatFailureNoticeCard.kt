package com.nuclearboy.ui.chat.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nuclearboy.ui.chat.parts.ChatFailureNotice

@Composable
internal fun ChatFailureNoticeCard(
    notice: ChatFailureNotice,
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
