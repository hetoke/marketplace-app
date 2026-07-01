package com.marketplace.upload.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.supabase")
public record SupabaseStorageProperties(
        String projectUrl,
        String publicUrl,
        String serviceRoleKey,
        String bucketName
) {}
