package com.binarray.binarix.core.impl.tool;

import com.binarray.binarix.core.api.tool.AgentTool;
import com.binarray.binarix.core.api.tool.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Central registry for all {@link AgentTool} beans in the Binarix Platform.
 * <p>
 * Implements {@link ApplicationContextAware} to scan the Spring application context
 * at startup for all beans annotated with {@link ToolDefinition}. Tools are indexed
 * both by name and by tag, allowing agents to retrieve only the tools relevant to
 * their task via {@link #getToolsForTags(String...)}.
 * </p>
 *
 * <p>Tool registration is automatic — domain teams simply annotate their tool classes
 * with {@code @ToolDefinition} and {@code @Component}; no manual wiring is required.</p>
 *
 * @author Ashesh
 */
@Component
public class ToolRegistry implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    /** All registered tools keyed by their {@link ToolDefinition#name()}. */
    private final Map<String, AgentTool> toolsByName = new LinkedHashMap<>();

    /** Inverted index from tag → set of tool names bearing that tag. */
    private final Map<String, Set<String>> tagIndex = new HashMap<>();

    /**
     * Scans the Spring application context for all {@link ToolDefinition}-annotated beans
     * and registers them into the name and tag indexes.
     * <p>
     * Called automatically by Spring after the context is fully initialised.
     * </p>
     *
     * @param ctx the Spring application context to scan
     */
    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        Map<String, Object> beans = ctx.getBeansWithAnnotation(ToolDefinition.class);
        beans.values().forEach(bean -> {
            if (bean instanceof AgentTool tool) {
                ToolDefinition def = bean.getClass().getAnnotation(ToolDefinition.class);
                toolsByName.put(def.name(), tool);
                for (String tag : def.tags()) {
                    tagIndex.computeIfAbsent(tag, t -> new HashSet<>()).add(def.name());
                }
                log.info("Registered tool: {} [tags={}]", def.name(), Arrays.toString(def.tags()));
            }
        });
        log.info("ToolRegistry initialised with {} tools", toolsByName.size());
    }

    /**
     * Returns the subset of registered tools whose {@link ToolDefinition#tags()} contain
     * at least one of the provided tag strings.
     * <p>
     * Used by {@code AbstractAgent} to filter the full tool catalog down to only the tools
     * relevant to a particular agent's task domain.
     * </p>
     *
     * @param tags one or more tag strings to filter by
     * @return an unmodifiable list of matching tools in registration order; never {@code null}
     */
    public List<AgentTool> getToolsForTags(String... tags) {
        return Arrays.stream(tags)
                .flatMap(t -> tagIndex.getOrDefault(t, Set.of()).stream())
                .distinct()
                .map(toolsByName::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Returns the tool registered under the given name, or {@code null} if not found.
     *
     * @param name the {@link ToolDefinition#name()} to look up
     * @return the registered {@link AgentTool}, or {@code null}
     */
    public AgentTool getTool(String name) { return toolsByName.get(name); }

    /**
     * Returns an unmodifiable list of all registered tools.
     *
     * @return all tools in registration order
     */
    public List<AgentTool> getAllTools() { return List.copyOf(toolsByName.values()); }

    /**
     * Returns an unmodifiable view of the name → tool map.
     *
     * @return the full tool registry map
     */
    public Map<String, AgentTool> getToolsByName() { return Collections.unmodifiableMap(toolsByName); }
}
