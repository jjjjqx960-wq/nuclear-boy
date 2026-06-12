package com.nuclearboy.common

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 远程电脑权限审批总线。
 *
 * 电脑端 claude 触发权限请求时，[PcBridgeClient] 经此总线把请求推给聊天界面，
 * 界面弹窗让用户批准/拒绝，再通过 [PermissionRequest.decision] 回传决定。
 *
 * 用 CompletableDeferred 而非另一条 Flow，保证一问一答精确配对，不会串台。
 */
object PermissionPromptBus {

    data class PermissionRequest(
        val taskId: String,
        val toolName: String,
        val inputSummary: String,
        val decision: CompletableDeferred<Boolean> = CompletableDeferred(),
    )

    private val _requests = MutableSharedFlow<PermissionRequest>(
        extraBufferCapacity = 16,
    )
    val requests: SharedFlow<PermissionRequest> = _requests.asSharedFlow()

    /**
     * 发起审批并挂起等待界面决定。无人订阅（聊天界面不在前台）时返回 false（保守拒绝）。
     */
    suspend fun requestApproval(taskId: String, toolName: String, inputSummary: String): Boolean {
        val request = PermissionRequest(taskId, toolName, inputSummary)
        val delivered = _requests.tryEmit(request)
        if (!delivered) return false
        return request.decision.await()
    }
}
