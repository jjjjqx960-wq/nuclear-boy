package com.nuclearboy.ui.chat.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuclearboy.ui.chat.NuclearBoyTheme

@Composable
internal fun FilePanelSearchField(
    query: String,
    resultSummary: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nc = NuclearBoyTheme.colorScheme
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth().heightIn(min = 44.dp),
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "过滤文件",
                modifier = Modifier.size(16.dp),
                tint = nc.material.onSurfaceVariant,
            )
        },
        trailingIcon = {
            if (query.isBlank()) {
                Text(
                    text = resultSummary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = nc.material.onSurfaceVariant.copy(alpha = 0.75f),
                )
            } else {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "清除过滤",
                        modifier = Modifier.size(15.dp),
                        tint = nc.material.onSurfaceVariant,
                    )
                }
            }
        },
        placeholder = {
            Text(
                text = "过滤文件",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = nc.material.onSurfaceVariant.copy(alpha = 0.5f),
            )
        },
        textStyle = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = nc.material.onSurface,
        ),
        shape = RoundedCornerShape(7.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = nc.material.surfaceVariant.copy(alpha = 0.25f),
            unfocusedContainerColor = Color.Transparent,
            focusedBorderColor = nc.material.primary.copy(alpha = 0.55f),
            unfocusedBorderColor = nc.material.outline.copy(alpha = 0.45f),
            cursorColor = nc.material.primary,
        ),
    )
}
