package com.marketplace.upload.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketplace.shared.config.AbstractPostgresIntegrationTest;
import com.marketplace.upload.model.EntityType;
import com.marketplace.upload.model.Image;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ImageRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID userId;
    private UUID entityId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, role, authentication_type, display_name, is_verified, is_mfa_enabled, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                userId, "user-" + userId + "@test.com", "hash", "BUYER", "LOCAL",
                "Test User", true, false, Instant.now(), Instant.now());

        entityId = UUID.randomUUID();
    }

    @Test
    void findByEntityTypeAndEntityId_returnsCorrectOrder() {
        Image img1 = saveImage("https://supabase.co/a.jpg", EntityType.USER, userId);
        Image img2 = saveImage("https://supabase.co/b.jpg", EntityType.USER, userId);
        Image img3 = saveImage("https://supabase.co/c.jpg", EntityType.USER, userId);

        List<Image> results = imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.USER, userId);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getId()).isEqualTo(img1.getId());
        assertThat(results.get(1).getId()).isEqualTo(img2.getId());
        assertThat(results.get(2).getId()).isEqualTo(img3.getId());
    }

    @Test
    void findByEntityTypeAndEntityId_noMatch_returnsEmpty() {
        UUID randomId = UUID.randomUUID();

        List<Image> results = imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(
                EntityType.PRODUCT, randomId);

        assertThat(results).isEmpty();
    }

    @Test
    void findByEntityTypeAndEntityId_differentEntityTypes_returnsOnlyMatching() {
        saveImage("https://supabase.co/user.jpg", EntityType.USER, userId);
        saveImage("https://supabase.co/product.jpg", EntityType.PRODUCT, entityId);

        List<Image> userResults = imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.USER, userId);
        List<Image> productResults = imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.PRODUCT, entityId);

        assertThat(userResults).hasSize(1);
        assertThat(userResults.get(0).getEntityType()).isEqualTo(EntityType.USER);
        assertThat(productResults).hasSize(1);
        assertThat(productResults.get(0).getEntityType()).isEqualTo(EntityType.PRODUCT);
    }

    @Test
    void deleteByEntityTypeAndEntityId_removesOnlyMatching() {
        saveImage("https://supabase.co/product.jpg", EntityType.PRODUCT, entityId);
        saveImage("https://supabase.co/user.jpg", EntityType.USER, userId);

        imageRepository.deleteByEntityTypeAndEntityId(EntityType.USER, userId);

        List<Image> remaining = imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.PRODUCT, entityId);
        assertThat(remaining).hasSize(1);

        List<Image> deleted = imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.USER, userId);
        assertThat(deleted).isEmpty();
    }

    @Test
    void findAllByOrderByCreatedAtAsc_pagination() {
        for (int i = 0; i < 5; i++) {
            saveImage("https://supabase.co/" + i + ".jpg", EntityType.USER, userId);
        }

        var page = imageRepository.findAllByOrderByCreatedAtAsc(
                org.springframework.data.domain.PageRequest.of(0, 3));

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void findAllByOrderByCreatedAtAsc_empty_returnsEmptyPage() {
        var page = imageRepository.findAllByOrderByCreatedAtAsc(
                org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void findByFileUrlContaining_findsMatch() {
        String url = "https://vuaqyfpnajahttjzfvds.supabase.co/storage/v1/object/public/marketplace-images/user1/photo.jpg";
        Image saved = saveImage(url, EntityType.USER, userId);

        Optional<Image> result = imageRepository.findByFileUrlContaining("photo.jpg");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void findByFileUrlContaining_noMatch_returnsEmpty() {
        Optional<Image> result = imageRepository.findByFileUrlContaining("nonexistent.jpg");

        assertThat(result).isEmpty();
    }

    @Test
    void deleteByEntityTypeAndEntityId_emptyDelete_noError() {
        UUID randomId = UUID.randomUUID();

        imageRepository.deleteByEntityTypeAndEntityId(EntityType.USER, randomId);

        assertThat(imageRepository.count()).isZero();
    }

    @Test
    void findByEntityTypeAndEntityIdAndUploadedByOrderByCreatedAtAsc_returnsCorrectImage() {
        UUID otherUserId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, role, authentication_type, display_name, is_verified, is_mfa_enabled, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                otherUserId, "other-" + otherUserId + "@test.com", "hash", "BUYER", "LOCAL",
                "Other User", true, false, Instant.now(), Instant.now());

        Image myImage = saveImage("https://supabase.co/mine.jpg", EntityType.USER, userId);
        saveImage("https://supabase.co/theirs.jpg", EntityType.USER, otherUserId);

        Optional<Image> result = imageRepository.findByEntityTypeAndEntityIdAndUploadedByOrderByCreatedAtAsc(
                EntityType.USER, userId, userId);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(myImage.getId());
        assertThat(result.get().getUploadedBy()).isEqualTo(userId);
    }

    private Image saveImage(String fileUrl, EntityType entityType, UUID entityId) {
        Image image = new Image();
        image.setFileUrl(fileUrl);
        image.setFileName(fileUrl.substring(fileUrl.lastIndexOf('/') + 1));
        image.setFileSize(1024L);
        image.setContentType("image/jpeg");
        image.setEntityType(entityType);
        image.setEntityId(entityId);
        image.setUploadedBy(userId);
        image.setCreatedAt(Instant.now());
        return imageRepository.save(image);
    }
}
