package com.marketplace.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.product.dto.CategoryResponse;
import com.marketplace.product.dto.ProductResponse;
import com.marketplace.product.service.ProductService;
import com.marketplace.shared.dto.PageResponse;
import com.marketplace.shared.exception.AccessDeniedException;
import com.marketplace.shared.exception.GlobalExceptionHandler;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

	private MockMvc mockMvc;

	@Mock
	private ProductService productService;

	@InjectMocks
	private ProductController productController;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final String sellerId = "test-seller-id";

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(productController)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();

		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(sellerId, null, List.of()));
	}

	private ProductResponse createTestProductResponse() {
		return new ProductResponse(
				UUID.randomUUID().toString(),
				sellerId,
				"Test Seller",
				new CategoryResponse(UUID.randomUUID().toString(), "Electronics",
						"Electronic devices", "electronics", null, true, java.time.Instant.now()),
				"Test Product",
				"test-product",
				"A test product",
				new BigDecimal("29.99"),
				10,
				0.0,
				0,
				0,
				true,
				List.of(),
				java.time.Instant.now(),
				new BigDecimal("29.99"),
				new BigDecimal("29.99"),
				false,
				null,
				null,
				null);
	}

	private record ProductBody(String categoryId, String name, String description,
			BigDecimal price, Integer stock) {}

	// ==================== CREATE PRODUCT ====================

	@Test
	void createProduct_success_returns201() throws Exception {
		ProductResponse productResponse = createTestProductResponse();
		when(productService.createProduct(anyString(), any())).thenReturn(productResponse);

		mockMvc.perform(post("/api/v1/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new ProductBody(
								productResponse.category().id(),
								"Test Product",
								"A test product",
								new BigDecimal("29.99"),
								10))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("Product created"))
				.andExpect(jsonPath("$.data.name").value("Test Product"));
	}

	@Test
	void createProduct_blankName_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoryId\":\"" + UUID.randomUUID()
								+ "\",\"name\":\"\",\"price\":29.99,\"stock\":10}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
	}

	@Test
	void createProduct_nameBelowLowerBound0chars_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoryId\":\"" + UUID.randomUUID()
								+ "\",\"name\":\"\",\"price\":29.99,\"stock\":10}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createProduct_nameAboveUpperBound256chars_returns400() throws Exception {
		String name256 = "A".repeat(256);

		mockMvc.perform(post("/api/v1/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new ProductBody(
								UUID.randomUUID().toString(),
								name256,
								"desc",
								new BigDecimal("29.99"),
								10))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createProduct_nullCategoryId_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Test\",\"price\":29.99,\"stock\":10}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("categoryId"));
	}

	@Test
	void createProduct_nullPrice_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoryId\":\"" + UUID.randomUUID()
								+ "\",\"name\":\"Test\",\"stock\":10}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("price"));
	}

	@Test
	void createProduct_negativePrice_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoryId\":\"" + UUID.randomUUID()
								+ "\",\"name\":\"Test\",\"price\":-1,\"stock\":10}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("price"));
	}

	@Test
	void createProduct_zeroPrice_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoryId\":\"" + UUID.randomUUID()
								+ "\",\"name\":\"Test\",\"price\":0,\"stock\":10}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("price"));
	}

	@Test
	void createProduct_nullStock_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoryId\":\"" + UUID.randomUUID()
								+ "\",\"name\":\"Test\",\"price\":29.99}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("stock"));
	}

	@Test
	void createProduct_negativeStock_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoryId\":\"" + UUID.randomUUID()
								+ "\",\"name\":\"Test\",\"price\":29.99,\"stock\":-1}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("stock"));
	}

	@Test
	void createProduct_categoryNotFound_returns404() throws Exception {
		when(productService.createProduct(anyString(), any()))
				.thenThrow(new ResourceNotFoundException("Category", "id", "nonexistent"));

		mockMvc.perform(post("/api/v1/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoryId\":\"" + UUID.randomUUID()
								+ "\",\"name\":\"Test\",\"price\":29.99,\"stock\":10}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void createProduct_malformedJson_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{bad}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Malformed request body"));
	}

	@Test
	void createProduct_postMethodNotAllowed_returns405() throws Exception {
		mockMvc.perform(put("/api/v1/products"))
				.andExpect(status().isMethodNotAllowed());
	}

	// ==================== SEARCH PRODUCTS ====================

	@Test
	void searchProducts_success_returns200() throws Exception {
		ProductResponse productResponse = createTestProductResponse();
		PageResponse<ProductResponse> pageResponse = new PageResponse<>(
				List.of(productResponse), 0, 20, 1, 1);
		when(productService.searchProductsPaged(any())).thenReturn(pageResponse);

		mockMvc.perform(get("/api/v1/products"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content[0].name").value("Test Product"))
				.andExpect(jsonPath("$.data.totalElements").value(1));
	}

	@Test
	void searchProducts_withQueryParams_returns200() throws Exception {
		PageResponse<ProductResponse> pageResponse = new PageResponse<>(
				List.of(), 0, 10, 0, 0);
		when(productService.searchProductsPaged(any())).thenReturn(pageResponse);

		mockMvc.perform(get("/api/v1/products")
						.param("query", "phone")
						.param("minPrice", "10")
						.param("maxPrice", "100")
						.param("page", "0")
						.param("size", "10")
						.param("sortBy", "price"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(0));
	}

	@Test
	void searchProducts_putMethodNotAllowed_returns405() throws Exception {
		mockMvc.perform(put("/api/v1/products"))
				.andExpect(status().isMethodNotAllowed());
	}

	// ==================== GET PRODUCT BY ID ====================

	@Test
	void getProductById_success_returns200() throws Exception {
		ProductResponse productResponse = createTestProductResponse();
		when(productService.getProductById(any(UUID.class))).thenReturn(productResponse);

		mockMvc.perform(get("/api/v1/products/" + productResponse.id()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("Test Product"))
				.andExpect(jsonPath("$.data.price").value(29.99));
	}

	@Test
	void getProductById_notFound_returns404() throws Exception {
		UUID productId = UUID.randomUUID();
		when(productService.getProductById(productId))
				.thenThrow(new ResourceNotFoundException("Product", "id", productId));

		mockMvc.perform(get("/api/v1/products/" + productId))
				.andExpect(status().isNotFound());
	}

	@Test
	void getProductById_postMethodNotAllowed_returns405() throws Exception {
		UUID productId = UUID.randomUUID();
		mockMvc.perform(post("/api/v1/products/" + productId))
				.andExpect(status().isMethodNotAllowed());
	}

	// ==================== UPDATE PRODUCT ====================

	@Test
	void updateProduct_success_returns200() throws Exception {
		ProductResponse productResponse = createTestProductResponse();
		when(productService.updateProduct(anyString(), any(UUID.class), any()))
				.thenReturn(productResponse);

		mockMvc.perform(put("/api/v1/products/" + productResponse.id())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new ProductBody(
								productResponse.category().id(),
								"Updated Product",
								"Updated description",
								new BigDecimal("49.99"),
								20))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Product updated"))
				.andExpect(jsonPath("$.data.name").value("Test Product"));
	}

	@Test
	void updateProduct_notFound_returns404() throws Exception {
		UUID productId = UUID.randomUUID();
		when(productService.updateProduct(anyString(), any(UUID.class), any()))
				.thenThrow(new ResourceNotFoundException("Product", "id", productId));

		mockMvc.perform(put("/api/v1/products/" + productId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoryId\":\"" + UUID.randomUUID()
								+ "\",\"name\":\"Test\",\"price\":29.99,\"stock\":10}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void updateProduct_sellerMismatch_returns403() throws Exception {
		UUID productId = UUID.randomUUID();
		when(productService.updateProduct(anyString(), any(UUID.class), any()))
				.thenThrow(new AccessDeniedException("You can only update your own products"));

		mockMvc.perform(put("/api/v1/products/" + productId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoryId\":\"" + UUID.randomUUID()
								+ "\",\"name\":\"Test\",\"price\":29.99,\"stock\":10}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("You can only update your own products"));
	}

	@Test
	void updateProduct_blankName_returns400() throws Exception {
		UUID productId = UUID.randomUUID();

		mockMvc.perform(put("/api/v1/products/" + productId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoryId\":\"" + UUID.randomUUID()
								+ "\",\"name\":\"\",\"price\":29.99,\"stock\":10}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
	}

	@Test
	void updateProduct_malformedJson_returns400() throws Exception {
		UUID productId = UUID.randomUUID();

		mockMvc.perform(put("/api/v1/products/" + productId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{bad}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Malformed request body"));
	}

	// ==================== DELETE PRODUCT ====================

	@Test
	void deleteProduct_success_returns200() throws Exception {
		UUID productId = UUID.randomUUID();

		mockMvc.perform(delete("/api/v1/products/" + productId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Product deleted"));
	}

	@Test
	void deleteProduct_notFound_returns404() throws Exception {
		UUID productId = UUID.randomUUID();
		doThrow(new ResourceNotFoundException("Product", "id", productId))
				.when(productService).deleteProduct(anyString(), any(UUID.class));

		mockMvc.perform(delete("/api/v1/products/" + productId))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteProduct_sellerMismatch_returns403() throws Exception {
		UUID productId = UUID.randomUUID();
		doThrow(new AccessDeniedException("You can only delete your own products"))
				.when(productService).deleteProduct(anyString(), any(UUID.class));

		mockMvc.perform(delete("/api/v1/products/" + productId))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("You can only delete your own products"));
	}
}
