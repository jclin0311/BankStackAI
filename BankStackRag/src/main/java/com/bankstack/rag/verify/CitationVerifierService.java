package com.bankstack.rag.verify;

import com.bankstack.rag.assemble.AssembledContext;
import com.bankstack.rag.assemble.ContextSnippet;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CitationVerifierService {
	
	private static final Pattern CITATION_PATTERN = Pattern.compile("\\[(?:C)?(\\d+)]");

    private static final String FALLBACK = "The provided documents do not contain enough information.";

    public VerifiedAnswer verify(String answer, AssembledContext context) {
        if (answer == null || answer.isBlank()) {
            return new VerifiedAnswer(FALLBACK, List.of(), false);
        }

        List<CitationReference> references = extractCitationReferences(answer);
        Map<Integer, ContextSnippet> snippetByRef = context.snippets().stream()
                .collect(Collectors.toMap(ContextSnippet::referenceNumber, s -> s));

        List<CitationCheckResult> checks = new ArrayList<>();

        for (CitationReference reference : references) {
            ContextSnippet snippet = snippetByRef.get(reference.referenceNumber());

            if (snippet == null) {
                checks.add(new CitationCheckResult(
                        reference.referenceNumber(),
                        false,
                        "Citation Reference number not found in assembled context"
                ));
            } else {
                checks.add(new CitationCheckResult(
                        reference.referenceNumber(),
                        true,
                        "Citation reference exists in assembled context"
                ));
            }
        }

        boolean fullyVerified = !references.isEmpty()
                && !checks.isEmpty()
                && checks.stream().allMatch(CitationCheckResult::valid);

        if (!references.isEmpty()) {
            return new VerifiedAnswer(answer.trim(), checks, fullyVerified);
        }

        return new VerifiedAnswer(
                "The answer could not be verified because no valid citation was found.",
                List.of(),
                false
        );
    }

    List<CitationReference> extractCitationReferences(String answer) {
        List<CitationReference> references = new ArrayList<>();

        for (String sentence : splitIntoSentences(answer)) {
            Matcher matcher = CITATION_PATTERN.matcher(sentence);
            while (matcher.find()) {
                int ref = Integer.parseInt(matcher.group(1));
                references.add(new CitationReference(ref, sentence.trim()));
            }
        }

        return references;
    }

    List<String> splitIntoSentences(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> raw = Arrays.stream(text.split("(?<=[.!?])\\s+|\\n+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        List<String> merged = new ArrayList<>();
        for (String part : raw) {
            if (part.matches("^\\[(?:C)?(\\d+)]$") && !merged.isEmpty()) {
                int last = merged.size() - 1;
                merged.set(last, merged.get(last) + " " + part);
            } else {
                merged.add(part);
            }
        }

        return merged;
    }

    private String summarizeTopSnippet(ContextSnippet snippet) {
        String text = snippet.text() == null ? "" : snippet.text().trim();

        List<String> lines = Arrays.stream(text.split("\\n"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        String summary;
        if (lines.isEmpty()) {
            summary = FALLBACK;
        } else if (lines.size() >= 2 && lines.get(1).length() < 220) {
            summary = lines.get(1);
        } else {
            summary = lines.get(0);
        }

        if (!summary.endsWith(".")) {
            summary = summary + ".";
        }

        return summary + " [" + snippet.referenceNumber() + "]";
    }
}