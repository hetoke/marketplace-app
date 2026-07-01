package com.marketplace.upload.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketplace.upload.dto.ImageResponse;
import com.marketplace.upload.model.EntityType;
import com.marketplace.upload.model.Image;
import com.marketplace.upload.repository.ImageRepository;
import com.marketplace.upload.storage.SupabaseStorageClient;
import com.marketplace.shared.exception.AccessDeniedException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

	@Mock
	private ImageRepository imageRepository;

	@Mock
	private SupabaseStorageClient storageClient;

	@InjectMocks
	private ImageService imageService;

	private static final UUID USER_ID = UUID.randomUUID();
	private static final UUID IMAGE_ID = UUID.randomUUID();

	// ==================== GET IMAGES ====================

	@Test
	void getImages_returnsList() {
		Image image = createTestImage();
		when(imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.USER, USER_ID))
				.thenReturn(List.of(image));

		List<ImageResponse> responses = imageService.getImages(EntityType.USER, USER_ID);

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).entityType()).isEqualTo(EntityType.USER);
	}

	@Test
	void getImages_noImages_returnsEmptyList() {
		when(imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.USER, USER_ID))
				.thenReturn(List.of());

		List<ImageResponse> responses = imageService.getImages(EntityType.USER, USER_ID);

		assertThat(responses).isEmpty();
	}

	// ==================== DELETE IMAGE ====================

	@Test
	void deleteImage_ownImage_success() {
		Image image = createTestImage();
		image.setUploadedBy(USER_ID);

		when(imageRepository.findById(IMAGE_ID)).thenReturn(Optional.of(image));

		imageService.deleteImage(USER_ID.toString(), IMAGE_ID);

		verify(storageClient).deleteFiles(any());
		verify(imageRepository).delete(image);
	}

	@Test
	void deleteImage_notOwner_throwsAccessDenied() {
		Image image = createTestImage();
		image.setUploadedBy(UUID.randomUUID());

		when(imageRepository.findById(IMAGE_ID)).thenReturn(Optional.of(image));

		assertThatThrownBy(() -> imageService.deleteImage(USER_ID.toString(), IMAGE_ID))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessage("You can only delete your own images");

		verify(imageRepository, never()).delete(any());
	}

	@Test
	void deleteImage_notFound_throwsResourceNotFound() {
		when(imageRepository.findById(IMAGE_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> imageService.deleteImage(USER_ID.toString(), IMAGE_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Image");

		verify(imageRepository, never()).delete(any());
	}

	@Test
	void deleteImage_storageDeleteFails_stillDeletesFromDb() {
		Image image = createTestImage();
		image.setUploadedBy(USER_ID);

		when(imageRepository.findById(IMAGE_ID)).thenReturn(Optional.of(image));
		doThrow(new RuntimeException("Storage unavailable"))
				.when(storageClient).deleteFiles(any());

		imageService.deleteImage(USER_ID.toString(), IMAGE_ID);

		verify(imageRepository).delete(image);
	}

	// ==================== DELETE IMAGES BY ENTITY ====================

	@Test
	void deleteImagesByEntity_deletesFromStorageAndDb() {
		Image image = createTestImage();
		when(imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.USER, USER_ID))
				.thenReturn(List.of(image));

		imageService.deleteImagesByEntity(EntityType.USER, USER_ID);

		verify(storageClient).deleteFiles(any());
		verify(imageRepository).deleteByEntityTypeAndEntityId(EntityType.USER, USER_ID);
	}

	@Test
	void deleteImagesByEntity_noImages_skipsStorage() {
		when(imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.USER, USER_ID))
				.thenReturn(List.of());

		imageService.deleteImagesByEntity(EntityType.USER, USER_ID);

		verify(storageClient, never()).deleteFiles(any());
		verify(imageRepository).deleteByEntityTypeAndEntityId(EntityType.USER, USER_ID);
	}

	// ==================== EXTRACT STORAGE PATH ====================

	@Test
	void extractStoragePath_validSupabaseUrl_extractsCorrectly() {
		Image image = createTestImage();
		image.setFileUrl("https://vuaqyfpnajahttjzfvds.supabase.co/storage/v1/object/public/marketplace-images/user1/photo.jpg");

		String path = imageService.extractStoragePath(image);

		assertThat(path).isEqualTo("marketplace-images/user1/photo.jpg");
	}

	@Test
	void extractStoragePath_noPublicMarker_returnsFullPath() {
		Image image = createTestImage();
		image.setFileUrl("https://example.com/some/path/file.jpg");

		String path = imageService.extractStoragePath(image);

		assertThat(path).isEqualTo("some/path/file.jpg");
	}

	@Test
	void extractStoragePath_invalidUrl_returnsNull() {
		Image image = createTestImage();
		image.setFileUrl("not a valid url");

		String path = imageService.extractStoragePath(image);

		assertThat(path).isNull();
	}

	// ==================== HELPERS ====================

	private Image createTestImage() {
		Image image = new Image();
		image.setId(IMAGE_ID);
		image.setFileUrl("https://vuaqyfpnajahttjzfvds.supabase.co/storage/v1/object/public/marketplace-images/user1/photo.jpg");
		image.setFileName("photo.jpg");
		image.setFileSize(1024L);
		image.setContentType("image/jpeg");
		image.setEntityType(EntityType.USER);
		image.setEntityId(USER_ID);
		image.setUploadedBy(USER_ID);
		return image;
	}
}
