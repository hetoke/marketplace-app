package com.marketplace.image.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(SupabaseStorageProperties.class)
public class SupabaseStorageClient {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageClient.class);

    private final HttpClient httpClient;
    private final SupabaseStorageProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SupabaseStorageClient(SupabaseStorageProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newHttpClient();
    }

    public List<StorageFile> listFiles(String prefix, int limit, int offset) {
        try {
            String url = properties.projectUrl() + "/storage/v1/object/list/" + properties.bucketName();
            String body = objectMapper.writeValueAsString(new ListFilesRequest(prefix, limit, offset,
                    new SortBy("name", "asc")));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("apikey", properties.serviceRoleKey())
                    .header("Authorization", "Bearer " + properties.serviceRoleKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to list files from Supabase storage", e);
            return List.of();
        }
    }

    public void deleteFiles(List<String> paths) {
        if (paths.isEmpty()) {
            return;
        }

        try {
            String pathsParam = String.join(",", paths);
            String url = properties.projectUrl() + "/storage/v1/object/" + properties.bucketName() + "/" + pathsParam;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("apikey", properties.serviceRoleKey())
                    .header("Authorization", "Bearer " + properties.serviceRoleKey())
                    .DELETE()
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("Deleted {} files from Supabase storage", paths.size());
        } catch (Exception e) {
            log.error("Failed to delete files from Supabase storage", e);
        }
    }

    public record StorageFile(String name, String id, String updated_at, Object metadata) {}

    private record ListFilesRequest(String prefix, int limit, int offset, SortBy sortBy) {}

    private record SortBy(String column, String order) {}
}
