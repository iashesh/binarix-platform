package com.binarray.binarix.rca.tools;

import com.binarray.binarix.core.api.adapter.CodebaseAdapter;
import com.binarray.binarix.core.api.tool.AgentTool;
import com.binarray.binarix.core.api.tool.ToolDefinition;
import com.binarray.binarix.rca.config.RcaProperties;
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
    private final RcaProperties rcaProps;

    /**
     * Constructs the tool with all available codebase adapters and RCA configuration.
     *
     * @param adapters the list of {@link CodebaseAdapter} implementations registered
     *                 in the Spring context
     * @param rcaProps the RCA configuration providing the active codebase type
     */
    public ReadFileTool(List<CodebaseAdapter> adapters, RcaProperties rcaProps) {
        this.adapters = adapters;
        this.rcaProps = rcaProps;
    }

    /**
     * Reads and returns the content of the source file at {@link #filePath}.
     * <p>
     * Delegates to the {@link CodebaseAdapter} matching the configured
     * {@code rca.codebase.type} (e.g. {@code "local"} or {@code "github"}).
     * </p>
     *
     * @return the file content as a string, or an error message if unavailable
     */
    @Override
    public String get() {
        if (filePath.isBlank()) return "Error: filePath is required";
        String codebaseType = rcaProps.getCodebase().getType();
        return adapters.stream().filter(a -> a.supports(codebaseType)).findFirst()
                .map(a -> a.readFile(filePath))
                .orElse("No codebase adapter available for type: " + codebaseType);
    }
}
