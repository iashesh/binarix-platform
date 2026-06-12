package com.binarray.binarix.rca.tools;

import com.binarray.binarix.core.api.adapter.CodebaseAdapter;
import com.binarray.binarix.core.api.tool.AgentTool;
import com.binarray.binarix.core.api.tool.ToolDefinition;
import com.binarray.binarix.rca.config.RcaProperties;
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
    private final RcaProperties rcaProps;

    /**
     * Constructs the tool with all available codebase adapters and RCA configuration.
     *
     * @param adapters the list of {@link CodebaseAdapter} implementations registered
     *                 in the Spring context
     * @param rcaProps the RCA configuration providing the active codebase type
     */
    public SearchCodebaseTool(List<CodebaseAdapter> adapters, RcaProperties rcaProps) {
        this.adapters = adapters;
        this.rcaProps = rcaProps;
    }

    /**
     * Searches the codebase for {@link #query} within {@link #baseDir}.
     * <p>
     * Delegates to the {@link CodebaseAdapter} matching the configured
     * {@code rca.codebase.type}. Returns up to 50 matches.
     * </p>
     *
     * @return newline-delimited matches in {@code file:lineNumber: content} format,
     *         a no-match message, or an error message
     */
    @Override
    public String get() {
        if (query.isBlank()) return "Error: query is required";
        String codebaseType = rcaProps.getCodebase().getType();
        List<String> results = adapters.stream().filter(a -> a.supports(codebaseType)).findFirst()
                .map(a -> a.searchText(query, baseDir))
                .orElse(List.of("No codebase adapter available for type: " + codebaseType));
        return results.isEmpty() ? "No matches found for: " + query
                : String.join("\n", results);
    }
}
