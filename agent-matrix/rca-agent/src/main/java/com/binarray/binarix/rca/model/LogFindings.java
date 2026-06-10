package com.binarray.binarix.rca.model;

import com.binarray.binarix.core.api.agent.AgentOutput;
import java.util.List;

/**
 * Typed output produced by the {@code LogAnalystAgent}.
 * <p>
 * Encapsulates the structured findings from log analysis: identified error patterns,
 * parsed stack trace frames, temporal bounds of the incident, and the raw analysis
 * text for the synthesizer agent.
 * </p>
 *
 * @param patterns             list of recurring error patterns grouped by type
 * @param frames               list of stack trace frames extracted from exception logs
 * @param errorSummary         a brief human-readable summary of the errors found
 * @param firstErrorTimestamp  the timestamp of the earliest error in ISO-8601 format, or {@code null}
 * @param lastErrorTimestamp   the timestamp of the most recent error in ISO-8601 format, or {@code null}
 * @param totalErrorCount      the total number of error-level log entries found
 * @param rawAnalysis          the full raw text analysis produced by Claude for downstream agents
 *
 * @author Ashesh
 */
public record LogFindings(
        List<ErrorPattern> patterns,
        List<StackTraceFrame> frames,
        String errorSummary,
        String firstErrorTimestamp,
        String lastErrorTimestamp,
        int totalErrorCount,
        String rawAnalysis
) implements AgentOutput {

    /**
     * Creates a minimal {@code LogFindings} from a raw analysis string.
     * <p>
     * Used when the agent returns unstructured text that has not yet been parsed
     * into structured patterns or frames. The raw text is passed directly to the
     * synthesizer agent.
     * </p>
     *
     * @param rawAnalysis the full analysis text returned by Claude
     * @return a {@code LogFindings} with empty structured fields and the given raw analysis
     */
    public static LogFindings fromRaw(String rawAnalysis) {
        return new LogFindings(List.of(), List.of(), rawAnalysis, null, null, 0, rawAnalysis);
    }
}
