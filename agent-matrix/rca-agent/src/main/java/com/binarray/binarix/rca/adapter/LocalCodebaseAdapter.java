package com.binarray.binarix.rca.adapter;

import com.binarray.binarix.core.api.adapter.CodebaseAdapter;
import com.binarray.binarix.core.impl.autoconfigure.AgentCoreProperties;
import com.binarray.binarix.rca.config.RcaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link CodebaseAdapter} implementation that reads source code from the local filesystem.
 * <p>
 * All file and directory access is jailed within the configured
 * {@code rca.codebase.local-base-path} to prevent directory traversal attacks.
 * Any attempt to read a path outside the base directory throws a
 * {@link SecurityException}.
 * </p>
 *
 * <p>Activated when {@code rca.codebase.type=local} (the default).</p>
 *
 * @author Ashesh
 */
@Component
public class LocalCodebaseAdapter implements CodebaseAdapter {

    private static final Logger log = LoggerFactory.getLogger(LocalCodebaseAdapter.class);
    private final RcaProperties props;
    private final AgentCoreProperties coreProps;

    /**
     * Constructs the adapter with the RCA and core properties for base path and security jail configuration.
     *
     * @param props     the RCA configuration properties providing the local codebase base path
     * @param coreProps the core configuration providing the security code jail path
     */
    public LocalCodebaseAdapter(RcaProperties props, AgentCoreProperties coreProps) {
        this.props = props;
        this.coreProps = coreProps;
    }

    /**
     * {@inheritDoc}
     *
     * @param type the codebase type string
     * @return {@code true} if {@code type} equals {@code "local"} (case-insensitive)
     */
    @Override
    public boolean supports(String type) { return "local".equalsIgnoreCase(type); }

    /**
     * {@inheritDoc}
     * <p>Reads the file from the local filesystem after resolving and jailing the path.</p>
     *
     * @param path the file path relative to the configured local base path
     * @return the file content as a string, or an error message if the file cannot be read
     */
    @Override
    public String readFile(String path) {
        try {
            Path resolved = resolveAndJail(path);
            return Files.readString(resolved);
        } catch (IOException e) {
            log.warn("Could not read file '{}': {}", path, e.getMessage());
            return "Error reading file: " + e.getMessage();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Lists files and sub-directories alphabetically.
     * Directory entries are suffixed with {@code /} to distinguish them from files.
     * </p>
     *
     * @param path the directory path relative to the local base path
     * @return an alphabetically sorted list of file and directory names
     */
    @Override
    public List<String> listDirectory(String path) {
        try {
            Path dir = resolveAndJail(path);
            if (!Files.isDirectory(dir)) return List.of("Not a directory: " + path);
            try (var stream = Files.list(dir)) {
                return stream.map(p -> {
                    String name = p.getFileName().toString();
                    return Files.isDirectory(p) ? name + "/" : name;
                }).sorted().collect(Collectors.toList());
            }
        } catch (IOException e) {
            return List.of("Error listing directory: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * <p>Performs a recursive full-text search, returning up to 50 matches.</p>
     *
     * @param query   the text string to search for within source files
     * @param baseDir the directory to restrict the search to, relative to the local base path
     * @return matching lines in {@code file:lineNumber: content} format
     */
    @Override
    public List<String> searchText(String query, String baseDir) {
        List<String> matches = new ArrayList<>();
        try {
            Path start = resolveAndJail(baseDir);
            Files.walkFileTree(start, new SimpleFileVisitor<>() {
                /**
                 * Searches each visited file for the query string and records matching lines.
                 *
                 * @param file  the current file being visited
                 * @param attrs the file attributes
                 * @return {@link FileVisitResult#CONTINUE} or {@link FileVisitResult#TERMINATE}
                 *         when the match limit is reached
                 */
                @Override
                public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) {
                    try {
                        String content = Files.readString(file);
                        if (content.contains(query)) {
                            String[] lines = content.split("\n");
                            for (int i = 0; i < lines.length; i++) {
                                if (lines[i].contains(query)) {
                                    matches.add(file + ":" + (i + 1) + ": " + lines[i].trim());
                                }
                            }
                        }
                    } catch (IOException ignored) {}
                    return matches.size() < 50 ? FileVisitResult.CONTINUE : FileVisitResult.TERMINATE;
                }
            });
        } catch (IOException e) {
            matches.add("Search error: " + e.getMessage());
        }
        return matches;
    }

    /**
     * Resolves the given relative path against the configured base path and validates
     * that the result remains within the jail.
     *
     * @param path the relative path to resolve
     * @return the resolved, normalised absolute {@link Path}
     * @throws IOException       if the path cannot be resolved
     * @throws SecurityException if the resolved path would escape the base directory
     */
    private Path resolveAndJail(String path) throws IOException {
        Path base = Path.of(props.getCodebase().getLocalBasePath()).toAbsolutePath().normalize();
        Path securityJail = Path.of(coreProps.getSecurity().getCodeBasePath()).toAbsolutePath().normalize();
        Path resolved = base.resolve(path).normalize();
        if (!resolved.startsWith(base)) {
            throw new SecurityException("Path escape attempt: " + path);
        }
        if (!resolved.startsWith(securityJail)) {
            throw new SecurityException("Path outside security boundary: " + path);
        }
        return resolved;
    }
}
