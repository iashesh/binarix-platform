package com.binarray.binarix.core.impl.retry;

import com.binarray.binarix.core.api.exception.AgentException;
import com.binarray.binarix.core.api.retry.RetryContext;
import com.binarray.binarix.core.api.retry.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Default {@link RetryPolicy} implementation using exponential backoff.
 * <p>
 * Retries the given action up to {@link RetryContext#maxAttempts()} times.
 * The delay between attempts doubles on each retry starting from
 * {@link RetryContext#baseDelayMs()} milliseconds:
 * <pre>
 *   attempt 1 → immediate
 *   attempt 2 → baseDelayMs
 *   attempt 3 → baseDelayMs × 2
 *   attempt N → baseDelayMs × 2^(N-2)
 * </pre>
 * </p>
 *
 * <p>Any exception type triggers a retry. Non-retryable conditions (e.g. invalid API key)
 * will still be retried up to the limit; callers that require fast-fail on specific errors
 * should provide a custom {@link RetryPolicy} implementation.</p>
 *
 * @author Ashesh
 */
@Component
public class ExponentialBackoffRetryPolicy implements RetryPolicy {

    private static final Logger log = LoggerFactory.getLogger(ExponentialBackoffRetryPolicy.class);

    /**
     * {@inheritDoc}
     * <p>
     * Executes the action, sleeping between attempts with exponentially increasing delays.
     * If the calling thread is interrupted during a sleep, the interrupt flag is restored
     * and the retry loop exits.
     * </p>
     *
     * @param <T>    the return type of the action
     * @param action the operation to execute and potentially retry
     * @param ctx    retry configuration governing attempt count and base delay
     * @return the result of the action on the first successful attempt
     * @throws AgentException wrapping the last exception if all attempts are exhausted
     */
    @Override
    public <T> T execute(Supplier<T> action, RetryContext ctx) {
        int maxAttempts = ctx.maxAttempts();
        long baseDelay = ctx.baseDelayMs();
        Exception lastEx = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (Exception e) {
                lastEx = e;
                if (attempt < maxAttempts) {
                    long delay = baseDelay * (long) Math.pow(2, attempt - 1);
                    log.error("Attempt {}/{} failed, retrying in {}ms: {}", attempt, maxAttempts, delay, e.getMessage());
                    try { Thread.sleep(delay); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }
        throw new AgentException("All " + maxAttempts + " attempts failed", lastEx);
    }
}
