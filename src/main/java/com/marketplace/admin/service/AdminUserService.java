package com.marketplace.admin.service;

import com.marketplace.admin.dto.AdminUserResponse;
import com.marketplace.admin.dto.UserStatusUpdateRequest;
import com.marketplace.admin.model.AdminActionLog;
import com.marketplace.admin.model.AdminActionLog.Action;
import com.marketplace.admin.repository.AdminActionLogRepository;
import com.marketplace.shared.dto.PageResponse;
import com.marketplace.shared.exception.ResourceNotFoundException;
import com.marketplace.user.model.User;
import com.marketplace.user.model.User.UserStatus;
import com.marketplace.user.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final AdminActionLogRepository actionLogRepository;

    public AdminUserService(UserRepository userRepository, AdminActionLogRepository actionLogRepository) {
        this.userRepository = userRepository;
        this.actionLogRepository = actionLogRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> getUsers(int page, int size, String role, String status, String search) {
        User.Role roleEnum = null;
        UserStatus statusEnum = null;

        if (role != null && !role.isBlank()) {
            roleEnum = User.Role.valueOf(role.toUpperCase());
        }
        if (status != null && !status.isBlank()) {
            statusEnum = UserStatus.valueOf(status.toUpperCase());
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> users = userRepository.searchUsers(search, roleEnum, statusEnum, pageRequest);

        return new PageResponse<>(
                users.getContent().stream().map(AdminUserResponse::from).toList(),
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages()
        );
    }

    @Transactional
    public AdminUserResponse updateUserStatus(UUID userId, UserStatus newStatus, UUID adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        UserStatus oldStatus = user.getStatus();
        user.setStatus(newStatus);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        String details = String.format("{\"oldStatus\":\"%s\",\"newStatus\":\"%s\"}", oldStatus, newStatus);
        AdminActionLog logEntry = new AdminActionLog(adminId, Action.USER_STATUS_CHANGE, "USER", userId, details);
        actionLogRepository.save(logEntry);

        return AdminUserResponse.from(user);
    }
}
