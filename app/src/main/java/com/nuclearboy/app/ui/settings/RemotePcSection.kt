package com.nuclearboy.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuclearboy.remotepc.PcBridgeConfigStore

/**
 * 设置页 · 远程电脑区块。
 *
 * 配置手机连电脑 nb-pc-bridge 的开关、地址和 token，
 * 并提供测试连接入口显示电脑主机名和可用 CLI。
 */
@Composable
fun RemotePcSection(
    config: PcBridgeConfigStore.PcBridgeConfig,
    testState: PcBridgeTestUiState,
    onEnabledChange: (Boolean) -> Unit,
    onSave: (url: String, token: String?) -> Unit,
    onTest: (url: String, token: String?) -> Unit,
) {
    Text(
        "🖥️ 远程电脑",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("让我控制电脑上的编程 CLI", fontWeight = FontWeight.Bold)
                    Text(
                        "把任务下发给电脑上的 Claude Code / Codex 执行",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = config.enabled, onCheckedChange = onEnabledChange)
            }

            if (config.enabled) {
                Spacer(Modifier.height(12.dp))

                var urlInput by remember(config.url) { mutableStateOf(config.url) }
                var tokenInput by remember { mutableStateOf("") }

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("电脑地址") },
                    placeholder = { Text("ws://192.168.1.10:7860") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = {
                        Text(if (config.hasToken) "Token（已保存 ${config.tokenMasked}，留空保持不变）" else "Token")
                    },
                    placeholder = { Text("电脑端 bridge.py init 生成的 token") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onSave(urlInput.trim(), tokenInput.trim().ifBlank { null }) },
                        enabled = urlInput.isNotBlank() && (tokenInput.isNotBlank() || config.hasToken),
                    ) { Text("保存") }
                    OutlinedButton(
                        onClick = { onTest(urlInput.trim(), tokenInput.trim().ifBlank { null }) },
                        enabled = !testState.inProgress &&
                            urlInput.isNotBlank() && (tokenInput.isNotBlank() || config.hasToken),
                    ) {
                        if (testState.inProgress) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(if (testState.inProgress) "连接中…" else "测试连接")
                    }
                }

                if (testState.message.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        testState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (testState.success) {
                            true -> Color(0xFF00E676)
                            false -> MaterialTheme.colorScheme.error
                            null -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        lineHeight = 18.sp,
                    )
                }

                if (config.lastConnectedHost.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "上次连上：${config.lastConnectedHost}（${config.lastConnectedClis}）",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "电脑端先运行 nb-pc-bridge：python bridge.py init 生成 token，python bridge.py serve 启动服务。手机和电脑要在同一网络（WiFi 或 USB 共享网络）。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}
