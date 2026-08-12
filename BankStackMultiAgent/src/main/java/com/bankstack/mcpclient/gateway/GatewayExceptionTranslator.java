package com.bankstack.mcpclient.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.client.RestClientException;

@Component
public class GatewayExceptionTranslator {

    public ErrorResponseException translate(Exception ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage();

        if (ex instanceof RestClientException) {
            return new ErrorResponseException(
                    HttpStatus.BAD_GATEWAY,
                    ProblemDetail.forStatusAndDetail(
                            HttpStatus.BAD_GATEWAY,
                            "The knowledge service is currently unavailable."
                    ),
                    ex
            );
        }

        if (message.contains("No prepared action exists")) {
            return new ErrorResponseException(
                    HttpStatus.BAD_REQUEST,
                    ProblemDetail.forStatusAndDetail(
                            HttpStatus.BAD_REQUEST,
                            "There is no prepared payment available to confirm for this user. Please start the payment first."
                    ),
                    ex
            );
        }

        if (message.contains("Invalid confirmation token")) {
            return new ErrorResponseException(
                    HttpStatus.BAD_REQUEST,
                    ProblemDetail.forStatusAndDetail(
                            HttpStatus.BAD_REQUEST,
                            "I could not complete the payment because the confirmation token is invalid or expired."
                    ),
                    ex
            );
        }

        if (message.contains("Customers can only access their own")
                || message.contains("not allowed to access")) {
            return new ErrorResponseException(
                    HttpStatus.FORBIDDEN,
                    ProblemDetail.forStatusAndDetail(
                            HttpStatus.FORBIDDEN,
                            "I could not complete that request because the authenticated user is not allowed to access that resource."
                    ),
                    ex
            );
        }

        if (message.contains("requires explicit confirmation")) {
            return new ErrorResponseException(
                    HttpStatus.BAD_REQUEST,
                    ProblemDetail.forStatusAndDetail(
                            HttpStatus.BAD_REQUEST,
                            "This action requires explicit user confirmation before it can be executed."
                    ),
                    ex
            );
        }

        return new ErrorResponseException(
                HttpStatus.BAD_GATEWAY,
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_GATEWAY,
                        "I could not complete that request safely at the moment."
                ),
                ex
        );
    }
}