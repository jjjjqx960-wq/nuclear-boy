package com.nuclearboy.ui.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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

@Composable
internal fun FileSelectionActionBar(
    selectedCount: Int,
    selectedVisibleCount: Int,
    selectedSizeLabel: String,
    visibleFileCount: Int,
    showSelectedOnly: Boolean,
    onSelectVisible: () -> Unit,
    onUnselectVisible: () -> Unit,
    onShowSelectedOnlyChange: (Boolean) -> Unit,
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
        val canUnselectVisible = hasSelection && selectedVisibleCount > 0 && !showSelectedOnly
        if (hasSelection) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                FileSelectionStatusText(
                    text = "已选 $selectedCount 个 · $selectedSizeLabel",
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
                            text = if (showSelectedOnly) "全部" else "只看",
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
                            contentDescription = "清空选择",
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
                                contentDescription = "取消当前可见选择",
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
                                text = "全选",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    FileSelectionReferenceButton(
                        enabled = true,
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
                    text = "可选 $visibleFileCount 个",
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
                        text = "全选可见",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                FileSelectionReferenceButton(
                    enabled = false,
                    onClick = onReferenceSelected,
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
    onClick: () -> Unit,
) {
    val nc = NuclearBoyTheme.colorScheme
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.height(30.dp),
        shape = RoundedCornerShape(7.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = nc.material.primary),
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "批量引用到输入",
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "引用",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}
