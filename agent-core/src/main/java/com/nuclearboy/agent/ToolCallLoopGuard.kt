package com.nuclearboy.agent

import com.nuclearboy.api.deepseek.ToolCallDto

/**
 * Detects model loops where the same batch of tool calls is requested repeatedly.
 *
 * This is intentionally not a general tool-iteration cap. Long agent runs can keep
 * going as long as each step makes visible progress; only exact consecutive repeats
 * are stopped.
 */
internal class ToolCallLoopGuard(
    private val maxConsecutiveDuplicateBatches: Int = 3,
) {
    private var lastSignature: String? = null
    private var consecutiveDuplicateBatches: Int = 0

    fun observe(toolCalls: List<ToolCallDto>): ToolCallLoopObservation {
        require(toolCalls.isNotEmpty()) { "toolCalls must not be empty" }
        val signature = toolCalls.signature()
        consecutiveDuplicateBatches = if (signature == lastSignature) {
            consecutiveDuplicateBatches + 1
        } else {
            lastSignature = signature
            1
        }
        return ToolCallLoopObservation(
            consecutiveDuplicateBatches = consecutiveDuplicateBatches,
            maxConsecutiveDuplicateBatches = maxConsecutiveDuplicateBatches,
            toolSummary = toolCalls.summary(),
        )
    }

    private fun List<ToolCallDto>.signature(): String =
        joinToString(separator = "\u001F") { call ->
            call.function.name.trim() + "\u001E" + call.function.arguments.trim()
        }

    private fun List<ToolCallDto>.summary(): String =
        joinToString(separator = ", ") { call ->
            val argsLen = call.function.arguments.length
            "${call.function.name}(argsLen=$argsLen)"
        }
}

internal data class ToolCallLoopObservation(
    val consecutiveDuplicateBatches: Int,
    val maxConsecutiveDuplicateBatches: Int,
    val toolSummary: String,
) {
    val shouldStop: Boolean
        get() = consecutiveDuplicateBatches >= maxConsecutiveDuplicateBatches
}
