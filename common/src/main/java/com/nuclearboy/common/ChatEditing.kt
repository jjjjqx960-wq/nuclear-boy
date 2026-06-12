package com.nuclearboy.common

/**
 * 编辑重发的纯逻辑：把某条用户消息及其之后的所有消息截断掉，取回它的内容供重新编辑。
 *
 * 典型场景：用户发现刚才的提问写错了，想改一下重新问，而不是从头打一遍。截断后由
 * 调用方把内容回填输入框，用户改完再发。纯函数、无 Android 依赖，便于单测。
 */
object ChatEditing {

    data class EditResult(val content: String, val remaining: List<ChatMessage>)

    /**
     * @return 目标用户消息的内容 + 截断后的消息列表（不含该消息及其之后）；
     *         消息不存在或不是用户消息时返回 null。
     */
    fun prepareEdit(messages: List<ChatMessage>, messageId: String): EditResult? {
        val index = messages.indexOfFirst { it.id == messageId }
        if (index < 0) return null
        val target = messages[index]
        if (target.role != MessageRole.USER) return null
        return EditResult(content = target.content, remaining = messages.subList(0, index).toList())
    }

    /** 删除指定 id 的单条消息，返回新列表（原列表不变）。id 不存在则原样返回。 */
    fun removeMessage(messages: List<ChatMessage>, messageId: String): List<ChatMessage> =
        messages.filterNot { it.id == messageId }
}
