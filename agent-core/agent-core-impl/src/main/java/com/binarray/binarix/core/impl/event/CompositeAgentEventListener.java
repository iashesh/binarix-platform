package com.binarray.binarix.core.impl.event;

import com.binarray.binarix.core.api.event.AgentEvent;
import com.binarray.binarix.core.api.event.AgentEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Composite implementation of {@link AgentEventListener} that fans lifecycle events
 * out to all registered delegate listeners.
 * <p>
 * This bean is auto-configured in {@code AgentCoreAutoConfiguration} and receives all
 * {@link AgentEventListener} beans from the application context. Domain teams can add
 * custom listeners simply by registering them as Spring beans — no wiring required.
 * </p>
 *
 * <p>Built-in behaviour: structured log lines are emitted at {@code INFO} or {@code DEBUG}
 * level for every lifecycle event, providing out-of-the-box observability without any
 * additional configuration.</p>
 *
 * @author Ashesh
 */
@Component
public class CompositeAgentEventListener implements AgentEventListener {

    private static final Logger log = LoggerFactory.getLogger(CompositeAgentEventListener.class);

    /** Delegate listeners, filtered to exclude self (prevents infinite recursion). */
    private final List<AgentEventListener> delegates;

    /**
     * Constructs the composite listener from all {@link AgentEventListener} beans found
     * in the application context, excluding itself to prevent infinite recursion.
     *
     * @param delegates all registered {@link AgentEventListener} implementations
     */
    public CompositeAgentEventListener(List<AgentEventListener> delegates) {
        // Filter self to avoid infinite recursion
        this.delegates = delegates.stream()
                .filter(d -> !(d instanceof CompositeAgentEventListener))
                .toList();
    }

    /**
     * {@inheritDoc}
     * <p>Logs at {@code INFO} level and delegates to all registered listeners.</p>
     *
     * @param e the event carrying the agent name and correlation ID
     */
    @Override
    public void onAgentStarted(AgentEvent e) {
        log.info("[{}] Agent '{}' started", e.correlationId(), e.agentName());
        delegates.forEach(d -> d.onAgentStarted(e));
    }

    /**
     * {@inheritDoc}
     * <p>Logs at {@code DEBUG} level and delegates to all registered listeners.</p>
     *
     * @param e the event carrying the agent name, tool name, and correlation ID
     */
    @Override
    public void onToolCalled(AgentEvent e) {
        log.debug("[{}] Agent '{}' calling tool '{}'", e.correlationId(), e.agentName(), e.toolName());
        delegates.forEach(d -> d.onToolCalled(e));
    }

    /**
     * {@inheritDoc}
     * <p>Logs at {@code DEBUG} level and delegates to all registered listeners.</p>
     *
     * @param e the event carrying the tool name and correlation ID
     */
    @Override
    public void onToolCompleted(AgentEvent e) {
        log.debug("[{}] Tool '{}' completed", e.correlationId(), e.toolName());
        delegates.forEach(d -> d.onToolCompleted(e));
    }

    /**
     * {@inheritDoc}
     * <p>Logs at {@code INFO} level and delegates to all registered listeners.</p>
     *
     * @param e the event carrying the agent output as payload
     */
    @Override
    public void onAgentCompleted(AgentEvent e) {
        log.info("[{}] Agent '{}' completed", e.correlationId(), e.agentName());
        delegates.forEach(d -> d.onAgentCompleted(e));
    }

    /**
     * {@inheritDoc}
     * <p>Logs at {@code ERROR} level and delegates to all registered listeners.</p>
     *
     * @param e the event carrying the error message as payload
     */
    @Override
    public void onAgentFailed(AgentEvent e) {
        log.error("[{}] Agent '{}' failed: {}", e.correlationId(), e.agentName(), e.payload());
        delegates.forEach(d -> d.onAgentFailed(e));
    }
}
