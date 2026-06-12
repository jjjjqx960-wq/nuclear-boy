package com.nuclearboy.app.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuclearboy.remotepc.TerminalAnsi

/**
 * 远程终端界面：手机上直接操作电脑的 ConPTY 终端。
 *
 * MVP：输出剥 ANSI 后以等宽文本展示，输入栏发整行命令，并提供 Ctrl-C/Tab/Enter
 * 等常用键。完整 xterm 渲染（颜色、全屏 TUI）后续增强。
 */
/** 把屏幕缓冲渲染出的按行 span 转成带颜色/加粗的 AnnotatedString。 */
private fun screenToAnnotated(lines: List<List<TerminalAnsi.Span>>): AnnotatedString {
    if (lines.isEmpty()) return AnnotatedString("(暂无输出)")
    return buildAnnotatedString {
        lines.forEachIndexed { idx, spans ->
            for (span in spans) {
                val style = SpanStyle(
                    color = span.fgArgb?.let { Color(it) } ?: Color.Unspecified,
                    fontWeight = if (span.bold) FontWeight.Bold else null,
                )
                withStyle(style) { append(span.text) }
            }
            if (idx < lines.size - 1) append('\n')
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    onNavigateBack: () -> Unit,
    cwd: String? = null,
    viewModel: TerminalViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.start(cwd) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🖥️ 远程终端") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.restart(cwd) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "重连")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // 状态条
            val statusText = when (state.status) {
                TerminalViewModel.Status.IDLE -> "准备中…"
                TerminalViewModel.Status.CONNECTING -> "连接中…"
                TerminalViewModel.Status.READY -> "● 已连接"
                TerminalViewModel.Status.EXITED -> state.message.ifBlank { "终端已退出" }
                TerminalViewModel.Status.FAILED -> "✕ ${state.message}"
            }
            Text(
                statusText,
                color = when (state.status) {
                    TerminalViewModel.Status.READY -> Color(0xFF00E676)
                    TerminalViewModel.Status.FAILED -> Color(0xFFFF6E6E)
                    else -> Color(0xFFB0B0B0)
                },
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )

            // 输出区（黑底等宽，自动滚到底，80 列宽允许横向滚动）
            val scroll = rememberScrollState()
            val hScroll = rememberScrollState()
            LaunchedEffect(state.lines) { scroll.scrollTo(scroll.maxValue) }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0C0C0C))
                    .verticalScroll(scroll)
                    .horizontalScroll(hScroll)
                    .padding(8.dp),
            ) {
                val rendered = remember(state.lines) { screenToAnnotated(state.lines) }
                Text(
                    text = rendered,
                    color = Color(0xFFD0D0D0),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    softWrap = false,
                )
            }

            // 快捷键行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedButton(onClick = { viewModel.sendRaw("\u0003") }) { Text("Ctrl-C", fontSize = 12.sp) }
                OutlinedButton(onClick = { viewModel.sendRaw("\t") }) { Text("Tab", fontSize = 12.sp) }
                OutlinedButton(onClick = { viewModel.sendRaw("\r") }) { Text("Enter", fontSize = 12.sp) }
                OutlinedButton(onClick = { viewModel.sendRaw("\u001B") }) { Text("Esc", fontSize = 12.sp) }
            }

            // 输入行
            var input by remember { mutableStateOf("") }
            val canType = state.status == TerminalViewModel.Status.READY
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    enabled = canType,
                    singleLine = true,
                    placeholder = { Text("输入命令，回车发送") },
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (input.isNotEmpty()) {
                            viewModel.sendCommand(input)
                            input = ""
                        } else {
                            viewModel.sendRaw("\r")
                        }
                    },
                    enabled = canType,
                ) { Text("发送") }
            }
        }
    }
}
