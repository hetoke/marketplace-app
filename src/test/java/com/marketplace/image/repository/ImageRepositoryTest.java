package com.marketplace.image.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketplace.image.model.EntityType;
import com.marketplace.image.model.Image;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ImageRepositoryTest {

	@Autowired
	private ImageRepository imageRepository;

	private static final UUID USER_ID = UUID.randomUUID();
	private static final UUID ENTITY_ID = UUID.randomUUID();

	// ==================== findByEntityTypeAndEntityIdOrderByCreatedAtAsc ====================

	@Test
	void findByEntityTypeAndEntityId_returnsCorrectOrder() {
		Image img1 = createAndSaveImage("https://supabase.co/a.jpg", EntityType.USER, USER_ID);
		Image img2 = createAndSaveImage("https://supabase.co/b.jpg", EntityType.USER, USER_ID);
		Image img3 = createAndSaveImage("https://supabase.co/c.jpg", EntityType.USER, USER_ID);

		List<Image> results = imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.USER, USER_ID);

		assertThat(results).hasSize(3);
		assertThat(results.get(0).getId()).isEqualTo(img1.getId());
		assertThat(results.get(1).getId()).isEqualTo(img2.getId());
		assertThat(results.get(2).getId()).isEqualTo(img3.getId());
	}

	@Test
	void findByEntityTypeAndEntityId_noMatch_returnsEmpty() {
		createAndSaveImage("https://supabase.co/a.jpg", EntityType.USER, USER_ID);

		List<Image> results = imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(
				EntityType.PRODUCT, UUID.randomUUID());

		assertThat(results).isEmpty();
	}

	@Test
	void findByEntityTypeAndEntityId_differentEntityTypes_returnsOnlyMatching() {
		createAndSaveImage("https://supabase.co/user.jpg", EntityType.USER, USER_ID);
		createAndSaveImage("https://supabase.co/product.jpg", EntityType.PRODUCT, ENTITY_ID);

		List<Image> userResults = imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.USER, USER_ID);
		List<Image> productResults = imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.PRODUCT, ENTITY_ID);

		assertThat(userResults).hasSize(1);
		assertThat(userResults.get(0).getEntityType()).isEqualTo(EntityType.USER);
		assertThat(productResults).hasSize(1);
		assertThat(productResults.get(0).getEntityType()).isEqualTo(EntityType.PRODUCT);
	}

	// ==================== deleteByEntityTypeAndEntityId ====================

	@Test
	void deleteByEntityTypeAndEntityId_removesOnlyMatching() {
		createAndSaveImage("https://supabase.co/user1.jpg", EntityType.USER, USER_ID);
		createAndSaveImage("https://supabase.co/user2.jpg", EntityType.USER, USER_ID);
		createAndSaveImage("https://supabase.co/product.jpg", EntityType.PRODUCT, ENTITY_ID);

		imageRepository.deleteByEntityTypeAndEntityId(EntityType.USER, USER_ID);

		List<Image> remaining = imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.PRODUCT, ENTITY_ID);
		assertThat(remaining).hasSize(1);

		List<Image> deleted = imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.USER, USER_ID);
		assertThat(deleted).isEmpty();
	}

	// ==================== findAllByOrderByCreatedAtAsc ====================

	@Test
	void findAllByOrderByCreatedAtAsc_pagination() {
		for (int i = 0; i < 5; i++) {
			createAndSaveImage("https://supabase.co/img" + i + ".jpg", EntityType.USER, USER_ID);
		}

		Page<Image> page1 = imageRepository.findAllByOrderByCreatedAtAsc(PageRequest.of(0, 2));
		Page<Image> page2 = imageRepository.findAllByOrderByCreatedAtAsc(PageRequest.of(1, 2));
		Page<Image> page3 = imageRepository.findAllByOrderByCreatedAtAsc(PageRequest.of(2, 2));

		assertThat(page1.getContent()).hasSize(2);
		assertThat(page2.getContent()).hasSize(2);
		assertThat(page3.getContent()).hasSize(1);
		assertThat(page1.getTotalElements()).isEqualTo(5);
		assertThat(page1.getTotalPages()).isEqualTo(3);
	}

	@Test
	void findAllByOrderByCreatedAtAsc_empty_returnsEmptyPage() {
		Page<Image> page = imageRepository.findAllByOrderByCreatedAtAsc(PageRequest.of(0, 10));

		assertThat(page.getContent()).isEmpty();
		assertThat(page.getTotalElements()).isZero();
	}

	// ==================== findByFileUrlContaining ====================

	@Test
	void findByFileUrlContaining_findsMatch() {
		createAndSaveImage("https://vuaqyfpnajahttjzfvds.supabase.co/storage/v1/object/public/marketplace-images/user1/photo.jpg",
				EntityType.USER, USER_ID);

		Optional<Image> result = imageRepository.findByFileUrlContaining("photo.jpg");

		assertThat(result).isPresent();
		assertThat(result.get().getFileName()).isEqualTo("photo.jpg");
	}

	@Test
	void findByFileUrlContaining_noMatch_returnsEmpty() {
		createAndSaveImage("https://supabase.co/other.jpg", EntityType.USER, USER_ID);

		Optional<Image> result = imageRepository.findByFileUrlContaining("nonexistent.jpg");

		assertThat(result).isEmpty();
	}

	// ==================== HELPERS ====================

	private Image createAndSaveImage(String fileUrl, EntityType entityType, UUID entityId) {
		Image image = new Image();
		image.setFileUrl(fileUrl);
		image.setFileName(fileUrl.substring(fileUrl.lastIndexOf('/') + 1));
		image.setFileSize(1024L);
		image.setContentType("image/jpeg");
		image.setEntityType(entityType);
		image.setEntityId(entityId);
		image.setUploadedBy(USER_ID);
		return imageRepository.save(image);
	}
}
