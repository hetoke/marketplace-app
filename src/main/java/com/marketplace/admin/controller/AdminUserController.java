package com.marketplace.admin.controller;

import com.marketplace.admin.dto.AdminUserResponse;
import com.marketplace.admin.dto.UserStatusUpdateRequest;
import com.marketplace.admin.service.AdminUserService;
import com.marketplace.shared.dto.ApiResponse;
import com.marketplace.shared.dto.PageResponse;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        PageResponse<AdminUserResponse> users = adminUserService.getUsers(page, size, role, status, search);
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserStatus(
            @PathVariable UUID userId,
            @RequestBody UserStatusUpdateRequest request,
            Authentication authentication) {
        UUID adminId = UUID.fromString(authentication.getName());
        AdminUserResponse user = adminUserService.updateUserStatus(userId, request.status(), adminId);
        return ResponseEntity.ok(ApiResponse.ok("User status updated", user));
    }
}
