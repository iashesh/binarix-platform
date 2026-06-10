package com.binarray.binarix.core.api.exception;

/**
 * Exception thrown when a tool invocation fails during the Claude agentic loop.
 * <p>
 * Extends {@link AgentException} and adds the name of the tool that failed, which
 * is included in the error message sent back to Claude so it can decide how to
 * handle the failure (retry, skip, or abandon the task).
 * </p>
 *
 * @author Ashesh
 */
public class ToolExecutionException extends AgentException {

    private final String toolName;

    /**
     * Constructs a {@code ToolExecutionException} for the named tool.
     *
     * @param toolName the {@link com.binarray.binarix.core.api.tool.ToolDefinition#name()} of the failing tool
     * @param message  a description of what went wrong during tool execution
     * @param cause    the underlying exception thrown by the tool
     */
    public ToolExecutionException(String toolName, String message, Throwable cause) {
        super("Tool [" + toolName + "] failed: " + message, cause);
        this.toolName = toolName;
    }

    /**
     * Returns the name of the tool that failed.
     *
     * @return the tool name as declared in its {@link com.binarray.binarix.core.api.tool.ToolDefinition}
     */
    public String getToolName() { return toolName; }
}
