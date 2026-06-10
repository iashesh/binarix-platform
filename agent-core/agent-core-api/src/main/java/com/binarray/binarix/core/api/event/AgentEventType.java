package com.binarray.binarix.core.api.event;
/**
 * Enumeration of lifecycle event types published by agents in the Binarix Platform.
 * <p>
 * Events are fired at key points in an agent's execution lifecycle and are consumed
 * by {@link AgentEventListener} implementations for logging, metrics, and auditing.
 * </p>
 *
 * @author Ashesh
 */
public enum AgentEventType {

    /** Fired immediately before an agent begins its first Claude API call. */
    AGENT_STARTED,

    /** Fired each time Claude requests a local tool invocation. */
    TOOL_CALLED,

    /** Fired when a local tool invocation has completed and the result is ready. */
    TOOL_COMPLETED,

    /** Fired when an agent successfully produces its typed output. */
    AGENT_COMPLETED,

    /** Fired when an agent throws an exception and cannot produce output. */
    AGENT_FAILED
}
