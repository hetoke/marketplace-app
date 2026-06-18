package com.marketplace.webhook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record StorageWebhookRequest(
		String type,
		@JsonProperty("record") StorageRecord record
) {
	public record StorageRecord(
			String id,
			String name,
			@JsonProperty("bucket_id") String bucketId,
			String mimetype,
			Map<String, Object> metadata
	) {}
}
