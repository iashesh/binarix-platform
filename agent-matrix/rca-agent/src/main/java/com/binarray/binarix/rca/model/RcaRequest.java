package com.binarray.binarix.rca.model;

import com.binarray.binarix.core.api.agent.AgentInput;

/**
 * Typed input record for the RCA Agent pipeline.
 * <p>
 * Carries all parameters required to run a root cause analysis: where to find the logs,
 * what type of codebase to explore, and optional tuning parameters.
 * </p>
 *
 * @param logErrorText      optional inline error text or stack trace provided directly by the caller;
 *                          when non-blank, the log file at {@code logLocation} is NOT read
 * @param logLocation       path to the log file (relative to {@code rca.log.base-path}),
 *                          HTTP URL, or S3 URI; ignored when {@code logErrorText} is set
 * @param codebaseType      the codebase adapter type — {@code "local"} or {@code "github"}
 * @param codebaseLocation  the codebase root path or GitHub repo identifier
 * @param gitBranch         the git branch to reference for context enrichment; defaults to {@code "main"}
 * @param maxLogLines       the maximum number of log lines to read; defaults to {@code 5000}
 *
 * @author Ashesh
 */
public record RcaRequest(
        String logErrorText,
        String logLocation,
        String codebaseType,
        String codebaseLocation,
        String gitBranch,
        int maxLogLines

) implements AgentInput {

    /** Returns {@code true} when the caller has supplied inline error text. */
    public boolean hasInlineError() {
        return logErrorText != null && !logErrorText.isBlank();
    }

    /**
     * Convenience factory that creates an {@code RcaRequest} with default
     * {@code maxLogLines=5000}, {@code gitBranch="main"}, and no inline error text.
     *
     * @param logLocation      path or URI to the log file to analyse
     * @param codebaseType     the codebase adapter type ({@code "local"} or {@code "github"})
     * @param codebaseLocation the root path or repository identifier for the codebase
     * @return a new {@code RcaRequest} with defaults applied
     */
    public static RcaRequest of(String logLocation, String codebaseType, String codebaseLocation) {
        return new RcaRequest(logLocation, codebaseType, codebaseLocation, 5000, "main", null);
    }
}
