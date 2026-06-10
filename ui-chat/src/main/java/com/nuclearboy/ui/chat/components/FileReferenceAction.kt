package com.nuclearboy.ui.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuclearboy.ui.chat.NuclearBoyTheme

@Composable
internal fun FileReferenceIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nc = NuclearBoyTheme.colorScheme
    IconButton(
        onClick = onClick,
        modifier = modifier.size(28.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "引用到输入",
            modifier = Modifier.size(15.dp),
            tint = nc.material.primary,
        )
    }
}

@Composable
internal fun FileReferenceTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nc = NuclearBoyTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(7.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        border = BorderStroke(1.dp, nc.material.primary.copy(alpha = 0.45f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = nc.material.primary),
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "引用到输入",
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "引用",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}
