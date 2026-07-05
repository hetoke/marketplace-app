package com.marketplace.notification.dto;

import java.util.Map;

public record NotificationSettingsRequest(
    Map<String, Boolean> emailEnabled,
    Map<String, Boolean> inAppEnabled
) {}
