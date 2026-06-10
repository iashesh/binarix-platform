package com.binarray.binarix.rca.model;

import java.util.List;

/**
 * Represents a recurring error signature identified in the application logs by the
 * {@code LogAnalystAgent}.
 * <p>
 * Groups all occurrences of the same error type together so the RCA synthesizer can
 * reason about frequency, timing, and the threads affected.
 * </p>
 *
 * @param errorType       the fully qualified exception class name or error code,
 *                        e.g. {@code "java.lang.NullPointerException"}
 * @param message         the first occurrence's error message text
 * @param occurrenceCount the total number of times this error pattern appeared in the logs
 * @param timestamps      the timestamps of the first few occurrences for temporal analysis
 * @param affectedThreads the thread names that logged this error
 *
 * @author Ashesh
 */
public record ErrorPattern(
        String errorType,
        String message,
        int occurrenceCount,
        List<String> timestamps,
        List<String> affectedThreads
) {}
