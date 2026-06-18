package com.marketplace.upload.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductImageUploadRequest(
    @NotBlank String fileName,
    Long fileSize,
    String contentType
) {}
