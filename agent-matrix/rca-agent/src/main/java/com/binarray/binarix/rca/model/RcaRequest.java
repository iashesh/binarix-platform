package com.binarray.binarix.rca.model;

import com.binarray.binarix.core.api.agent.AgentInput;

/**
 * Typed input record for the RCA Agent pipeline.
 * <p>
 * Carries all parameters required to run a root cause analysis: where to find the logs,
 * what type of codebase to explore, and optional tuning parameters.
 * </p>
 *
 * @param logLocation       path to the log file (relative to {@code rca.log.base-path}),
 *                          HTTP URL, or S3 URI
 * @param codebaseType      the codebase adapter type — {@code "local"} or {@code "github"}
 * @param codebaseLocation  the codebase root path or GitHub repo identifier
 * @param maxLogLines       the maximum number of log lines to read; defaults to {@code 5000}
 * @param gitBranch         the git branch to reference for context enrichment; defaults to {@code "main"}
 *
 * @author Ashesh
 */
public record RcaRequest(
        String logLocation,
        String codebaseType,
        String codebaseLocation,
        int maxLogLines,
        String gitBranch
) implements AgentInput {

    /**
     * Convenience factory that creates an {@code RcaRequest} with default
     * {@code maxLogLines=5000} and {@code gitBranch="main"}.
     *
     * @param logLocation      path or URI to the log file to analyse
     * @param codebaseType     the codebase adapter type ({@code "local"} or {@code "github"})
     * @param codebaseLocation the root path or repository identifier for the codebase
     * @return a new {@code RcaRequest} with defaults applied
     */
    public static RcaRequest of(String logLocation, String codebaseType, String codebaseLocation) {
        return new RcaRequest(logLocation, codebaseType, codebaseLocation, 5000, "main");
    }
}
