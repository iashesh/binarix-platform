package com.binarray.binarix.rca.tools;

import com.binarray.binarix.core.api.tool.AgentTool;
import com.binarray.binarix.core.api.tool.ToolDefinition;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.*;

/**
 * Tool to process stacktrace.
 *
 * @author Ashesh
 */
@ToolDefinition(
    name = "parse_stack_trace",
    description = "Parses a Java/Python/Node.js stack trace text and returns structured frame information.",
    tags = {"log-reading"},
    readOnly = true
)
@Component
public class ParseStackTraceTool implements AgentTool, Supplier<String> {

    @JsonPropertyDescription("The raw stack trace text to parse")
    public String stackTraceText = "";

    private static final Pattern JAVA_FRAME = Pattern.compile("\tat ([\\w.$]+)\\.([\\w$<>]+)\\(([\\w.]+):(\\d+)\\)");
    private static final Pattern EXCEPTION_LINE = Pattern.compile("^([\\w.$]+(?:Exception|Error|Throwable)[^:]*):(.*)");

    /**
     * Parses {@link #stackTraceText} and returns a structured plain-text representation.
     * <p>
     * Extracts exception class names, messages, and individual stack frames.
     * Each frame is output as {@code FRAME: ClassName.method at FileName:lineNumber}.
     * </p>
     *
     * @return a formatted multi-line string with exception type, message, and frames;
     *         or an error message if {@link #stackTraceText} is blank
     */
    @Override
    public String get() {
        if (stackTraceText.isBlank()) return "Error: stackTraceText is required";
        StringBuilder result = new StringBuilder();
        String[] lines = stackTraceText.split("\n");

        result.append("=== PARSED STACK TRACE ===\n");
        for (String line : lines) {
            Matcher exMatcher = EXCEPTION_LINE.matcher(line.trim());
            if (exMatcher.matches()) {
                result.append("EXCEPTION: ").append(exMatcher.group(1))
                      .append("\nMESSAGE: ").append(exMatcher.group(2).trim()).append("\n");
            }
            Matcher frameMatcher = JAVA_FRAME.matcher(line);
            if (frameMatcher.find()) {
                result.append("  FRAME: ").append(frameMatcher.group(1))
                      .append(".").append(frameMatcher.group(2))
                      .append(" at ").append(frameMatcher.group(3))
                      .append(":").append(frameMatcher.group(4)).append("\n");
            }
        }
        return result.toString();
    }
}
