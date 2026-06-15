package com.marketplace.image.service;

import com.marketplace.image.model.Image;
import com.marketplace.image.repository.ImageRepository;
import com.marketplace.image.storage.SupabaseStorageClient;
import com.marketplace.image.storage.SupabaseStorageClient.StorageFile;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImageSyncService {

    private static final Logger log = LoggerFactory.getLogger(ImageSyncService.class);
    private static final int BATCH_SIZE = 100;

    private final ImageRepository imageRepository;
    private final SupabaseStorageClient storageClient;

    public ImageSyncService(ImageRepository imageRepository, SupabaseStorageClient storageClient) {
        this.imageRepository = imageRepository;
        this.storageClient = storageClient;
    }

    @Transactional
    public void syncAll() {
        log.info("Starting image sync...");
        int dbOrphans = deleteDbOrphans();
        int storageOrphans = deleteStorageOrphans();
        log.info("Image sync completed: {} DB orphans removed, {} storage orphans removed",
                dbOrphans, storageOrphans);
    }

    @Transactional
    public int deleteDbOrphans() {
        int totalDeleted = 0;
        int page = 0;

        while (true) {
            Page<Image> imagePage = imageRepository.findAllByOrderByCreatedAtAsc(
                    PageRequest.of(page, BATCH_SIZE));

            if (imagePage.isEmpty()) {
                break;
            }

            List<String> paths = imagePage.getContent().stream()
                    .map(this::extractStoragePath)
                    .filter(path -> path != null)
                    .toList();

            if (!paths.isEmpty()) {
                List<StorageFile> storageFiles = storageClient.listFiles(
                        paths.get(0).substring(0, paths.get(0).lastIndexOf('/') + 1),
                        BATCH_SIZE, 0);

                Set<String> storagePaths = storageFiles.stream()
                        .map(f -> paths.get(0).substring(0, paths.get(0).lastIndexOf('/') + 1) + f.name())
                        .collect(Collectors.toSet());

                List<Image> orphans = imagePage.getContent().stream()
                        .filter(img -> {
                            String path = extractStoragePath(img);
                            return path != null && !storagePaths.contains(path);
                        })
                        .toList();

                for (Image orphan : orphans) {
                    imageRepository.delete(orphan);
                    totalDeleted++;
                }
            }

            if (imagePage.isLast()) {
                break;
            }
            page++;
        }

        if (totalDeleted > 0) {
            log.info("Deleted {} DB orphan records", totalDeleted);
        }
        return totalDeleted;
    }

    @Transactional
    public int deleteStorageOrphans() {
        int totalDeleted = 0;
        int offset = 0;

        while (true) {
            List<StorageFile> files = storageClient.listFiles("", BATCH_SIZE, offset);

            if (files.isEmpty()) {
                break;
            }

            for (StorageFile file : files) {
                if (file.name() == null || file.name().isEmpty()) {
                    continue;
                }

                boolean existsInDb = imageRepository.findByFileUrlContaining(file.name()).isPresent();

                if (!existsInDb) {
                    try {
                        storageClient.deleteFiles(List.of(file.name()));
                        totalDeleted++;
                    } catch (Exception e) {
                        log.warn("Failed to delete orphan file from storage: {}", file.name(), e);
                    }
                }
            }

            if (files.size() < BATCH_SIZE) {
                break;
            }
            offset += BATCH_SIZE;
        }

        if (totalDeleted > 0) {
            log.info("Deleted {} storage orphan files", totalDeleted);
        }
        return totalDeleted;
    }

    private String extractStoragePath(Image image) {
        try {
            String url = image.getFileUrl();
            URI uri = URI.create(url);
            String fullPath = uri.getPath();
            String marker = "/object/public/";
            int idx = fullPath.indexOf(marker);
            if (idx >= 0) {
                String path = fullPath.substring(idx + marker.length());
                int bucketSeparator = path.indexOf('/');
                if (bucketSeparator >= 0) {
                    return path.substring(bucketSeparator + 1);
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to extract storage path from URL: {}", image.getFileUrl(), e);
            return null;
        }
    }
}
