package com.bankstack.mcpclient.guardrails;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Agent-side input hygiene guard.
 *
 * This guard is deliberately narrow. It is not a second refusal classifier and
 * it does not perform banking authorization. It only catches attempts to
 * manipulate the agent's instructions or bypass the conversation workflow
 * before the request reaches route classification.
 *
 * Unsafe banking intent, such as bypassing bank authentication or accessing
 * another customer's account, belongs to SafetyRouteGuard and should become
 * REFUSE through normal routing.
 */
@Component
public class PromptInjectionGuard {

    private static final List<Pattern> INSTRUCTION_MANIPULATION_PATTERNS = List.of(
            Pattern.compile("\\bignore\\s+(all\\s+)?(previous|prior|above)\\s+instructions\\b"),
            Pattern.compile("\\bforget\\s+(all\\s+)?(previous|prior|above)\\s+instructions\\b"),
            Pattern.compile("\\boverride\\s+(your\\s+)?(instructions|rules|system\\s+message|developer\\s+message)\\b"),
            Pattern.compile("\\bdo\\s+not\\s+follow\\s+(the\\s+)?(instructions|rules|policy|policies)\\b"),
            Pattern.compile("\\bdisregard\\s+(the\\s+)?(instructions|rules|policy|policies)\\b"),
            Pattern.compile("\\breveal\\s+(the\\s+)?(system|developer)\\s+prompt\\b"),
            Pattern.compile("\\bshow\\s+(me\\s+)?(the\\s+)?(system|developer)\\s+prompt\\b"),
            Pattern.compile("\\bjailbreak\\b"),
            Pattern.compile("\\bdeveloper\\s+mode\\b"),
            Pattern.compile("\\bpretend\\s+you\\s+are\\s+(an\\s+)?(admin|administrator|internal\\s+user|system)\\b"),
            Pattern.compile("\\bact\\s+as\\s+(an\\s+)?(admin|administrator|root|system|internal\\s+user)\\b"),
            Pattern.compile("\\bbypass\\s+(the\\s+)?confirmation\\b"),
            Pattern.compile("\\bskip\\s+(the\\s+)?confirmation\\b"),
            Pattern.compile("\\bconfirm\\s+.*\\bwithout\\s+(a\\s+)?(token|confirmation)\\b")
    );

    public GuardrailDecision evaluate(String message) {
        if (message == null || message.isBlank()) {
            return GuardrailDecision.allow("PROMPT_INJECTION");
        }

        String normalized = message.toLowerCase(Locale.ROOT).trim();

        for (Pattern pattern : INSTRUCTION_MANIPULATION_PATTERNS) {
            if (pattern.matcher(normalized).find()) {
                return GuardrailDecision.deny(
                        "PROMPT_INJECTION",
                        "HIGH",
                        "The request looks like an attempt to manipulate agent instructions or bypass the conversation workflow."
                );
            }
        }

        return GuardrailDecision.allow("PROMPT_INJECTION");
    }
}
