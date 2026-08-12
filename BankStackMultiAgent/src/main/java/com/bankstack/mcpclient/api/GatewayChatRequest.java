package com.bankstack.mcpclient.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GatewayChatRequest(
	      @NotBlank(message = "Message is required.")
	        @Size(max = 4000, message = "Message must not exceed 4000 characters.")
	        String message
) {
}
