package com.marketplace.upload.service;

import com.marketplace.image.model.EntityType;
import com.marketplace.image.model.Image;
import com.marketplace.image.repository.ImageRepository;
import com.marketplace.image.storage.SupabaseStorageClient;
import com.marketplace.image.storage.SupabaseStorageProperties;
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
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(
        UploadService.class
    );

    private final UploadSessionRepository uploadSessionRepository;
    private final ImageRepository imageRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final SupabaseStorageClient storageClient;
    private final SupabaseStorageProperties storageProperties;

    @Value("${app.upload.max-images-per-product:10}")
    private int maxImagesPerProduct;

    @Value("${app.upload.max-file-size-bytes:5242880}")
    private long maxFileSizeBytes;

    @Value(
        "${app.upload.allowed-content-types:image/jpeg,image/png,image/webp,image/gif}"
    )
    private String allowedContentTypes;

    @Value("${app.upload.signed-url-expiry-hours:2}")
    private int signedUrlExpiryHours;

    public UploadService(
        UploadSessionRepository uploadSessionRepository,
        ImageRepository imageRepository,
        ProductRepository productRepository,
        UserRepository userRepository,
        SupabaseStorageClient storageClient,
        SupabaseStorageProperties storageProperties
    ) {
        this.uploadSessionRepository = uploadSessionRepository;
        this.imageRepository = imageRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.storageClient = storageClient;
        this.storageProperties = storageProperties;
    }

    @Transactional
    public UploadResponse requestUserUpload(String userId) {
        UUID userUuid = UUID.fromString(userId);
        EntityType entityType = EntityType.USER;
        UUID entityId = userUuid;

        checkUserQuota(entityId);

        String fileName = "avatar";
        String storagePath = buildStoragePath(entityType, entityId, fileName);
        SupabaseStorageClient.SignedUploadUrl signedUrl =
            storageClient.createSignedUploadUrl(
                storagePath,
                signedUrlExpiryHours
            );

        UploadSession session = createSession(
            entityType,
            entityId,
            userUuid,
            fileName,
            storagePath,
            signedUrl
        );

        log.info(
            "Created upload session: id={}, entityType=USER, entityId={}, path={}",
            session.getId(),
            entityId,
            storagePath
        );

        return new UploadResponse(
            session.getId(),
            signedUrl.uploadUrl(),
            signedUrl.token(),
            signedUrl.expiresAt()
        );
    }

    @Transactional
    public UploadResponse requestProductUpload(String userId, UUID productId) {
        UUID userUuid = UUID.fromString(userId);
        EntityType entityType = EntityType.PRODUCT;

        Product product = productRepository
            .findById(productId)
            .orElseThrow(() ->
                new ResourceNotFoundException("Product", "id", productId)
            );
        if (!product.getSellerId().equals(userUuid)) {
            throw new AccessDeniedException(
                "You can only upload images for your own products"
            );
        }

        checkProductQuota(productId);

        String fileName = "image-" + UUID.randomUUID();
        String storagePath = buildStoragePath(entityType, productId, fileName);
        SupabaseStorageClient.SignedUploadUrl signedUrl =
            storageClient.createSignedUploadUrl(
                storagePath,
                signedUrlExpiryHours
            );

        UploadSession session = createSession(
            entityType,
            productId,
            userUuid,
            fileName,
            storagePath,
            signedUrl
        );

        log.info(
            "Created upload session: id={}, entityType=PRODUCT, entityId={}, path={}",
            session.getId(),
            productId,
            storagePath
        );

        return new UploadResponse(
            session.getId(),
            signedUrl.uploadUrl(),
            signedUrl.token(),
            signedUrl.expiresAt()
        );
    }

    @Transactional
    public void completeUploadFromWebhook(
        String storagePath,
        StorageRecord record
    ) {
        Optional<UploadSession> sessionOpt =
            uploadSessionRepository.findFirstByStoragePathAndStatusOrderByCreatedAtDesc(
                storagePath,
                UploadStatus.PENDING
            );

        if (sessionOpt.isEmpty()) {
            log.warn(
                "No pending upload session found for storage path: {}",
                storagePath
            );
            return;
        }

        UploadSession session = sessionOpt.get();

        if (session.getExpiresAt().isBefore(Instant.now())) {
            session.setStatus(UploadStatus.EXPIRED);
            session.setUpdatedAt(Instant.now());
            uploadSessionRepository.save(session);
            log.warn("Upload session expired: id={}", session.getId());
            return;
        }

        try {
            validateFileType(record.mimetype());
            Object sizeObj =
                record.metadata() != null
                    ? record.metadata().get("size")
                    : null;
            if (sizeObj != null) {
                validateFileSize(Long.valueOf(sizeObj.toString()));
            }
        } catch (BusinessException e) {
            session.setStatus(UploadStatus.FAILED);
            session.setUpdatedAt(Instant.now());
            uploadSessionRepository.save(session);
            log.warn(
                "Upload rejected: id={}, reason={}",
                session.getId(),
                e.getMessage()
            );
            return;
        }

        String fileUrl =
            storageProperties.publicUrl() +
            "/storage/v1/object/public/" +
            storageProperties.bucketName() +
            "/" +
            storagePath;

        if (session.getEntityType() == EntityType.USER) {
            handleUserImageUpsert(session, fileUrl, record);
        } else {
            handleProductImageInsert(session, fileUrl, record);
        }

        session.setStatus(UploadStatus.COMPLETED);
        session.setUpdatedAt(Instant.now());
        uploadSessionRepository.save(session);

        log.info(
            "Completed upload session: id={}, entityType={}, entityId={}",
            session.getId(),
            session.getEntityType(),
            session.getEntityId()
        );
    }

    private UploadSession createSession(
        EntityType entityType,
        UUID entityId,
        UUID uploadedBy,
        String fileName,
        String storagePath,
        SupabaseStorageClient.SignedUploadUrl signedUrl
    ) {
        UploadSession session = new UploadSession();
        session.setEntityType(entityType);
        session.setEntityId(entityId);
        session.setUploadedBy(uploadedBy);
        session.setFileName(fileName);
        session.setStoragePath(storagePath);
        session.setSupabaseToken(signedUrl.token());
        session.setStatus(UploadStatus.PENDING);
        session.setExpiresAt(signedUrl.expiresAt());
        uploadSessionRepository.save(session);
        return session;
    }

    private void handleUserImageUpsert(
        UploadSession session,
        String fileUrl,
        StorageRecord record
    ) {
        Optional<Image> existing =
            imageRepository.findByEntityTypeAndEntityIdAndUploadedByOrderByCreatedAtAsc(
                EntityType.USER,
                session.getEntityId(),
                session.getUploadedBy()
            );

        if (existing.isPresent()) {
            Image image = existing.get();
            image.setFileUrl(fileUrl);
            image.setFileName(extractFileName(session.getStoragePath()));
            image.setFileSize(
                record.metadata() != null &&
                    record.metadata().get("size") != null
                    ? Long.valueOf(record.metadata().get("size").toString())
                    : session.getFileSize()
            );
            image.setContentType(record.mimetype());
            imageRepository.save(image);
        } else {
            insertNewImage(session, fileUrl, record);
        }

        userRepository.findById(session.getEntityId()).ifPresent(user -> {
            user.setProfilePictureUrl(fileUrl);
            userRepository.save(user);
        });
    }

    private void handleProductImageInsert(
        UploadSession session,
        String fileUrl,
        StorageRecord record
    ) {
        long count =
            uploadSessionRepository.countByEntityTypeAndEntityIdAndStatus(
                EntityType.PRODUCT,
                session.getEntityId(),
                UploadStatus.COMPLETED
            );
        if (count >= maxImagesPerProduct) {
            log.warn(
                "Product image limit reached: entityId={}, count={}",
                session.getEntityId(),
                count
            );
            return;
        }
        insertNewImage(session, fileUrl, record);
    }

    private void insertNewImage(
        UploadSession session,
        String fileUrl,
        StorageRecord record
    ) {
        Image image = new Image();
        image.setFileUrl(fileUrl);
        image.setFileName(extractFileName(session.getStoragePath()));
        image.setFileSize(
            record.metadata() != null && record.metadata().get("size") != null
                ? Long.valueOf(record.metadata().get("size").toString())
                : session.getFileSize()
        );
        image.setContentType(record.mimetype());
        image.setEntityType(session.getEntityType());
        image.setEntityId(session.getEntityId());
        image.setUploadedBy(session.getUploadedBy());
        imageRepository.save(image);
    }

    private void validateFileType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return;
        }
        List<String> allowed = List.of(allowedContentTypes.split(","));
        if (
            allowed
                .stream()
                .noneMatch(t -> t.trim().equalsIgnoreCase(contentType))
        ) {
            throw new BusinessException(
                "File type not allowed: " + contentType
            );
        }
    }

    private void validateFileSize(Long fileSize) {
        if (fileSize != null && fileSize > maxFileSizeBytes) {
            throw new BusinessException(
                "File size exceeds maximum allowed: " +
                    maxFileSizeBytes +
                    " bytes"
            );
        }
    }

    private void checkUserQuota(UUID entityId) {
        long count = imageRepository
            .findByEntityTypeAndEntityIdOrderByCreatedAtAsc(
                EntityType.USER,
                entityId
            )
            .size();
        if (count >= 1) {
            log.info(
                "User already has an image, will upsert: entityId={}",
                entityId
            );
        }
    }

    private void checkProductQuota(UUID entityId) {
        long count =
            uploadSessionRepository.countByEntityTypeAndEntityIdAndStatus(
                EntityType.PRODUCT,
                entityId,
                UploadStatus.COMPLETED
            );
        if (count >= maxImagesPerProduct) {
            throw new BusinessException(
                "Product image limit reached: " + maxImagesPerProduct
            );
        }
    }

    private String buildStoragePath(
        EntityType entityType,
        UUID entityId,
        String fileName
    ) {
        String prefix = entityType == EntityType.USER ? "users" : "products";
        String safeFileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return prefix + "/" + entityId + "/" + safeFileName;
    }

    private String extractFileName(String storagePath) {
        int lastSlash = storagePath.lastIndexOf('/');
        return lastSlash >= 0
            ? storagePath.substring(lastSlash + 1)
            : storagePath;
    }
}
