package com.marketplace.webhook.controller;

import com.marketplace.shared.dto.ApiResponse;
import com.marketplace.webhook.dto.StorageWebhookRequest;
import com.marketplace.webhook.service.WebhookService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

	private final WebhookService webhookService;
	private final String expectedSecret;

	public WebhookController(WebhookService webhookService,
			@Value("${app.webhook.secret:changeme}") String expectedSecret) {
		this.webhookService = webhookService;
		this.expectedSecret = expectedSecret;
	}

	@PostMapping("/storage")
	public ResponseEntity<ApiResponse<Void>> handleStorageWebhook(
			@RequestBody StorageWebhookRequest request,
			@RequestHeader(value = "X-Webhook-Secret", required = false) String secret) {
		if (secret == null || !expectedSecret.equals(secret)) {
			return ResponseEntity.status(403).body(ApiResponse.error("Invalid webhook secret"));
		}

		if (!"INSERT".equals(request.type())) {
			return ResponseEntity.ok(ApiResponse.ok("Event type ignored", null));
		}

		webhookService.handleStorageInsert(request.record());
		return ResponseEntity.ok(ApiResponse.ok("Webhook processed", null));
	}
}
