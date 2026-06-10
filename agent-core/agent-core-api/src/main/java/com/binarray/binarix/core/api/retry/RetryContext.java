package com.binarray.binarix.core.api.retry;

/**
 * Immutable configuration record governing retry behaviour for Claude API calls.
 * <p>
 * Carried inside {@link com.binarray.binarix.core.api.agent.AgentContext} and consulted
 * by {@link RetryPolicy} implementations. Use {@link #defaultContext()} for standard
 * production settings, or construct a custom instance for testing or special workloads.
 * </p>
 *
 * @param maxAttempts  the maximum number of total attempts (including the first try)
 * @param baseDelayMs  the initial delay in milliseconds before the first retry;
 *                     subsequent delays are doubled on each attempt (exponential backoff)
 *
 * @author Ashesh
 */
public record RetryContext(int maxAttempts, long baseDelayMs) {

    /**
     * Returns a {@code RetryContext} with sensible production defaults:
     * 3 total attempts with a 2-second base delay.
     *
     * @return the default retry context
     */
    public static RetryContext defaultContext() { return new RetryContext(3, 2000L); }
}
