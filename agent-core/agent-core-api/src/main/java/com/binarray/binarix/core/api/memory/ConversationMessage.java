package com.binarray.binarix.core.api.memory;

import java.time.Instant;

/**
 * Immutable record representing a single message in an agent's conversation history.
 * <p>
 * Stored in a {@link MemoryStore} and replayed into Claude's context window on subsequent
 * turns to maintain multi-turn conversation continuity across agent invocations.
 * </p>
 *
 * @param role        the role of the message author — user, assistant, or tool result
 * @param content     the text content of the message
 * @param toolCallId  the tool-use ID this message responds to; {@code null} for non-tool messages
 * @param timestamp   the wall-clock time when this message was created
 *
 * @author Ashesh
 */
public record ConversationMessage(
        MessageRole role,
        String content,
        String toolCallId,
        Instant timestamp
) {

    /**
     * Creates a {@link MessageRole#USER} message with the given content.
     *
     * @param content the user message text
     * @return a new {@code ConversationMessage} with role {@code USER} and current timestamp
     */
    public static ConversationMessage user(String content) {
        return new ConversationMessage(MessageRole.USER, content, null, Instant.now());
    }

    /**
     * Creates a {@link MessageRole#ASSISTANT} message with the given content.
     *
     * @param content the assistant (Claude) response text
     * @return a new {@code ConversationMessage} with role {@code ASSISTANT} and current timestamp
     */
    public static ConversationMessage assistant(String content) {
        return new ConversationMessage(MessageRole.ASSISTANT, content, null, Instant.now());
    }
}
