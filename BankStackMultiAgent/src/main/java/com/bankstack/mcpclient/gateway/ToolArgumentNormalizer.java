package com.bankstack.mcpclient.gateway;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ToolArgumentNormalizer {

    /**
     * An amount as a customer writes it: "$45", "45 dollars", "of 45.00 CAD".
     * Requires a currency marker so a bare number — an account fragment, a date, a
     * limit — is not mistaken for money.
     */
    private static final Pattern SPOKEN_AMOUNT_PATTERN = Pattern.compile(
            "(?i)(?:\\$\\s*([0-9]+(?:\\.[0-9]{1,2})?))"
                    + "|(?:([0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:dollars?|cad|usd)\\b)"
    );

    /** Mirrors AccountService's TransactionType enum. */
    private static final Set<String> LEDGER_TRANSACTION_TYPES =
            Set.of("CREDIT", "DEBIT", "HOLD_PLACED", "HOLD_RELEASED");

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
    );

    private static final Pattern AMOUNT_CURRENCY_PATTERN = Pattern.compile(
            "(?i)(?:amount|for|of)?\\s*([0-9]+(?:\\.[0-9]{1,2})?)\\s*(CAD|USD)\\b"
    );

    private static final Pattern BILLER_REFERENCE_PATTERN = Pattern.compile(
            "(?i)(?:biller\\s*(?:reference|ref)?|payee|biller)\\s*[:#-]?\\s*([A-Z0-9][A-Z0-9_-]{2,})"
    );

    private static final Pattern INVOICE_PATTERN = Pattern.compile(
            "(?i)(?:invoice|inv)\\s*[:#-]?\\s*([A-Z0-9][A-Z0-9_-]{2,})"
    );

    private static final Pattern EXECUTION_DATE_PATTERN = Pattern.compile(
            "(?i)(?:execution\\s*date|payment\\s*date|scheduled\\s*date|date)\\s*[:#-]?\\s*(\\d{4}-\\d{2}-\\d{2}|\\d{1,2}-\\d{1,2}|\\d{1,2}/\\d{1,2})"
    );

    public Map<String, Object> normalize(String toolName,
                                         String userMessage,
                                         Map<String, Object> input) {
        Map<String, Object> args = new HashMap<>(input == null ? Map.of() : input);

        switch (toolName) {
            case "getAccountBalance" -> normalizeAccountBalance(userMessage, args);
            case "getTransactions" -> normalizeTransactions(userMessage, args);
            case "getCustomerProfile" -> normalizeCustomerProfile(args);
            case "getPaymentStatus" -> normalizePaymentStatus(userMessage, args);
            case "searchPolicyDocuments" -> normalizePolicySearch(userMessage, args);
            case "prepareBillPay" -> normalizeBillPay(userMessage, args);
            case "confirmBillPay" -> normalizeConfirmBillPay(args);
            case "assessTransactionRisk" -> normalizeRiskAssessment(userMessage, args);
            default -> { }
        }

        recomputeConfidence(toolName, args);
        return args;
    }

    private void normalizeAccountBalance(String message, Map<String, Object> args) {
        putAlias(args, "accountId", "account", "accountNumber", "accountNo", "acctId");
        recoverUuid(message, args, "accountId");
    }

    private void normalizeTransactions(String message, Map<String, Object> args) {
        putAlias(args, "accountId", "account", "accountNumber", "accountNo", "acctId");
        recoverUuid(message, args, "accountId");

        Object date = removeFirst(args, "date", "transactionDate", "onDate");
        if (!isBlank(date)) {
            LocalDate parsedDate = parseLocalDate(date.toString());
            if (parsedDate != null) {
                args.putIfAbsent("startDate", parsedDate + "T00:00:00Z");
                args.putIfAbsent("endDate", parsedDate + "T23:59:59Z");
            }
        }

        normalizeOffsetDateTime(args, "startDate");
        normalizeOffsetDateTime(args, "endDate");
        normalizeTransactionType(args);
        normalizeTransactionAmount(message, args);

        args.putIfAbsent("limit", 20);
        args.putIfAbsent("offset", 0);
    }

    /**
     * Lifts an amount out of the question so retrieval can narrow to it.
     *
     * <p>"what was the spending of $45 for" names the one detail that identifies the entry,
     * and without this the amount is discarded and the caller gets the whole recent window
     * back. The MCP tool widens again when nothing matches exactly, so a misremembered
     * figure degrades to a broader answer rather than an empty one.</p>
     */
    private void normalizeTransactionAmount(String message, Map<String, Object> args) {
        putAlias(args, "amount", "value", "transactionAmount");
        if (!isBlank(args.get("amount")) || message == null) {
            return;
        }
        Matcher matcher = SPOKEN_AMOUNT_PATTERN.matcher(message);
        if (matcher.find()) {
            // group 1 is the "$45" form, group 2 the "45 dollars" form
            String value = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (value != null) {
                args.put("amount", value);
            }
        }
    }

    /**
     * Drops a transaction type the ledger does not define.
     *
     * <p>The model readily turns a descriptive word into a filter — "the spending of $45"
     * becomes {@code type=spending}. Passing that through matches no rows, so the caller is
     * told there are zero transactions when in fact the question simply carried no valid
     * filter. Answering from the unfiltered set is right; inventing an empty result is not.</p>
     */
    private void normalizeTransactionType(Map<String, Object> args) {
        Object type = args.get("type");
        if (isBlank(type)) {
            args.remove("type");
            return;
        }
        String candidate = type.toString().trim().toUpperCase(Locale.ROOT);
        if (LEDGER_TRANSACTION_TYPES.contains(candidate)) {
            args.put("type", candidate);
        } else {
            args.remove("type");
        }
    }

    private void normalizeCustomerProfile(Map<String, Object> args) {
        putAlias(args, "customerExternalId", "externalId", "customerId", "customer");
    }

    private void normalizePaymentStatus(String message, Map<String, Object> args) {
        putAlias(args, "paymentId", "payment", "paymentNumber", "paymentNo");
        recoverUuid(message, args, "paymentId");
    }

    private void normalizePolicySearch(String message, Map<String, Object> args) {
        putAlias(args, "query", "question", "q");
        if (isBlank(args.get("query")) && message != null && !message.isBlank()) {
            args.put("query", message.trim());
        }
    }
    private void normalizeBillPay(String message, Map<String, Object> args) {
        putAlias(args, "debtorAccountId", "accountId", "fromAccountId", "account", "debtorAccount");
        putAlias(args, "billerReferenceNumber", "billerRef", "biller", "payee");
        putAlias(args, "invoiceReference", "invoice", "invoiceNumber", "invoiceNo");
        putAlias(args, "executionDate", "date", "paymentDate", "scheduledDate");

        recoverUuid(message, args, "debtorAccountId");
        recoverBillerReference(message, args);
        recoverInvoiceReference(message, args);
        recoverAmountAndCurrency(message, args);
        recoverExecutionDate(message, args);

        normalizeLocalDate(args, "executionDate");
        uppercase(args, "currency");
    }

    private void normalizeConfirmBillPay(Map<String, Object> args) {
        putAlias(args, "confirmationToken", "token", "confirmation", "confirmationId");
    }

    private void normalizeRiskAssessment(String message, Map<String, Object> args) {
        putAlias(args, "accountId", "account", "accountNumber", "accountNo", "acctId", "debtorAccountId");
        putAlias(args, "payee", "biller", "billerReferenceNumber", "beneficiary");
        putAlias(args, "channel", "paymentChannel", "transactionChannel");

        recoverUuid(message, args, "accountId");
        recoverAmountAndCurrency(message, args);
        uppercase(args, "channel");
    }

    private void recoverUuid(String message, Map<String, Object> args, String targetField) {
        if (!isBlank(args.get(targetField)) || message == null) {
            return;
        }
        Matcher matcher = UUID_PATTERN.matcher(message);
        if (matcher.find()) {
            args.put(targetField, matcher.group());
        }
    }

    private void recoverBillerReference(String message, Map<String, Object> args) {
        if (!isBlank(args.get("billerReferenceNumber")) || message == null) {
            return;
        }
        Matcher matcher = BILLER_REFERENCE_PATTERN.matcher(message);
        if (matcher.find()) {
            args.put("billerReferenceNumber", matcher.group(1).toUpperCase(Locale.ROOT));
        }
    }

    private void recoverInvoiceReference(String message, Map<String, Object> args) {
        if (!isBlank(args.get("invoiceReference")) || message == null) {
            return;
        }
        Matcher matcher = INVOICE_PATTERN.matcher(message);
        if (matcher.find()) {
            args.put("invoiceReference", matcher.group(1).toUpperCase(Locale.ROOT));
        }
    }

    private void recoverAmountAndCurrency(String message, Map<String, Object> args) {
        if (message == null) {
            return;
        }
        Matcher matcher = AMOUNT_CURRENCY_PATTERN.matcher(message);
        if (matcher.find()) {
            if (isBlank(args.get("amount"))) {
                args.put("amount", new BigDecimal(matcher.group(1)));
            }
            if (isBlank(args.get("currency"))) {
                args.put("currency", matcher.group(2).toUpperCase(Locale.ROOT));
            }
        }
    }

    private void recoverExecutionDate(String message, Map<String, Object> args) {
        if (!isBlank(args.get("executionDate")) || message == null) {
            return;
        }
        Matcher matcher = EXECUTION_DATE_PATTERN.matcher(message);
        if (matcher.find()) {
            args.put("executionDate", matcher.group(1));
        }
    }

    private void normalizeLocalDate(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (isBlank(value)) {
            return;
        }
        LocalDate parsed = parseLocalDate(value.toString());
        if (parsed != null) {
            args.put(key, parsed.format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
    }

    private void normalizeOffsetDateTime(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (isBlank(value)) {
            return;
        }

        String text = value.toString().trim();
        try {
            OffsetDateTime.parse(text);
            return;
        } catch (DateTimeParseException ignored) {
            // try local date below
        }

        LocalDate date = parseLocalDate(text);
        if (date != null) {
            if ("startDate".equals(key)) {
                args.put(key, date + "T00:00:00Z");
            } else if ("endDate".equals(key)) {
                args.put(key, date + "T23:59:59Z");
            }
        }
    }

    private LocalDate parseLocalDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String text = value.trim();
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException ignored) {
            // try MM-dd or MM/dd below
        }

        String normalized = text.replace('/', '-');
        if (normalized.matches("\\d{1,2}-\\d{1,2}")) {
            String[] parts = normalized.split("-");
            int month = Integer.parseInt(parts[0]);
            int day = Integer.parseInt(parts[1]);
            return LocalDate.of(Year.now().getValue(), month, day);
        }

        return null;
    }

    private void uppercase(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (!isBlank(value)) {
            args.put(key, value.toString().trim().toUpperCase(Locale.ROOT));
        }
    }

    private void putAlias(Map<String, Object> args, String canonical, String... aliases) {
        if (!isBlank(args.get(canonical))) {
            return;
        }

        for (String alias : aliases) {
            Object value = args.remove(alias);
            if (!isBlank(value)) {
                args.put(canonical, value);
                return;
            }
        }
    }

    private Object removeFirst(Map<String, Object> args, String... keys) {
        for (String key : keys) {
            Object value = args.remove(key);
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private void recomputeConfidence(String toolName, Map<String, Object> args) {
        String[] required = switch (toolName) {
            case "getAccountBalance" -> new String[] {"accountId"};
            case "getTransactions" -> new String[] {"accountId"};
            case "getCustomerProfile" -> new String[] {"customerExternalId"};
            case "getPaymentStatus" -> new String[] {"paymentId"};
            case "assessTransactionRisk" -> new String[] {"accountId", "amount", "payee", "channel"};
            case "prepareBillPay" -> new String[] {"debtorAccountId", "billerReferenceNumber", "invoiceReference", "executionDate", "amount", "currency"};
            default -> new String[0];
        };

        if (required.length == 0) {
            return;
        }

        int present = 0;
        for (String field : required) {
            if (!isBlank(args.get(field))) {
                present++;
            }
        }
        args.put("_normalizedConfidence", Math.round(((double) present / required.length) * 100.0) / 100.0);
    }

    private boolean isBlank(Object value) {
        return value == null || value.toString().trim().isEmpty();
    }
}
