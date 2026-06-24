package com.marketplace.upload.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketplace.shared.exception.AccessDeniedException;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.GlobalExceptionHandler;
import com.marketplace.shared.exception.ResourceNotFoundException;
import com.marketplace.upload.dto.UploadResponse;
import com.marketplace.upload.service.UploadService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UploadControllerTest {

	private MockMvc mockMvc;

	@Mock
	private UploadService uploadService;

	@InjectMocks
	private UploadController uploadController;

	private final String userId = UUID.randomUUID().toString();

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(uploadController)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();

		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(userId, null, java.util.List.of()));
	}

	// ==================== USER AVATAR ====================

	@Test
	void requestUserAvatar_success() throws Exception {
		UploadResponse response = new UploadResponse(
				UUID.randomUUID(),
				"https://supabase.co/signed-url",
				"token-abc",
				Instant.now().plusSeconds(7200));

		when(uploadService.requestUserUpload(anyString())).thenReturn(response);

		mockMvc.perform(get("/api/v1/users/avatar/upload-url"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("Upload session created"))
				.andExpect(jsonPath("$.data.uploadUrl").value("https://supabase.co/signed-url"))
				.andExpect(jsonPath("$.data.token").value("token-abc"));
	}

	// ==================== PRODUCT IMAGES ====================

	@Test
	void requestProductImages_success() throws Exception {
		UploadResponse response = new UploadResponse(
				UUID.randomUUID(),
				"https://supabase.co/signed-url",
				"token-456",
				Instant.now().plusSeconds(7200));

		UUID productId = UUID.randomUUID();
		when(uploadService.requestProductUpload(anyString(), eq(productId))).thenReturn(response);

		mockMvc.perform(get("/api/v1/products/" + productId + "/images/upload-url"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("Upload session created"))
				.andExpect(jsonPath("$.data.token").value("token-456"));
	}

	@Test
	void requestProductImages_productNotFound_returns404() throws Exception {
		UUID productId = UUID.randomUUID();
		when(uploadService.requestProductUpload(anyString(), eq(productId)))
				.thenThrow(new ResourceNotFoundException("Product", "id", productId));

		mockMvc.perform(get("/api/v1/products/" + productId + "/images/upload-url"))
				.andExpect(status().isNotFound());
	}

	@Test
	void requestProductImages_notSeller_returns403() throws Exception {
		UUID productId = UUID.randomUUID();
		when(uploadService.requestProductUpload(anyString(), eq(productId)))
				.thenThrow(new AccessDeniedException("You can only upload images for your own products"));

		mockMvc.perform(get("/api/v1/products/" + productId + "/images/upload-url"))
				.andExpect(status().isForbidden());
	}
}
