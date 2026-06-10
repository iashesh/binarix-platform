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
 * This tool lists files and directories at a given path in the codebase.
 *
 * @author Ashesh
 */
@ToolDefinition(
    name = "list_directory",
    description = "Lists files and directories at a given path in the codebase.",
    tags = {"code-reading"},
    readOnly = true
)
@Component
public class ListDirectoryTool implements AgentTool, Supplier<String> {

    @JsonPropertyDescription("Directory path relative to the codebase root")
    public String directoryPath = ".";

    private final List<CodebaseAdapter> adapters;

    /**
     * Constructs the tool with all available codebase adapters.
     *
     * @param adapters the list of {@link CodebaseAdapter} implementations registered
     *                 in the Spring context
     */
    public ListDirectoryTool(List<CodebaseAdapter> adapters) { this.adapters = adapters; }

    /**
     * Lists the contents of the directory at {@link #directoryPath}.
     * <p>
     * Results are alphabetically sorted. Directory entries are suffixed with {@code /}.
     * </p>
     *
     * @return a newline-delimited list of file and directory names,
     *         or an error message if the directory cannot be listed
     */
    @Override
    public String get() {
        return adapters.stream().filter(a -> a.supports("local")).findFirst()
                .map(a -> String.join("\n", a.listDirectory(directoryPath)))
                .orElse("No codebase adapter available");
    }
}
