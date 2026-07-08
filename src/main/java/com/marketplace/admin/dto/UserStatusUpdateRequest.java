package com.marketplace.admin.dto;

import com.marketplace.user.model.User;

public record UserStatusUpdateRequest(User.UserStatus status) {}
