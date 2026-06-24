package com.marketplace.shared.exception;

import java.time.Instant;

public class EmailVerificationRequiredException extends RuntimeException {

	private final ErrorResponse errorResponse;

	public EmailVerificationRequiredException(String message) {
		super(message);
		this.errorResponse = ErrorResponse.builder()
				.status(400)
				.error("Bad Request")
				.message(message)
				.errorCode("EMAIL_VERIFICATION_REQUIRED")
				.timestamp(Instant.now())
				.build();
	}

	public ErrorResponse getErrorResponse() {
		return errorResponse;
	}
}
