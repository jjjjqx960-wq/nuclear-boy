package com.nuclearboy.app.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuclearboy.remotepc.PcBridgeConfigStore
import com.nuclearboy.remotepc.PcTerminalSession
import com.nuclearboy.remotepc.TerminalAnsi
import com.nuclearboy.remotepc.TerminalEmulator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 远程终端界面的状态机：用 [PcTerminalSession] 连电脑开 ConPTY，输出喂进
 * [TerminalEmulator] 屏幕缓冲（支持光标定位/擦除/滚动/颜色/备用屏，能跑全屏 TUI），
 * 渲染成按行的彩色 span 给界面。属于"工具调用 + 远程终端界面"里的远程终端形态。
 */
@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val configStore: PcBridgeConfigStore,
) : ViewModel() {

    enum class Status { IDLE, CONNECTING, READY, EXITED, FAILED }

    data class UiState(
        val status: Status = Status.IDLE,
        val lines: List<List<TerminalAnsi.Span>> = emptyList(),
        val message: String = "",
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var session: PcTerminalSession? = null
    private var collectJob: Job? = null
    private var emulator = TerminalEmulator(COLS, ROWS)

    val isConfigured: Boolean get() = configStore.isConfigured()

    fun start(cwd: String? = null) {
        if (_state.value.status == Status.CONNECTING || _state.value.status == Status.READY) return
        val url = configStore.currentUrl()
        val token = configStore.currentToken()
        if (url.isBlank() || token.isBlank()) {
            _state.value = UiState(status = Status.FAILED, message = "还没配置远程电脑，去设置页填地址和 token")
            return
        }
        emulator = TerminalEmulator(COLS, ROWS)
        _state.value = UiState(status = Status.CONNECTING, message = "正在连接电脑…")
        val newSession = PcTerminalSession(
            url = url, token = token, cwd = cwd?.takeIf { it.isNotBlank() },
            cols = COLS, rows = ROWS,
        )
        session = newSession
        collectJob = viewModelScope.launch {
            newSession.connect().collect { event ->
                when (event) {
                    is PcTerminalSession.TerminalEvent.Ready ->
                        _state.value = _state.value.copy(status = Status.READY, message = "")
                    is PcTerminalSession.TerminalEvent.Output -> appendOutput(event.data)
                    is PcTerminalSession.TerminalEvent.Exit ->
                        _state.value = _state.value.copy(
                            status = Status.EXITED, message = "终端已退出（code ${event.code}）"
                        )
                    is PcTerminalSession.TerminalEvent.Failed ->
                        _state.value = _state.value.copy(status = Status.FAILED, message = event.message)
                }
            }
        }
    }

    /** 发送一行命令（自动补回车）。 */
    fun sendCommand(line: String) {
        session?.input(line + "\r")
    }

    /** 发送原始键入（控制字符等，如 Ctrl-C）。 */
    fun sendRaw(data: String) {
        session?.input(data)
    }

    fun restart(cwd: String? = null) {
        stop()
        start(cwd)
    }

    private fun appendOutput(data: String) {
        emulator.feed(data)
        _state.value = _state.value.copy(lines = emulator.renderWithScrollback())
    }

    private fun stop() {
        collectJob?.cancel()
        collectJob = null
        session?.close()
        session = null
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }

    companion object {
        private const val COLS = 80
        private const val ROWS = 24
    }
}
