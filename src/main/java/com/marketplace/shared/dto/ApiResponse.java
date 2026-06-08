package com.marketplace.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(String message, T data) {

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(null, data);
	}

	public static <T> ApiResponse<T> ok(String message, T data) {
		return new ApiResponse<>(message, data);
	}

	public static <T> ApiResponse<T> error(String message) {
		return new ApiResponse<>(message, null);
	}
}
