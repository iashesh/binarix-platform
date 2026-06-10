package com.binarray.binarix.core.api.memory;
/**
 * Enumeration of roles that a {@link ConversationMessage} may carry.
 * <p>
 * Mirrors the Claude API message roles used in multi-turn conversations stored
 * in a {@link MemoryStore}.
 * </p>
 *
 * @author Ashesh
 */
public enum MessageRole {

    /** A message authored by the human user or the agent orchestrator. */
    USER,

    /** A message authored by Claude (the assistant). */
    ASSISTANT,

    /** A message containing the result of a tool invocation, sent back to Claude. */
    TOOL_RESULT
}
