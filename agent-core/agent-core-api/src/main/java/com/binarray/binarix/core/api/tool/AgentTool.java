package com.binarray.binarix.core.api.tool;

/**
 * Marker interface for all Binarix Platform agent tools.
 * <p>
 * A tool is a locally-executed Java component that Claude may invoke during an agentic
 * loop. Every tool class must also:
 * <ul>
 *   <li>Be annotated with {@link ToolDefinition} to declare its name, description, and tags.</li>
 *   <li>Be a Spring {@code @Component} so it is discovered by {@code ToolRegistry}.</li>
 *   <li>Implement {@link java.util.function.Supplier}{@code <String>} to provide the
 *       {@code get()} execution method.</li>
 *   <li>Expose public instance fields that map to JSON Schema properties Claude uses
 *       when constructing tool-call arguments.</li>
 * </ul>
 * </p>
 *
 * @author Ashesh
 * @see ToolDefinition
 */
public interface AgentTool {}
