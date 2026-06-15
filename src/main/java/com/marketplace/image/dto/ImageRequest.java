package com.marketplace.image.dto;

import com.marketplace.image.model.EntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ImageRequest(
        @NotBlank @Size(max = 1024) String fileUrl,
        @Size(max = 255) String fileName,
        Long fileSize,
        @Size(max = 100) String contentType,
        @NotNull EntityType entityType,
        @NotNull UUID entityId
) {}
