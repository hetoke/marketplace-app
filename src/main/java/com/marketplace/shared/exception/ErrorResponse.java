package com.marketplace.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

	private final int status;
	private final String error;
	private final String message;
	private final String errorCode;
	private final String path;
	private final Instant timestamp;
	private final List<FieldError> fieldErrors;

	protected ErrorResponse(int status, String error, String message, String errorCode,
			String path, Instant timestamp, List<FieldError> fieldErrors) {
		this.status = status;
		this.error = error;
		this.message = message;
		this.errorCode = errorCode;
		this.path = path;
		this.timestamp = timestamp;
		this.fieldErrors = fieldErrors;
	}

	public int getStatus() { return status; }
	public String getError() { return error; }
	public String getMessage() { return message; }
	public String getErrorCode() { return errorCode; }
	public String getPath() { return path; }
	public Instant getTimestamp() { return timestamp; }
	public List<FieldError> getFieldErrors() { return fieldErrors; }

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private int status;
		private String error;
		private String message;
		private String errorCode;
		private String path;
		private Instant timestamp;
		private List<FieldError> fieldErrors;

		public Builder status(int status) { this.status = status; return this; }
		public Builder error(String error) { this.error = error; return this; }
		public Builder message(String message) { this.message = message; return this; }
		public Builder errorCode(String errorCode) { this.errorCode = errorCode; return this; }
		public Builder path(String path) { this.path = path; return this; }
		public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
		public Builder fieldErrors(List<FieldError> fieldErrors) { this.fieldErrors = fieldErrors; return this; }

		public ErrorResponse build() {
			return new ErrorResponse(status, error, message, errorCode, path, timestamp, fieldErrors);
		}
	}

	public static class FieldError {
		private final String field;
		private final String message;

		public FieldError(String field, String message) {
			this.field = field;
			this.message = message;
		}

		public String getField() { return field; }
		public String getMessage() { return message; }
	}
}
