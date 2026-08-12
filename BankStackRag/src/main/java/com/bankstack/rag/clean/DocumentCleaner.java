package com.bankstack.rag.clean;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Pre-embedding document cleaner.
 *
 * Production idea:
 * The vector database should store useful knowledge, not repeated headers,
 * footers, page numbers, legal boilerplate, or empty formatting noise.
 */
@Component
public class DocumentCleaner {

    private static final Pattern PAGE_NUMBER = Pattern.compile("^\\s*(page\\s+)?\\d+\\s*(of\\s+\\d+)?\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONFIDENTIAL_FOOTER = Pattern.compile(".*(confidential|internal use only|all rights reserved).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXCESS_BLANK_LINES = Pattern.compile("\\n{3,}");

    /**
     * Clean raw text before chunking and embedding.
     */
    public String clean(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        List<String> cleanedLines = Arrays.stream(rawText.replace("\r\n", "\n").replace('\r', '\n').split("\n"))
                .map(String::strip)
                .filter(line -> !line.isBlank())
                .filter(line -> !PAGE_NUMBER.matcher(line).matches())
                .filter(line -> !CONFIDENTIAL_FOOTER.matcher(line).matches())
                .filter(line -> !isRepeatedDivider(line))
                .collect(Collectors.toList());

        String cleaned = String.join("\n", cleanedLines);
        cleaned = EXCESS_BLANK_LINES.matcher(cleaned).replaceAll("\n\n");
        return cleaned.strip();
    }

    private boolean isRepeatedDivider(String line) {
        return line.matches("^[=_\\-*]{3,}$");
    }
}
