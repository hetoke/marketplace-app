package com.marketplace.image.repository;

import com.marketplace.image.model.EntityType;
import com.marketplace.image.model.Image;
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

    Optional<Image> findByFileUrlContaining(String fileName);
}
