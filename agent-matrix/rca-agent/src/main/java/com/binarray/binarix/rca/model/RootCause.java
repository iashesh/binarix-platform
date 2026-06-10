package com.binarray.binarix.rca.model;

import java.util.List;

/**
 * Represents a single identified root cause within an {@link RcaReport}.
 * <p>
 * Root causes are ranked by {@link #confidenceScore()} (descending) so the most
 * likely cause appears first. Each root cause is backed by concrete evidence
 * extracted from the log and code analysis.
 * </p>
 *
 * @param description     a human-readable explanation of this root cause
 * @param confidenceScore a value between {@code 0.0} (uncertain) and {@code 1.0} (certain)
 *                        indicating how likely this is the actual root cause
 * @param evidence        list of evidence items — log lines, file:line references, commit hashes
 * @param category        a category label, e.g. {@code "null-pointer"}, {@code "config"},
 *                        {@code "oom"}, {@code "race-condition"}
 * @param affectedFile    the source file most directly responsible, or {@code null} if unknown
 * @param affectedLine    the line number within {@code affectedFile}, or {@code 0} if unknown
 *
 * @author Ashesh
 */
public record RootCause(
        String description,
        double confidenceScore,
        List<String> evidence,
        String category,
        String affectedFile,
        int affectedLine
) {}
