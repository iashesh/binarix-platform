package com.binarray.binarix.core.impl.memory;

import com.binarray.binarix.core.api.memory.ConversationMessage;
import com.binarray.binarix.core.api.memory.MemoryStore;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory implementation of {@link MemoryStore}.
 * <p>
 * Stores conversation history in a {@link ConcurrentHashMap} keyed by session ID.
 * Suitable for single-node deployments and testing. For multi-node or persistent
 * memory requirements, replace this bean with a Redis-backed implementation.
 * </p>
 *
 * <p>This bean is registered with {@code @ConditionalOnMissingBean(MemoryStore.class)}
 * in {@code AgentCoreAutoConfiguration}, so it is only activated when no other
 * {@code MemoryStore} implementation is present in the application context.</p>
 *
 * @author Ashesh
 */
@Component
public class InMemoryStore implements MemoryStore {

    /** Thread-safe storage map: sessionId → synchronised list of messages. */
    private final Map<String, List<ConversationMessage>> store = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     * <p>Appends the message to the session's list, creating the list if this is the first message.</p>
     *
     * @param sessionId the unique session identifier
     * @param message   the message to append
     */
    @Override
    public void save(String sessionId, ConversationMessage message) {
        store.computeIfAbsent(sessionId, k -> Collections.synchronizedList(new ArrayList<>())).add(message);
    }

    /**
     * {@inheritDoc}
     * <p>Returns the last {@code maxMessages} entries from the session's history.</p>
     *
     * @param sessionId   the unique session identifier
     * @param maxMessages the maximum number of recent messages to return
     * @return an immutable list of the most recent messages, oldest first
     */
    @Override
    public List<ConversationMessage> load(String sessionId, int maxMessages) {
        List<ConversationMessage> all = store.getOrDefault(sessionId, Collections.emptyList());
        int start = Math.max(0, all.size() - maxMessages);
        return List.copyOf(all.subList(start, all.size()));
    }

    /**
     * {@inheritDoc}
     *
     * @param sessionId the session identifier whose history to delete
     */
    @Override
    public void clear(String sessionId) { store.remove(sessionId); }

    /**
     * {@inheritDoc}
     *
     * @param sessionId the session identifier to check
     * @return {@code true} if at least one message has been saved for this session
     */
    @Override
    public boolean exists(String sessionId) { return store.containsKey(sessionId); }
}
