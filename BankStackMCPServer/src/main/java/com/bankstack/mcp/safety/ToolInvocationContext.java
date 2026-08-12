package com.bankstack.mcp.safety;

public record ToolInvocationContext(
		    String actorId,
	        String subject,
	        String customerExternalId,
	        ToolAccessRole actorRole,
	        boolean confirmationPresent,
	        boolean otpVerified
) {}