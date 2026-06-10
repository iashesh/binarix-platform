package com.binarray.binarix.core.api.agent;
/**
 * Marker interface for all typed agent inputs within the Binarix Platform.
 * <p>
 * Every domain-specific request object (e.g. {@code RcaRequest}) must implement
 * this interface so it can be passed safely through the generic
 * {@link Agent#execute(AgentInput, AgentContext)} contract.
 * </p>
 *
 * @author Ashesh
 */
public interface AgentInput {}
