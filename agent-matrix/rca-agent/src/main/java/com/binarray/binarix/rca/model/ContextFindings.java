package com.binarray.binarix.rca.model;

import com.binarray.binarix.core.api.agent.AgentOutput;
import java.util.List;

/**
 * Typed output produced by the {@code ContextEnricherAgent}.
 * <p>
 * Encapsulates contextual information gathered from git history and the deployment
 * environment — providing the synthesizer with temporal change context to correlate
 * against the observed errors.
 * </p>
 *
 * @param recentCommits        list of recent git commit summaries in {@code hash date author: message} format
 * @param environmentVariables list of relevant environment variable values (whitelisted only)
 * @param deploymentContext    a description of the most recent deployment, or {@code null} if unavailable
 * @param rawAnalysis          the full raw analysis text produced by Claude for downstream agents
 *
 * @author Ashesh
 */
public record ContextFindings(
        List<String> recentCommits,
        List<String> environmentVariables,
        String deploymentContext,
        String rawAnalysis
) implements AgentOutput {

    /**
     * Creates a minimal {@code ContextFindings} from a raw analysis string.
     *
     * @param rawAnalysis the full analysis text returned by Claude
     * @return a {@code ContextFindings} with empty structured fields and the given raw analysis
     */
    public static ContextFindings fromRaw(String rawAnalysis) {
        return new ContextFindings(List.of(), List.of(), null, rawAnalysis);
    }
}
