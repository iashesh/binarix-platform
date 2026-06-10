package com.binarray.binarix.core.api.orchestration;

import com.binarray.binarix.core.api.agent.*;
import com.binarray.binarix.core.api.agent.Agent;
import com.binarray.binarix.core.api.agent.AgentInput;
import com.binarray.binarix.core.api.agent.AgentOutput;

/**
 * Represents a single node in an {@link AgentPipeline} - the pairing of an {@link Agent}
 * with the {@link AgentInput} it should receive when executed.
 * <p>
 * Nodes are immutable records. The {@code nodeId} defaults to the agent's name and is used
 * in log output and trace span names to identify which step of the pipeline is executing.
 * </p>
 *
 * @param agent  the agent to execute at this node
 * @param input  the typed input to pass to the agent
 * @param nodeId the unique identifier for this node within the pipeline (defaults to agent name)
 *
 * @author Ashesh
 */
public record AgentNode(
        Agent<? extends AgentInput, ? extends AgentOutput> agent,
        AgentInput input,
        String nodeId
) {

    /**
     * Factory method that creates an {@code AgentNode} using the agent's own name as the node ID.
     *
     * @param agent the agent to run at this node
     * @param input the typed input the agent will receive
     * @return a new {@code AgentNode} with {@code nodeId} set to {@code agent.getName()}
     */
    public static AgentNode of(Agent<? extends AgentInput, ? extends AgentOutput> agent, AgentInput input) {
        return new AgentNode(agent, input, agent.getName());
    }
}
