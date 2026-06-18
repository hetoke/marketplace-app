package com.marketplace.upload.dto;

import java.time.Instant;
import java.util.UUID;

public record UploadResponse(
    UUID uploadId,
    String uploadUrl,
    String token,
    Instant expiresAt
) {}
