package com.marketplace.image.scheduler;

import com.marketplace.image.service.ImageSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ImageSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(ImageSyncScheduler.class);

    private final ImageSyncService imageSyncService;

    @Value("${app.image-sync.enabled:true}")
    private boolean syncEnabled;

    public ImageSyncScheduler(ImageSyncService imageSyncService) {
        this.imageSyncService = imageSyncService;
    }

    @Scheduled(fixedDelayString = "${app.image-sync.fixed-delay:3600000}")
    public void syncImages() {
        if (!syncEnabled) {
            return;
        }

        try {
            imageSyncService.syncAll();
        } catch (Exception e) {
            log.error("Image sync failed", e);
        }
    }
}
