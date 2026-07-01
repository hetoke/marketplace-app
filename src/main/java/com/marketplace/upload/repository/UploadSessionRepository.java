package com.marketplace.upload.repository;

import com.marketplace.upload.model.EntityType;
import com.marketplace.upload.model.UploadSession;
import com.marketplace.upload.model.UploadStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadSessionRepository
    extends JpaRepository<UploadSession, UUID>
{
    Optional<UploadSession> findFirstByStoragePathAndStatusOrderByCreatedAtDesc(
        String storagePath,
        UploadStatus status
    );

    long countByEntityTypeAndEntityIdAndStatus(
        EntityType entityType,
        UUID entityId,
        UploadStatus status
    );
}
