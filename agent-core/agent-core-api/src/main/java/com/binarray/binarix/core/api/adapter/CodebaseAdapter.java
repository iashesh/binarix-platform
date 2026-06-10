package com.binarray.binarix.core.api.adapter;

import java.util.List;

/**
 * Adapter interface for reading source code from various codebase backends.
 * <p>
 * Implementations exist for local file-system paths ({@code LocalCodebaseAdapter})
 * and remote GitHub repositories ({@code GithubCodebaseAdapter}). The active adapter
 * is selected based on the {@code rca.codebase.type} configuration property.
 * </p>
 *
 * @author Ashesh
 */
public interface CodebaseAdapter {

    /**
     * Reads and returns the full content of a source file at the given path.
     * <p>
     * The path is resolved relative to the configured codebase root and is subject to
     * path-jailing to prevent directory traversal attacks.
     * </p>
     *
     * @param path the file path relative to the codebase root
     * @return the file content as a string, or an error message if the file cannot be read
     */
    String readFile(String path);

    /**
     * Lists the files and sub-directories at the given directory path.
     * <p>
     * Directory entries are suffixed with {@code /} to distinguish them from files.
     * Results are returned in alphabetical order.
     * </p>
     *
     * @param path the directory path relative to the codebase root
     * @return an alphabetically sorted list of file and directory names
     */
    List<String> listDirectory(String path);

    /**
     * Searches the codebase for files containing the given text query.
     * <p>
     * Returns matches in the format {@code file:lineNumber: matchedLine}.
     * Results are capped at a reasonable limit to avoid overwhelming the context window.
     * </p>
     *
     * @param query   the text or pattern to search for within the codebase
     * @param baseDir the directory to restrict the search to, relative to the codebase root
     * @return a list of match strings in {@code file:line: content} format
     */
    List<String> searchText(String query, String baseDir);

    /**
     * Returns {@code true} if this adapter supports the given codebase type identifier.
     * <p>
     * Type identifiers match the {@code rca.codebase.type} configuration value,
     * e.g. {@code "local"} or {@code "github"}.
     * </p>
     *
     * @param type the codebase type string to test
     * @return {@code true} if this adapter handles the given type
     */
    boolean supports(String type);
}
