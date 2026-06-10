package com.binarray.binarix.core.api.retry;

import java.util.function.Supplier;

/**
 * Strategy interface for executing an action with configurable retry behaviour.
 * <p>
 * The Binarix Platform ships {@code ExponentialBackoffRetryPolicy} as the default
 * implementation, which retries on any exception using exponential backoff up to
 * {@link RetryContext#maxAttempts()} total attempts.
 * </p>
 *
 * <p>Custom implementations can be registered as Spring beans to override the default.</p>
 *
 * @author Ashesh
 */
public interface RetryPolicy {

    /**
     * Executes the given action, retrying on failure according to the supplied context.
     * <p>
     * The action is called up to {@link RetryContext#maxAttempts()} times. If all attempts
     * fail, an {@link com.binarray.binarix.core.api.exception.AgentException} is thrown
     * wrapping the last exception encountered.
     * </p>
     *
     * @param <T>    the return type of the action
     * @param action the operation to execute and potentially retry
     * @param ctx    retry configuration including max attempts and base delay
     * @return the result of the action on the first successful attempt
     * @throws com.binarray.binarix.core.api.exception.AgentException if all attempts are exhausted
     */
    <T> T execute(Supplier<T> action, RetryContext ctx);
}
