package com.marketplace.webhook.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketplace.upload.storage.SupabaseStorageProperties;
import com.marketplace.shared.exception.ResourceNotFoundException;
import com.marketplace.upload.service.UploadService;
import com.marketplace.webhook.dto.StorageWebhookRequest.StorageRecord;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

	@Mock
	private UploadService uploadService;

	@Mock
	private SupabaseStorageProperties storageProperties;

	@InjectMocks
	private WebhookService webhookService;

	// ==================== HANDLE STORAGE INSERT ====================

	@Test
	void handleStorageInsert_delegatesToUploadService() {
		StorageRecord record = new StorageRecord(
				"test-id",
				"users/" + UUID.randomUUID() + "/avatar.jpg",
				"marketplace-images",
				"image/jpeg",
				Map.of("size", 2048));

		webhookService.handleStorageInsert(record);

		verify(uploadService).completeUploadFromWebhook(record.name(), record);
	}

	@Test
	void handleStorageInsert_nullPath_doesNotCallUploadService() {
		StorageRecord record = new StorageRecord(
				"test-id",
				null,
				"marketplace-images",
				"image/jpeg",
				null);

		webhookService.handleStorageInsert(record);

		verify(uploadService, never()).completeUploadFromWebhook(anyString(), any());
	}

	@Test
	void handleStorageInsert_emptyPath_doesNotCallUploadService() {
		StorageRecord record = new StorageRecord(
				"test-id",
				"",
				"marketplace-images",
				"image/jpeg",
				null);

		webhookService.handleStorageInsert(record);

		verify(uploadService, never()).completeUploadFromWebhook(anyString(), any());
	}

	@Test
	void handleStorageInsert_uploadServiceThrows_propagatesException() {
		String path = "users/" + UUID.randomUUID() + "/avatar.jpg";
		StorageRecord record = new StorageRecord("test-id", path, "marketplace-images", "image/jpeg", null);

		doThrow(new ResourceNotFoundException("UploadSession", "storagePath", path))
				.when(uploadService).completeUploadFromWebhook(path, record);

		try {
			webhookService.handleStorageInsert(record);
		} catch (ResourceNotFoundException e) {
			// expected
		}

		verify(uploadService).completeUploadFromWebhook(path, record);
	}
}
