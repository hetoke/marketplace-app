package com.marketplace.image.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.supabase")
public record SupabaseStorageProperties(
        String projectUrl,
        String serviceRoleKey,
        String bucketName
) {}
