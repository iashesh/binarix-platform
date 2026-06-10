package com.binarray.binarix.core.api.agent;

import com.binarray.binarix.core.api.retry.RetryContext;
import java.time.Instant;
import java.util.*;

/**
 * Immutable context record propagated through the entire agent call chain within a single pipeline run.
 * <p>
 * {@code AgentContext} is the shared carrier for cross-cutting concerns: tracing identifiers,
 * token budget, the original input, and the accumulated findings from every agent that has
 * already executed in the pipeline. It is never mutated — all modification methods return
 * a new instance via copy-on-write semantics.
 * </p>
 *
 * <p>Create a fresh context at the start of each pipeline run with {@link #create(AgentInput)},
 * then accumulate findings via {@link #withFinding(String, Object)} as each agent completes.</p>
 *
 * @param correlationId      UUID linking all log lines, spans, and events for this pipeline run
 * @param traceId            OpenTelemetry trace ID for distributed tracing propagation
 * @param startedAt          wall-clock timestamp when this pipeline run began
 * @param originalInput      the original typed input that triggered the pipeline
 * @param findings           immutable map of agent-name → output accumulated so far
 * @param remainingTokenBudget approximate token budget remaining across all agents in this run
 * @param retryContext       retry configuration for Claude API calls in this run
 *
 * @author Ashesh
 */
public record AgentContext(
        String correlationId,
        String traceId,
        Instant startedAt,
        AgentInput originalInput,
        Map<String, Object> findings,
        int remainingTokenBudget,
        RetryContext retryContext
) {
    public static AgentContext create(AgentInput input) {
        return new AgentContext(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                Instant.now(),
                input,
                Collections.emptyMap(),
                100_000,
                RetryContext.defaultContext()
        );
    }

    /**
     * Returns a new {@code AgentContext} with the given finding added to the findings map.
     * <p>
     * The original context is not modified. Typically called by the orchestrator after
     * each agent completes, using the agent's {@link Agent#getName()} as the key.
     * </p>
     *
     * @param key   the agent name or finding identifier
     * @param value the typed output produced by the agent
     * @return a new {@code AgentContext} containing all previous findings plus this one
     */
    public AgentContext withFinding(String key, Object value) {
        Map<String, Object> updated = new LinkedHashMap<>(findings);
        updated.put(key, value);
        return new AgentContext(correlationId, traceId, startedAt,
                originalInput, Collections.unmodifiableMap(updated),
                remainingTokenBudget, retryContext);
    }

    /**
     * Merges the findings from another {@code AgentContext} into this one.
     * <p>
     * Used by the parallel orchestration strategy to combine results from concurrently
     * executing agents. The remaining token budget is set to the minimum of the two contexts.
     * Entries from {@code other} overwrite entries in {@code this} for duplicate keys.
     * </p>
     *
     * @param other the context whose findings should be merged in
     * @return a new {@code AgentContext} containing the union of both finding maps
     */
    public AgentContext merge(AgentContext other) {
        Map<String, Object> merged = new LinkedHashMap<>(findings);
        merged.putAll(other.findings());
        return new AgentContext(correlationId, traceId, startedAt,
                originalInput, Collections.unmodifiableMap(merged),
                Math.min(remainingTokenBudget, other.remainingTokenBudget()),
                retryContext);
    }
}
