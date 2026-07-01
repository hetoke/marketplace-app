package com.marketplace.upload.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketplace.upload.model.EntityType;
import com.marketplace.upload.model.Image;
import com.marketplace.upload.repository.ImageRepository;
import com.marketplace.upload.storage.SupabaseStorageClient;
import com.marketplace.upload.storage.SupabaseStorageClient.SignedUploadUrl;
import com.marketplace.upload.storage.SupabaseStorageProperties;
import com.marketplace.product.model.Product;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.shared.exception.AccessDeniedException;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import com.marketplace.upload.dto.UploadResponse;
import com.marketplace.upload.model.UploadSession;
import com.marketplace.upload.model.UploadStatus;
import com.marketplace.upload.repository.UploadSessionRepository;
import com.marketplace.user.model.User;
import com.marketplace.user.repository.UserRepository;
import com.marketplace.webhook.dto.StorageWebhookRequest.StorageRecord;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {

    @Mock
    private UploadSessionRepository uploadSessionRepository;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SupabaseStorageClient storageClient;

    @Mock
    private SupabaseStorageProperties storageProperties;

    @InjectMocks
    private UploadService uploadService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(uploadService, "maxImagesPerProduct", 10);
        ReflectionTestUtils.setField(
            uploadService,
            "maxFileSizeBytes",
            5242880L
        );
        ReflectionTestUtils.setField(
            uploadService,
            "allowedContentTypes",
            "image/jpeg,image/png,image/webp,image/gif"
        );
        ReflectionTestUtils.setField(uploadService, "signedUrlExpiryHours", 2);
    }

    // ==================== USER AVATAR UPLOAD ====================

    @Test
    void requestUserUpload_success() {
        when(
            imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(
                EntityType.USER,
                USER_ID
            )
        ).thenReturn(List.of());
        when(
            storageClient.createSignedUploadUrl(anyString(), anyInt())
        ).thenReturn(
            new SignedUploadUrl(
                "https://supabase.co/signed",
                "token-123",
                Instant.now().plusSeconds(7200)
            )
        );
        when(uploadSessionRepository.save(any(UploadSession.class))).thenAnswer(
            inv -> inv.getArgument(0)
        );

        UploadResponse response = uploadService.requestUserUpload(
            USER_ID.toString()
        );

        assertThat(response.uploadUrl()).contains("signed");
        assertThat(response.token()).isEqualTo("token-123");
        verify(uploadSessionRepository).save(any(UploadSession.class));
    }

    @Test
    void requestUserUpload_sessionStoresJwtUserId() {
        when(
            imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(
                EntityType.USER,
                USER_ID
            )
        ).thenReturn(List.of());
        when(
            storageClient.createSignedUploadUrl(anyString(), anyInt())
        ).thenReturn(
            new SignedUploadUrl(
                "https://supabase.co/signed",
                "token-xyz",
                Instant.now().plusSeconds(7200)
            )
        );
        when(uploadSessionRepository.save(any(UploadSession.class))).thenAnswer(
            inv -> inv.getArgument(0)
        );

        uploadService.requestUserUpload(USER_ID.toString());

        ArgumentCaptor<UploadSession> captor = ArgumentCaptor.forClass(
            UploadSession.class
        );
        verify(uploadSessionRepository).save(captor.capture());
        UploadSession saved = captor.getValue();

        assertThat(saved.getEntityType()).isEqualTo(EntityType.USER);
        assertThat(saved.getEntityId()).isEqualTo(USER_ID);
        assertThat(saved.getUploadedBy()).isEqualTo(USER_ID);
        assertThat(saved.getStoragePath()).startsWith("users/" + USER_ID + "/");
        assertThat(saved.getFileName()).isEqualTo("avatar");
    }

    // ==================== PRODUCT IMAGE UPLOAD ====================

    @Test
    void requestProductUpload_success() {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setSellerId(USER_ID);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(
            Optional.of(product)
        );
        when(
            uploadSessionRepository.countByEntityTypeAndEntityIdAndStatus(
                EntityType.PRODUCT,
                PRODUCT_ID,
                UploadStatus.COMPLETED
            )
        ).thenReturn(0L);
        when(
            storageClient.createSignedUploadUrl(anyString(), anyInt())
        ).thenReturn(
            new SignedUploadUrl(
                "https://supabase.co/signed",
                "token-456",
                Instant.now().plusSeconds(7200)
            )
        );
        when(uploadSessionRepository.save(any(UploadSession.class))).thenAnswer(
            inv -> inv.getArgument(0)
        );

        UploadResponse response = uploadService.requestProductUpload(
            USER_ID.toString(),
            PRODUCT_ID
        );

        assertThat(response.token()).isEqualTo("token-456");
    }

    @Test
    void requestProductUpload_notSeller_throwsAccessDenied() {
        UUID otherSeller = UUID.randomUUID();
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setSellerId(otherSeller);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(
            Optional.of(product)
        );

        assertThatThrownBy(() ->
            uploadService.requestProductUpload(USER_ID.toString(), PRODUCT_ID)
        )
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("You can only upload images for your own products");
    }

    @Test
    void requestProductUpload_productNotFound_throwsResourceNotFound() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(
            Optional.empty()
        );

        assertThatThrownBy(() ->
            uploadService.requestProductUpload(USER_ID.toString(), PRODUCT_ID)
        )
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Product");
    }

    @Test
    void requestProductUpload_quotaExceeded_throwsBusinessException() {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setSellerId(USER_ID);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(
            Optional.of(product)
        );
        when(
            uploadSessionRepository.countByEntityTypeAndEntityIdAndStatus(
                EntityType.PRODUCT,
                PRODUCT_ID,
                UploadStatus.COMPLETED
            )
        ).thenReturn(10L);

        assertThatThrownBy(() ->
            uploadService.requestProductUpload(USER_ID.toString(), PRODUCT_ID)
        )
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Product image limit reached");
    }

    // ==================== COMPLETE UPLOAD FROM WEBHOOK ====================

    @Test
    void completeUploadFromWebhook_validSession_createsImage() {
        UploadSession session = createPendingSession();
        StorageRecord record = new StorageRecord(
            "test-id",
            session.getStoragePath(),
            "marketplace-images",
            "image/jpeg",
            Map.of("size", 1024)
        );

        when(
            uploadSessionRepository.findFirstByStoragePathAndStatusOrderByCreatedAtDesc(
                session.getStoragePath(),
                UploadStatus.PENDING
            )
        ).thenReturn(Optional.of(session));
        when(
            imageRepository.findByEntityTypeAndEntityIdAndUploadedByOrderByCreatedAtAsc(
                EntityType.USER,
                USER_ID,
                USER_ID
            )
        ).thenReturn(Optional.empty());
        when(storageProperties.publicUrl()).thenReturn(
            "https://placeholder.supabase.co"
        );
        when(storageProperties.bucketName()).thenReturn("marketplace-images");

        User user = new User();
        user.setId(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        uploadService.completeUploadFromWebhook(
            session.getStoragePath(),
            record
        );

        verify(imageRepository).save(any(Image.class));
        assertThat(session.getStatus()).isEqualTo(UploadStatus.COMPLETED);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getProfilePictureUrl()).contains(
            "placeholder.supabase.co"
        );
    }

    @Test
    void completeUploadFromWebhook_noSession_logsAndReturns() {
        StorageRecord record = new StorageRecord(
            "test-id",
            "users/unknown/file.jpg",
            "marketplace-images",
            "image/jpeg",
            null
        );

        when(
            uploadSessionRepository.findFirstByStoragePathAndStatusOrderByCreatedAtDesc(
                "users/unknown/file.jpg",
                UploadStatus.PENDING
            )
        ).thenReturn(Optional.empty());

        uploadService.completeUploadFromWebhook(
            "users/unknown/file.jpg",
            record
        );

        verify(imageRepository, never()).save(any());
    }

    @Test
    void completeUploadFromWebhook_expiredSession_marksExpired() {
        UploadSession session = createPendingSession();
        session.setExpiresAt(Instant.now().minusSeconds(3600));

        StorageRecord record = new StorageRecord(
            "test-id",
            session.getStoragePath(),
            "marketplace-images",
            "image/jpeg",
            null
        );

        when(
            uploadSessionRepository.findFirstByStoragePathAndStatusOrderByCreatedAtDesc(
                session.getStoragePath(),
                UploadStatus.PENDING
            )
        ).thenReturn(Optional.of(session));

        uploadService.completeUploadFromWebhook(
            session.getStoragePath(),
            record
        );

        assertThat(session.getStatus()).isEqualTo(UploadStatus.EXPIRED);
        verify(imageRepository, never()).save(any());
    }

    @Test
    void completeUploadFromWebhook_invalidFileType_marksFailed() {
        UploadSession session = createPendingSession();
        StorageRecord record = new StorageRecord(
            "test-id",
            session.getStoragePath(),
            "marketplace-images",
            "application/pdf",
            Map.of("size", 1024)
        );

        when(
            uploadSessionRepository.findFirstByStoragePathAndStatusOrderByCreatedAtDesc(
                session.getStoragePath(),
                UploadStatus.PENDING
            )
        ).thenReturn(Optional.of(session));

        uploadService.completeUploadFromWebhook(
            session.getStoragePath(),
            record
        );

        assertThat(session.getStatus()).isEqualTo(UploadStatus.FAILED);
        verify(imageRepository, never()).save(any());
    }

    @Test
    void completeUploadFromWebhook_fileTooLarge_marksFailed() {
        UploadSession session = createPendingSession();
        StorageRecord record = new StorageRecord(
            "test-id",
            session.getStoragePath(),
            "marketplace-images",
            "image/jpeg",
            Map.of("size", 10_000_000)
        );

        when(
            uploadSessionRepository.findFirstByStoragePathAndStatusOrderByCreatedAtDesc(
                session.getStoragePath(),
                UploadStatus.PENDING
            )
        ).thenReturn(Optional.of(session));

        uploadService.completeUploadFromWebhook(
            session.getStoragePath(),
            record
        );

        assertThat(session.getStatus()).isEqualTo(UploadStatus.FAILED);
        verify(imageRepository, never()).save(any());
    }

    @Test
    void completeUploadFromWebhook_userUpsert_replacesExistingImage() {
        UploadSession session = createPendingSession();
        Image existingImage = new Image();
        existingImage.setId(UUID.randomUUID());
        existingImage.setFileUrl("https://old-url.com/old.jpg");

        StorageRecord record = new StorageRecord(
            "test-id",
            session.getStoragePath(),
            "marketplace-images",
            "image/jpeg",
            Map.of("size", 2048)
        );

        when(
            uploadSessionRepository.findFirstByStoragePathAndStatusOrderByCreatedAtDesc(
                session.getStoragePath(),
                UploadStatus.PENDING
            )
        ).thenReturn(Optional.of(session));
        when(
            imageRepository.findByEntityTypeAndEntityIdAndUploadedByOrderByCreatedAtAsc(
                EntityType.USER,
                USER_ID,
                USER_ID
            )
        ).thenReturn(Optional.of(existingImage));
        when(storageProperties.publicUrl()).thenReturn(
            "https://placeholder.supabase.co"
        );
        when(storageProperties.bucketName()).thenReturn("marketplace-images");

        User user = new User();
        user.setId(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        uploadService.completeUploadFromWebhook(
            session.getStoragePath(),
            record
        );

        verify(imageRepository).save(existingImage);
        assertThat(existingImage.getFileSize()).isEqualTo(2048L);
    }

    // ==================== HELPERS ====================

    private UploadSession createPendingSession() {
        UploadSession session = new UploadSession();
        session.setId(UUID.randomUUID());
        session.setEntityType(EntityType.USER);
        session.setEntityId(USER_ID);
        session.setUploadedBy(USER_ID);
        session.setFileName("avatar");
        session.setStoragePath("users/" + USER_ID + "/avatar");
        session.setSupabaseToken("token-123");
        session.setStatus(UploadStatus.PENDING);
        session.setExpiresAt(Instant.now().plusSeconds(7200));
        return session;
    }
}
