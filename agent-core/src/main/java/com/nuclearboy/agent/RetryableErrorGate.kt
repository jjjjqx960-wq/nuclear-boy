package com.nuclearboy.agent

import com.nuclearboy.common.AppError

/**
 * Tracks consecutive retryable API errors for one agent run.
 *
 * A retryable error should be rendered as transient "retrying" state first.
 * Only the first non-retryable error, or a retryable error after the limit is
 * exhausted, should become a user-facing final error.
 */
internal class RetryableErrorGate(
    private val maxConsecutiveRetryableErrors: Int = 3,
) {
    private var consecutiveRetryableErrors = 0

    fun onSuccessfulApiRound() {
        consecutiveRetryableErrors = 0
    }

    fun classify(error: AppError): RetryDecision {
        if (!error.isRetryable) {
            return RetryDecision.FinalError
        }
        if (consecutiveRetryableErrors < maxConsecutiveRetryableErrors) {
            consecutiveRetryableErrors++
            return RetryDecision.Retry(
                attempt = consecutiveRetryableErrors,
                maxAttempts = maxConsecutiveRetryableErrors,
            )
        }
        return RetryDecision.FinalError
    }
}

internal sealed class RetryDecision {
    data class Retry(
        val attempt: Int,
        val maxAttempts: Int,
    ) : RetryDecision()

    data object FinalError : RetryDecision()
}
