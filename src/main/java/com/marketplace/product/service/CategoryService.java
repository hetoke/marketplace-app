package com.marketplace.product.service;

import com.marketplace.product.dto.CategoryRequest;
import com.marketplace.product.dto.CategoryResponse;
import com.marketplace.product.model.Category;
import com.marketplace.product.repository.CategoryRepository;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsBySlug(request.slug())) {
            throw new BusinessException("Category slug already exists");
        }
        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());
        category.setSlug(request.slug());
        if (request.parentId() != null) {
            Category parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.parentId()));
            category.setParent(parent);
        }
        categoryRepository.save(category);
        return CategoryResponse.from(category);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID categoryId, CategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
        if (!category.getSlug().equals(request.slug()) && categoryRepository.existsBySlug(request.slug())) {
            throw new BusinessException("Category slug already exists");
        }
        category.setName(request.name());
        category.setDescription(request.description());
        category.setSlug(request.slug());
        if (request.parentId() != null) {
            if (request.parentId().equals(categoryId)) {
                throw new BusinessException("Category cannot be its own parent");
            }
            Category parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.parentId()));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }
        category.setUpdatedAt(Instant.now());
        categoryRepository.save(category);
        return CategoryResponse.from(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
        return CategoryResponse.from(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public void deleteCategory(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
        if (!categoryRepository.findByParentId(categoryId).isEmpty()) {
            throw new BusinessException("Cannot delete category with subcategories");
        }
        categoryRepository.delete(category);
    }
}
