package com.binarray.binarix.core.impl.agent;

import com.binarray.binarix.core.api.agent.*;
import com.binarray.binarix.core.api.event.AgentEvent;
import com.binarray.binarix.core.api.event.AgentEventType;
import com.binarray.binarix.core.api.exception.AgentException;
import com.binarray.binarix.core.api.tool.AgentTool;
import com.binarray.binarix.core.impl.anthropic.AnthropicGateway;
import com.binarray.binarix.core.impl.event.CompositeAgentEventListener;
import com.binarray.binarix.core.impl.tool.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Abstract base class for all domain agents in the Binarix Platform.
 * <p>
 * Encapsulates the full agent execution lifecycle so that domain teams only need to
 * provide three things: a system prompt, a set of tool tags, and logic to convert
 * Claude's raw text response into a typed output object.
 * </p>
 *
 * <p>The execution sequence on each {@link #execute(AgentInput, AgentContext)} call:</p>
 * <ol>
 *   <li>Fire {@link AgentEventType#AGENT_STARTED} via the event bus.</li>
 *   <li>Resolve tools from the {@link ToolRegistry} using {@link #getToolTags()}.</li>
 *   <li>Build the user-turn message via {@link #buildUserMessage(AgentInput, AgentContext)}.</li>
 *   <li>Run the full Claude tool-call loop via {@link AnthropicGateway#runToolLoop}.</li>
 *   <li>Parse the final text response via {@link #parseResponse(String, AgentContext)}.</li>
 *   <li>Fire {@link AgentEventType#AGENT_COMPLETED} or {@link AgentEventType#AGENT_FAILED}.</li>
 * </ol>
 *
 * @param <I> the typed agent input — must implement {@link AgentInput}
 * @param <O> the typed agent output — must implement {@link AgentOutput}
 *
 * @author Ashesh
 */
@Component
public abstract class AbstractAgent<I extends AgentInput, O extends AgentOutput>
        implements Agent<I, O> {

    /** SLF4J logger scoped to the concrete subclass for clear log attribution. */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /** Gateway to the Anthropic Claude API, managing the full tool-call loop. */
    @Autowired
    protected AnthropicGateway gateway;

    /** Registry of all available tools, indexed by name and tag. */
    @Autowired
    protected ToolRegistry toolRegistry;

    /** Composite event bus that fans lifecycle events to all registered listeners. */
    @Autowired
    protected CompositeAgentEventListener eventBus;

    /**
     * {@inheritDoc}
     * <p>
     * Drives the full execution lifecycle: resolves tools, builds the user message,
     * runs the Claude tool-call loop, and parses the response. Fires lifecycle events
     * at the start and on completion or failure.
     * </p>
     *
     * @param input the typed domain input for this agent
     * @param ctx   the shared, immutable agent context for this pipeline run
     * @return the typed domain output produced by this agent
     * @throws AgentException if the Claude API call fails or response parsing fails
     */
    @Override
    public O execute(I input, AgentContext ctx) {
        eventBus.onAgentStarted(AgentEvent.started(getName(), ctx.correlationId()));
        try {
            List<AgentTool> tools = toolRegistry.getToolsForTags(getToolTags());
            String userMessage = buildUserMessage(input, ctx);
            AgentMetadata meta = getMetadata();
            log.info("[{}] Agent '{}' invoking Claude with {} tool(s)",
                    ctx.correlationId(), getName(), tools.size());
            String rawResponse = gateway.runToolLoop(meta, userMessage, tools, ctx);
            O result = parseResponse(rawResponse, ctx);
            eventBus.onAgentCompleted(AgentEvent.completed(getName(), ctx.correlationId(), result));
            return result;
        } catch (Exception ex) {
            eventBus.onAgentFailed(AgentEvent.failed(getName(), ctx.correlationId(), ex));
            throw new AgentException("Agent '" + getName() + "' failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Returns the tool tags used to filter which tools this agent receives from the registry.
     * <p>
     * Only tools annotated with at least one of the returned tags will be included in
     * the Claude tool-call schema for this agent's invocations.
     * </p>
     *
     * @return an array of tag strings, e.g. {@code {"log-reading", "git"}}
     */
    protected abstract String[] getToolTags();

    /**
     * Builds the user-turn message string to send to Claude for this agent's task.
     * <p>
     * Implementations typically embed key values from {@code input} and relevant
     * findings from {@code ctx} (produced by previously-run agents) to give Claude
     * the context it needs to use its tools effectively.
     * </p>
     *
     * @param input the typed domain input received by this agent
     * @param ctx   the current agent context containing findings from prior agents
     * @return the user message string to send as the first turn to Claude
     */
    protected abstract String buildUserMessage(I input, AgentContext ctx);

    /**
     * Parses Claude's final plain-text response into this agent's typed output.
     * <p>
     * Called once the Claude tool-call loop has completed and Claude has produced
     * its concluding text response. Implementations may parse structured text,
     * deserialise JSON, or wrap the raw string in a record.
     * </p>
     *
     * @param rawResponse the raw text returned by Claude after all tool calls are complete
     * @param ctx         the current agent context, available for correlation ID etc.
     * @return the typed {@link AgentOutput} for this agent
     */
    protected abstract O parseResponse(String rawResponse, AgentContext ctx);
}
