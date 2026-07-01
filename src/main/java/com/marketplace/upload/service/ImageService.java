package com.marketplace.upload.service;

import com.marketplace.upload.dto.ImageResponse;
import com.marketplace.upload.model.EntityType;
import com.marketplace.upload.model.Image;
import com.marketplace.upload.repository.ImageRepository;
import com.marketplace.upload.storage.SupabaseStorageClient;
import com.marketplace.shared.exception.AccessDeniedException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    private final ImageRepository imageRepository;
    private final SupabaseStorageClient storageClient;

    public ImageService(ImageRepository imageRepository,
                        SupabaseStorageClient storageClient) {
        this.imageRepository = imageRepository;
        this.storageClient = storageClient;
    }

    @Transactional(readOnly = true)
    public List<ImageResponse> getImages(EntityType entityType, UUID entityId) {
        return imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(entityType, entityId)
                .stream()
                .map(ImageResponse::from)
                .toList();
    }

    @Transactional
    public void deleteImage(String userId, UUID imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image", "id", imageId));
        if (!image.getUploadedBy().toString().equals(userId)) {
            throw new AccessDeniedException("You can only delete your own images");
        }
        deleteFromStorage(image);
        imageRepository.delete(image);
    }

    @Transactional
    public void deleteById(UUID imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image", "id", imageId));
        deleteFromStorage(image);
        imageRepository.delete(image);
    }

    @Transactional
    public void deleteImagesByEntity(EntityType entityType, UUID entityId) {
        List<Image> images = imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(entityType, entityId);
        List<String> paths = images.stream()
                .map(this::extractStoragePath)
                .filter(path -> path != null)
                .toList();
        if (!paths.isEmpty()) {
            storageClient.deleteFiles(paths);
        }
        imageRepository.deleteByEntityTypeAndEntityId(entityType, entityId);
    }

    private void deleteFromStorage(Image image) {
        String path = extractStoragePath(image);
        if (path != null) {
            try {
                storageClient.deleteFiles(List.of(path));
            } catch (Exception e) {
                log.warn("Failed to delete file from storage: {}", path, e);
            }
        }
    }

    String extractStoragePath(Image image) {
        try {
            String url = image.getFileUrl();
            URI uri = URI.create(url);
            String fullPath = uri.getPath();
            String marker = "/object/public/";
            int idx = fullPath.indexOf(marker);
            if (idx >= 0) {
                return fullPath.substring(idx + marker.length());
            }
            return fullPath.startsWith("/") ? fullPath.substring(1) : fullPath;
        } catch (Exception e) {
            log.warn("Failed to extract storage path from URL: {}", image.getFileUrl(), e);
            return null;
        }
    }
}
