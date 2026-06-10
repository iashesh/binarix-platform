package com.binarray.binarix.core.api.agent;

/**
 * Root contract for every agent in the Binarix Platform.
 * <p>
 * An agent is a focused, single-responsibility AI component that accepts a typed input,
 * orchestrates one or more Claude API calls (optionally with tool use), and returns
 * a typed output. Implementations should be stateless and idempotent for the same
 * {@link AgentContext}.
 * </p>
 *
 * <p>Extend {@code AbstractAgent} rather than implementing this interface
 * directly. This interface exists purely as the public contract consumed by the
 * orchestration layer.</p>
 *
 * @param <I> the typed input — must implement {@link AgentInput}
 * @param <O> the typed output — must implement {@link AgentOutput}
 *
 * @author Ashesh
 */
public interface Agent<I extends AgentInput, O extends AgentOutput> {

    /**
     * Executes the agent with the given input and shared context.
     * <p>
     * Implementations drive the full Claude tool-call loop, collecting findings
     * and returning a typed result. The {@code ctx} carries the correlation ID,
     * token budget, and accumulated findings from previously-run agents in the
     * same pipeline.
     * </p>
     *
     * @param input the typed domain input for this agent
     * @param ctx   the shared, immutable agent context for this pipeline run
     * @return the typed domain output produced by this agent
     */
    O execute(I input, AgentContext ctx);

    /**
     * Returns the unique name of this agent.
     * <p>
     * This name is used as the key in {@link AgentContext#findings()}, in structured
     * log output, and in distributed trace span names. It must be stable across
     * deployments (e.g. {@code "log-analyst"}, {@code "rca-synthesizer"}).
     * </p>
     *
     * @return the agent name, never {@code null} or blank
     */
    String getName();

    /**
     * Returns the metadata governing how this agent calls LLM.
     * <p>
     * Metadata includes the model identifier, max token budget, temperature,
     * and system prompt. Each agent declares its own metadata so different agents
     * in the same pipeline can use different models or budgets.
     * </p>
     *
     * @return the agent metadata, never {@code null}
     */
    AgentMetadata getMetadata();
}
