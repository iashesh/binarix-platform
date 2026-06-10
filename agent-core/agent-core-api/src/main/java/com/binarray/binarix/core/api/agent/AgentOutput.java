package com.binarray.binarix.core.api.agent;
/**
 * Marker interface for all typed agent outputs within the Binarix Platform.
 * <p>
 * Every domain-specific result object (e.g. {@code RcaReport}) must implement
 * this interface so it can be returned safely from the generic
 * {@link Agent#execute(AgentInput, AgentContext)} contract.
 * </p>
 *
 * @author Ashesh
 */
public interface AgentOutput {}
