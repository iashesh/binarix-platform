package com.binarray.binarix.core.api.orchestration;

import com.binarray.binarix.core.api.agent.AgentContext;

/**
 * Pluggable strategy that determines how a set of {@link AgentNode}s within an
 * {@link AgentPipeline} are executed and how their results are combined.
 * <p>
 * The Binarix Platform ships three built-in strategies:
 * <ul>
 *   <li>{@code parallel} — all nodes run concurrently via virtual threads; results merged.</li>
 *   <li>{@code sequential} — nodes execute one after another; each receives the previous output.</li>
 *   <li>{@code graph} — nodes execute in topological order respecting conditional edges.</li>
 * </ul>
 * Custom strategies are registered as Spring beans and discovered automatically by
 * {@code OrchestratorService}.
 * </p>
 *
 * @author Ashesh
 */
public interface OrchestratorStrategy {

    /**
     * Executes all nodes in the given pipeline and returns an enriched {@link AgentContext}
     * containing the combined findings from every node that ran.
     *
     * @param pipeline the pipeline describing which agents to run and in what order
     * @param ctx      the current agent context carrying correlation ID, findings, and token budget
     * @return an updated {@link AgentContext} with findings from all executed nodes merged in
     */
    AgentContext execute(AgentPipeline pipeline, AgentContext ctx);

    /**
     * Returns the unique identifier for this strategy.
     * <p>
     * This ID is matched against the {@code agent.core.orchestration.strategy} property
     * and against {@link AgentPipeline#getStrategy()} at runtime. Must be lowercase and
     * stable across deployments (e.g. {@code "parallel"}, {@code "sequential"}).
     * </p>
     *
     * @return the strategy identifier string, never {@code null} or blank
     */
    String strategyId();
}
