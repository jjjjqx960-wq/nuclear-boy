package com.nuclearboy.ui.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuclearboy.ui.chat.NuclearBoyTheme
import com.nuclearboy.ui.chat.parts.fileSelectionClearActionDescription
import com.nuclearboy.ui.chat.parts.fileSelectionReferenceMatchedActionLabel
import com.nuclearboy.ui.chat.parts.fileSelectionReferenceSelectedActionLabel
import com.nuclearboy.ui.chat.parts.fileSelectionReferenceVisibleActionDescription
import com.nuclearboy.ui.chat.parts.fileSelectionReferenceVisibleActionLabel
import com.nuclearboy.ui.chat.parts.fileSelectionSelectedOnlyToggleLabel
import com.nuclearboy.ui.chat.parts.fileSelectionSelectVisibleActionLabel
import com.nuclearboy.ui.chat.parts.fileSelectionUnselectHiddenActionDescription
import com.nuclearboy.ui.chat.parts.fileSelectionUnselectVisibleActionDescription
import com.nuclearboy.ui.chat.parts.shouldShowReferenceMatchedAction
import com.nuclearboy.ui.chat.parts.shouldShowUnselectVisibleAction

@Composable
internal fun FileSelectionActionBar(
    selectedCount: Int,
    selectedVisibleCount: Int,
    statusLabel: String,
    visibleFileCount: Int,
    allVisibleFileCount: Int,
    showSelectedOnly: Boolean,
    hasFilterQuery: Boolean,
    onSelectVisible: () -> Unit,
    onUnselectVisible: () -> Unit,
    onUnselectHidden: () -> Unit,
    onShowSelectedOnlyChange: (Boolean) -> Unit,
    onReferenceVisible: () -> Unit,
    onReferenceVisibleFiles: () -> Unit,
    onReferenceSelected: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nc = NuclearBoyTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(7.dp),
        color = nc.material.primary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, nc.material.primary.copy(alpha = 0.28f)),
    ) {
        val hasSelection = selectedCount > 0
        val hiddenSelectedCount = (selectedCount - selectedVisibleCount).coerceAtLeast(0)
        val canUnselectVisible = shouldShowUnselectVisibleAction(
            selectedCount = selectedCount,
            selectedVisibleCount = selectedVisibleCount,
            showSelectedOnly = showSelectedOnly,
            hasFilterQuery = hasFilterQuery,
        )
        val canUnselectHidden = hasSelection && hiddenSelectedCount > 0 && !showSelectedOnly
        val canReferenceMatched = shouldShowReferenceMatchedAction(
            selectedCount = selectedCount,
            selectedVisibleCount = selectedVisibleCount,
            showSelectedOnly = showSelectedOnly,
            hasFilterQuery = hasFilterQuery,
        )
        if (hasSelection) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                FileSelectionStatusText(
                    text = statusLabel,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    OutlinedButton(
                        onClick = { onShowSelectedOnlyChange(!showSelectedOnly) },
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(7.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        border = BorderStroke(1.dp, nc.material.secondary.copy(alpha = 0.35f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = nc.material.secondary),
                    ) {
                        Text(
                            text = fileSelectionSelectedOnlyToggleLabel(
                                showSelectedOnly = showSelectedOnly,
                                selectedCount = selectedCount,
                                selectedVisibleCount = selectedVisibleCount,
                                allVisibleFileCount = allVisibleFileCount,
                                hasFilterQuery = hasFilterQuery,
                            ),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    IconButton(
                        onClick = onClearSelection,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = fileSelectionClearActionDescription(selectedCount),
                            modifier = Modifier.size(15.dp),
                            tint = nc.material.onSurfaceVariant,
                        )
                    }
                    if (canUnselectVisible) {
                        IconButton(
                            onClick = onUnselectVisible,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.RemoveCircleOutline,
                                contentDescription = fileSelectionUnselectVisibleActionDescription(
                                    selectedVisibleCount = selectedVisibleCount,
                                    showSelectedOnly = showSelectedOnly,
                                ),
                                modifier = Modifier.size(16.dp),
                                tint = nc.material.secondary,
                            )
                        }
                    }
                    if (canUnselectHidden) {
                        IconButton(
                            onClick = onUnselectHidden,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = fileSelectionUnselectHiddenActionDescription(
                                    hiddenSelectedCount = hiddenSelectedCount,
                                ),
                                modifier = Modifier.size(16.dp),
                                tint = nc.material.secondary,
                            )
                        }
                    }
                    if (selectedVisibleCount < visibleFileCount) {
                        OutlinedButton(
                            onClick = onSelectVisible,
                            modifier = Modifier.height(30.dp),
                            shape = RoundedCornerShape(7.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            border = BorderStroke(1.dp, nc.material.primary.copy(alpha = 0.35f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = nc.material.primary),
                        ) {
                            Text(
                                text = fileSelectionSelectVisibleActionLabel(
                                    selectedVisibleCount = selectedVisibleCount,
                                    visibleFileCount = visibleFileCount,
                                ),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (canReferenceMatched) {
                        FileSelectionReferenceButton(
                            enabled = true,
                            label = fileSelectionReferenceMatchedActionLabel(selectedVisibleCount),
                            contentDescription = "引用当前匹配选择到输入",
                            onClick = onReferenceVisible,
                            outlined = true,
                        )
                    }
                    FileSelectionReferenceButton(
                        enabled = true,
                        label = fileSelectionReferenceSelectedActionLabel(
                            selectedCount = selectedCount,
                            hasMatchedAction = canReferenceMatched,
                        ),
                        contentDescription = if (canReferenceMatched) {
                            "引用全部已选文件到输入"
                        } else {
                            "批量引用到输入"
                        },
                        onClick = onReferenceSelected,
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FileSelectionStatusText(
                    text = statusLabel,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = onSelectVisible,
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(7.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    border = BorderStroke(1.dp, nc.material.primary.copy(alpha = 0.35f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = nc.material.primary),
                ) {
                    Text(
                        text = fileSelectionSelectVisibleActionLabel(
                            selectedVisibleCount = selectedVisibleCount,
                            visibleFileCount = visibleFileCount,
                        ),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                FileSelectionReferenceButton(
                    enabled = visibleFileCount > 0,
                    label = fileSelectionReferenceVisibleActionLabel(visibleFileCount),
                    contentDescription = fileSelectionReferenceVisibleActionDescription(visibleFileCount),
                    onClick = onReferenceVisibleFiles,
                )
            }
        }
    }
}

@Composable
private fun FileSelectionStatusText(
    text: String,
    modifier: Modifier = Modifier,
) {
    val nc = NuclearBoyTheme.colorScheme
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = nc.material.primary,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun FileSelectionReferenceButton(
    enabled: Boolean,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    outlined: Boolean = false,
) {
    val nc = NuclearBoyTheme.colorScheme
    val content: @Composable RowScope.() -> Unit = {
        FileSelectionReferenceButtonContent(
            label = label,
            contentDescription = contentDescription,
        )
    }
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.height(30.dp),
            shape = RoundedCornerShape(7.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            border = BorderStroke(1.dp, nc.material.primary.copy(alpha = 0.35f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = nc.material.primary),
            content = content,
        )
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.height(30.dp),
            shape = RoundedCornerShape(7.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = nc.material.primary),
            content = content,
        )
    }
}

@Composable
private fun FileSelectionReferenceButtonContent(
    label: String,
    contentDescription: String,
) {
    Icon(
        imageVector = Icons.Default.Add,
        contentDescription = contentDescription,
        modifier = Modifier.size(14.dp),
    )
    Spacer(Modifier.width(4.dp))
    Text(
        text = label,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
    )
}
