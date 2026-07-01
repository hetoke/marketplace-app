package com.marketplace.webhook.service;

import com.marketplace.upload.storage.SupabaseStorageProperties;
import com.marketplace.upload.service.UploadService;
import com.marketplace.webhook.dto.StorageWebhookRequest.StorageRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(
        WebhookService.class
    );

    private final UploadService uploadService;
    private final SupabaseStorageProperties storageProperties;

    public WebhookService(
        UploadService uploadService,
        SupabaseStorageProperties storageProperties
    ) {
        this.uploadService = uploadService;
        this.storageProperties = storageProperties;
    }

    @Transactional
    public void handleStorageInsert(StorageRecord record) {
        String storagePath = record.name();
        log.info("Storage path: {}", storagePath);
        if (storagePath == null || storagePath.isEmpty()) {
            log.warn("Webhook received with empty storage path");
            return;
        }

        uploadService.completeUploadFromWebhook(storagePath, record);
    }
}
