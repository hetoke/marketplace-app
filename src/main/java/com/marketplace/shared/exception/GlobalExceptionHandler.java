package com.marketplace.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.apache.tomcat.util.http.InvalidParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
		ErrorResponse body = ErrorResponse.builder()
				.status(HttpStatus.NOT_FOUND.value())
				.error(HttpStatus.NOT_FOUND.getReasonPhrase())
				.message(ex.getMessage())
				.path(request.getRequestURI())
				.timestamp(Instant.now())
				.build();
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
		log.warn("Business exception at {}: {}", request.getRequestURI(), ex.getMessage());
		ErrorResponse body = ErrorResponse.builder()
				.status(HttpStatus.BAD_REQUEST.value())
				.error(HttpStatus.BAD_REQUEST.getReasonPhrase())
				.message(ex.getMessage())
				.path(request.getRequestURI())
				.timestamp(Instant.now())
				.build();
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(EmailVerificationRequiredException.class)
	public ResponseEntity<ErrorResponse> handleEmailVerificationRequired(
			EmailVerificationRequiredException ex, HttpServletRequest request) {
		return ResponseEntity.badRequest().body(ex.getErrorResponse());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		ErrorResponse body = ErrorResponse.builder()
				.status(HttpStatus.FORBIDDEN.value())
				.error(HttpStatus.FORBIDDEN.getReasonPhrase())
				.message(ex.getMessage())
				.path(request.getRequestURI())
				.timestamp(Instant.now())
				.build();
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
	}

	@ExceptionHandler(org.springframework.security.authorization.AuthorizationDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAuthorizationDenied(
			org.springframework.security.authorization.AuthorizationDeniedException ex,
			HttpServletRequest request) {
		ErrorResponse body = ErrorResponse.builder()
				.status(HttpStatus.FORBIDDEN.value())
				.error(HttpStatus.FORBIDDEN.getReasonPhrase())
				.message("Access denied")
				.path(request.getRequestURI())
				.timestamp(Instant.now())
				.build();
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
				.toList();
		ErrorResponse body = ErrorResponse.builder()
				.status(HttpStatus.BAD_REQUEST.value())
				.error(HttpStatus.BAD_REQUEST.getReasonPhrase())
				.message("Validation failed")
				.path(request.getRequestURI())
				.timestamp(Instant.now())
				.fieldErrors(fieldErrors)
				.build();
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleMalformedBody(HttpMessageNotReadableException ex,
			HttpServletRequest request) {
		log.debug("Malformed request body at {}", request.getRequestURI());
		ErrorResponse body = ErrorResponse.builder()
				.status(HttpStatus.BAD_REQUEST.value())
				.error(HttpStatus.BAD_REQUEST.getReasonPhrase())
				.message("Malformed request body")
				.path(request.getRequestURI())
				.timestamp(Instant.now())
				.build();
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
			HttpServletRequest request) {
		ErrorResponse body = ErrorResponse.builder()
				.status(HttpStatus.BAD_REQUEST.value())
				.error(HttpStatus.BAD_REQUEST.getReasonPhrase())
				.message("Parameter '" + ex.getName() + "' must be of type " + ex.getRequiredType().getSimpleName())
				.path(request.getRequestURI())
				.timestamp(Instant.now())
				.build();
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
			HttpServletRequest request) {
		ErrorResponse body = ErrorResponse.builder()
				.status(HttpStatus.BAD_REQUEST.value())
				.error(HttpStatus.BAD_REQUEST.getReasonPhrase())
				.message("Required parameter '" + ex.getParameterName() + "' is missing")
				.path(request.getRequestURI())
				.timestamp(Instant.now())
				.build();
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
			HttpServletRequest request) {
		ErrorResponse body = ErrorResponse.builder()
				.status(HttpStatus.METHOD_NOT_ALLOWED.value())
				.error(HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase())
				.message("Method " + ex.getMethod() + " not allowed for " + request.getRequestURI())
				.path(request.getRequestURI())
				.timestamp(Instant.now())
				.build();
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
	}

	@ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
			org.springframework.web.HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
		log.debug("Unsupported media type at {}", request.getRequestURI(), ex);
		ErrorResponse body = ErrorResponse.builder()
				.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
				.error(HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase())
				.message("Expected Content-Type: application/json")
				.path(request.getRequestURI())
				.timestamp(Instant.now())
				.build();
		return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
	}

	@ExceptionHandler(InvalidParameterException.class)
	public ResponseEntity<ErrorResponse> handleInvalidParameter(InvalidParameterException ex,
			HttpServletRequest request) {
		log.debug("Invalid query parameter at {}: {}", request.getRequestURI(), ex.getMessage());
		ErrorResponse body = ErrorResponse.builder()
				.status(HttpStatus.BAD_REQUEST.value())
				.error(HttpStatus.BAD_REQUEST.getReasonPhrase())
				.message("Invalid query parameter")
				.path(request.getRequestURI())
				.timestamp(Instant.now())
				.build();
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	public ResponseEntity<ErrorResponse> handleOptimisticLock(
			ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
		log.warn("Optimistic lock failure at {}", request.getRequestURI());
		ErrorResponse body = ErrorResponse.builder()
				.status(HttpStatus.CONFLICT.value())
				.error(HttpStatus.CONFLICT.getReasonPhrase())
				.message("Your cart is being updated. Please try again.")
				.path(request.getRequestURI())
				.timestamp(Instant.now())
				.build();
		return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
		ErrorResponse body = ErrorResponse.builder()
				.status(HttpStatus.NOT_FOUND.value())
				.error(HttpStatus.NOT_FOUND.getReasonPhrase())
				.message("Endpoint not found: " + request.getRequestURI())
				.path(request.getRequestURI())
				.timestamp(Instant.now())
				.build();
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
			HttpServletRequest request) {
		log.debug("Illegal argument at {}: {}", request.getRequestURI(), ex.getMessage());
		ErrorResponse body = ErrorResponse.builder()
				.status(HttpStatus.BAD_REQUEST.value())
				.error(HttpStatus.BAD_REQUEST.getReasonPhrase())
				.message(ex.getMessage())
				.path(request.getRequestURI())
				.timestamp(Instant.now())
				.build();
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception at {}", request.getRequestURI(), ex);
		ErrorResponse body = ErrorResponse.builder()
				.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
				.error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
				.message("An unexpected error occurred")
				.path(request.getRequestURI())
				.timestamp(Instant.now())
				.build();
		return ResponseEntity.internalServerError().body(body);
	}
}
