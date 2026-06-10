package com.binarray.binarix.core.api.adapter;

import java.util.List;

/**
 * Adapter interface for reading log content from various sources.
 * <p>
 * Implementations exist for local file-system paths, HTTP endpoints, S3 URIs, and
 * streaming sources. At runtime, {@code AnthropicGateway} tools use whichever adapter
 * {@link #supports(String)} the given location string.
 * </p>
 *
 * @author Ashesh
 */
public interface LogSourceAdapter {

    /**
     * Reads and returns the full content of the log at the given location.
     *
     * @param location a file path, HTTP URL, or S3 URI identifying the log source
     * @return the log content as a single string
     */
    String read(String location);

    /**
     * Reads and returns up to {@code maxLines} lines from the log at the given location.
     *
     * @param location the file path, HTTP URL, or S3 URI identifying the log source
     * @param maxLines the maximum number of lines to return; pass {@code -1} for all lines
     * @return an ordered list of log lines, never {@code null}
     */
    List<String> readLines(String location, int maxLines);

    /**
     * Returns {@code true} if this adapter is capable of handling the given location string.
     * <p>
     * Used by the adapter chain to select the correct implementation at runtime.
     * For example, a file adapter would return {@code true} for paths starting with {@code /}
     * or {@code ./}, while an S3 adapter would return {@code true} for {@code s3://} URIs.
     * </p>
     *
     * @param location the location string to test
     * @return {@code true} if this adapter can read from {@code location}
     */
    boolean supports(String location);
}
