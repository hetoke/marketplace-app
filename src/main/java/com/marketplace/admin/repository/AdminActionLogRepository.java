package com.marketplace.admin.repository;

import com.marketplace.admin.model.AdminActionLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, UUID> {

    List<AdminActionLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, UUID targetId);

    List<AdminActionLog> findByAdminIdOrderByCreatedAtDesc(UUID adminId);
}
