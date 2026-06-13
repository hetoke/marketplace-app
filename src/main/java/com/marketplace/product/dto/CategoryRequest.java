package com.marketplace.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CategoryRequest(
        @NotBlank @Size(min = 1, max = 255) String name,
        @Size(max = 2000) String description,
        @NotBlank @Size(min = 1, max = 255) String slug,
        UUID parentId
) {}
