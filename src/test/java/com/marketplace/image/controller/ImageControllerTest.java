package com.marketplace.image.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.image.dto.ImageRequest;
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
import org.springframework.http.MediaType;
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

	private final ObjectMapper objectMapper = new ObjectMapper();

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

	// ==================== POST /api/v1/images ====================

	// --- Happy paths ---

	@Test
	void saveImage_allFieldsPresent_success() throws Exception {
		ImageResponse response = createTestResponse();
		when(imageService.saveImage(anyString(), any())).thenReturn(response);

		mockMvc.perform(post("/api/v1/images")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(createFullRequest())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("Image saved"))
				.andExpect(jsonPath("$.data.fileUrl").value("https://supabase.co/storage/v1/object/public/marketplace-images/test.jpg"));
	}

	@Test
	void saveImage_minimalRequiredFields_success() throws Exception {
		ImageResponse response = createTestResponse();
		when(imageService.saveImage(anyString(), any())).thenReturn(response);

		mockMvc.perform(post("/api/v1/images")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"fileUrl\":\"https://supabase.co/storage/v1/object/public/marketplace-images/test.jpg\",\"entityType\":\"USER\",\"entityId\":\"" + USER_ID + "\"}"))
				.andExpect(status().isCreated());
	}

	// --- Invalid: fileUrl ---

	@Test
	void saveImage_missingFileUrl_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/images")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"entityType\":\"USER\",\"entityId\":\"" + USER_ID + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("fileUrl"));
	}

	@Test
	void saveImage_blankFileUrl_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/images")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"fileUrl\":\"\",\"entityType\":\"USER\",\"entityId\":\"" + USER_ID + "\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void saveImage_fileUrl1023chars_success() throws Exception {
		ImageResponse response = createTestResponse();
		when(imageService.saveImage(anyString(), any())).thenReturn(response);

		String url1023 = "a".repeat(1023);
		mockMvc.perform(post("/api/v1/images")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new ImageRequest(url1023, "f.jpg", 100L, "image/jpeg", EntityType.USER, UUID.fromString(USER_ID)))))
				.andExpect(status().isCreated());
	}

	@Test
	void saveImage_fileUrl1024chars_success() throws Exception {
		ImageResponse response = createTestResponse();
		when(imageService.saveImage(anyString(), any())).thenReturn(response);

		String url1024 = "a".repeat(1024);
		mockMvc.perform(post("/api/v1/images")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new ImageRequest(url1024, "f.jpg", 100L, "image/jpeg", EntityType.USER, UUID.fromString(USER_ID)))))
				.andExpect(status().isCreated());
	}

	@Test
	void saveImage_fileUrl1025chars_returns400() throws Exception {
		String url1025 = "a".repeat(1025);
		mockMvc.perform(post("/api/v1/images")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new ImageRequest(url1025, "f.jpg", 100L, "image/jpeg", EntityType.USER, UUID.fromString(USER_ID)))))
				.andExpect(status().isBadRequest());
	}

	// --- Invalid: fileName ---

	@Test
	void saveImage_fileName255chars_success() throws Exception {
		ImageResponse response = createTestResponse();
		when(imageService.saveImage(anyString(), any())).thenReturn(response);

		String name255 = "a".repeat(255);
		mockMvc.perform(post("/api/v1/images")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new ImageRequest("https://supabase.co/test.jpg", name255, 100L, "image/jpeg", EntityType.USER, UUID.fromString(USER_ID)))))
				.andExpect(status().isCreated());
	}

	@Test
	void saveImage_fileName256chars_returns400() throws Exception {
		String name256 = "a".repeat(256);
		mockMvc.perform(post("/api/v1/images")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new ImageRequest("https://supabase.co/test.jpg", name256, 100L, "image/jpeg", EntityType.USER, UUID.fromString(USER_ID)))))
				.andExpect(status().isBadRequest());
	}

	// --- Invalid: contentType ---

	@Test
	void saveImage_contentType100chars_success() throws Exception {
		ImageResponse response = createTestResponse();
		when(imageService.saveImage(anyString(), any())).thenReturn(response);

		String ct100 = "a".repeat(100);
		mockMvc.perform(post("/api/v1/images")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new ImageRequest("https://supabase.co/test.jpg", "f.jpg", 100L, ct100, EntityType.USER, UUID.fromString(USER_ID)))))
				.andExpect(status().isCreated());
	}

	@Test
	void saveImage_contentType101chars_returns400() throws Exception {
		String ct101 = "a".repeat(101);
		mockMvc.perform(post("/api/v1/images")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new ImageRequest("https://supabase.co/test.jpg", "f.jpg", 100L, ct101, EntityType.USER, UUID.fromString(USER_ID)))))
				.andExpect(status().isBadRequest());
	}

	// --- Invalid: entityType ---

	@Test
	void saveImage_missingEntityType_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/images")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"fileUrl\":\"https://supabase.co/test.jpg\",\"entityId\":\"" + USER_ID + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("entityType"));
	}

	@Test
	void saveImage_invalidEntityType_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/images")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"fileUrl\":\"https://supabase.co/test.jpg\",\"entityType\":\"INVALID\",\"entityId\":\"" + USER_ID + "\"}"))
				.andExpect(status().isBadRequest());
	}

	// --- Invalid: entityId ---

	@Test
	void saveImage_missingEntityId_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/images")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"fileUrl\":\"https://supabase.co/test.jpg\",\"entityType\":\"USER\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("entityId"));
	}

	@Test
	void saveImage_invalidEntityIdFormat_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/images")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"fileUrl\":\"https://supabase.co/test.jpg\",\"entityType\":\"USER\",\"entityId\":\"not-a-uuid\"}"))
				.andExpect(status().isBadRequest());
	}

	// --- Malformed request ---

	@Test
	void saveImage_emptyBody_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/images")
						.contentType(MediaType.APPLICATION_JSON)
						.content(""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void saveImage_malformedJson_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/images")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{bad}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Malformed request body"));
	}

	@Test
	void saveImage_wrongContentType_returns415() throws Exception {
		mockMvc.perform(post("/api/v1/images")
						.contentType(MediaType.TEXT_PLAIN)
						.content("some text"))
				.andExpect(status().isUnsupportedMediaType());
	}

	// --- HTTP method ---

	@Test
	void saveImage_putMethodNotAllowed_returns405() throws Exception {
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
						.put("/api/v1/images"))
				.andExpect(status().isMethodNotAllowed());
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

	private ImageRequest createFullRequest() {
		return new ImageRequest(
				"https://supabase.co/storage/v1/object/public/marketplace-images/test.jpg",
				"test.jpg",
				1024L,
				"image/jpeg",
				EntityType.USER,
				UUID.fromString(USER_ID));
	}
}
