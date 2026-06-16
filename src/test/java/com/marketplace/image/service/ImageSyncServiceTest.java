package com.marketplace.image.service;

// NOTICE: These tests use WireMock to stub the Supabase Storage API.
// Real integration tests against a Supabase test bucket (marketplace-images-test)
// will be added in a future sprint. Do NOT use the production bucket for tests.

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.marketplace.image.model.EntityType;
import com.marketplace.image.model.Image;
import com.marketplace.image.repository.ImageRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
class ImageSyncServiceTest {

	private static WireMockServer wireMockServer;

	@Autowired
	private ImageRepository imageRepository;

	@Autowired
	private ImageSyncService imageSyncService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private static final UUID USER_ID = UUID.randomUUID();
	private static final String BUCKET = "marketplace-images";
	private static final String LIST_URL = "/storage/v1/object/list/" + BUCKET;

	@BeforeAll
	static void startWireMock() {
		wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig()
				.dynamicPort());
		wireMockServer.start();
		WireMock.configureFor("localhost", wireMockServer.port());
	}

	@AfterAll
	static void stopWireMock() {
		wireMockServer.stop();
	}

	@DynamicPropertySource
	static void overrideProperties(DynamicPropertyRegistry registry) {
		registry.add("app.supabase.project-url",
				() -> "http://localhost:" + wireMockServer.port());
		registry.add("app.supabase.service-role-key",
				() -> "test-service-role-key");
		registry.add("app.supabase.bucket-name",
				() -> BUCKET);
	}

	@BeforeEach
	void setUp() {
		imageRepository.deleteAll();
		wireMockServer.resetAll();
	}

	// ==================== DELETE DB ORPHANS ====================

	@Test
	void deleteDbOrphans_storageHasFile_keepsImage() {
		Image image = saveImage("marketplace-images/user1/photo.jpg");

		stubListFilesForPrefix("user1/", List.of("photo.jpg"));

		int deleted = imageSyncService.deleteDbOrphans();

		assertThat(deleted).isZero();
		assertThat(imageRepository.findById(image.getId())).isPresent();
	}

	@Test
	void deleteDbOrphans_storageMissingFile_deletesImage() {
		Image orphan = saveImage("marketplace-images/user1/deleted.jpg");

		stubListFilesForPrefix("user1/", List.of());

		int deleted = imageSyncService.deleteDbOrphans();

		assertThat(deleted).isEqualTo(1);
		assertThat(imageRepository.findById(orphan.getId())).isEmpty();
	}

	@Test
	void deleteDbOrphans_mixedKeepsAndDeletes() {
		Image kept = saveImage("marketplace-images/user1/existing.jpg");
		Image deleted1 = saveImage("marketplace-images/user1/old1.jpg");
		Image deleted2 = saveImage("marketplace-images/user1/old2.jpg");

		stubListFilesForPrefix("user1/", List.of("existing.jpg"));

		int deletedCount = imageSyncService.deleteDbOrphans();

		assertThat(deletedCount).isEqualTo(2);
		assertThat(imageRepository.findById(kept.getId())).isPresent();
		assertThat(imageRepository.findById(deleted1.getId())).isEmpty();
		assertThat(imageRepository.findById(deleted2.getId())).isEmpty();
	}

	@Test
	void deleteDbOrphans_emptyDb_returnsZero() {
		int deleted = imageSyncService.deleteDbOrphans();

		assertThat(deleted).isZero();
	}

	@Test
	void deleteDbOrphans_multiplePrefixes_onlyFirstPrefixQueried() {
		// Known behavior: deleteDbOrphans only queries storage for the FIRST image's prefix per page.
		// Images with different prefixes on the same page are treated as orphans.
		Image userImg = saveImage("marketplace-images/user1/a.jpg");
		Image productImg = saveImage("marketplace-images/product1/b.jpg");

		stubListFilesForPrefix("user1/", List.of("a.jpg"));

		int deleted = imageSyncService.deleteDbOrphans();

		assertThat(deleted).isEqualTo(1);
		assertThat(imageRepository.findById(userImg.getId())).isPresent();
		assertThat(imageRepository.findById(productImg.getId())).isEmpty();

		verify(postRequestedFor(urlPathEqualTo(LIST_URL))
				.withRequestBody(containing("\"prefix\":\"user1/\"")));
	}

	// ==================== DELETE STORAGE ORPHANS ====================

	@Test
	void deleteStorageOrphans_dbHasFile_keepsFile() {
		saveImage("marketplace-images/user1/photo.jpg");

		stubListFilesForPrefix("", List.of("user1/photo.jpg"));
		stubDeleteFiles();

		int deleted = imageSyncService.deleteStorageOrphans();

		assertThat(deleted).isZero();
	}

	@Test
	void deleteStorageOrphans_dbMissingFile_deletesFile() {
		stubListFilesForPrefix("", List.of("orphan.png"));
		stubDeleteFiles();

		int deleted = imageSyncService.deleteStorageOrphans();

		assertThat(deleted).isEqualTo(1);
	}

	@Test
	void deleteStorageOrphans_emptyStorage_returnsZero() {
		stubListFilesForPrefix("", List.of());

		int deleted = imageSyncService.deleteStorageOrphans();

		assertThat(deleted).isZero();
	}

	@Test
	void deleteStorageOrphans_validFileInDb_keepsAll() {
		saveImage("marketplace-images/valid.jpg");
		stubListFilesForPrefix("", List.of("valid.jpg"));
		stubDeleteFiles();

		int deleted = imageSyncService.deleteStorageOrphans();

		assertThat(deleted).isZero();
	}

	// ==================== SYNC ALL ====================

	@Test
	void syncAll_executesBothPhases() {
		saveImage("marketplace-images/user1/photo.jpg");
		stubListFilesForPrefix("user1/", List.of("photo.jpg"));
		stubListFilesForPrefix("", List.of("user1/photo.jpg"));
		stubDeleteFiles();

		imageSyncService.syncAll();

		verify(postRequestedFor(urlPathEqualTo(LIST_URL))
				.withRequestBody(containing("\"prefix\":\"user1/\"")));
		verify(postRequestedFor(urlPathEqualTo(LIST_URL))
				.withRequestBody(containing("\"prefix\":\"\"")));
	}

	// ==================== HELPERS ====================

	private Image saveImage(String storagePath) {
		Image image = new Image();
		image.setFileUrl("https://vuaqyfpnajahttjzfvds.supabase.co/storage/v1/object/public/" + storagePath);
		image.setFileName(storagePath.substring(storagePath.lastIndexOf('/') + 1));
		image.setFileSize(1024L);
		image.setContentType("image/jpeg");
		image.setEntityType(EntityType.USER);
		image.setEntityId(UUID.randomUUID());
		image.setUploadedBy(USER_ID);
		return imageRepository.save(image);
	}

	private void stubListFilesForPrefix(String prefix, List<String> fileNames) {
		try {
			String body = objectMapper.writeValueAsString(
					fileNames.stream().map(StorageFileStub::new).toList());
			stubFor(post(urlPathEqualTo(LIST_URL))
					.withHeader("Content-Type", equalTo("application/json"))
					.withRequestBody(containing("\"prefix\":\"" + prefix + "\""))
					.willReturn(aResponse()
							.withStatus(200)
							.withHeader("Content-Type", "application/json")
							.withBody(body)));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void stubDeleteFiles() {
		stubFor(com.github.tomakehurst.wiremock.client.WireMock
				.delete(com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo(
						"/storage/v1/object/" + BUCKET))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{}")));
	}

	private record StorageFileStub(String name) {}
}
