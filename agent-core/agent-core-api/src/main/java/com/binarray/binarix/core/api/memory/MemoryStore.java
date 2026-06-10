package com.binarray.binarix.core.api.memory;

import java.util.List;

/**
 * Storage abstraction for agent conversation history in the Binarix Platform.
 * <p>
 * A {@code MemoryStore} persists the message history for a given session so that agents
 * can maintain multi-turn conversational context across invocations. The default
 * implementation is {@code InMemoryStore}; a Redis-backed implementation can be substituted
 * via Spring's {@code @ConditionalOnMissingBean} mechanism.
 * </p>
 *
 * @author Ashesh
 */
public interface MemoryStore {

    /**
     * Persists a single message to the conversation history for the given session.
     *
     * @param sessionId the unique session or correlation identifier
     * @param message   the message to append to the history
     */
    void save(String sessionId, ConversationMessage message);

    /**
     * Loads the most recent {@code maxMessages} messages for the given session.
     * <p>
     * If the session has fewer than {@code maxMessages} messages, all available messages
     * are returned. Messages are returned in chronological order (oldest first).
     * </p>
     *
     * @param sessionId   the unique session or correlation identifier
     * @param maxMessages the maximum number of recent messages to return
     * @return an ordered list of messages, never {@code null}, may be empty
     */
    List<ConversationMessage> load(String sessionId, int maxMessages);

    /**
     * Clears all conversation history for the given session.
     *
     * @param sessionId the unique session or correlation identifier to clear
     */
    void clear(String sessionId);

    /**
     * Returns {@code true} if any messages exist for the given session.
     *
     * @param sessionId the session identifier to check
     * @return {@code true} if the session has at least one stored message
     */
    boolean exists(String sessionId);
}
