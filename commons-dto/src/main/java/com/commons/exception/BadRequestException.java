package com.commons.exception;

import java.util.UUID;

public class BadRequestException extends RuntimeException {
	
    public BadRequestException(String desc) {
        super(desc);
    }

}
