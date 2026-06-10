package com.binarray.binarix.core.impl.anthropic;

import com.binarray.binarix.core.api.agent.AgentContext;
import com.binarray.binarix.core.api.agent.AgentMetadata;
import com.binarray.binarix.core.api.exception.AgentException;
import com.binarray.binarix.core.api.retry.RetryPolicy;
import com.binarray.binarix.core.api.tool.AgentTool;
import com.binarray.binarix.core.api.tool.ToolDefinition;
import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.*;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Single integration point with the Anthropic Java SDK (2.35.0).
 * Drives the full agentic tool-call loop until Claude returns a pure-text response.
 *
 * <p>Verified API surface used:
 * <ul>
 *   <li>{@link ContentBlock#text()} / {@link ContentBlock#toolUse()} — Optional-based accessors</li>
 *   <li>{@link ToolUseBlock#input()} — returns {@code Object}, cast to {@link Map}</li>
 *   <li>{@link ToolResultBlockParam.Builder#content(String)} — plain string content</li>
 *   <li>{@link MessageParam.Content#ofContentBlockParams(List)} — tool result turn</li>
 *   <li>{@link ToolUnion#ofTool(Tool)} — wraps Tool for builder.tools()</li>
 *   <li>{@link Tool.InputSchema.Builder} — nested inside Tool</li>
 * </ul>
 *
 * @author Ashesh
 */
@Component
public class AnthropicGateway {

    private static final Logger log = LoggerFactory.getLogger(AnthropicGateway.class);
    private static final int MAX_ITERATIONS = 10;

    private final AnthropicClient client;
    private final RetryPolicy retryPolicy;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Constructs the gateway with the Anthropic SDK client and retry policy.
     *
     * @param client      the configured {@link AnthropicClient} for API calls
     * @param retryPolicy the retry policy applied to every Claude API call
     */
    public AnthropicGateway(AnthropicClient client, RetryPolicy retryPolicy) {
        this.client = client;
        this.retryPolicy = retryPolicy;
    }

    /**
     * Runs the full agentic tool-call loop until Claude stops requesting tools.
     */
    public String runToolLoop(AgentMetadata meta,
                              String userMessage,
                              List<AgentTool> tools,
                              AgentContext ctx) {

        Map<String, AgentTool> toolMap = buildToolMap(tools);
        List<ToolUnion> sdkTools = buildSdkTools(tools);

        MessageCreateParams.Builder paramsBuilder = MessageCreateParams.builder()
                .model(meta.getModel())
                .maxTokens(meta.getMaxTokens())
                .addUserMessage(userMessage);

        if (meta.getSystemPrompt() != null && !meta.getSystemPrompt().isBlank()) {
            paramsBuilder.system(meta.getSystemPrompt());
        }
        if (!sdkTools.isEmpty()) {
            paramsBuilder.tools(sdkTools);
        }

        return retryPolicy.execute(
                () -> loop(paramsBuilder, toolMap, ctx),
                ctx.retryContext()
        );
    }

    private String loop(MessageCreateParams.Builder paramsBuilder,
                        Map<String, AgentTool> toolMap,
                        AgentContext ctx) {

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {

            Message response = client.messages().create(paramsBuilder.build());
            log.debug("[{}] iteration={} stop_reason={} blocks={}",
                    ctx.correlationId(), iteration,
                    response.stopReason(), response.content().size());

            // Collect text using Optional accessor — ContentBlock.text() returns Optional<TextBlock>
            String text = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(TextBlock::text)
                    .collect(Collectors.joining());

            // Collect tool_use blocks — ContentBlock.toolUse() returns Optional<ToolUseBlock>
            List<ToolUseBlock> toolCalls = response.content().stream()
                    .flatMap(block -> block.toolUse().stream())
                    .collect(Collectors.toList());

            // No tool calls → Claude is done
            if (toolCalls.isEmpty()) {
                return text;
            }

            // Append Claude's assistant turn to history via addMessage(Message)
            paramsBuilder.addMessage(response);

            // Execute each tool and collect results
            List<ContentBlockParam> toolResultParams = new ArrayList<>();
            for (ToolUseBlock tc : toolCalls) {
                log.debug("[{}] Invoking tool '{}'", ctx.correlationId(), tc.name());

                // _input() returns JsonValue — convert to Map<String, Object> for field injection
                Map<String, Object> rawInput = tc._input().convert(new TypeReference<Map<String, Object>>() {});
                Map<String, Object> inputMap = rawInput != null ? rawInput : Collections.emptyMap();

                String result = invokeTool(tc.name(), inputMap, toolMap, ctx);

                toolResultParams.add(
                        ContentBlockParam.ofToolResult(
                                ToolResultBlockParam.builder()
                                        .toolUseId(tc.id())
                                        .content(result)
                                        .build()
                        )
                );
            }

            paramsBuilder.addMessage(
                    MessageParam.builder()
                            .role(MessageParam.Role.USER)
                            .content(MessageParam.Content.ofBlockParams(toolResultParams))
                            .build()
            );
        }

        throw new AgentException("Tool loop exceeded max iterations (" + MAX_ITERATIONS + ")");
    }

    // ── Tool invocation ────────────────────────────────────────────────────────

    private String invokeTool(String name,
                              Map<String, Object> inputMap,
                              Map<String, AgentTool> toolMap,
                              AgentContext ctx) {
        AgentTool tool = toolMap.get(name);
        if (tool == null) {
            log.warn("[{}] Unknown tool: '{}'", ctx.correlationId(), name);
            return "Error: unknown tool '" + name + "'";
        }
        try {
            // Populate public instance fields from the input map
            for (Field field : tool.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                Object val = inputMap.get(field.getName());
                if (val == null) continue;

                if (field.getType() == String.class) {
                    field.set(tool, val.toString());
                } else if (field.getType() == int.class || field.getType() == Integer.class) {
                    field.setInt(tool, ((Number) val).intValue());
                } else if (field.getType() == long.class || field.getType() == Long.class) {
                    field.setLong(tool, ((Number) val).longValue());
                } else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                    field.setBoolean(tool, (Boolean) val);
                } else if (field.getType() == double.class || field.getType() == Double.class) {
                    field.setDouble(tool, ((Number) val).doubleValue());
                }
            }
            // All tools implement Supplier<String>
            if (tool instanceof Supplier<?> supplier) {
                Object result = supplier.get();
                return result != null ? result.toString() : "(null result)";
            }
            return "Tool executed (missing Supplier<String>)";
        } catch (Exception e) {
            log.error("[{}] Tool '{}' threw: {}", ctx.correlationId(), name, e.getMessage(), e);
            return "Tool error [" + name + "]: " + e.getMessage();
        }
    }

    // ── SDK schema + tool map builders ────────────────────────────────────────

    private Map<String, AgentTool> buildToolMap(List<AgentTool> tools) {
        Map<String, AgentTool> map = new LinkedHashMap<>();
        for (AgentTool t : tools) {
            ToolDefinition def = t.getClass().getAnnotation(ToolDefinition.class);
            if (def != null) map.put(def.name(), t);
        }
        return map;
    }

    /**
     * Builds the list of {@link ToolUnion} that {@code MessageCreateParams.Builder.tools()} expects.
     * Each tool's public instance fields become the JSON Schema properties.
     */
    private List<ToolUnion> buildSdkTools(List<AgentTool> tools) {
        List<ToolUnion> result = new ArrayList<>();

        for (AgentTool tool : tools) {
            ToolDefinition def = tool.getClass().getAnnotation(ToolDefinition.class);
            if (def == null) continue;

            // Build JSON Schema properties: each field → JsonValue-wrapped ObjectNode
            Tool.InputSchema.Properties.Builder propsBuilder = Tool.InputSchema.Properties.builder();
            for (Field field : tool.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                ObjectNode propDef = mapper.createObjectNode();
                propDef.put("type", toJsonSchemaType(field.getType()));
                JsonPropertyDescription desc = field.getAnnotation(JsonPropertyDescription.class);
                if (desc != null) propDef.put("description", desc.value());
                propsBuilder.putAdditionalProperty(field.getName(), JsonValue.fromJsonNode(propDef));
            }

            Tool.InputSchema inputSchema = Tool.InputSchema.builder()
                    .type(JsonValue.from("object"))
                    .properties(propsBuilder.build())
                    .build();

            // Wrap in ToolUnion — what MessageCreateParams.Builder.tools(List<ToolUnion>) requires
            result.add(ToolUnion.ofTool(
                    Tool.builder()
                            .name(def.name())
                            .description(def.description())
                            .inputSchema(inputSchema)
                            .build()
            ));
        }
        return result;
    }

    private String toJsonSchemaType(Class<?> type) {
        if (type == int.class || type == Integer.class
                || type == long.class || type == Long.class)    return "integer";
        if (type == boolean.class || type == Boolean.class)     return "boolean";
        if (type == double.class || type == Double.class
                || type == float.class || type == Float.class)  return "number";
        return "string";
    }
}
