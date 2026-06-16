package com.marketplace.image.scheduler;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.marketplace.image.service.ImageSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ImageSyncSchedulerTest {

	@Mock
	private ImageSyncService imageSyncService;

	@InjectMocks
	private ImageSyncScheduler imageSyncScheduler;

	// ==================== SYNC IMAGES ====================

	@Test
	void syncImages_enabled_callsService() {
		ReflectionTestUtils.setField(imageSyncScheduler, "syncEnabled", true);

		imageSyncScheduler.syncImages();

		verify(imageSyncService).syncAll();
	}

	@Test
	void syncImages_disabled_doesNotCallService() {
		ReflectionTestUtils.setField(imageSyncScheduler, "syncEnabled", false);

		imageSyncScheduler.syncImages();

		verify(imageSyncService, never()).syncAll();
	}

	@Test
	void syncImages_serviceThrows_doesNotPropagate() {
		ReflectionTestUtils.setField(imageSyncScheduler, "syncEnabled", true);
		doThrow(new RuntimeException("Storage unavailable"))
				.when(imageSyncService).syncAll();

		imageSyncScheduler.syncImages();

		verify(imageSyncService).syncAll();
	}
}
