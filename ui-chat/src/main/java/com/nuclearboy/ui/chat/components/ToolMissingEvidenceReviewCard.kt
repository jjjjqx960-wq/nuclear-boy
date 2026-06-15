package com.nuclearboy.ui.chat.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nuclearboy.ui.chat.parts.ToolMissingEvidenceReviewNotice

@Composable
internal fun ToolMissingEvidenceReviewCard(
    notice: ToolMissingEvidenceReviewNotice,
    modifier: Modifier = Modifier,
) {
    WarningNoticeCard(
        title = notice.title,
        summary = notice.summary,
        actions = notice.actions,
        diagnosticLabel = notice.diagnosticLabel,
        verificationLabel = notice.verificationLabel,
        semantics = notice.semantics,
        modifier = modifier,
    )
}
