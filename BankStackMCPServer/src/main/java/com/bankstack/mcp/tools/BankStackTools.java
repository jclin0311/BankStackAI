package com.bankstack.mcp.tools;

import com.account.dto.AccountBalanceResponse;
import com.account.dto.AmountDTO;
import com.account.dto.CustomerResponse;
import com.account.dto.TransactionResponse;
import com.bankstack.mcp.audit.ToolAuditService;
import com.bankstack.mcp.client.AccountServiceClient;
import com.bankstack.mcp.client.CustomerServiceClient;
import com.bankstack.mcp.client.PaymentOrchestratorClient;
import com.bankstack.mcp.client.PolicySearchClient;
import com.bankstack.mcp.client.TransactionServiceClient;
import com.bankstack.mcp.confirmation.ConfirmationService;
import com.bankstack.mcp.confirmation.PendingToolAction;
import com.bankstack.mcp.dto.PaymentView;
import com.bankstack.mcp.dto.TransactionItem;
import com.commons.exception.InvalidToolInputException;
import com.bankstack.mcp.safety.ToolGuardService;
import com.bankstack.mcp.safety.ToolInvocationContext;
import com.bankstack.mcp.security.CurrentActor;
import com.bankstack.mcp.security.CurrentActorResolver;
import com.bankstack.mcp.security.OwnershipGuardService;
import com.bankstack.mcp.tools.model.AccountBalanceToolResponse;
import com.bankstack.mcp.tools.model.BillPayPreparationResponse;
import com.bankstack.mcp.tools.model.BillPayToolResponse;
import com.bankstack.mcp.tools.model.CustomerProfileToolResponse;
import com.bankstack.mcp.tools.model.FraudRiskAssessmentResponse;
import com.bankstack.mcp.tools.model.PaymentStatusToolResponse;
import com.bankstack.mcp.tools.model.TransactionsToolResponse;
import com.billpay.dto.BillPayRequest;
import com.billpay.dto.PaymentAcceptedResponse;
import com.commons.dto.ToolExecutionResult;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class BankStackTools {

    private final AccountServiceClient accountServiceClient;
    private final TransactionServiceClient transactionServiceClient;
    private final CustomerServiceClient customerServiceClient;
    private final PaymentOrchestratorClient paymentOrchestratorClient;
    private final ToolGuardService toolGuardService;
    private final ConfirmationService confirmationService;
    private final CurrentActorResolver currentActorResolver;
    private final OwnershipGuardService ownershipGuardService;
    private final ToolAuditService toolAuditService;
    private final PolicySearchClient policySearchClient;

    public BankStackTools(AccountServiceClient accountServiceClient,
                          TransactionServiceClient transactionServiceClient,
                          CustomerServiceClient customerServiceClient,
                          PaymentOrchestratorClient paymentOrchestratorClient,
                          ToolGuardService toolGuardService,
                          ConfirmationService confirmationService,
                          CurrentActorResolver currentActorResolver,
                          OwnershipGuardService ownershipGuardService,
                          ToolAuditService toolAuditService,
                          PolicySearchClient policySearchClient) {
        this.accountServiceClient = accountServiceClient;
        this.transactionServiceClient = transactionServiceClient;
        this.customerServiceClient = customerServiceClient;
        this.paymentOrchestratorClient = paymentOrchestratorClient;
        this.toolGuardService = toolGuardService;
        this.confirmationService = confirmationService;
        this.currentActorResolver = currentActorResolver;
        this.ownershipGuardService = ownershipGuardService;
        this.toolAuditService = toolAuditService;
        this.policySearchClient = policySearchClient;
    }

    @McpTool(
            name = "getAccountBalance",
            description = "Get the balance for a specific BankStack account using its accountId."
    )
    public ToolExecutionResult getAccountBalance(
            @McpToolParam(description = "Account UUID", required = true)
            String accountId
    ) {
        CurrentActor actor = currentActorResolver.resolve();
        ToolInvocationContext context = currentContext(actor, false, false);
        String requestSummary = "accountId=" + accountId;

        if (isBlank(accountId)) {
            return needInput("Please provide accountId to retrieve account balance.", "READ_ACCOUNT_BALANCE", "accountId");
        }

        try {
            toolGuardService.check("getAccountBalance", context);

            UUID parsedAccountId = parseUuid(accountId, "accountId");
            ownershipGuardService.checkAccountAccess(actor, parsedAccountId);

            AccountBalanceResponse response = accountServiceClient.getAccountBalance(parsedAccountId);

            AccountBalanceToolResponse toolResponse = new AccountBalanceToolResponse(
                    parsedAccountId,
                    response.balance(),
                    response.available(),
                    "CAD",
                    "Account balance retrieved successfully."
            );

            toolAuditService.success(
                    "getAccountBalance",
                    context,
                    requestSummary,
                    "accountId=%s currency=%s".formatted(parsedAccountId, "CAD")
            );

            return ToolExecutionResult.success(
                    "I retrieved your account balance.",
                    "READ_ACCOUNT_BALANCE",
                    data(toolResponse)
            );
        } catch (Exception ex) {
            toolAuditService.failure("getAccountBalance", context, requestSummary, ex);
            throw ex;
        }
    }

    @McpTool(
            name = "getTransactions",
            description = "Retrieve transactions for an account. Optional filters include startDate, endDate, type, limit, and offset."
    )
    public ToolExecutionResult getTransactions(
            @McpToolParam(description = "Account UUID", required = true)
            String accountId,
            @McpToolParam(description = "Optional ISO-8601 start date-time, e.g. 2026-03-01T00:00:00Z", required = false)
            String startDate,
            @McpToolParam(description = "Optional ISO-8601 end date-time, e.g. 2026-03-13T23:59:59Z", required = false)
            String endDate,
            @McpToolParam(description = "Optional transaction type filter", required = false)
            String type,
            @McpToolParam(description = "Optional exact amount filter, e.g. 45.00", required = false)
            String amount,
            @McpToolParam(description = "Optional page size. Default is 5.", required = false)
            Integer limit,
            @McpToolParam(description = "Optional offset. Default is 0.", required = false)
            Integer offset
    ) {
        CurrentActor actor = currentActorResolver.resolve();
        ToolInvocationContext context = currentContext(actor, false, false);
        String requestSummary = "accountId=%s,startDate=%s,endDate=%s,type=%s,limit=%s,offset=%s"
                .formatted(accountId, startDate, endDate, type, limit, offset) + ",amount=" + amount;

        if (isBlank(accountId)) {
            return needInput("Please provide accountId to retrieve transactions.", "READ_TRANSACTIONS", "accountId");
        }

        try {
            toolGuardService.check("getTransactions", context);

            UUID parsedAccountId = parseUuid(accountId, "accountId");
            OffsetDateTime parsedStartDate = parseOffsetDateTime(startDate, "startDate");
            OffsetDateTime parsedEndDate = parseOffsetDateTime(endDate, "endDate");

            validateDateRange(parsedStartDate, parsedEndDate);
            ownershipGuardService.checkAccountAccess(actor, parsedAccountId);

            int safeLimit = limit == null ? 5 : Math.min(Math.max(limit, 1), 100);
            int safeOffset = offset == null ? 0 : Math.max(offset, 0);

            java.math.BigDecimal parsedAmount = parseAmount(amount);

            List<TransactionItem> items = fetchPostings(
                    parsedAccountId, parsedStartDate, parsedEndDate, safeLimit, safeOffset, type, parsedAmount);

            // An amount the customer half-remembers should not produce a dead end: if the
            // exact figure matches nothing, answer from the unfiltered window instead.
            if (items.isEmpty() && parsedAmount != null) {
                items = fetchPostings(
                        parsedAccountId, parsedStartDate, parsedEndDate, safeLimit, safeOffset, type, null);
            }

            TransactionsToolResponse toolResponse = new TransactionsToolResponse(
                    parsedAccountId,
                    items.size(),
                    safeLimit,
                    safeOffset,
                    items,
                    "Transactions retrieved successfully."
            );

            toolAuditService.success(
                    "getTransactions",
                    context,
                    requestSummary,
                    "accountId=%s,count=%d".formatted(parsedAccountId, items.size())
            );

            return ToolExecutionResult.success(
                    "I retrieved your transactions.",
                    "READ_TRANSACTIONS",
                    data(toolResponse)
            );
        } catch (Exception ex) {
            toolAuditService.failure("getTransactions", context, requestSummary, ex);
            throw ex;
        }
    }

    @McpTool(
            name = "getCustomerProfile",
            description = "Get a customer profile using the customer's externalId."
    )
    public ToolExecutionResult getCustomerProfile(
            @McpToolParam(description = "Customer externalId", required = true)
            String customerExternalId
    ) {
        CurrentActor actor = currentActorResolver.resolve();
        ToolInvocationContext context = currentContext(actor, false, false);
        String requestSummary = "customerExternalId=" + customerExternalId;

        if (isBlank(customerExternalId)) {
            return needInput("Please provide customerExternalId to retrieve customer profile.", "READ_CUSTOMER_PROFILE", "customerExternalId");
        }

        try {
            toolGuardService.check("getCustomerProfile", context);

            String safeExternalId = requiredText(customerExternalId, "customerExternalId");
            ownershipGuardService.checkCustomerProfileAccess(actor, safeExternalId);

            CustomerResponse customer = customerServiceClient.getCustomerByExternalId(safeExternalId);

            CustomerProfileToolResponse toolResponse = new CustomerProfileToolResponse(
                    customer.getExternalId(),
                    buildFullName(customer.getFirstName(), customer.getLastName()),
                    customer.getEmail(),
                    customer.getPhone(),
                    customer.getAddress(),
                    customer.getActive(),
                    customer.getKycStatus().toString(),
                    "Customer profile retrieved successfully."
            );

            toolAuditService.success(
                    "getCustomerProfile",
                    context,
                    requestSummary,
                    "customerExternalId=%s active=%s".formatted(customer.getExternalId(), customer.getActive())
            );

            return ToolExecutionResult.success(
                    "I retrieved your customer profile.",
                    "READ_CUSTOMER_PROFILE",
                    data(toolResponse)
            );
        } catch (Exception ex) {
            toolAuditService.failure("getCustomerProfile", context, requestSummary, ex);
            throw ex;
        }
    }

   

    @McpTool(
            name = "prepareBillPay",
            description = "Prepare a bill payment and generate a confirmation token. This does not execute the payment."
    )
    public ToolExecutionResult prepareBillPay(
            @McpToolParam(description = "Debtor account UUID", required = true)
            String debtorAccountId,
            @McpToolParam(description = "Biller reference number", required = true)
            String billerReferenceNumber,
            @McpToolParam(description = "Invoice reference", required = true)
            String invoiceReference,
            @McpToolParam(description = "Execution date in yyyy-MM-dd format", required = true)
            String executionDate,
            @McpToolParam(description = "Payment amount, e.g. 120.50", required = true)
            BigDecimal amount,
            @McpToolParam(description = "Currency code, e.g. CAD", required = true)
            String currency,
            @McpToolParam(description = "Optional payment note", required = false)
            String note,
            @McpToolParam(description = "Optional idempotency key. If omitted, one will be generated at confirmation time.", required = false)
            String idempotencyKey
    ) {
        CurrentActor actor = currentActorResolver.resolve();
        ToolInvocationContext context = currentContext(actor, false, false);
        String requestSummary = "debtorAccountId=%s,billerReferenceNumber=%s,invoiceReference=%s,executionDate=%s,amount=%s,currency=%s"
                .formatted(debtorAccountId, billerReferenceNumber, invoiceReference, executionDate, amount, currency);

        List<String> missing = new ArrayList<>();
        if (isBlank(debtorAccountId)) missing.add("debtorAccountId");
        if (isBlank(billerReferenceNumber)) missing.add("billerReferenceNumber");
        if (isBlank(invoiceReference)) missing.add("invoiceReference");
        if (isBlank(executionDate)) missing.add("executionDate");
        if (amount == null) missing.add("amount");
        if (isBlank(currency)) missing.add("currency");
        if (!missing.isEmpty()) {
            return ToolExecutionResult.needInput("Please provide the missing bill payment details.", missing, "BILL_PAY");
        }

        try {
            toolGuardService.check("prepareBillPay", context);

            UUID parsedAccountId = parseUuid(debtorAccountId, "debtorAccountId");
            BigDecimal safeAmount = requirePositive(amount, "amount");
            String safeCurrency = requireCurrency(currency, "currency");
            String safeBillerReferenceNumber = requiredText(billerReferenceNumber, "billerReferenceNumber");
            String safeInvoiceReference = requiredText(invoiceReference, "invoiceReference");
            String safeExecutionDate = requireLocalDate(executionDate, "executionDate").toString();
            String safeNote = blankToNull(note);
            String safeIdempotencyKey = blankToNull(idempotencyKey);

            ownershipGuardService.checkAccountAccess(actor, parsedAccountId);

            FraudRiskAssessmentResponse riskAssessment = assessTransactionRiskCore(
                    parsedAccountId,
                    safeAmount,
                    safeBillerReferenceNumber,
                    "BILL_PAY"
            );

            if ("HIGH".equals(riskAssessment.riskLevel())) {
                throw new InvalidToolInputException(
                        "Bill payment requires manual review before preparation. Risk score=%d, reasons=%s"
                                .formatted(riskAssessment.riskScore(), riskAssessment.reasons())
                );
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("fraudRiskScore", riskAssessment.riskScore());
            payload.put("fraudRiskLevel", riskAssessment.riskLevel());
            payload.put("debtorAccountId", parsedAccountId.toString());
            payload.put("billerReferenceNumber", safeBillerReferenceNumber);
            payload.put("invoiceReference", safeInvoiceReference);
            payload.put("executionDate", safeExecutionDate);
            payload.put("amount", safeAmount.toPlainString());
            payload.put("currency", safeCurrency);

            if (safeNote != null) {
                payload.put("note", safeNote);
            }

            if (safeIdempotencyKey != null) {
                payload.put("idempotencyKey", safeIdempotencyKey);
            }

            PendingToolAction pendingAction = confirmationService.createPendingAction(
                    "initiateBillPay",
                    actor.actorId(),
                    payload
            );

            BillPayPreparationResponse toolResponse = new BillPayPreparationResponse(
                    pendingAction.confirmationToken(),
                    "initiateBillPay",
                    parsedAccountId,
                    safeBillerReferenceNumber,
                    safeInvoiceReference,
                    safeExecutionDate,
                    safeAmount,
                    safeCurrency,
                    safeNote,
                    pendingAction.expiresAt(),
                    "Bill payment prepared successfully. Ask the user to confirm, then call confirmBillPay with the confirmation token."
            );

            toolAuditService.success(
                    "prepareBillPay",
                    context,
                    requestSummary,
                    "confirmationToken=%s expiresAt=%s".formatted(
                            maskSecret(pendingAction.confirmationToken()),
                            pendingAction.expiresAt()
                    )
            );

            return ToolExecutionResult.prepared(
                    "Your bill payment has been prepared but not executed yet. Please confirm if you want to proceed.",
                    pendingAction.confirmationToken(),
                    "BILL_PAY",
                    data(toolResponse)
            );
        } catch (Exception ex) {
            toolAuditService.failure("prepareBillPay", context, requestSummary, ex);
            throw ex;
        }
    }

    @McpTool(
            name = "confirmBillPay",
            description = "Confirm and execute a previously prepared bill payment using a confirmation token."
    )
    public ToolExecutionResult confirmBillPay(
            @McpToolParam(description = "Confirmation token received from prepareBillPay", required = true)
            String confirmationToken
    ) {
        CurrentActor actor = currentActorResolver.resolve();
        ToolInvocationContext context = currentContext(actor, true, false);
        String requestSummary = "confirmationToken=" + maskSecret(confirmationToken);

        if (isBlank(confirmationToken)) {
            return needInput("Please provide the confirmation token to execute the bill payment.", "BILL_PAY", "confirmationToken");
        }

        try {
            toolGuardService.check("confirmBillPay", context);

            String safeToken = requiredText(confirmationToken, "confirmationToken");

            PendingToolAction pendingAction = confirmationService.consume(
                    safeToken,
                    "initiateBillPay",
                    actor.actorId()
            );

            UUID debtorAccountId = UUID.fromString((String) pendingAction.payload().get("debtorAccountId"));
            String billerReferenceNumber = (String) pendingAction.payload().get("billerReferenceNumber");
            String invoiceReference = (String) pendingAction.payload().get("invoiceReference");
            String executionDate = (String) pendingAction.payload().get("executionDate");
            BigDecimal amount = new BigDecimal((String) pendingAction.payload().get("amount"));
            String currency = (String) pendingAction.payload().get("currency");
            String note = (String) pendingAction.payload().get("note");
            String idempotencyKey = (String) pendingAction.payload().get("idempotencyKey");

            ownershipGuardService.checkAccountAccess(actor, debtorAccountId);

            String finalIdempotencyKey = (idempotencyKey == null || idempotencyKey.isBlank())
                    ? UUID.randomUUID().toString()
                    : idempotencyKey;

            BillPayRequest request = new BillPayRequest(
                    debtorAccountId,
                    billerReferenceNumber,
                    invoiceReference,
                    executionDate,
                    new AmountDTO(currency, amount),
                    note
            );

            PaymentAcceptedResponse accepted = paymentOrchestratorClient.initiateBillPay(
                    finalIdempotencyKey,
                    request
            );

            BillPayToolResponse toolResponse = new BillPayToolResponse(
                    accepted.paymentId(),
                    accepted.state(),
                    accepted.statusUrl(),
                    debtorAccountId,
                    billerReferenceNumber,
                    invoiceReference,
                    amount,
                    currency,
                    executionDate,
                    finalIdempotencyKey,
                    "Bill payment confirmed and submitted successfully. Use getPaymentStatus to check progress."
            );

            toolAuditService.success(
                    "confirmBillPay",
                    context,
                    requestSummary,
                    "paymentId=%s state=%s".formatted(accepted.paymentId(), accepted.state())
            );

            return ToolExecutionResult.executed(
                    "Your bill payment has been submitted successfully.",
                    "BILL_PAY",
                    data(toolResponse)
            );
        } catch (Exception ex) {
            toolAuditService.failure("confirmBillPay", context, requestSummary, ex);
            throw ex;
        }
    }

    @McpTool(
            name = "getPaymentStatus",
            description = "Get the latest status of a previously initiated payment using paymentId."
    )
    public ToolExecutionResult getPaymentStatus(
            @McpToolParam(description = "Payment UUID", required = true)
            String paymentId
    ) {
        CurrentActor actor = currentActorResolver.resolve();
        ToolInvocationContext context = currentContext(actor, false, false);
        String requestSummary = "paymentId=" + paymentId;

        if (isBlank(paymentId)) {
            return needInput("Please provide paymentId to retrieve payment status.", "PAYMENT_STATUS", "paymentId");
        }

        try {
            toolGuardService.check("getPaymentStatus", context);

            UUID parsedPaymentId = parseUuid(paymentId, "paymentId");
            PaymentView payment = paymentOrchestratorClient.getPayment(parsedPaymentId);

            ownershipGuardService.checkPaymentAccess(actor, payment.debtorAccountId());

            PaymentStatusToolResponse toolResponse = new PaymentStatusToolResponse(
                    payment.paymentId(),
                    payment.state(),
                    payment.debtorAccountId(),
                    payment.billerRefNumber(),
                    payment.invoiceReference(),
                    payment.executionDate(),
                    payment.amountValue(),
                    payment.amountCcy(),
                    payment.externalStatusCode(),
                    payment.reason(),
                    payment.createdAt(),
                    payment.updatedAt(),
                    "Payment status retrieved successfully."
            );

            toolAuditService.success(
                    "getPaymentStatus",
                    context,
                    requestSummary,
                    "paymentId=%s state=%s".formatted(payment.paymentId(), payment.state())
            );

            return ToolExecutionResult.success(
                    "I retrieved the payment status.",
                    "PAYMENT_STATUS",
                    data(toolResponse)
            );
        } catch (Exception ex) {
            toolAuditService.failure("getPaymentStatus", context, requestSummary, ex);
            throw ex;
        }
    }

    @McpTool(
            name = "assessTransactionRisk",
            description = "Read-only fraud control tool that assesses transaction risk for an account, amount, payee, and channel before payment execution. Does not move money."
    )
    public ToolExecutionResult assessTransactionRisk(
            @McpToolParam(description = "Account UUID", required = true)
            String accountId,
            @McpToolParam(description = "Payment amount, e.g. 120.50", required = true)
            BigDecimal amount,
            @McpToolParam(description = "Payee or biller reference", required = true)
            String payee,
            @McpToolParam(description = "Payment channel, e.g. BILL_PAY, RTR, ACH", required = true)
            String channel
    ) {
        CurrentActor actor = currentActorResolver.resolve();
        ToolInvocationContext context = currentContext(actor, false, false);
        String requestSummary = "accountId=%s,amount=%s,payee=%s,channel=%s"
                .formatted(accountId, amount, payee, channel);

        List<String> missing = new ArrayList<>();
        if (isBlank(accountId)) missing.add("accountId");
        if (amount == null) missing.add("amount");
        if (isBlank(payee)) missing.add("payee");
        if (isBlank(channel)) missing.add("channel");
        if (!missing.isEmpty()) {
            return ToolExecutionResult.needInput("Please provide the missing risk assessment details.", missing, "FRAUD_RISK_ASSESSMENT");
        }

        try {
            toolGuardService.check("assessTransactionRisk", context);

            UUID parsedAccountId = parseUuid(accountId, "accountId");
            BigDecimal safeAmount = requirePositive(amount, "amount");
            String safePayee = requiredText(payee, "payee");
            String safeChannel = requiredText(channel, "channel").toUpperCase();

            ownershipGuardService.checkAccountAccess(actor, parsedAccountId);

            FraudRiskAssessmentResponse toolResponse = assessTransactionRiskCore(
                    parsedAccountId,
                    safeAmount,
                    safePayee,
                    safeChannel
            );

            toolAuditService.success(
                    "assessTransactionRisk",
                    context,
                    requestSummary,
                    "accountId=%s riskScore=%d riskLevel=%s".formatted(
                            parsedAccountId,
                            toolResponse.riskScore(),
                            toolResponse.riskLevel()
                    )
            );

            return ToolExecutionResult.success(
                    "I assessed the transaction risk.",
                    "FRAUD_RISK_ASSESSMENT",
                    data(toolResponse)
            );
        } catch (Exception ex) {
            toolAuditService.failure("assessTransactionRisk", context, requestSummary, ex);
            throw ex;
        }
    }

    @McpTool(
            name = "searchPolicyDocuments",
            description = "description = \"Search BankStack policy documents for rules, procedures, and guidelines. \n"
            		+ "Use when a customer asks about policies, eligibility criteria, refund rules, \n"
            		+ "dispute procedures, or any question that requires knowledge from documents \n"
            		+ "rather than live transactional data."
    )
    public ToolExecutionResult searchPolicyDocuments(
            @McpToolParam(description = "Policy search query", required = true)
            String query
    ) {
        CurrentActor actor = currentActorResolver.resolve();
        ToolInvocationContext context = currentContext(actor, false, false);

        toolGuardService.check("searchPolicyDocuments", context);

        if (isBlank(query)) {
            return needInput("Please provide a policy question to search documents.", "SEARCH_POLICY_DOCUMENTS", "query");
        }

        String safeQuery = query.trim();

        try {
            var response = policySearchClient.searchPolicyDocuments(safeQuery);

            toolAuditService.success(
                    "searchPolicyDocuments",
                    context,
                    "query=" + safeQuery,
                    "Policy search completed."
            );

            return ToolExecutionResult.success(
                    "I found relevant policy information.",
                    "SEARCH_POLICY_DOCUMENTS",
                    data(compactPolicySearchResponse(response))
            );
        } catch (RuntimeException ex) {
            toolAuditService.failure(
                    "searchPolicyDocuments",
                    context,
                    "query=" + safeQuery,
                    ex
            );
            throw ex;
        }
    }

    private Map<String, Object> compactPolicySearchResponse(com.bankstack.mcp.dto.PolicySearchResponse response) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (response == null) {
            result.put("answer", "No policy response was returned.");
            result.put("taskType", "POLICY_LOOKUP");
            result.put("fullyVerified", false);
            result.put("citationChecks", List.of());
            result.put("debug", Map.of("renderedContextAvailable", false));
            return result;
        }

        result.put("answer", response.answer());
        result.put("taskType", response.taskType());
        result.put("fullyVerified", response.fullyVerified());
        result.put("citationChecks", response.citationChecks() == null ? List.of() : response.citationChecks());

        String renderedContext = response.renderedContext();
        result.put("debug", Map.of(
                "renderedContextAvailable", renderedContext != null && !renderedContext.isBlank(),
                "renderedContextLength", renderedContext == null ? 0 : renderedContext.length()
        ));

        return result;
    }

    private FraudRiskAssessmentResponse assessTransactionRiskCore(UUID accountId,
                                                                  BigDecimal amount,
                                                                  String payee,
                                                                  String channel) {
        AccountBalanceResponse balanceResponse = accountServiceClient.getAccountBalance(accountId);

        BigDecimal availableBalance = balanceResponse.available() == null
                ? BigDecimal.ZERO
                : balanceResponse.available();

        OffsetDateTime startDate = OffsetDateTime.now().minusDays(30);
        OffsetDateTime endDate = OffsetDateTime.now();

        List<TransactionItem> recentTransactions = transactionServiceClient.getTransactions(
                        accountId,
                        startDate,
                        endDate,
                        20,
                        0,
                        null,
                        null)
                .stream()
                .map(this::toTransactionItem)
                .toList();

        int riskScore = 0;
        List<String> reasons = new ArrayList<>();

        if (availableBalance.compareTo(amount) < 0) {
            riskScore += 35;
            reasons.add("Amount is higher than available balance.");
        } else if (availableBalance.signum() > 0
                && amount.compareTo(availableBalance.multiply(new BigDecimal("0.90"))) > 0) {
            riskScore += 20;
            reasons.add("Amount uses more than 90% of available balance.");
        }

        BigDecimal averageRecentAmount = averageRecentTransactionAmount(recentTransactions);

        if (averageRecentAmount.signum() > 0) {
            if (amount.compareTo(averageRecentAmount.multiply(new BigDecimal("4"))) > 0) {
                riskScore += 25;
                reasons.add("Amount is more than 4x higher than recent average transaction amount.");
            } else if (amount.compareTo(averageRecentAmount.multiply(new BigDecimal("2"))) > 0) {
                riskScore += 15;
                reasons.add("Amount is more than 2x higher than recent average transaction amount.");
            }
        }

        long rapidPaymentCount = recentTransactions.stream()
                .filter(tx -> tx.amount() != null && tx.amount().signum() > 0)
                .limit(10)
                .count();

        if (rapidPaymentCount >= 5) {
            riskScore += 15;
            reasons.add("Multiple recent outgoing transactions detected in the lookback window.");
        }

        boolean knownPayee = recentTransactions.stream()
                .map(TransactionItem::reason)
                .filter(reason -> reason != null && !reason.isBlank())
                .anyMatch(reason -> reason.toLowerCase().contains(payee.toLowerCase()));

        if (!knownPayee) {
            riskScore += 15;
            reasons.add("Payee or biller reference was not found in recent transaction history.");
        }

        long failedAttemptCount = recentTransactions.stream()
                .map(TransactionItem::status)
                .filter(status -> status != null)
                .filter(status -> {
                    String normalized = status.toUpperCase();
                    return normalized.contains("FAIL")
                            || normalized.contains("REJECT")
                            || normalized.contains("DECLIN");
                })
                .count();

        if (failedAttemptCount >= 2) {
            riskScore += 15;
            reasons.add("Repeated failed, rejected, or declined attempts were found recently.");
        }

        if ("RTR".equalsIgnoreCase(channel) || "ACH".equalsIgnoreCase(channel)) {
            riskScore += 5;
            reasons.add("External payment channel requires additional risk review.");
        }

        riskScore = Math.min(riskScore, 100);

        String riskLevel = riskScore >= 70 ? "HIGH" : riskScore >= 40 ? "MEDIUM" : "LOW";

        String recommendedAction = switch (riskLevel) {
            case "HIGH" -> "Block preparation or require manual fraud review before execution.";
            case "MEDIUM" -> "Require step-up confirmation before execution.";
            default -> "Allow preparation and continue with normal confirmation flow.";
        };

        if (reasons.isEmpty()) {
            reasons.add("No significant risk patterns detected in the available data.");
        }

        return new FraudRiskAssessmentResponse(
                accountId,
                amount,
                payee,
                channel,
                riskScore,
                riskLevel,
                reasons,
                recommendedAction,
                "Transaction risk assessed successfully. This is a read-only fraud control decision."
        );
    }

    private BigDecimal averageRecentTransactionAmount(List<TransactionItem> transactions) {
        List<BigDecimal> amounts = transactions.stream()
                .map(TransactionItem::amount)
                .filter(value -> value != null && value.signum() != 0)
                .map(BigDecimal::abs)
                .toList();

        if (amounts.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(
                BigDecimal.valueOf(amounts.size()),
                2,
                RoundingMode.HALF_UP
        );
    }

    /** Fetches ledger entries for an account and reduces them to customer-visible postings. */
    private List<TransactionItem> fetchPostings(UUID accountId,
                                                OffsetDateTime startDate,
                                                OffsetDateTime endDate,
                                                int limit,
                                                int offset,
                                                String type,
                                                BigDecimal amount) {
        return transactionServiceClient.getTransactions(
                        accountId, startDate, endDate, limit, offset, blankToNull(type), amount)
                .stream()
                .map(this::toTransactionItem)
                .filter(BankStackTools::isCustomerVisiblePosting)
                .toList();
    }

    /** Accepts "45", "45.00" or "$45"; anything unparseable is treated as no filter. */
    private BigDecimal parseAmount(String amount) {
        String cleaned = blankToNull(amount);
        if (cleaned == null) {
            return null;
        }
        try {
            return new BigDecimal(cleaned.replace("$", "").replace(",", "").trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Keeps postings and drops holds.
     *
     * <p>A single bill payment writes HOLD_PLACED, HOLD_RELEASED and DEBIT to the ledger.
     * The hold pair is an internal reservation that nets to zero — surfacing it makes one
     * $45 payment look like three $45 events, which is what a customer would read as being
     * charged three times. The ledger keeps every row; the customer-facing tool shows the
     * money that actually moved. Holds remain visible through the account API and are
     * reflected in the available balance.</p>
     */
    private static boolean isCustomerVisiblePosting(TransactionItem item) {
        String type = item.type();
        if (type == null) {
            return true; // unclassified rows are shown rather than silently hidden
        }
        return !type.startsWith("HOLD_");
    }

    private TransactionItem toTransactionItem(TransactionResponse response) {
        return new TransactionItem(
                response.id(),
                response.type(),
                response.status(),
                response.amount(),
                response.currency(),
                response.reason(),
                response.balanceAfter(),
                response.occurredAt()
        );
    }

    private UUID parseUuid(String value, String fieldName) {
        try {
            return UUID.fromString(requiredText(value, fieldName));
        } catch (IllegalArgumentException ex) {
            throw new InvalidToolInputException("%s must be a valid UUID.".formatted(fieldName));
        }
    }

    private OffsetDateTime parseOffsetDateTime(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new InvalidToolInputException(
                    "%s must be a valid ISO-8601 date-time, for example 2026-03-01T00:00:00Z."
                            .formatted(fieldName)
            );
        }
    }

    private LocalDate requireLocalDate(String value, String fieldName) {
        try {
            return LocalDate.parse(requiredText(value, fieldName));
        } catch (DateTimeParseException ex) {
            throw new InvalidToolInputException(
                    "%s must be a valid date in yyyy-MM-dd format.".formatted(fieldName)
            );
        }
    }

    private String requiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidToolInputException("%s is required.".formatted(fieldName));
        }

        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal requirePositive(BigDecimal value, String fieldName) {
        if (value == null || value.signum() <= 0) {
            throw new InvalidToolInputException("%s must be greater than zero.".formatted(fieldName));
        }

        return value;
    }

    private String requireCurrency(String value, String fieldName) {
        String currency = requiredText(value, fieldName).toUpperCase();

        if (!currency.matches("^[A-Z]{3}$")) {
            throw new InvalidToolInputException(
                    "%s must be a 3-letter currency code, for example CAD.".formatted(fieldName)
            );
        }

        return currency;
    }

    private void validateDateRange(OffsetDateTime startDate, OffsetDateTime endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidToolInputException("endDate must be greater than or equal to startDate.");
        }
    }

    private String buildFullName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        String fullName = (first + " " + last).trim();

        return fullName.isBlank() ? null : fullName;
    }

    private String maskSecret(String value) {
        if (value == null || value.isBlank()) {
            return "<missing>";
        }

        String trimmed = value.trim();

        if (trimmed.length() <= 8) {
            return "****";
        }

        return trimmed.substring(0, 4) + "..." + trimmed.substring(trimmed.length() - 4);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ToolExecutionResult needInput(String message,
                                          String actionType,
                                          String... fields) {
        return ToolExecutionResult.needInput(message, List.of(fields), actionType);
    }

    private Map<String, Object> data(Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("result", value);
        return map;
    }

    private ToolInvocationContext currentContext(CurrentActor actor,
                                                 boolean confirmationPresent,
                                                 boolean otpVerified) {
        return new ToolInvocationContext(
                actor.actorId(),
                actor.subject(),
                actor.customerExternalId(),
                actor.role(),
                confirmationPresent,
                otpVerified
        );
    }
}