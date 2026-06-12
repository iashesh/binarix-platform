package com.binarray.binarix.core.impl.orchestration;

import com.binarray.binarix.core.api.agent.Agent;
import com.binarray.binarix.core.api.agent.AgentContext;
import com.binarray.binarix.core.api.orchestration.AgentNode;
import com.binarray.binarix.core.api.orchestration.AgentPipeline;
import com.binarray.binarix.core.api.orchestration.OrchestratorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

/**
 * Orchestration strategy that executes all pipeline nodes concurrently using Java 21
 * virtual threads, then merges their results into a single {@link AgentContext}.
 * <p>
 * This is the default strategy and is best suited for independent analysis agents that
 * do not depend on each other's output (e.g. log analysis, code exploration, and context
 * enrichment running simultaneously). Wall-clock time is reduced by a factor of N where N
 * is the number of nodes.
 * </p>
 *
 * <p>If an individual node fails, its error is captured as a finding keyed by
 * {@code agentName_error} rather than propagating the exception, so other nodes
 * can still complete.</p>
 *
 * @author Ashesh
 */
public class ParallelStrategy implements OrchestratorStrategy {

    private static final Logger log = LoggerFactory.getLogger(ParallelStrategy.class);

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Duration agentTimeout;

    public ParallelStrategy(Duration agentTimeout) {
        this.agentTimeout = agentTimeout;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code "parallel"}
     */
    @Override
    public String strategyId() { return "parallel"; }

    /**
     * {@inheritDoc}
     * <p>
     * Submits each node as a virtual-thread task, waits for all to complete via
     * {@link CompletableFuture#allOf}, then reduces the individual contexts into one
     * via {@link AgentContext#merge(AgentContext)}.
     * </p>
     *
     * @param pipeline the pipeline whose nodes to execute concurrently
     * @param ctx      the shared context distributed to all nodes
     * @return a merged {@link AgentContext} containing findings from all completed nodes
     */
    @Override
    @SuppressWarnings("unchecked")
    public AgentContext execute(AgentPipeline pipeline, AgentContext ctx) {
        List<CompletableFuture<AgentContext>> futures = pipeline.nodes().stream()
                .map(node -> CompletableFuture.supplyAsync(() -> runNode(node, ctx), executor)
                        .orTimeout(agentTimeout.toMillis(), TimeUnit.MILLISECONDS))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream()
                .map(CompletableFuture::join)
                .reduce(ctx, AgentContext::merge);
    }

    /**
     * Executes a single pipeline node and wraps its output as an {@link AgentContext} finding.
     * <p>
     * On exception, logs the error and stores it as a {@code agentName_error} finding
     * rather than propagating, allowing sibling nodes to complete normally.
     * </p>
     *
     * @param node the pipeline node to execute
     * @param ctx  the shared context passed to the agent
     * @return a context with the agent's output (or error) stored as a finding
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private AgentContext runNode(AgentNode node, AgentContext ctx) {
        try {
            Object result = ((Agent) node.agent())
                    .execute(node.input(), ctx);
            return ctx.withFinding(node.agent().getName(), result);
        } catch (Exception e) {
            log.error("Node '{}' failed in parallel execution: {}", node.nodeId(), e.getMessage(), e);
            return ctx.withFinding(node.agent().getName() + "_error", e.getMessage());
        }
    }
}
