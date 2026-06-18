package com.binarray.binarix.core.api.event;

import java.time.Instant;

/**
 * Immutable record representing a lifecycle event fired during agent execution.
 * <p>
 * Events carry enough context for structured logging, distributed tracing, and
 * audit trails without requiring the listener to hold any additional state.
 * Use the static factory methods to construct instances for each {@link AgentEventType}.
 * </p>
 *
 * @param type          the lifecycle event type
 * @param agentName     the name of the agent that fired this event
 * @param correlationId the pipeline run correlation ID for log correlation
 * @param toolName      the name of the tool being invoked; {@code null} for non-tool events
 * @param payload       optional payload — the agent output on completion, or error message on failure
 * @param occurredAt    the wall-clock timestamp when this event was fired
 *
 * @author Ashesh
 */
public record AgentEvent(
        AgentEventType type,
        String agentName,
        String correlationId,
        String toolName,
        Object payload,
        Instant occurredAt
) {

    /**
     * Creates an {@link AgentEventType#AGENT_STARTED} event.
     *
     * @param agentName     the name of the agent starting execution
     * @param correlationId the pipeline run correlation ID
     * @return a new {@code AgentEvent} of type {@code AGENT_STARTED}
     */
    public static AgentEvent started(String agentName, String correlationId) {
        return new AgentEvent(AgentEventType.AGENT_STARTED, agentName, correlationId, null, null, Instant.now());
    }

    /**
     * Creates an {@link AgentEventType#TOOL_CALLED} event.
     *
     * @param agentName     the name of the agent invoking the tool
     * @param correlationId the pipeline run correlation ID
     * @param toolName      the name of the tool being called
     * @param inputs        the input parameters Claude passed to the tool
     * @return a new {@code AgentEvent} of type {@code TOOL_CALLED}
     */
    public static AgentEvent toolCalled(String agentName, String correlationId, String toolName, Object inputs) {
        return new AgentEvent(AgentEventType.TOOL_CALLED, agentName, correlationId, toolName, inputs, Instant.now());
    }

    /**
     * Creates an {@link AgentEventType#TOOL_COMPLETED} event.
     *
     * @param agentName     the name of the agent that invoked the tool
     * @param correlationId the pipeline run correlation ID
     * @param toolName      the name of the tool that completed
     * @param result        the result string returned by the tool
     * @return a new {@code AgentEvent} of type {@code TOOL_COMPLETED}
     */
    public static AgentEvent toolCompleted(String agentName, String correlationId, String toolName, String result) {
        return new AgentEvent(AgentEventType.TOOL_COMPLETED, agentName, correlationId, toolName, result, Instant.now());
    }

    /**
     * Creates an {@link AgentEventType#AGENT_COMPLETED} event.
     *
     * @param agentName     the name of the agent that completed
     * @param correlationId the pipeline run correlation ID
     * @param payload       the typed output produced by the agent
     * @return a new {@code AgentEvent} of type {@code AGENT_COMPLETED}
     */
    public static AgentEvent completed(String agentName, String correlationId, Object payload) {
        return new AgentEvent(AgentEventType.AGENT_COMPLETED, agentName, correlationId, null, payload, Instant.now());
    }

    /**
     * Creates an {@link AgentEventType#AGENT_FAILED} event.
     *
     * @param agentName     the name of the agent that failed
     * @param correlationId the pipeline run correlation ID
     * @param ex            the exception that caused the failure
     * @return a new {@code AgentEvent} of type {@code AGENT_FAILED}
     */
    public static AgentEvent failed(String agentName, String correlationId, Throwable ex) {
        return new AgentEvent(AgentEventType.AGENT_FAILED, agentName, correlationId, null, ex.getMessage(), Instant.now());
    }
}
