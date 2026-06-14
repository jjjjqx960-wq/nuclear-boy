package com.nuclearboy.agent

import com.nuclearboy.common.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetryableErrorGateTest {

    @Test
    fun `retryable errors retry until limit then become final`() {
        val gate = RetryableErrorGate(maxConsecutiveRetryableErrors = 3)

        assertRetry(1, gate.classify(AppError.NetworkTimeout))
        assertRetry(2, gate.classify(AppError.NetworkTimeout))
        assertRetry(3, gate.classify(AppError.NetworkTimeout))
        assertTrue(gate.classify(AppError.NetworkTimeout) is RetryDecision.FinalError)
    }

    @Test
    fun `successful api round resets retry count`() {
        val gate = RetryableErrorGate(maxConsecutiveRetryableErrors = 3)

        assertRetry(1, gate.classify(AppError.ServerError))
        assertRetry(2, gate.classify(AppError.ServerError))
        gate.onSuccessfulApiRound()

        assertRetry(1, gate.classify(AppError.ServerError))
    }

    @Test
    fun `non retryable error is immediately final`() {
        val gate = RetryableErrorGate()

        assertTrue(gate.classify(AppError.ApiKeyInvalid) is RetryDecision.FinalError)
    }

    private fun assertRetry(expectedAttempt: Int, decision: RetryDecision) {
        assertTrue(decision is RetryDecision.Retry)
        val retry = decision as RetryDecision.Retry
        assertEquals(expectedAttempt, retry.attempt)
    }
}
