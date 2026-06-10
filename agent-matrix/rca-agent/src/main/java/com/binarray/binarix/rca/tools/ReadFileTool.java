package com.binarray.binarix.rca.tools;

import com.binarray.binarix.core.api.adapter.CodebaseAdapter;
import com.binarray.binarix.core.api.tool.AgentTool;
import com.binarray.binarix.core.api.tool.ToolDefinition;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

/**
 * Tool to read a source-code file.
 *
 * @author Ashesh
 */
@ToolDefinition(
    name = "read_file",
    description = "Reads a source code file from the configured codebase. Returns file content.",
    tags = {"code-reading"},
    readOnly = true
)
@Component
public class ReadFileTool implements AgentTool, Supplier<String> {

    @JsonPropertyDescription("Path to the source file relative to the codebase root")
    public String filePath = "";

    private final List<CodebaseAdapter> adapters;

    /**
     * Constructs the tool with all available codebase adapters.
     *
     * @param adapters the list of {@link CodebaseAdapter} implementations registered
     *                 in the Spring context
     */
    public ReadFileTool(List<CodebaseAdapter> adapters) { this.adapters = adapters; }

    /**
     * Reads and returns the content of the source file at {@link #filePath}.
     * <p>
     * Delegates to the first {@link CodebaseAdapter} that supports the {@code "local"} type.
     * </p>
     *
     * @return the file content as a string, or an error message if unavailable
     */
    @Override
    public String get() {
        if (filePath.isBlank()) return "Error: filePath is required";
        return adapters.stream().filter(a -> a.supports("local")).findFirst()
                .map(a -> a.readFile(filePath))
                .orElse("No codebase adapter available");
    }
}
