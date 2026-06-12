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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This tool searches a log file for lines matching a regex pattern.
 *
 * @author Ashesh
 */
@ToolDefinition(
    name = "grep_pattern",
    description = "Searches a log file for lines matching a regex pattern. Returns matching lines with line numbers.",
    tags = {"log-reading"},
    readOnly = true
)
@Component
public class GrepPatternTool implements AgentTool, Supplier<String> {

    @JsonPropertyDescription("Path to the log file to search")
    public String filePath = "";

    @JsonPropertyDescription("Regular expression pattern to search for")
    public String pattern = "";

    @JsonPropertyDescription("Maximum number of matching lines to return")
    public int maxMatches = 100;

    private final RcaProperties props;
    private final AgentCoreProperties coreProps;

    /**
     * Constructs the tool with RCA and core properties for log path resolution and security jailing.
     *
     * @param props     the RCA configuration properties providing the log base path
     * @param coreProps the core configuration providing the security log jail path
     */
    public GrepPatternTool(RcaProperties props, AgentCoreProperties coreProps) {
        this.props = props;
        this.coreProps = coreProps;
    }

    /**
     * Searches the log file at {@link #filePath} for lines matching {@link #pattern}.
     *
     * @return newline-delimited matches in {@code "lineNum: content"} format,
     *         a no-match message, or an error message if the search fails
     */
    @Override
    public String get() {
        if (filePath.isBlank() || pattern.isBlank()) return "Error: filePath and pattern are required";
        try {
            Path base = Path.of(props.getLog().getBasePath()).toAbsolutePath().normalize();
            Path securityJail = Path.of(coreProps.getSecurity().getLogBasePath()).toAbsolutePath().normalize();
            Path target = base.resolve(filePath).normalize();
            if (!target.startsWith(base)) return "Security error: path outside base directory";
            if (!target.startsWith(securityJail)) return "Security error: path outside security boundary";

            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            final int[] lineNum = {0};
            try (var lines = Files.lines(target)) {
                String result = lines
                        .peek(l -> lineNum[0]++)
                        .filter(l -> regex.matcher(l).find())
                        .limit(maxMatches)
                        .map(l -> lineNum[0] + ": " + l)
                        .collect(Collectors.joining("\n"));
                return result.isEmpty() ? "No matches found for pattern: " + pattern : result;
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
