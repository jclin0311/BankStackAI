package com.bankstack.mcpclient.extraction;

import com.bankstack.mcpclient.gateway.ToolArgumentExtractionService.BillPayArgs;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class ExtractionGroundingValidator {

    public GroundingResult validate(String message, BillPayArgs args) {

        String lower = message == null
                ? ""
                : message.toLowerCase(Locale.ROOT);

        List<String> removed = new ArrayList<>();

        String debtorAccountId = args.debtorAccountId();
        String billerReferenceNumber = args.billerReferenceNumber();
        String invoiceReference = args.invoiceReference();
        String executionDate = args.executionDate();
        BigDecimal amount = args.amount();
        String currency = args.currency();
        String note = args.note();
        String idempotencyKey = args.idempotencyKey();

        if (currency != null && !lower.contains(currency.toLowerCase(Locale.ROOT))) {
            currency = null;
            removed.add("currency");
        }

        if (executionDate != null && !containsDateEvidence(lower)) {
            executionDate = null;
            removed.add("executionDate");
        }

        if (amount != null && !containsAmountEvidence(lower, amount)) {
            amount = null;
            removed.add("amount");
        }

        if (billerReferenceNumber != null
                && !containsLabeledValueEvidence(lower, billerReferenceNumber,
                "biller reference", "biller ref", "biller", "payee")) {
            billerReferenceNumber = null;
            removed.add("billerReferenceNumber");
        }

        if (invoiceReference != null
                && !containsLabeledValueEvidence(lower, invoiceReference,
                "invoice", "inv")) {
            invoiceReference = null;
            removed.add("invoiceReference");
        }

        BillPayArgs groundedArgs = new BillPayArgs(
                debtorAccountId,
                billerReferenceNumber,
                invoiceReference,
                executionDate,
                amount,
                currency,
                note,
                idempotencyKey
        );

        return new GroundingResult(groundedArgs, removed);
    }

    private boolean containsAmountEvidence(String lower, BigDecimal amount) {
        String plain = amount.stripTrailingZeros().toPlainString();
        return lower.contains(plain);
    }

    private boolean containsDateEvidence(String lower) {
        return lower.matches(".*\\d{4}-\\d{2}-\\d{2}.*")
                || lower.matches(".*\\d{2}-\\d{2}.*")
                || lower.contains("tomorrow")
                || lower.contains("today");
    }

    private boolean containsLabeledValueEvidence(String lower, String value, String... labels) {
        if (lower == null || value == null || value.isBlank()) {
            return false;
        }

        String quotedValue = Pattern.quote(value.toLowerCase(Locale.ROOT));
        for (String label : labels) {
            String quotedLabel = Pattern.quote(label.toLowerCase(Locale.ROOT));
            String pattern = ".*" + quotedLabel + "\\s*[:#-]?\\s*" + quotedValue + ".*";
            if (lower.matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    public record GroundingResult(
            BillPayArgs args,
            List<String> removedFields
    ) {}
}