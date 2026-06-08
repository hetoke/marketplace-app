package com.marketplace.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
		int status,
		String error,
		String message,
		String path,
		Instant timestamp,
		List<FieldError> fieldErrors
) {
	public record FieldError(String field, String message) {}
}
