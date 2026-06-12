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
import androidx.activity.compose.rememberLauncherForActivityResult
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
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.nuclearboy.remotepc.PcBridgeClient
import com.nuclearboy.remotepc.PcBridgeConfigStore
import com.nuclearboy.remotepc.PcPairingPayload

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
    runningTasks: List<PcBridgeClient.RunningTask>?,
    onEnabledChange: (Boolean) -> Unit,
    onSave: (url: String, token: String?) -> Unit,
    onTest: (url: String, token: String?) -> Unit,
    onLoadTasks: () -> Unit,
    onOpenTerminal: () -> Unit = {},
    onEncryptionChange: (Boolean) -> Unit = {},
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
                var scanMessage by remember { mutableStateOf("") }

                val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
                    val contents = result.contents
                    when {
                        contents == null -> scanMessage = ""  // 用户取消
                        else -> {
                            val payload = PcPairingPayload.parse(contents)
                            if (payload == null) {
                                scanMessage = "这个二维码不是核弹男孩配对码 🤔 请扫电脑端 bridge.py pair 生成的码"
                            } else {
                                urlInput = payload.url
                                tokenInput = payload.token
                                scanMessage = "扫到了，正在保存… ✨"
                                onSave(payload.url, payload.token)
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        scanMessage = ""
                        scanLauncher.launch(
                            ScanOptions()
                                .setOrientationLocked(false)
                                .setBeepEnabled(false)
                                .setPrompt("对准电脑屏幕上 bridge.py pair 的二维码")
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("📷 扫码配对（推荐）") }

                if (scanMessage.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        scanMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "或手动填写：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))

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

                if (config.hasToken && config.url.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onLoadTasks) { Text("查看电脑任务") }
                        OutlinedButton(onClick = onOpenTerminal) { Text("🖥️ 远程终端") }
                    }
                    if (runningTasks != null) {
                        Spacer(Modifier.height(4.dp))
                        if (runningTasks.isEmpty()) {
                            Text(
                                "电脑当前没有正在执行的远程任务",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            runningTasks.forEach { task ->
                                Text(
                                    "· [${task.cli}] ${task.elapsedMs / 1000}s — ${task.promptPreview}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
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
                    "电脑端先运行 nb-pc-bridge：python bridge.py init 生成 token，python bridge.py serve 启动服务。手机和电脑要在同一网络（WiFi 或 USB 共享网络）。配对最省事：电脑端 python bridge.py pair 打印二维码，手机点上面「扫码配对」对准即可。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                )

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("端到端加密", fontWeight = FontWeight.Bold)
                        Text(
                            "走公网中继时，中继也看不到任务内容和 token（AES-256-GCM）。开启前请先更新并重启电脑端 bridge。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp,
                        )
                    }
                    Switch(checked = config.encrypted, onCheckedChange = onEncryptionChange)
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "外网控制：在公网服务器跑 python relay/relay_server.py --port 8970 --key 口令，电脑端加 --relay ws://服务器IP:8970 --relay-key 口令 反连，手机这里填 ws://服务器IP:8970/client/<room>?key=口令（room 即电脑端 serve 显示的 room）。token 仍端到端校验，开启上面的加密后中继连密文都看不懂。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}
