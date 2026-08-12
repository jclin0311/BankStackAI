package com.bankstack.mcpclient.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.bankstack.mcpclient.extraction.ExtractionGroundingValidator;

import java.math.BigDecimal;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


@Component
public class ToolArgumentExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ToolArgumentExtractionService.class);

    private final LlmStructuredOutputService llmStructuredOutputService;
    private final ExtractionGroundingValidator groundingValidator;

    public ToolArgumentExtractionService(
            LlmStructuredOutputService llmStructuredOutputService,
            ExtractionGroundingValidator groundingValidator
    ) {
        this.llmStructuredOutputService = llmStructuredOutputService;
        this.groundingValidator = groundingValidator;
    }

    public Map<String, Object> extract(String toolName,
                                       String userMessage,
                                       ConversationContext context) {
        if (toolName == null || toolName.isBlank()) {
            return Map.of();
        }

        Map<String, Object> extracted = switch (toolName) {
            case "getAccountBalance" -> toMap(extractAccountBalance(userMessage));
            case "getTransactions" -> toMap(extractTransactions(userMessage));
            case "getCustomerProfile" -> toMap(extractCustomerProfile(userMessage));
            case "getPaymentStatus" -> toMap(extractPaymentStatus(userMessage));
            case "prepareBillPay" -> toMap(extractBillPay(userMessage));
            case "searchPolicyDocuments", "confirmBillPay" -> Map.of();
            default -> Map.of();
        };

        return withConfidence(toolName, extracted);
    }


    private Map<String, Object> withConfidence(String toolName, Map<String, Object> args) {
        Map<String, Object> map = new HashMap<>(args == null ? Map.of() : args);
        map.put("_extractionModel", "qwen2.5:1.5b");
        map.put("_extractionConfidence", estimateConfidence(toolName, map));
        return map;
    }

    private double estimateConfidence(String toolName, Map<String, Object> args) {
        if (toolName == null || args == null || args.isEmpty()) {
            return 0.0;
        }

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
            return 0.5;
        }

        int present = 0;
        for (String field : required) {
            Object value = args.get(field);
            if (value != null && !value.toString().trim().isEmpty()) {
                present++;
            }
        }
        return Math.round(((double) present / required.length) * 100.0) / 100.0;
    }

    private AccountBalanceArgs extractAccountBalance(String message) {
        String system = """
                Extract arguments for getAccountBalance.
                Return STRICT JSON only. No markdown. No explanation. Return one complete JSON object with a closing brace.
                JSON shape:
                {"accountId": null}
                Rules:
                - Copy account identifiers exactly as written.
                - Do not invent missing values.
                """;
        return call(system, message, AccountBalanceArgs.class, new AccountBalanceArgs(null));
    }

    private TransactionArgs extractTransactions(String message) {
        String system = """
                Extract arguments for getTransactions.
                Return STRICT JSON only. No markdown. No explanation. Return one complete JSON object with a closing brace.
                JSON shape:
                {
                  "accountId": null,
                  "date": null,
                  "startDate": null,
                  "endDate": null,
                  "type": null,
                  "limit": null,
                  "offset": null
                }
                Rules:
                - If the user gives one calendar date, put it in startDate as yyyy-MM-dd.
                - If the user gives a date range, use startDate and endDate.
                - Convert natural dates like "4th May 2016" to "2016-05-04".
                - Copy account identifiers exactly as written.
                - Do not invent missing values.
                """;
        return call(system, message, TransactionArgs.class,
                new TransactionArgs(null, null, null, null, null, null, null));
    }

    private CustomerProfileArgs extractCustomerProfile(String message) {
        String system = """
                Extract arguments for getCustomerProfile.
                Return STRICT JSON only. No markdown. No explanation. Return one complete JSON object with a closing brace.
                JSON shape:
                {"customerExternalId": null}
                Rules:
                - Extract customerExternalId only if explicitly provided.
                - Do not treat generic phrases like "my profile" as an id.
                - Do not invent missing values.
                """;
        return call(system, message, CustomerProfileArgs.class, new CustomerProfileArgs(null));
    }

    private PaymentStatusArgs extractPaymentStatus(String message) {
        String system = """
                Extract arguments for getPaymentStatus.
                Return STRICT JSON only. No markdown. No explanation. Return one complete JSON object with a closing brace.
                JSON shape:
                {"paymentId": null}
                Rules:
                - Copy payment identifiers exactly as written.
                - Do not invent missing values.
                """;
        return call(system, message, PaymentStatusArgs.class, new PaymentStatusArgs(null));
    }
    private BillPayArgs extractBillPay(String message) {
        String system = """
                 You extract arguments for prepareBillPay.

            Return STRICT JSON only. No markdown. No explanation. Return one complete JSON object with a closing brace.

            JSON shape:
            {
              "debtorAccountId": null,
              "billerReferenceNumber": null,
              "invoiceReference": null,
              "executionDate": null,
              "amount": null,
              "currency": null,
              "note": null,
              "idempotencyKey": null
            }

            Rules:
            - Extract debtorAccountId from phrases like "from account <value>" or "account <value>".
            - Extract billerReferenceNumber only from explicit phrases like "biller reference <value>", "biller ref <value>", "biller <value>", or "payee <value>".
            - Extract invoiceReference only from explicit phrases like "invoice <value>" or "inv <value>".
            - NEVER copy an invoice value into billerReferenceNumber.
            - NEVER copy a biller reference value into invoiceReference.
            - Extract amount as a number only, for example 5.25.
            - Extract currency separately, for example CAD.
            - Normalize currency to uppercase.
            - executionDate must be yyyy-MM-dd only.
            - If date is written as MM-dd with no year, assume current year 2026.
              Example: "05-10" means "2026-05-10".
            - If date is written as "4th May 2016", return "2016-05-04".
            - Copy identifiers exactly as written.
            - NEVER invent values.
            - If a value is not explicitly present in the user message, return null.
            - Do NOT infer amount.
            - Do NOT infer executionDate.
            - Do NOT infer currency.
            - Do NOT infer invoiceReference.
            - Do NOT infer billerReferenceNumber.
            - Do NOT infer debtorAccountId.
            - Missing values MUST remain null.
            """;

        BillPayArgs rawArgs = call(system, message, BillPayArgs.class,
            new BillPayArgs(null, null, null, null, null, null, null, null));
    
    
    ExtractionGroundingValidator.GroundingResult grounding =
            groundingValidator.validate(message, rawArgs);

    return grounding.args();
    
    
    }


    
    private String normalizeForContains(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    @FunctionalInterface
    private interface GroundingCheck {
        boolean isGrounded(Object value);
    }

    private <T> T call(String systemPrompt, String message, Class<T> targetType, T fallback) {
        return llmStructuredOutputService.call(
                systemPrompt,
                message == null ? "" : message,
                targetType,
                fallback
        );
    }

    private Map<String, Object> toMap(AccountBalanceArgs args) {
        Map<String, Object> map = new HashMap<>();
        put(map, "accountId", args.accountId());
        return map;
    }

    private Map<String, Object> toMap(TransactionArgs args) {
        Map<String, Object> map = new HashMap<>();
        put(map, "accountId", args.accountId());
        put(map, "date", args.date());
        put(map, "startDate", args.startDate());
        put(map, "endDate", args.endDate());
        put(map, "type", args.type());
        put(map, "limit", args.limit());
        put(map, "offset", args.offset());
        return map;
    }

    private Map<String, Object> toMap(CustomerProfileArgs args) {
        Map<String, Object> map = new HashMap<>();
        put(map, "customerExternalId", args.customerExternalId());
        return map;
    }

    private Map<String, Object> toMap(PaymentStatusArgs args) {
        Map<String, Object> map = new HashMap<>();
        put(map, "paymentId", args.paymentId());
        return map;
    }
    private Map<String, Object> toMap(BillPayArgs args) {
        Map<String, Object> map = new HashMap<>();
        put(map, "debtorAccountId", args.debtorAccountId());
        put(map, "billerReferenceNumber", args.billerReferenceNumber());
        put(map, "invoiceReference", args.invoiceReference());
        put(map, "executionDate", args.executionDate());
        put(map, "amount", args.amount());
        put(map, "currency", args.currency());
        put(map, "note", args.note());
        put(map, "idempotencyKey", args.idempotencyKey());
        return map;
    }

    private void put(Map<String, Object> map, String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String text && text.isBlank()) {
            return;
        }
        map.put(key, value);
    }

    public record AccountBalanceArgs(String accountId) {}

    public record TransactionArgs(String accountId,
                                  String date,
                                  String startDate,
                                  String endDate,
                                  String type,
                                  Integer limit,
                                  Integer offset) {}

    public record CustomerProfileArgs(String customerExternalId) {}

    public record PaymentStatusArgs(String paymentId) {}

    public record RiskAssessmentArgs(String accountId,
                                     BigDecimal amount,
                                     String payee,
                                     String channel) {}

    public record BillPayArgs(String debtorAccountId,
                              String billerReferenceNumber,
                              String invoiceReference,
                              String executionDate,
                              BigDecimal amount,
                              String currency,
                              String note,
                              String idempotencyKey) {}
}
