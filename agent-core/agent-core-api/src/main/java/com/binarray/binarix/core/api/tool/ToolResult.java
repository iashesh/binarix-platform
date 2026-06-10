package com.binarray.binarix.core.api.tool;

/**
 * Represents the outcome of a tool execution within the Binarix Platform.
 * <p>
 * Wraps either a successful string result or an error message in a type-safe record.
 * Use the static factory methods {@link #ok(String)} and {@link #fail(String)} to
 * construct instances.
 * </p>
 *
 * @param success {@code true} if the tool executed successfully, {@code false} on failure
 * @param content the tool output content; {@code null} when {@code success} is {@code false}
 * @param error   the error message; {@code null} when {@code success} is {@code true}
 *
 * @author Ashesh
 */
public record ToolResult(boolean success, String content, String error) {

    /**
     * Creates a successful {@code ToolResult} with the given content.
     *
     * @param content the string output produced by the tool
     * @return a successful result carrying {@code content}
     */
    public static ToolResult ok(String content) { return new ToolResult(true, content, null); }

    /**
     * Creates a failed {@code ToolResult} with the given error message.
     *
     * @param error a human-readable description of what went wrong
     * @return a failed result carrying {@code error}
     */
    public static ToolResult fail(String error) { return new ToolResult(false, null, error); }
}
