package com.marketplace.image.repository;

import com.marketplace.image.model.EntityType;
import com.marketplace.image.model.Image;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, UUID> {

    List<Image> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType entityType, UUID entityId);

    void deleteByEntityTypeAndEntityId(EntityType entityType, UUID entityId);
}
