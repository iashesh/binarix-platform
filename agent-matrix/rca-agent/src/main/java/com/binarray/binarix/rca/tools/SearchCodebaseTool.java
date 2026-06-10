package com.binarray.binarix.rca.tools;

import com.binarray.binarix.core.api.adapter.CodebaseAdapter;
import com.binarray.binarix.core.api.tool.AgentTool;
import com.binarray.binarix.core.api.tool.ToolDefinition;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Search source code for text.
 *
 * @author Ashesh
 */
@ToolDefinition(
    name = "search_codebase",
    description = "Searches the codebase for a text pattern. Returns file:line matches.",
    tags = {"code-reading"},
    readOnly = true
)
@Component
public class SearchCodebaseTool implements AgentTool, Supplier<String> {

    @JsonPropertyDescription("Text or pattern to search for in the codebase")
    public String query = "";

    @JsonPropertyDescription("Base directory to search in, relative to codebase root")
    public String baseDir = ".";

    private final List<CodebaseAdapter> adapters;

    /**
     * Constructs the tool with all available codebase adapters.
     *
     * @param adapters the list of {@link CodebaseAdapter} implementations registered
     *                 in the Spring context
     */
    public SearchCodebaseTool(List<CodebaseAdapter> adapters) { this.adapters = adapters; }

    /**
     * Searches the codebase for {@link #query} within {@link #baseDir}.
     * <p>
     * Delegates to the first {@link CodebaseAdapter} that supports the {@code "local"} type.
     * Returns up to 50 matches.
     * </p>
     *
     * @return newline-delimited matches in {@code file:lineNumber: content} format,
     *         a no-match message, or an error message
     */
    @Override
    public String get() {
        if (query.isBlank()) return "Error: query is required";
        List<String> results = adapters.stream().filter(a -> a.supports("local")).findFirst()
                .map(a -> a.searchText(query, baseDir))
                .orElse(List.of("No codebase adapter available"));
        return results.isEmpty() ? "No matches found for: " + query
                : String.join("\n", results);
    }
}
