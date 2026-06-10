package com.binarray.binarix.core.api.orchestration;

import lombok.Getter;

import java.util.*;

/**
 * Describes the set of {@link AgentNode}s to run and the {@link OrchestratorStrategy}
 * to use when executing them.
 * <p>
 * A pipeline is constructed once per orchestration call using the nested {@link Builder},
 * then passed to {@code OrchestratorService.orchestrate()} which selects the correct
 * strategy and drives execution.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * AgentPipeline pipeline = AgentPipeline.builder()
 *         .strategy("parallel")
 *         .addNode(AgentNode.of(logAnalyst, request))
 *         .addNode(AgentNode.of(codeExplorer, request))
 *         .build();
 * }</pre>
 *
 * @author Ashesh
 */
public class AgentPipeline {
    @Getter
    private final String strategy;
    private final List<AgentNode> nodes;

    private AgentPipeline(Builder b) {
        this.strategy = b.strategy;
        this.nodes = List.copyOf(b.nodes);
    }

    /**
     * Returns an immutable ordered list of nodes in this pipeline.
     *
     * @return the list of {@link AgentNode}s, never {@code null}
     */
    public List<AgentNode> nodes() { return nodes; }

    /**
     * Returns a new {@link Builder} with default strategy set to {@code "parallel"}.
     *
     * @return a fresh builder instance
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Fluent builder for {@link AgentPipeline}.
     */
    public static class Builder {

        private String strategy = "parallel";
        private final List<AgentNode> nodes = new ArrayList<>();

        /**
         * Sets the orchestration strategy for this pipeline.
         *
         * @param s the strategy ID, e.g. {@code "parallel"} or {@code "sequential"}
         * @return this builder
         */
        public Builder strategy(String s) { this.strategy = s; return this; }

        /**
         * Adds an agent node to this pipeline.
         *
         * @param n the node to append
         * @return this builder
         */
        public Builder addNode(AgentNode n) { this.nodes.add(n); return this; }

        /**
         * Builds and returns the immutable {@link AgentPipeline}.
         *
         * @return a new {@code AgentPipeline}
         */
        public AgentPipeline build() { return new AgentPipeline(this); }
    }
}
