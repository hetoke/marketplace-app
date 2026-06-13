package com.marketplace.product.controller;

import static org.mockito.ArgumentMatchers.any;
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
import com.marketplace.product.service.CategoryService;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.GlobalExceptionHandler;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

	private MockMvc mockMvc;

	@Mock
	private CategoryService categoryService;

	@InjectMocks
	private CategoryController categoryController;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(categoryController)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	private CategoryResponse createTestCategoryResponse() {
		return new CategoryResponse(
				UUID.randomUUID().toString(),
				"Electronics",
				"Electronic devices",
				"electronics",
				null,
				true,
				Instant.now());
	}

	private record CategoryBody(String name, String description, String slug, String parentId) {}

	// ==================== CREATE CATEGORY ====================

	@Test
	void createCategory_success_returns201() throws Exception {
		CategoryResponse categoryResponse = createTestCategoryResponse();
		when(categoryService.createCategory(any())).thenReturn(categoryResponse);

		mockMvc.perform(post("/api/v1/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CategoryBody(
								"Electronics", "Electronic devices", "electronics", null))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("Category created"))
				.andExpect(jsonPath("$.data.name").value("Electronics"))
				.andExpect(jsonPath("$.data.slug").value("electronics"));
	}

	@Test
	void createCategory_blankName_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"\",\"description\":\"desc\",\"slug\":\"slug\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
	}

	@Test
	void createCategory_blankSlug_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Test\",\"description\":\"desc\",\"slug\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("slug"));
	}

	@Test
	void createCategory_nameAboveUpperBound256chars_returns400() throws Exception {
		String name256 = "A".repeat(256);

		mockMvc.perform(post("/api/v1/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CategoryBody(
								name256, "desc", "slug", null))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createCategory_slugAlreadyExists_returns409() throws Exception {
		when(categoryService.createCategory(any()))
				.thenThrow(new BusinessException("Category slug already exists"));

		mockMvc.perform(post("/api/v1/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Test\",\"description\":\"desc\",\"slug\":\"existing\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Category slug already exists"));
	}

	@Test
	void createCategory_malformedJson_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{bad}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Malformed request body"));
	}

	// ==================== GET ALL CATEGORIES ====================

	@Test
	void getAllCategories_success_returns200() throws Exception {
		CategoryResponse cat1 = createTestCategoryResponse();
		CategoryResponse cat2 = createTestCategoryResponse();
		when(categoryService.getAllCategories()).thenReturn(List.of(cat1, cat2));

		mockMvc.perform(get("/api/v1/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(2));
	}

	// ==================== GET CATEGORY BY ID ====================

	@Test
	void getCategoryById_success_returns200() throws Exception {
		CategoryResponse categoryResponse = createTestCategoryResponse();
		when(categoryService.getCategoryById(any(UUID.class))).thenReturn(categoryResponse);

		mockMvc.perform(get("/api/v1/categories/" + categoryResponse.id()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("Electronics"))
				.andExpect(jsonPath("$.data.slug").value("electronics"));
	}

	@Test
	void getCategoryById_notFound_returns404() throws Exception {
		UUID categoryId = UUID.randomUUID();
		when(categoryService.getCategoryById(categoryId))
				.thenThrow(new ResourceNotFoundException("Category", "id", categoryId));

		mockMvc.perform(get("/api/v1/categories/" + categoryId))
				.andExpect(status().isNotFound());
	}

	// ==================== UPDATE CATEGORY ====================

	@Test
	void updateCategory_success_returns200() throws Exception {
		CategoryResponse categoryResponse = createTestCategoryResponse();
		when(categoryService.updateCategory(any(UUID.class), any())).thenReturn(categoryResponse);

		mockMvc.perform(put("/api/v1/categories/" + categoryResponse.id())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CategoryBody(
								"Updated Electronics", "Updated desc", "updated-electronics", null))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Category updated"))
				.andExpect(jsonPath("$.data.name").value("Electronics"));
	}

	@Test
	void updateCategory_notFound_returns404() throws Exception {
		UUID categoryId = UUID.randomUUID();
		when(categoryService.updateCategory(any(UUID.class), any()))
				.thenThrow(new ResourceNotFoundException("Category", "id", categoryId));

		mockMvc.perform(put("/api/v1/categories/" + categoryId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Test\",\"description\":\"desc\",\"slug\":\"slug\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void updateCategory_slugAlreadyExists_returns409() throws Exception {
		UUID categoryId = UUID.randomUUID();
		when(categoryService.updateCategory(any(UUID.class), any()))
				.thenThrow(new BusinessException("Category slug already exists"));

		mockMvc.perform(put("/api/v1/categories/" + categoryId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Test\",\"description\":\"desc\",\"slug\":\"existing\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Category slug already exists"));
	}

	@Test
	void updateCategory_blankName_returns400() throws Exception {
		UUID categoryId = UUID.randomUUID();

		mockMvc.perform(put("/api/v1/categories/" + categoryId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"\",\"description\":\"desc\",\"slug\":\"slug\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
	}

	@Test
	void updateCategory_malformedJson_returns400() throws Exception {
		UUID categoryId = UUID.randomUUID();

		mockMvc.perform(put("/api/v1/categories/" + categoryId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{bad}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Malformed request body"));
	}

	// ==================== DELETE CATEGORY ====================

	@Test
	void deleteCategory_success_returns200() throws Exception {
		UUID categoryId = UUID.randomUUID();

		mockMvc.perform(delete("/api/v1/categories/" + categoryId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Category deleted"));
	}

	@Test
	void deleteCategory_notFound_returns404() throws Exception {
		UUID categoryId = UUID.randomUUID();
		doThrow(new ResourceNotFoundException("Category", "id", categoryId))
				.when(categoryService).deleteCategory(any(UUID.class));

		mockMvc.perform(delete("/api/v1/categories/" + categoryId))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteCategory_hasSubcategories_returns409() throws Exception {
		UUID categoryId = UUID.randomUUID();
		doThrow(new BusinessException("Cannot delete category with subcategories"))
				.when(categoryService).deleteCategory(any(UUID.class));

		mockMvc.perform(delete("/api/v1/categories/" + categoryId))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Cannot delete category with subcategories"));
	}
}
