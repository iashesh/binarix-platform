package com.binarray.binarix.rca.model;

/**
 * HTTP response envelope for the RCA REST API ({@code POST /api/v1/analyze}).
 * <p>
 * Wraps either a successful {@link RcaReport} or an error message, along with
 * metadata useful for client-side logging and SLA tracking.
 * </p>
 *
 * @param correlationId a unique identifier for this API request, for log correlation
 * @param status        {@code "SUCCESS"} or {@code "FAILED"}
 * @param report        the generated RCA report; {@code null} when {@code status} is {@code "FAILED"}
 * @param errorMessage  a human-readable error description; {@code null} when {@code status} is {@code "SUCCESS"}
 * @param durationMs    the total wall-clock time taken to complete the analysis in milliseconds
 *
 * @author Ashesh
 */
public record RcaResponse(
        String correlationId,
        String status,
        RcaReport report,
        String errorMessage,
        long durationMs
) {

    /**
     * Creates a successful {@code RcaResponse} wrapping the given report.
     *
     * @param correlationId the request correlation ID
     * @param report        the completed RCA report
     * @param durationMs    the elapsed analysis time in milliseconds
     * @return a {@code RcaResponse} with {@code status="SUCCESS"}
     */
    public static RcaResponse success(String correlationId, RcaReport report, long durationMs) {
        return new RcaResponse(correlationId, "SUCCESS", report, null, durationMs);
    }

    /**
     * Creates a failed {@code RcaResponse} with the given error message.
     *
     * @param correlationId the request correlation ID
     * @param error         a human-readable description of what went wrong
     * @param durationMs    the elapsed time before failure in milliseconds
     * @return a {@code RcaResponse} with {@code status="FAILED"}
     */
    public static RcaResponse failure(String correlationId, String error, long durationMs) {
        return new RcaResponse(correlationId, "FAILED", null, error, durationMs);
    }
}
