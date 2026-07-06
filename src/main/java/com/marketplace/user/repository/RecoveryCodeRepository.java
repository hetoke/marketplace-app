package com.marketplace.user.repository;

import com.marketplace.user.model.RecoveryCode;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, UUID> {

    List<RecoveryCode> findByUserIdAndUsedFalse(UUID userId);

    long countByUserIdAndUsedFalse(UUID userId);

    @Query("SELECT COUNT(r) FROM RecoveryCode r WHERE r.user.id = :userId")
    long countByUserId(UUID userId);
}
