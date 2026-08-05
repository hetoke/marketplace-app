package com.marketplace.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketplace.upload.repository.ImageRepository;
import com.marketplace.upload.service.ImageService;
import com.marketplace.product.dto.ProductRequest;
import com.marketplace.product.dto.ProductResponse;
import com.marketplace.product.dto.ProductSearchRequest;
import com.marketplace.product.model.Category;
import com.marketplace.product.model.Product;
import com.marketplace.product.model.ProductImage;
import com.marketplace.product.repository.CategoryRepository;
import com.marketplace.product.repository.ProductImageRepository;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.shared.dto.PageResponse;
import com.marketplace.shared.exception.AccessDeniedException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private ProductImageRepository productImageRepository;

	@Mock
	private ImageRepository imageRepository;

	@Mock
	private ImageService imageService;

	@Mock
	private com.marketplace.product.service.DiscountService discountService;

	@Mock
	private com.marketplace.user.repository.UserRepository userRepository;

	@InjectMocks
	private ProductService productService;

	private Category createTestCategory() {
		Category category = new Category();
		category.setId(UUID.randomUUID());
		category.setName("Electronics");
		category.setSlug("electronics");
		category.setActive(true);
		category.setCreatedAt(Instant.now());
		category.setUpdatedAt(Instant.now());
		return category;
	}

	private Product createTestProduct(Category category) {
		Product product = new Product();
		product.setId(UUID.randomUUID());
		product.setSellerId(UUID.randomUUID());
		product.setCategory(category);
		product.setName("Test Product");
		product.setSlug("test-product");
		product.setDescription("A test product");
		product.setPrice(new BigDecimal("29.99"));
		product.setStock(10);
		product.setActive(true);
		product.setCreatedAt(Instant.now());
		product.setUpdatedAt(Instant.now());
		return product;
	}

	private ProductRequest createProductRequest(Category category) {
		return new ProductRequest(
				category.getId(),
				"Test Product",
				"A test product",
				new BigDecimal("29.99"),
				10,
				null,
				null,
				null,
				null,
				null);
	}

	// ==================== CREATE PRODUCT ====================

	@Test
	void createProduct_success() {
		Category category = createTestCategory();
		ProductRequest request = createProductRequest(category);
		Product product = createTestProduct(category);

		when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
		when(productRepository.findBySlug("test-product")).thenReturn(Optional.empty());
		when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
			Product p = inv.getArgument(0);
			p.setId(product.getId());
			return p;
		});

		ProductResponse response = productService.createProduct(
				product.getSellerId().toString(), request);

		assertThat(response.name()).isEqualTo("Test Product");
		assertThat(response.slug()).isEqualTo("test-product");
		assertThat(response.price()).isEqualByComparingTo(new BigDecimal("29.99"));
		assertThat(response.stock()).isEqualTo(10);
		verify(productRepository).save(any(Product.class));
	}

	@Test
	void createProduct_success_appendsUuidOnSlugCollision() {
		Category category = createTestCategory();
		ProductRequest request = createProductRequest(category);
		Product product = createTestProduct(category);

		when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
		when(productRepository.findBySlug("test-product")).thenReturn(Optional.of(new Product()));
		when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
			Product p = inv.getArgument(0);
			p.setId(product.getId());
			return p;
		});

		ProductResponse response = productService.createProduct(
				product.getSellerId().toString(), request);

		assertThat(response.slug()).startsWith("test-product-");
		assertThat(response.slug()).hasSize("test-product-".length() + 8);
		verify(productRepository).save(any(Product.class));
	}

	@Test
	void createProduct_categoryNotFound_throwsResourceNotFoundException() {
		Category category = createTestCategory();
		ProductRequest request = createProductRequest(category);
		UUID sellerId = UUID.randomUUID();

		when(categoryRepository.findById(category.getId())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.createProduct(sellerId.toString(), request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Category")
				.hasMessageContaining(category.getId().toString());
	}

	// ==================== UPDATE PRODUCT ====================

	@Test
	void updateProduct_success() {
		Category category = createTestCategory();
		Product product = createTestProduct(category);
		ProductRequest request = new ProductRequest(
				category.getId(), "Updated Product", "Updated description",
				new BigDecimal("49.99"), 20, null, null, null, null, null);

		when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
		when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
		when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

		ProductResponse response = productService.updateProduct(
				product.getSellerId().toString(), product.getId(), request);

		assertThat(response.name()).isEqualTo("Updated Product");
		assertThat(response.description()).isEqualTo("Updated description");
		assertThat(response.price()).isEqualByComparingTo(new BigDecimal("49.99"));
		assertThat(response.stock()).isEqualTo(20);
		verify(productRepository).save(product);
	}

	@Test
	void updateProduct_productNotFound_throwsResourceNotFoundException() {
		UUID productId = UUID.randomUUID();
		UUID sellerId = UUID.randomUUID();
		Category category = createTestCategory();
		ProductRequest request = createProductRequest(category);

		when(productRepository.findById(productId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.updateProduct(
				sellerId.toString(), productId, request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Product")
				.hasMessageContaining(productId.toString());
	}

	@Test
	void updateProduct_sellerMismatch_throwsAccessDeniedException() {
		Category category = createTestCategory();
		Product product = createTestProduct(category);
		UUID otherSellerId = UUID.randomUUID();
		ProductRequest request = createProductRequest(category);

		when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

		assertThatThrownBy(() -> productService.updateProduct(
				otherSellerId.toString(), product.getId(), request))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessage("You can only update your own products");
	}

	@Test
	void updateProduct_categoryNotFound_throwsResourceNotFoundException() {
		Category category = createTestCategory();
		Product product = createTestProduct(category);
		ProductRequest request = createProductRequest(category);

		when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
		when(categoryRepository.findById(category.getId())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.updateProduct(
				product.getSellerId().toString(), product.getId(), request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Category");
	}

	// ==================== DELETE PRODUCT ====================

	@Test
	void deleteProduct_success() {
		Category category = createTestCategory();
		Product product = createTestProduct(category);

		when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

		productService.deleteProduct(product.getSellerId().toString(), product.getId());

		verify(productRepository).delete(product);
	}

	@Test
	void deleteProduct_productNotFound_throwsResourceNotFoundException() {
		UUID productId = UUID.randomUUID();
		UUID sellerId = UUID.randomUUID();

		when(productRepository.findById(productId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.deleteProduct(sellerId.toString(), productId))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Product")
				.hasMessageContaining(productId.toString());
	}

	@Test
	void deleteProduct_sellerMismatch_throwsAccessDeniedException() {
		Category category = createTestCategory();
		Product product = createTestProduct(category);
		UUID otherSellerId = UUID.randomUUID();

		when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

		assertThatThrownBy(() -> productService.deleteProduct(
				otherSellerId.toString(), product.getId()))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessage("You can only delete your own products");
	}

	// ==================== GET PRODUCT BY ID ====================

	@Test
	void getProductById_success() {
		Category category = createTestCategory();
		Product product = createTestProduct(category);

		when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
		when(productImageRepository.findByProductIdOrderBySortOrderAsc(product.getId()))
				.thenReturn(List.of());

		ProductResponse response = productService.getProductById(product.getId());

		assertThat(response.id()).isEqualTo(product.getId().toString());
		assertThat(response.name()).isEqualTo("Test Product");
		assertThat(response.images()).isEmpty();
	}

	@Test
	void getProductById_withImages() {
		Category category = createTestCategory();
		Product product = createTestProduct(category);
		ProductImage image = new ProductImage();
		image.setId(UUID.randomUUID());
		image.setProduct(product);
		image.setUrl("https://example.com/image.jpg");
		image.setAltText("Test image");
		image.setSortOrder(0);
		image.setPrimary(true);

		when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
		when(productImageRepository.findByProductIdOrderBySortOrderAsc(product.getId()))
				.thenReturn(List.of(image));

		ProductResponse response = productService.getProductById(product.getId());

		assertThat(response.images()).hasSize(1);
		assertThat(response.images().get(0).url()).isEqualTo("https://example.com/image.jpg");
	}

	@Test
	void getProductById_productNotFound_throwsResourceNotFoundException() {
		UUID productId = UUID.randomUUID();

		when(productRepository.findById(productId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.getProductById(productId))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Product")
				.hasMessageContaining(productId.toString());
	}

	// ==================== SEARCH PRODUCTS ====================

	@Test
	void searchProducts_success() {
		Category category = createTestCategory();
		Product product = createTestProduct(category);
		Page<Product> page = new PageImpl<>(List.of(product));
		ProductSearchRequest request = new ProductSearchRequest(
				"test", null, null, null, null, 0, 20, "createdAt");

		when(productRepository.search("test", null, null, null, null,
				PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(
						org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))))
				.thenReturn(page);
		when(productRepository.countBySearchFilters("test", null, null, null, null))
				.thenReturn(1L);

		PageResponse<ProductResponse> response = productService.searchProductsPaged(request);

		assertThat(response.content()).hasSize(1);
		assertThat(response.content().get(0).name()).isEqualTo("Test Product");
		assertThat(response.totalElements()).isEqualTo(1);
	}

	@Test
	void searchProducts_emptyResults() {
		Page<Product> page = new PageImpl<>(List.of());
		ProductSearchRequest request = new ProductSearchRequest(
				"nonexistent", null, null, null, null, 0, 20, "createdAt");

		when(productRepository.search("nonexistent", null, null, null, null,
				PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(
						org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))))
				.thenReturn(page);
		when(productRepository.countBySearchFilters("nonexistent", null, null, null, null))
				.thenReturn(0L);

		PageResponse<ProductResponse> response = productService.searchProductsPaged(request);

		assertThat(response.content()).isEmpty();
		assertThat(response.totalElements()).isEqualTo(0);
	}

	@Test
	void searchProducts_defaultPagination() {
		Category category = createTestCategory();
		List<Product> products = new java.util.ArrayList<>();
		for (int i = 0; i < 25; i++) {
			Product p = createTestProduct(category);
			p.setName("Product " + i);
			p.setSlug("product-" + i);
			products.add(p);
		}
		Page<Product> page = new PageImpl<>(products.subList(0, 20),
				PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(
						org.springframework.data.domain.Sort.Direction.DESC, "createdAt")),
				25);
		ProductSearchRequest request = new ProductSearchRequest(null, null, null, null, null, null, null, null);

		when(productRepository.search(null, null, null, null, null,
				PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(
						org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))))
				.thenReturn(page);
		when(productRepository.countBySearchFilters(null, null, null, null, null))
				.thenReturn(25L);

		PageResponse<ProductResponse> response = productService.searchProductsPaged(request);

		assertThat(response.page()).isEqualTo(0);
		assertThat(response.size()).isEqualTo(20);
		assertThat(response.totalElements()).isEqualTo(25);
		assertThat(response.totalPages()).isEqualTo(2);
		assertThat(response.content()).hasSize(20);
	}

	@Test
	void searchProducts_customPagination() {
		Category category = createTestCategory();
		Product product = createTestProduct(category);
		Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(1, 5), 10);
		ProductSearchRequest request = new ProductSearchRequest(
				null, null, null, new BigDecimal("10"), new BigDecimal("100"), 1, 5, "price");

		when(productRepository.search(null, null, null, new BigDecimal("10"), new BigDecimal("100"),
				PageRequest.of(1, 5, org.springframework.data.domain.Sort.by(
						org.springframework.data.domain.Sort.Direction.DESC, "price"))))
				.thenReturn(page);
		when(productRepository.countBySearchFilters(null, null, null, new BigDecimal("10"), new BigDecimal("100")))
				.thenReturn(10L);

		PageResponse<ProductResponse> response = productService.searchProductsPaged(request);

		assertThat(response.page()).isEqualTo(1);
		assertThat(response.size()).isEqualTo(5);
		assertThat(response.totalElements()).isEqualTo(10);
		assertThat(response.totalPages()).isEqualTo(2);
	}

	// ==================== GET PRODUCTS BY CATEGORY ====================

	@Test
	void getProductsByCategory_success() {
		Category category = createTestCategory();
		Product product = createTestProduct(category);
		Page<Product> page = new PageImpl<>(List.of(product));

		when(productRepository.findByCategoryIdAndActiveTrue(
				eq(category.getId()), any(PageRequest.class)))
				.thenReturn(page);

		PageResponse<ProductResponse> response = productService.getProductsByCategory(
				category.getId(), 0, 20);

		assertThat(response.content()).hasSize(1);
		assertThat(response.totalElements()).isEqualTo(1);
	}

	// ==================== GET SELLER PRODUCTS ====================

	@Test
	void getSellerProducts_success() {
		Category category = createTestCategory();
		Product product = createTestProduct(category);
		Page<Product> page = new PageImpl<>(List.of(product));

		when(productRepository.findBySellerId(
				eq(product.getSellerId()), any(PageRequest.class)))
				.thenReturn(page);

		PageResponse<ProductResponse> response = productService.getSellerProducts(
				product.getSellerId().toString(), 0, 20);

		assertThat(response.content()).hasSize(1);
		assertThat(response.totalElements()).isEqualTo(1);
	}

	// ==================== GENERATE SLUG (tested via createProduct) ====================

	@Test
	void createProduct_nameWithSpecialChars_generatesCleanSlug() {
		Category category = createTestCategory();
		ProductRequest request = new ProductRequest(
				category.getId(), "Hello! @World# $100%", "desc",
				new BigDecimal("9.99"), 1, null, null, null, null, null);
		Product product = createTestProduct(category);

		when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
		when(productRepository.findBySlug("hello-world-100")).thenReturn(Optional.empty());
		when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
			Product p = inv.getArgument(0);
			p.setId(product.getId());
			return p;
		});

		ProductResponse response = productService.createProduct(
				product.getSellerId().toString(), request);

		assertThat(response.slug()).isEqualTo("hello-world-100");
	}

	@Test
	void createProduct_nameWithSpaces_generatesHyphens() {
		Category category = createTestCategory();
		ProductRequest request = new ProductRequest(
				category.getId(), "My Test Product", "desc",
				new BigDecimal("9.99"), 1, null, null, null, null, null);
		Product product = createTestProduct(category);

		when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
		when(productRepository.findBySlug("my-test-product")).thenReturn(Optional.empty());
		when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
			Product p = inv.getArgument(0);
			p.setId(product.getId());
			return p;
		});

		ProductResponse response = productService.createProduct(
				product.getSellerId().toString(), request);

		assertThat(response.slug()).isEqualTo("my-test-product");
	}

	@Test
	void createProduct_nameWithConsecutiveHyphens_collapsesHyphens() {
		Category category = createTestCategory();
		ProductRequest request = new ProductRequest(
				category.getId(), "Hello   World", "desc",
				new BigDecimal("9.99"), 1, null, null, null, null, null);
		Product product = createTestProduct(category);

		when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
		when(productRepository.findBySlug("hello-world")).thenReturn(Optional.empty());
		when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
			Product p = inv.getArgument(0);
			p.setId(product.getId());
			return p;
		});

		ProductResponse response = productService.createProduct(
				product.getSellerId().toString(), request);

		assertThat(response.slug()).isEqualTo("hello-world");
	}

}
