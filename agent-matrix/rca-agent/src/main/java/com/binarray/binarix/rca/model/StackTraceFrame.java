package com.binarray.binarix.rca.model;

/**
 * Represents a single frame extracted from a Java stack trace by the
 * {@code ParseStackTraceTool}.
 * <p>
 * Provides a structured view of the class, method, and source location for each
 * frame so that the {@code CodeExplorerAgent} can look up the corresponding
 * source code directly.
 * </p>
 *
 * @param className  the fully qualified class name, e.g. {@code "com.example.PaymentGateway"}
 * @param methodName the method name, e.g. {@code "processPayment"}
 * @param fileName   the source file name, e.g. {@code "PaymentGateway.java"}
 * @param lineNumber the line number within the source file
 *
 * @author Ashesh
 */
public record StackTraceFrame(
        String className,
        String methodName,
        String fileName,
        int lineNumber
) {

    /**
     * Returns a human-readable reference string in the format
     * {@code ClassName.methodName(FileName:lineNumber)}.
     * <p>
     * This format matches the standard Java stack trace output and can be used
     * directly in log searches and code navigation.
     * </p>
     *
     * @return the formatted stack frame reference string
     */
    public String toReference() {
        return className + "." + methodName + "(" + fileName + ":" + lineNumber + ")";
    }
}
