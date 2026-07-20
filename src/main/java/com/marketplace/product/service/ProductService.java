package com.marketplace.product.service;

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
import com.marketplace.upload.model.EntityType;
import com.marketplace.upload.repository.ImageRepository;
import com.marketplace.upload.service.ImageService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ImageRepository imageRepository;
    private final ImageService imageService;
    private final DiscountService discountService;

    public ProductService(
        ProductRepository productRepository,
        CategoryRepository categoryRepository,
        ProductImageRepository productImageRepository,
        ImageRepository imageRepository,
        ImageService imageService,
        DiscountService discountService
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productImageRepository = productImageRepository;
        this.imageRepository = imageRepository;
        this.imageService = imageService;
        this.discountService = discountService;
    }

    private ProductResponse toResponse(Product product, List<ProductImage> images) {
        boolean active = discountService.isDiscountActive(product);
        BigDecimal effective = discountService.computeEffectivePrice(product);
        return ProductResponse.from(product, images, effective, active);
    }

    private void applyDiscount(Product product, ProductRequest request) {
        product.setDiscountType(request.discountType());
        product.setDiscountValue(request.discountValue());
        product.setDiscountStart(request.discountStart());
        product.setDiscountEnd(request.discountEnd());
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse createProduct(
        String sellerId,
        ProductRequest request
    ) {
        Category category = categoryRepository
            .findById(request.categoryId())
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Category",
                    "id",
                    request.categoryId()
                )
            );
        String slug = generateSlug(request.name());
        if (productRepository.findBySlug(slug).isPresent()) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
        Product product = new Product();
        product.setSellerId(UUID.fromString(sellerId));
        product.setCategory(category);
        product.setName(request.name());
        product.setSlug(slug);
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        applyDiscount(product, request);
        productRepository.save(product);
        return toResponse(product, List.of());
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse updateProduct(
        String sellerId,
        UUID productId,
        ProductRequest request
    ) {
        Product product = productRepository
            .findById(productId)
            .orElseThrow(() ->
                new ResourceNotFoundException("Product", "id", productId)
            );
        if (!product.getSellerId().toString().equals(sellerId)) {
            throw new AccessDeniedException(
                "You can only update your own products"
            );
        }
        Category category = categoryRepository
            .findById(request.categoryId())
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Category",
                    "id",
                    request.categoryId()
                )
            );
        product.setCategory(category);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        applyDiscount(product, request);
        product.setUpdatedAt(Instant.now());
        productRepository.save(product);
        return toResponse(product, List.of());
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteProduct(String sellerId, UUID productId) {
        Product product = productRepository
            .findById(productId)
            .orElseThrow(() ->
                new ResourceNotFoundException("Product", "id", productId)
            );
        if (!product.getSellerId().toString().equals(sellerId)) {
            throw new AccessDeniedException(
                "You can only delete your own products"
            );
        }
        productImageRepository.deleteByProductId(productId);
        imageService.deleteImagesByEntity(EntityType.PRODUCT, productId);
        productRepository.delete(product);
    }

    @Transactional
    public void deleteProductImage(
        String sellerId,
        UUID productId,
        UUID imageId
    ) {
        Product product = productRepository
            .findById(productId)
            .orElseThrow(() ->
                new ResourceNotFoundException("Product", "id", productId)
            );
        if (!product.getSellerId().toString().equals(sellerId)) {
            throw new AccessDeniedException(
                "You can only delete images from your own products"
            );
        }
        ProductImage productImage = productImageRepository
            .findById(imageId)
            .orElseThrow(() ->
                new ResourceNotFoundException("Product image", "id", imageId)
            );
        if (!productImage.getProduct().getId().equals(productId)) {
            throw new AccessDeniedException(
                "Image does not belong to this product"
            );
        }
        imageRepository
            .findByFileUrlContaining(productImage.getUrl())
            .ifPresent(image -> imageService.deleteById(image.getId()));
        productImageRepository.delete(productImage);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "productById", key = "#productId")
    public ProductResponse getProductById(UUID productId) {
        Product product = productRepository
            .findById(productId)
            .orElseThrow(() ->
                new ResourceNotFoundException("Product", "id", productId)
            );
        var images = productImageRepository.findByProductIdOrderBySortOrderAsc(
            productId
        );
        return toResponse(product, images);
    }

    @Transactional(readOnly = true)
    @Cacheable(
        value = "products",
        key = "'search:' + #request.query() + ':' + #request.categoryId() + ':' + #request.minPrice() + ':' + #request.maxPrice() + ':' + #request.page() + ':' + #request.size()"
    )
    public PageResponse<ProductResponse> searchProducts(
        ProductSearchRequest request
    ) {
        Sort sort = Sort.by(Sort.Direction.DESC, request.getSortBy());
        //System.out.println(sort);
        PageRequest pageRequest = PageRequest.of(
            request.getPage(),
            request.getSize(),
            sort
        );
        Page<Product> page = productRepository.search(
            request.query(),
            request.categoryId(),
            request.minPrice(),
            request.maxPrice(),
            pageRequest
        );
        var content = toProductResponses(page.getContent());
        return new PageResponse<>(
            content,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductsByCategory(
        UUID categoryId,
        int page,
        int size
    ) {
        PageRequest pageRequest = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<Product> pageResult =
            productRepository.findByCategoryIdAndActiveTrue(
                categoryId,
                pageRequest
            );
        var content = toProductResponses(pageResult.getContent());
        return new PageResponse<>(
            content,
            pageResult.getNumber(),
            pageResult.getSize(),
            pageResult.getTotalElements(),
            pageResult.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getSellerProducts(
        String sellerId,
        int page,
        int size
    ) {
        PageRequest pageRequest = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<Product> pageResult = productRepository.findBySellerId(
            UUID.fromString(sellerId),
            pageRequest
        );
        var content = toProductResponses(pageResult.getContent());
        return new PageResponse<>(
            content,
            pageResult.getNumber(),
            pageResult.getSize(),
            pageResult.getTotalElements(),
            pageResult.getTotalPages()
        );
    }

    private List<ProductResponse> toProductResponses(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<ProductImage>> imagesByProduct = productImageRepository
            .findByProductIdInOrderBySortOrderAsc(
                products.stream().map(Product::getId).toList()
            )
            .stream()
            .collect(
                Collectors.groupingBy(
                    image -> image.getProduct().getId(),
                    Collectors.toList()
                )
            );
        return products
            .stream()
            .map(product ->
                toResponse(
                    product,
                    imagesByProduct.getOrDefault(
                        product.getId(),
                        List.of()
                    )
                )
            )
            .toList();
    }

    private String generateSlug(String name) {
        return name
            .toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
    }
}
