package com.marketplace.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketplace.product.dto.CategoryRequest;
import com.marketplace.product.dto.CategoryResponse;
import com.marketplace.product.model.Category;
import com.marketplace.product.repository.CategoryRepository;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

	@Mock
	private CategoryRepository categoryRepository;

	@InjectMocks
	private CategoryService categoryService;

	private Category createTestCategory() {
		Category category = new Category();
		category.setId(UUID.randomUUID());
		category.setName("Electronics");
		category.setDescription("Electronic devices");
		category.setSlug("electronics");
		category.setActive(true);
		category.setCreatedAt(Instant.now());
		category.setUpdatedAt(Instant.now());
		return category;
	}

	private CategoryRequest createCategoryRequest() {
		return new CategoryRequest("Electronics", "Electronic devices", "electronics", null);
	}

	// ==================== CREATE CATEGORY ====================

	@Test
	void createCategory_success() {
		CategoryRequest request = createCategoryRequest();
		Category category = createTestCategory();

		when(categoryRepository.existsBySlug("electronics")).thenReturn(false);
		when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
			Category c = inv.getArgument(0);
			c.setId(category.getId());
			return c;
		});

		CategoryResponse response = categoryService.createCategory(request);

		assertThat(response.name()).isEqualTo("Electronics");
		assertThat(response.slug()).isEqualTo("electronics");
		verify(categoryRepository).save(any(Category.class));
	}

	@Test
	void createCategory_withParent() {
		Category parent = createTestCategory();
		CategoryRequest request = new CategoryRequest(
				"Phones", "Mobile phones", "phones", parent.getId());

		when(categoryRepository.existsBySlug("phones")).thenReturn(false);
		when(categoryRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
		when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
			Category c = inv.getArgument(0);
			c.setId(UUID.randomUUID());
			return c;
		});

		CategoryResponse response = categoryService.createCategory(request);

		assertThat(response.name()).isEqualTo("Phones");
		assertThat(response.parentId()).isEqualTo(parent.getId().toString());
		verify(categoryRepository).save(any(Category.class));
	}

	@Test
	void createCategory_slugAlreadyExists_throwsBusinessException() {
		CategoryRequest request = createCategoryRequest();

		when(categoryRepository.existsBySlug("electronics")).thenReturn(true);

		assertThatThrownBy(() -> categoryService.createCategory(request))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Category slug already exists");
	}

	@Test
	void createCategory_parentNotFound_throwsResourceNotFoundException() {
		UUID parentId = UUID.randomUUID();
		CategoryRequest request = new CategoryRequest(
				"Phones", "Mobile phones", "phones", parentId);

		when(categoryRepository.existsBySlug("phones")).thenReturn(false);
		when(categoryRepository.findById(parentId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> categoryService.createCategory(request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Category")
				.hasMessageContaining(parentId.toString());
	}

	// ==================== UPDATE CATEGORY ====================

	@Test
	void updateCategory_success() {
		Category category = createTestCategory();
		CategoryRequest request = new CategoryRequest(
				"Updated Electronics", "Updated desc", "updated-electronics", null);

		when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
		when(categoryRepository.existsBySlug("updated-electronics")).thenReturn(false);
		when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

		CategoryResponse response = categoryService.updateCategory(category.getId(), request);

		assertThat(response.name()).isEqualTo("Updated Electronics");
		assertThat(response.slug()).isEqualTo("updated-electronics");
		verify(categoryRepository).save(category);
	}

	@Test
	void updateCategory_categoryNotFound_throwsResourceNotFoundException() {
		UUID categoryId = UUID.randomUUID();
		CategoryRequest request = createCategoryRequest();

		when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> categoryService.updateCategory(categoryId, request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Category")
				.hasMessageContaining(categoryId.toString());
	}

	@Test
	void updateCategory_slugAlreadyExistsDifferentCategory_throwsBusinessException() {
		Category category = createTestCategory();
		CategoryRequest request = new CategoryRequest(
				"New Name", "New desc", "existing-slug", null);

		when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
		when(categoryRepository.existsBySlug("existing-slug")).thenReturn(true);

		assertThatThrownBy(() -> categoryService.updateCategory(category.getId(), request))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Category slug already exists");
	}

	@Test
	void updateCategory_selfParenting_throwsBusinessException() {
		Category category = createTestCategory();
		CategoryRequest request = new CategoryRequest(
				category.getName(), category.getDescription(), category.getSlug(),
				category.getId());

		when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

		assertThatThrownBy(() -> categoryService.updateCategory(category.getId(), request))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Category cannot be its own parent");
	}

	@Test
	void updateCategory_parentNotFound_throwsResourceNotFoundException() {
		Category category = createTestCategory();
		UUID parentId = UUID.randomUUID();
		CategoryRequest request = new CategoryRequest(
				category.getName(), category.getDescription(), category.getSlug(),
				parentId);

		when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
		when(categoryRepository.findById(parentId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> categoryService.updateCategory(category.getId(), request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Category")
				.hasMessageContaining(parentId.toString());
	}

	@Test
	void updateCategory_clearParent_success() {
		Category parent = createTestCategory();
		Category child = createTestCategory();
		child.setParent(parent);

		CategoryRequest request = new CategoryRequest(
				child.getName(), child.getDescription(), child.getSlug(), null);

		when(categoryRepository.findById(child.getId())).thenReturn(Optional.of(child));
		when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

		CategoryResponse response = categoryService.updateCategory(child.getId(), request);

		assertThat(response.parentId()).isNull();
		verify(categoryRepository).save(child);
	}

	// ==================== GET CATEGORY BY ID ====================

	@Test
	void getCategoryById_success() {
		Category category = createTestCategory();

		when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

		CategoryResponse response = categoryService.getCategoryById(category.getId());

		assertThat(response.id()).isEqualTo(category.getId().toString());
		assertThat(response.name()).isEqualTo("Electronics");
		assertThat(response.slug()).isEqualTo("electronics");
	}

	@Test
	void getCategoryById_notFound_throwsResourceNotFoundException() {
		UUID categoryId = UUID.randomUUID();

		when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> categoryService.getCategoryById(categoryId))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Category")
				.hasMessageContaining(categoryId.toString());
	}

	// ==================== GET ALL CATEGORIES ====================

	@Test
	void getAllCategories_success() {
		Category category1 = createTestCategory();
		Category category2 = createTestCategory();

		when(categoryRepository.findAll()).thenReturn(List.of(category1, category2));

		List<CategoryResponse> responses = categoryService.getAllCategories();

		assertThat(responses).hasSize(2);
		assertThat(responses.get(0).name()).isEqualTo("Electronics");
		assertThat(responses.get(1).name()).isEqualTo("Electronics");
	}

	// ==================== DELETE CATEGORY ====================

	@Test
	void deleteCategory_success() {
		Category category = createTestCategory();

		when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
		when(categoryRepository.findByParentId(category.getId())).thenReturn(List.of());

		categoryService.deleteCategory(category.getId());

		verify(categoryRepository).delete(category);
	}

	@Test
	void deleteCategory_notFound_throwsResourceNotFoundException() {
		UUID categoryId = UUID.randomUUID();

		when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> categoryService.deleteCategory(categoryId))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Category")
				.hasMessageContaining(categoryId.toString());
	}

	@Test
	void deleteCategory_hasSubcategories_throwsBusinessException() {
		Category category = createTestCategory();
		Category subCategory = createTestCategory();

		when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
		when(categoryRepository.findByParentId(category.getId())).thenReturn(List.of(subCategory));

		assertThatThrownBy(() -> categoryService.deleteCategory(category.getId()))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Cannot delete category with subcategories");
	}
}
