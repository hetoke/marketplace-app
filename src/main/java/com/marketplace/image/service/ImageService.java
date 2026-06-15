package com.marketplace.image.service;

import com.marketplace.image.dto.ImageRequest;
import com.marketplace.image.dto.ImageResponse;
import com.marketplace.image.model.EntityType;
import com.marketplace.image.model.Image;
import com.marketplace.image.repository.ImageRepository;
import com.marketplace.shared.exception.AccessDeniedException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImageService {

    private final ImageRepository imageRepository;

    public ImageService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    @Transactional
    public ImageResponse saveImage(String uploadedById, ImageRequest request) {
        Image image = new Image();
        image.setFileUrl(request.fileUrl());
        image.setFileName(request.fileName());
        image.setFileSize(request.fileSize());
        image.setContentType(request.contentType());
        image.setEntityType(request.entityType());
        image.setEntityId(request.entityId());
        image.setUploadedBy(UUID.fromString(uploadedById));
        imageRepository.save(image);
        return ImageResponse.from(image);
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
        imageRepository.delete(image);
    }

    @Transactional
    public void deleteImagesByEntity(EntityType entityType, UUID entityId) {
        imageRepository.deleteByEntityTypeAndEntityId(entityType, entityId);
    }
}
