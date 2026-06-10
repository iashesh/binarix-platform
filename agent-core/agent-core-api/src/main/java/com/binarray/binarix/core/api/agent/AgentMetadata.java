package com.binarray.binarix.core.api.agent;

import lombok.Getter;

/**
 * Immutable configuration governing how an {@link Agent} invokes the Claude API.
 * <p>
 * Each agent declares its own {@code AgentMetadata} via {@link Agent#getMetadata()},
 * allowing different agents in the same pipeline to use different models, token budgets,
 * temperatures, and system prompts.
 * </p>
 *
 * <p>Instances are constructed via the nested {@link Builder}:</p>
 * <pre>{@code
 * AgentMetadata meta = AgentMetadata.builder()
 *         .model("claude-sonnet-4-5")
 *         .maxTokens(8192)
 *         .systemPrompt("You are an expert log analyst...")
 *         .build();
 * }</pre>
 *
 * @author Ashesh
 */
@Getter
public class AgentMetadata {

    private final String model;
    private final int maxTokens;
    private final double temperature;
    private final String systemPrompt;

    private AgentMetadata(Builder b) {
        this.model = b.model;
        this.maxTokens = b.maxTokens;
        this.temperature = b.temperature;
        this.systemPrompt = b.systemPrompt;
    }

    public static Builder builder() { return new Builder(); }

    /**
     * Fluent builder for {@link AgentMetadata}.
     */
    public static class Builder {

        private String model = "claude-sonnet-4-5";
        private int maxTokens = 8192;
        private double temperature = 0.3;
        private String systemPrompt = "";

        public Builder model(String m) { this.model = m; return this; }
        public Builder maxTokens(int t) { this.maxTokens = t; return this; }
        public Builder temperature(double t) { this.temperature = t; return this; }
        public Builder systemPrompt(String s) { this.systemPrompt = s; return this; }
        public AgentMetadata build() { return new AgentMetadata(this); }
    }
}
