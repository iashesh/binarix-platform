package com.binarray.binarix.core.api.exception;

/**
 * Base unchecked exception for all agent execution failures in the Binarix Platform.
 * <p>
 * Thrown by {@code AbstractAgent}, {@code OrchestratorService}, and
 * {@code AnthropicGateway} when an unrecoverable error occurs. Callers may catch
 * this exception to handle agent failures without declaring checked exceptions
 * throughout the call chain.
 * </p>
 *
 * @author Ashesh
 */
public class AgentException extends RuntimeException {

    /**
     * Constructs an {@code AgentException} with the given message.
     *
     * @param message a human-readable description of the failure
     */
    public AgentException(String message) { super(message); }

    /**
     * Constructs an {@code AgentException} with the given message and root cause.
     *
     * @param message a human-readable description of the failure
     * @param cause   the underlying exception that caused this failure
     */
    public AgentException(String message, Throwable cause) { super(message, cause); }
}
