package com.marketplace.upload.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.marketplace.upload.model.EntityType;
import com.marketplace.upload.model.Image;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ImageRepositoryTest {

    @Mock
    private ImageRepository imageRepository;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ENTITY_ID = UUID.randomUUID();

    private Image img1;
    private Image img2;
    private Image img3;

    @BeforeEach
    void setUp() {
        img1 = createImage("https://supabase.co/a.jpg", EntityType.USER, USER_ID);
        img2 = createImage("https://supabase.co/b.jpg", EntityType.USER, USER_ID);
        img3 = createImage("https://supabase.co/c.jpg", EntityType.USER, USER_ID);
    }

    @Test
    void findByEntityTypeAndEntityId_returnsCorrectOrder() {
        when(imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.USER, USER_ID))
                .thenReturn(List.of(img1, img2, img3));

        List<Image> results = imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.USER, USER_ID);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getId()).isEqualTo(img1.getId());
        assertThat(results.get(1).getId()).isEqualTo(img2.getId());
        assertThat(results.get(2).getId()).isEqualTo(img3.getId());
    }

    @Test
    void findByEntityTypeAndEntityId_noMatch_returnsEmpty() {
        UUID randomId = UUID.randomUUID();
        when(imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.PRODUCT, randomId))
                .thenReturn(List.of());

        List<Image> results = imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(
                EntityType.PRODUCT, randomId);

        assertThat(results).isEmpty();
    }

    @Test
    void findByEntityTypeAndEntityId_differentEntityTypes_returnsOnlyMatching() {
        Image productImg = createImage("https://supabase.co/product.jpg", EntityType.PRODUCT, ENTITY_ID);

        when(imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.USER, USER_ID))
                .thenReturn(List.of(img1));
        when(imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.PRODUCT, ENTITY_ID))
                .thenReturn(List.of(productImg));

        List<Image> userResults = imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.USER, USER_ID);
        List<Image> productResults = imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.PRODUCT, ENTITY_ID);

        assertThat(userResults).hasSize(1);
        assertThat(userResults.get(0).getEntityType()).isEqualTo(EntityType.USER);
        assertThat(productResults).hasSize(1);
        assertThat(productResults.get(0).getEntityType()).isEqualTo(EntityType.PRODUCT);
    }

    @Test
    void deleteByEntityTypeAndEntityId_removesOnlyMatching() {
        Image productImg = createImage("https://supabase.co/product.jpg", EntityType.PRODUCT, ENTITY_ID);

        when(imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.PRODUCT, ENTITY_ID))
                .thenReturn(List.of(productImg));
        when(imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.USER, USER_ID))
                .thenReturn(List.of());

        imageRepository.deleteByEntityTypeAndEntityId(EntityType.USER, USER_ID);

        List<Image> remaining = imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.PRODUCT, ENTITY_ID);
        assertThat(remaining).hasSize(1);

        List<Image> deleted = imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType.USER, USER_ID);
        assertThat(deleted).isEmpty();
    }

    @Test
    void findAllByOrderByCreatedAtAsc_pagination() {
        List<Image> allImages = List.of(img1, img2, img3,
                createImage("https://supabase.co/d.jpg", EntityType.USER, USER_ID),
                createImage("https://supabase.co/e.jpg", EntityType.USER, USER_ID));

        Page<Image> page = new PageImpl<>(allImages, PageRequest.of(0, 5), 5);
        when(imageRepository.findAllByOrderByCreatedAtAsc(any(PageRequest.class))).thenReturn(page);

        Page<Image> result = imageRepository.findAllByOrderByCreatedAtAsc(PageRequest.of(0, 5));

        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getTotalElements()).isEqualTo(5);
    }

    @Test
    void findAllByOrderByCreatedAtAsc_empty_returnsEmptyPage() {
        Page<Image> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(imageRepository.findAllByOrderByCreatedAtAsc(any(PageRequest.class))).thenReturn(emptyPage);

        Page<Image> result = imageRepository.findAllByOrderByCreatedAtAsc(PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void findByFileUrlContaining_findsMatch() {
        Image userImg = createImage(
                "https://vuaqyfpnajahttjzfvds.supabase.co/storage/v1/object/public/marketplace-images/user1/photo.jpg",
                EntityType.USER, USER_ID);
        userImg.setFileName("photo.jpg");

        when(imageRepository.findByFileUrlContaining("photo.jpg")).thenReturn(Optional.of(userImg));

        Optional<Image> result = imageRepository.findByFileUrlContaining("photo.jpg");

        assertThat(result).isPresent();
        assertThat(result.get().getFileName()).isEqualTo("photo.jpg");
    }

    @Test
    void findByFileUrlContaining_noMatch_returnsEmpty() {
        when(imageRepository.findByFileUrlContaining("nonexistent.jpg")).thenReturn(Optional.empty());

        Optional<Image> result = imageRepository.findByFileUrlContaining("nonexistent.jpg");

        assertThat(result).isEmpty();
    }

    private Image createImage(String fileUrl, EntityType entityType, UUID entityId) {
        Image image = new Image();
        image.setId(UUID.randomUUID());
        image.setFileUrl(fileUrl);
        image.setFileName(fileUrl.substring(fileUrl.lastIndexOf('/') + 1));
        image.setFileSize(1024L);
        image.setContentType("image/jpeg");
        image.setEntityType(entityType);
        image.setEntityId(entityId);
        image.setUploadedBy(USER_ID);
        image.setCreatedAt(Instant.now());
        return image;
    }
}
