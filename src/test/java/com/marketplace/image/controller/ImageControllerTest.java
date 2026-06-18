package com.marketplace.image.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketplace.image.dto.ImageResponse;
import com.marketplace.image.model.EntityType;
import com.marketplace.image.service.ImageService;
import com.marketplace.shared.exception.AccessDeniedException;
import com.marketplace.shared.exception.GlobalExceptionHandler;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.util.List;
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
class ImageControllerTest {

	private MockMvc mockMvc;

	@Mock
	private ImageService imageService;

	@InjectMocks
	private ImageController imageController;

	private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
	private static final String IMAGE_ID = "22222222-2222-2222-2222-222222222222";

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(imageController)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();

		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()));
	}

	// ==================== GET /api/v1/images ====================

	@Test
	void getImages_success() throws Exception {
		ImageResponse response = createTestResponse();
		when(imageService.getImages(any(), any())).thenReturn(List.of(response));

		mockMvc.perform(get("/api/v1/images")
						.param("entityType", "USER")
						.param("entityId", USER_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].fileUrl").value("https://supabase.co/storage/v1/object/public/marketplace-images/test.jpg"));
	}

	@Test
	void getImages_noResults_returnsEmptyList() throws Exception {
		when(imageService.getImages(any(), any())).thenReturn(List.of());

		mockMvc.perform(get("/api/v1/images")
						.param("entityType", "USER")
						.param("entityId", USER_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").isEmpty());
	}

	// ==================== DELETE /api/v1/images/{imageId} ====================

	@Test
	void deleteImage_success() throws Exception {
		mockMvc.perform(delete("/api/v1/images/" + IMAGE_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Image deleted"));
	}

	@Test
	void deleteImage_notFound_returns404() throws Exception {
		String notFoundId = UUID.randomUUID().toString();
		org.mockito.Mockito.doThrow(new ResourceNotFoundException("Image", "id", notFoundId))
				.when(imageService).deleteImage(anyString(), any());

		mockMvc.perform(delete("/api/v1/images/" + notFoundId))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteImage_notOwner_returns403() throws Exception {
		org.mockito.Mockito.doThrow(new AccessDeniedException("You can only delete your own images"))
				.when(imageService).deleteImage(anyString(), any());

		mockMvc.perform(delete("/api/v1/images/" + IMAGE_ID))
				.andExpect(status().isForbidden());
	}

	// ==================== HELPERS ====================

	private ImageResponse createTestResponse() {
		return new ImageResponse(
				IMAGE_ID,
				"https://supabase.co/storage/v1/object/public/marketplace-images/test.jpg",
				"test.jpg",
				1024L,
				"image/jpeg",
				EntityType.USER,
				USER_ID,
				USER_ID,
				null);
	}
}
