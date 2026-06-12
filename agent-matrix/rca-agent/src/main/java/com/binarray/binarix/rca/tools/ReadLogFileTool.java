package com.binarray.binarix.rca.tools;

import com.binarray.binarix.core.api.tool.AgentTool;
import com.binarray.binarix.core.api.tool.ToolDefinition;
import com.binarray.binarix.core.impl.autoconfigure.AgentCoreProperties;
import com.binarray.binarix.rca.config.RcaProperties;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Tool to read the log file.
 *
 * @author Ashesh
 */
@ToolDefinition(
    name = "read_log_file",
    description = "Reads a log file from the configured log base path. Returns file content.",
    tags = {"log-reading"},
    readOnly = true
)
@Component
public class ReadLogFileTool implements AgentTool, Supplier<String> {

    @JsonPropertyDescription("Path to the log file relative to the configured log base path")
    public String filePath = "";

    @JsonPropertyDescription("Maximum number of lines to read. Default is 1000. Use -1 for all lines.")
    public int maxLines = 1000;

    private final RcaProperties props;
    private final AgentCoreProperties coreProps;

    /**
     * Constructs the tool with RCA and core properties for log path resolution and security jailing.
     *
     * @param props     the RCA configuration properties providing the log base path
     * @param coreProps the core configuration providing the security log jail path
     */
    public ReadLogFileTool(RcaProperties props, AgentCoreProperties coreProps) {
        this.props = props;
        this.coreProps = coreProps;
    }

    /**
     * Reads and returns the content of the log file at {@link #filePath}.
     * <p>
     * The path is resolved relative to the configured log base path and validated
     * against the jail before reading. Lines are limited to {@link #maxLines}.
     * </p>
     *
     * @return the log file content as a newline-delimited string,
     *         or a descriptive error message if the file cannot be read
     */
    @Override
    public String get() {
        if (filePath == null || filePath.isBlank()) return "Error: filePath is required";
        try {
            Path base = Path.of(props.getLog().getBasePath()).toAbsolutePath().normalize();
            Path securityJail = Path.of(coreProps.getSecurity().getLogBasePath()).toAbsolutePath().normalize();
            Path target = base.resolve(filePath).normalize();
            if (!target.startsWith(base)) return "Security error: path outside base directory";
            if (!target.startsWith(securityJail)) return "Security error: path outside security boundary";
            if (!Files.exists(target)) return "File not found: " + filePath;
            if (maxLines <= 0) {
                return Files.readString(target);
            }
            try (var lines = Files.lines(target)) {
                return lines.limit(maxLines).collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            return "Error reading log file: " + e.getMessage();
        }
    }
}
