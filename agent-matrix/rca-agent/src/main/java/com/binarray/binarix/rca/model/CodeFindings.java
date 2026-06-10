package com.binarray.binarix.rca.model;

import com.binarray.binarix.core.api.agent.AgentOutput;
import java.util.*;

/**
 * Typed output produced by the {@code CodeExplorerAgent}.
 * <p>
 * Encapsulates the findings from codebase exploration: the relevant source files
 * that were examined, suspicious lines identified, the call chain traced from the
 * error, and the raw analysis text for the synthesizer agent.
 * </p>
 *
 * @param relevantFiles    map of file path → content snippet for files most relevant to the error
 * @param suspiciousLines  list of {@code "file:line: content"} strings identifying suspect code
 * @param callChainSummary a human-readable description of the call chain leading to the error
 * @param rawAnalysis      the full raw analysis text produced by Claude for downstream agents
 *
 * @author Ashesh
 */
public record CodeFindings(
        Map<String, String> relevantFiles,
        List<String> suspiciousLines,
        String callChainSummary,
        String rawAnalysis
) implements AgentOutput {

    /**
     * Creates a minimal {@code CodeFindings} from a raw analysis string.
     *
     * @param rawAnalysis the full analysis text returned by Claude
     * @return a {@code CodeFindings} with empty structured fields and the given raw analysis
     */
    public static CodeFindings fromRaw(String rawAnalysis) {
        return new CodeFindings(Map.of(), List.of(), rawAnalysis, rawAnalysis);
    }
}
