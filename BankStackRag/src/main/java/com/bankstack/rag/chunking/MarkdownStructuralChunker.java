package com.bankstack.rag.chunking;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * MarkdownStructuralChunker:
 *
 * This is a "predictable" chunker designed for teaching + production readability.
 *
 * It treats lines starting with "# " as section headings, like Markdown H1.
 *
 * Example input:
 *   # Security
 *   Token relay forwards user JWT.
 *
 *   # Payments
 *   CanonicalPayment is stored in DB.
 *
 * Output:
 *   RawChunk(sectionPath="Security", ordinal=0, text="Token relay...")
 *   RawChunk(sectionPath="Payments", ordinal=1, text="CanonicalPayment...")
 *
 * Why start with this simple design?
 * - Stability matters more than "perfect chunking" in compliance systems.
 * - Predictable chunk boundaries make citation verification easier later.
 *


 */
public class MarkdownStructuralChunker implements StructuralChunker {
   @Override
    public List<RawChunk> chunk(String inputText) {
        List<RawChunk> out = new ArrayList<>();
        // Track current heading path. For now only top-level, but stack design allows nesting later.
        Stack<String> headingStack = new Stack<>();

        // Buffer collects lines belonging to the current section
        StringBuilder buffer = new StringBuilder();

        // We'll assign ordinals after chunking (cleanest approach)
        int ordinal = 0;

        // Normalize line endings, split into lines
        String[] lines = inputText.replace("\r\n", "\n").split("\n");
         for (String rawLine : lines) {
        	 // Trim only the end so indentation isn’t fully lost
            String line = rawLine.stripTrailing();

            // If this line is a heading, we need to:
            // 1) flush existing buffer as a chunk (previous section)
            // 2) update heading stack to new heading
            if (isHeading(line)) {

                flushIfNeeded(out, headingStack, buffer, ordinal);

                // reset buffer after flush
                buffer.setLength(0);

                // update the "current section"
                String title = line.replaceFirst("^#+\\s*", "").trim();

                headingStack.clear();
                headingStack.push(title);

                continue;
            }
            // If not a heading, it is content under current section.
            // We ignore blank lines (but you could keep them if you want).
            if (!line.isBlank()) {
                buffer.append(line).append("\n");
            }
        }
        flushIfNeeded(out, headingStack, buffer, ordinal);
        List<RawChunk> normalized = new ArrayList<>(out.size());
        for (int i = 0; i < out.size(); i++) {
            RawChunk c = out.get(i);
            normalized.add(new RawChunk(c.sectionPath(), i, c.text()));
        }
        return normalized;
    }

    /**
     * Flushes current buffer into output list if it has content.
     */
    private static void flushIfNeeded(List<RawChunk> out,
                                      Stack<String> headingStack,
                                      StringBuilder buffer,
                                      int ordinal) {

        String text = buffer.toString().trim();

        if (!text.isEmpty()) {
            out.add(new RawChunk(sectionPath(headingStack), ordinal, text));
        }
    }

    /**
     * Checks if a line is a heading.
     * Commit 2 simplicity: only "# " is treated as heading.
     */
    private static boolean isHeading(String line) {
        return line.startsWith("# ");
    }

    /**
     * Builds sectionPath used for citations.
     *
     * Example:
     * headingStack = ["Security", "Token Relay"]
     * sectionPath = "Security > Token Relay"
     *
     * Commit 2: headingStack max 1 entry, but we keep it future-proof.
     */
    private static String sectionPath(Stack<String> headingStack) {
        if (headingStack.isEmpty()) return "ROOT";
        return String.join(" > ", headingStack);
    }
}