package com.nuclearboy.api.deepseek

import com.nuclearboy.common.AppConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the 1M token context window budget.
 * Tracks allocations and triggers compression when needed.
 */
data class ContextBudget(
    val systemPrompt: Long = 0,
    val userProfile: Long = 0,
    val projectContext: Long = 0,
    val conversationHistory: Long = 0,
    val toolDefinitions: Long = 0,
    val attachedFiles: Long = 0,
) {
    val totalUsed: Long
        get() = systemPrompt + userProfile + projectContext +
                conversationHistory + toolDefinitions + attachedFiles

    val remaining: Long
        get() = (AppConstants.DEEPSEEK_CONTEXT_WINDOW - totalUsed).coerceAtLeast(0)

    val usagePercent: Double
        get() = totalUsed.toDouble() / AppConstants.DEEPSEEK_CONTEXT_WINDOW

    val warningLevel: ContextWarningLevel
        get() = when {
            totalUsed >= AppConstants.CONTEXT_WARNING_RED -> ContextWarningLevel.RED
            totalUsed >= AppConstants.CONTEXT_WARNING_YELLOW -> ContextWarningLevel.YELLOW
            totalUsed >= AppConstants.DEEPSEEK_CONTEXT_WINDOW * 0.3 -> ContextWarningLevel.GREEN
            else -> ContextWarningLevel.OK
        }
}

enum class ContextWarningLevel(val label: String, val colorHex: Long) {
    OK("正常", 0xFF4CAF50),
    GREEN("良好", 0xFF4CAF50),
    YELLOW("注意", 0xFFFFC107),
    RED("危险", 0xFFFF5252),
}

class ContextWindowManager {

    private val _budget = MutableStateFlow(ContextBudget())
    val budget: StateFlow<ContextBudget> = _budget.asStateFlow()

    private var conversationTurnCount = 0

    /**
     * Check if there's enough room in the context window for additional tokens.
     */
    fun canFit(additionalTokens: Long): Boolean {
        return _budget.value.remaining >= additionalTokens
    }

    /**
     * Check if compression is needed based on current budget.
     */
    fun needsCompression(): Boolean {
        return _budget.value.totalUsed >= AppConstants.CONTEXT_WARNING_YELLOW
    }

    /**
     * Check if compression is urgent (red zone).
     */
    fun needsUrgentCompression(): Boolean {
        return _budget.value.totalUsed >= AppConstants.CONTEXT_WARNING_RED
    }

    /**
     * Update budget allocation for a specific category.
     */
    fun updateAllocation(
        systemPrompt: Long? = null,
        userProfile: Long? = null,
        projectContext: Long? = null,
        conversationHistory: Long? = null,
        toolDefinitions: Long? = null,
        attachedFiles: Long? = null,
    ) {
        _budget.value = _budget.value.copy(
            systemPrompt = systemPrompt ?: _budget.value.systemPrompt,
            userProfile = userProfile ?: _budget.value.userProfile,
            projectContext = projectContext ?: _budget.value.projectContext,
            conversationHistory = conversationHistory ?: _budget.value.conversationHistory,
            toolDefinitions = toolDefinitions ?: _budget.value.toolDefinitions,
            attachedFiles = attachedFiles ?: _budget.value.attachedFiles,
        )
    }

    /**
     * Estimate token count for a string. Rough heuristic: ~4 chars per token.
     */
    fun estimateTokens(text: String): Long {
        // GPT/DeepSeek tokenizer averages ~3-4 characters per token for Chinese+English mixed text
        return (text.length / 3.5).toLong().coerceAtLeast(1)
    }

    /**
     * Reset the context window for a new conversation.
     */
    fun reset() {
        _budget.value = ContextBudget()
        conversationTurnCount = 0
    }

    fun incrementTurn() {
        conversationTurnCount++
    }
}
