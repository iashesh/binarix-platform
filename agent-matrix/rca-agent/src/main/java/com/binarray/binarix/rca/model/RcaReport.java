package com.binarray.binarix.rca.model;

import com.binarray.binarix.core.api.agent.AgentOutput;
import java.time.Instant;
import java.util.List;

/**
 * The final Root Cause Analysis report produced by the {@code RcaSynthesizerAgent}.
 * <p>
 * This is the primary output of the RCA pipeline. It aggregates findings from all
 * three analyst agents and presents a structured, actionable report for engineers
 * responding to an incident.
 * </p>
 *
 * @param correlationId     the pipeline run correlation ID for traceability
 * @param rootCauses        list of identified root causes ranked by {@code confidenceScore} descending
 * @param executiveSummary  a 2-3 sentence human-readable summary of the incident and its cause
 * @param remediationSteps  ordered list of concrete actionable steps to resolve the issue
 * @param affectedComponent the service, class, or method identified as the epicentre of the failure
 * @param severity          severity classification — {@code "CRITICAL"}, {@code "HIGH"},
 *                          {@code "MEDIUM"}, or {@code "LOW"}
 * @param generatedAt       the wall-clock timestamp when this report was produced
 * @param rawAnalysis       the full raw synthesizer output for archival and debugging
 *
 * @author Ashesh
 */
public record RcaReport(
        String correlationId,
        List<RootCause> rootCauses,
        String executiveSummary,
        List<String> remediationSteps,
        String affectedComponent,
        String severity,
        Instant generatedAt,
        String rawAnalysis
) implements AgentOutput {

    /**
     * Creates a minimal {@code RcaReport} from a raw analysis string.
     * <p>
     * Used as a fallback when structured parsing is not performed on the synthesizer's output.
     * The full raw text is preserved for manual review.
     * </p>
     *
     * @param correlationId the pipeline run correlation ID
     * @param rawAnalysis   the full synthesis text returned by Claude
     * @return an {@code RcaReport} with the summary set to the raw analysis and empty structured fields
     */
    public static RcaReport fromRaw(String correlationId, String rawAnalysis) {
        return new RcaReport(correlationId, List.of(), rawAnalysis,
                List.of(), "Unknown", "UNKNOWN", Instant.now(), rawAnalysis);
    }
}
