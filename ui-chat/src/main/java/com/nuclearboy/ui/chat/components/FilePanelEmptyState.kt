package com.nuclearboy.ui.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuclearboy.ui.chat.NuclearBoyTheme

@Composable
internal fun FilePanelEmptyState(
    message: String,
    showClearFilter: Boolean,
    clearFilterContentDescription: String,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nc = NuclearBoyTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            ),
            color = nc.material.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (showClearFilter) {
            Spacer(Modifier.height(7.dp))
            OutlinedButton(
                onClick = onClearFilter,
                modifier = Modifier.height(30.dp),
                shape = RoundedCornerShape(7.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                border = BorderStroke(1.dp, nc.material.primary.copy(alpha = 0.35f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = nc.material.primary),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = clearFilterContentDescription,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "清除过滤",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
