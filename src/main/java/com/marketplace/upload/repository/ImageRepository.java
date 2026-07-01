package com.marketplace.upload.repository;

import com.marketplace.upload.model.EntityType;
import com.marketplace.upload.model.Image;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, UUID> {

    List<Image> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType entityType, UUID entityId);

    void deleteByEntityTypeAndEntityId(EntityType entityType, UUID entityId);

    Page<Image> findAllByOrderByCreatedAtAsc(Pageable pageable);

    Optional<Image> findByEntityTypeAndEntityIdAndUploadedByOrderByCreatedAtAsc(EntityType entityType, UUID entityId, UUID uploadedBy);

    Optional<Image> findByFileUrlContaining(String fileName);
}
