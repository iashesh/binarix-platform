package com.binarray.binarix.core.api.event;

/**
 * Listener interface for agent lifecycle events in the Binarix Platform.
 * <p>
 * Implement and register this as a Spring bean to hook into agent execution without
 * modifying agent code. The {@code CompositeAgentEventListener} collects all registered
 * implementations and fans events out to each in turn.
 * </p>
 *
 * <p>Common use-cases include structured audit logging, custom metrics, and
 * alerting on agent failures.</p>
 *
 * @author Ashesh
 */
public interface AgentEventListener {

    /**
     * Called immediately before an agent begins its first Claude API call.
     *
     * @param event the event carrying the agent name and correlation ID
     */
    void onAgentStarted(AgentEvent event);

    /**
     * Called each time Claude requests a local tool invocation during the agentic loop.
     *
     * @param event the event carrying the agent name, tool name, and correlation ID
     */
    void onToolCalled(AgentEvent event);

    /**
     * Called when a local tool invocation has returned its result to Claude.
     *
     * @param event the event carrying the tool name and correlation ID
     */
    void onToolCompleted(AgentEvent event);

    /**
     * Called when an agent successfully produces its typed output and the agentic loop ends.
     *
     * @param event the event carrying the agent output as {@link AgentEvent#payload()}
     */
    void onAgentCompleted(AgentEvent event);

    /**
     * Called when an agent throws an unrecoverable exception.
     *
     * @param event the event carrying the error message as {@link AgentEvent#payload()}
     */
    void onAgentFailed(AgentEvent event);
}
