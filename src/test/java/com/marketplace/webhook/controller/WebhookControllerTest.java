package com.marketplace.webhook.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.shared.exception.GlobalExceptionHandler;
import com.marketplace.shared.exception.ResourceNotFoundException;
import com.marketplace.webhook.dto.StorageWebhookRequest;
import com.marketplace.webhook.dto.StorageWebhookRequest.StorageRecord;
import com.marketplace.webhook.service.WebhookService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

	private MockMvc mockMvc;

	@Mock
	private WebhookService webhookService;

	private WebhookController webhookController;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private static final String WEBHOOK_SECRET = "test-secret";

	@BeforeEach
	void setUp() {
		webhookController = new WebhookController(webhookService, WEBHOOK_SECRET);
		mockMvc = MockMvcBuilders.standaloneSetup(webhookController)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	// ==================== VALID WEBHOOK ====================

	@Test
	void handleStorageWebhook_validInsert_processesAndReturns200() throws Exception {
		StorageWebhookRequest request = new StorageWebhookRequest(
				"INSERT",
				new StorageRecord(
						UUID.randomUUID().toString(),
						"users/" + UUID.randomUUID() + "/avatar.jpg",
						"marketplace-images",
						"image/jpeg",
						Map.of("size", 1024)));

		mockMvc.perform(post("/api/v1/webhooks/storage")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.header("X-Webhook-Secret", WEBHOOK_SECRET))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Webhook processed"));

		verify(webhookService).handleStorageInsert(request.record());
	}

	@Test
	void handleStorageWebhook_validDelete_returns200WithIgnored() throws Exception {
		StorageWebhookRequest request = new StorageWebhookRequest(
				"DELETE",
				new StorageRecord(
						UUID.randomUUID().toString(),
						"users/" + UUID.randomUUID() + "/avatar.jpg",
						"marketplace-images",
						"image/jpeg",
						null));

		mockMvc.perform(post("/api/v1/webhooks/storage")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.header("X-Webhook-Secret", WEBHOOK_SECRET))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Event type ignored"));

		verify(webhookService, never()).handleStorageInsert(any());
	}

	// ==================== AUTHENTICATION ====================

	@Test
	void handleStorageWebhook_wrongSecret_returns403() throws Exception {
		StorageWebhookRequest request = new StorageWebhookRequest(
				"INSERT",
				new StorageRecord(
						UUID.randomUUID().toString(),
						"users/" + UUID.randomUUID() + "/avatar.jpg",
						"marketplace-images",
						"image/jpeg",
						null));

		mockMvc.perform(post("/api/v1/webhooks/storage")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.header("X-Webhook-Secret", "wrong-secret"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Invalid webhook secret"));

		verify(webhookService, never()).handleStorageInsert(any());
	}

	@Test
	void handleStorageWebhook_missingSecret_returns403() throws Exception {
		StorageWebhookRequest request = new StorageWebhookRequest(
				"INSERT",
				new StorageRecord(
						UUID.randomUUID().toString(),
						"users/" + UUID.randomUUID() + "/avatar.jpg",
						"marketplace-images",
						"image/jpeg",
						null));

		mockMvc.perform(post("/api/v1/webhooks/storage")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isForbidden());

		verify(webhookService, never()).handleStorageInsert(any());
	}

	// ==================== ERROR HANDLING ====================

	@Test
	void handleStorageWebhook_serviceThrowsException_propagates() throws Exception {
		StorageWebhookRequest request = new StorageWebhookRequest(
				"INSERT",
				new StorageRecord(
						UUID.randomUUID().toString(),
						"products/" + UUID.randomUUID() + "/img.jpg",
						"marketplace-images",
						"image/png",
						null));

		doThrow(new ResourceNotFoundException("Product", "id", UUID.randomUUID()))
				.when(webhookService).handleStorageInsert(any());

		mockMvc.perform(post("/api/v1/webhooks/storage")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.header("X-Webhook-Secret", WEBHOOK_SECRET))
				.andExpect(status().isNotFound());
	}
}
