package com.marketplace.product.service;

import com.marketplace.product.dto.ProductCache;
import com.marketplace.product.dto.ProductRequest;
import com.marketplace.product.dto.ProductResponse;
import com.marketplace.product.dto.ProductSearchRequest;
import com.marketplace.product.dto.ProductVariantRequest;
import com.marketplace.product.model.Category;
import com.marketplace.product.model.Product;
import com.marketplace.product.model.ProductImage;
import com.marketplace.product.model.ProductVariant;
import com.marketplace.product.repository.CategoryRepository;
import com.marketplace.product.repository.ProductImageRepository;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.product.repository.ProductVariantRepository;
import com.marketplace.shared.dto.PageResponse;
import com.marketplace.shared.exception.AccessDeniedException;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import com.marketplace.upload.model.EntityType;
import com.marketplace.upload.repository.ImageRepository;
import com.marketplace.upload.service.ImageService;
import com.marketplace.user.model.User;
import com.marketplace.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
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
    private final ProductVariantRepository productVariantRepository;
    private final ImageRepository imageRepository;
    private final ImageService imageService;
    private final DiscountService discountService;
    private final UserRepository userRepository;

    public ProductService(
        ProductRepository productRepository,
        CategoryRepository categoryRepository,
        ProductImageRepository productImageRepository,
        ProductVariantRepository productVariantRepository,
        ImageRepository imageRepository,
        ImageService imageService,
        DiscountService discountService,
        UserRepository userRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productImageRepository = productImageRepository;
        this.productVariantRepository = productVariantRepository;
        this.imageRepository = imageRepository;
        this.imageService = imageService;
        this.discountService = discountService;
        this.userRepository = userRepository;
    }

private ProductResponse toResponse(Product product, List<ProductImage> images) {
         boolean active = discountService.isDiscountActive(product);
         BigDecimal effective = discountService.computeEffectivePrice(product);
         Long timeLeft = discountService.computeTimeLeft(product);
         String sellerName = userRepository.findById(product.getSellerId())
                 .map(User::getDisplayName)
                 .orElse(null);
         return ProductResponse.from(product, images, effective, active, sellerName, timeLeft);
     }

private void applyDiscount(Product product, ProductRequest request) {
        if (request.discountType() != null && request.discountValue() != null && request.discountValue().compareTo(BigDecimal.ZERO) > 0) {
            if (request.discountStart() != null && request.discountStart().isBefore(Instant.now())) {
                throw new IllegalArgumentException("discountStart must not be in the past");
            }
        }
        product.setDiscountType(request.discountType());
        product.setDiscountValue(request.discountValue());
        product.setDiscountStart(request.discountStart());
        product.setDiscountEnd(request.discountEnd());
    }

    @Transactional
    @CacheEvict(value = {"products", "productById"}, allEntries = true)
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
        applyDiscount(product, request);

        List<ProductVariantRequest> variantRequests = request.variants();
        boolean hasVariants = variantRequests != null && !variantRequests.isEmpty();

        if (hasVariants) {
            List<ProductVariant> variants = new ArrayList<>();
            int totalStock = 0;
            for (ProductVariantRequest vr : variantRequests) {
                if (productVariantRepository.existsBySku(vr.sku())) {
                    throw new BusinessException("SKU '" + vr.sku() + "' already exists");
                }
                ProductVariant variant = new ProductVariant(
                    product, vr.sku(), vr.price(), vr.stock(),
                    vr.attributes() != null ? vr.attributes() : Map.of(),
                    vr.sortOrder() != null ? vr.sortOrder() : 0
                );
                variants.add(variant);
                totalStock += vr.stock();
            }
            product.setVariants(variants);
            product.setStock(totalStock);
            product.setPrice(variantRequests.get(0).price());
        } else {
            product.setPrice(request.price());
            product.setStock(request.stock());
        }

        productRepository.save(product);
        return toResponse(product, List.of());
    }

    @Transactional
    @CacheEvict(value = {"products", "productById"}, allEntries = true)
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
        applyDiscount(product, request);

        List<ProductVariantRequest> variantRequests = request.variants();
        boolean hasVariants = variantRequests != null && !variantRequests.isEmpty();

        if (hasVariants) {
            List<ProductVariant> existingVariants = productVariantRepository.findByProductIdOrderBySortOrderAsc(productId);
            for (ProductVariant ev : existingVariants) {
                productVariantRepository.delete(ev);
            }

            List<ProductVariant> variants = new ArrayList<>();
            int totalStock = 0;
            for (ProductVariantRequest vr : variantRequests) {
                if (productVariantRepository.existsBySku(vr.sku())) {
                    throw new BusinessException("SKU '" + vr.sku() + "' already exists");
                }
                ProductVariant variant = new ProductVariant(
                    product, vr.sku(), vr.price(), vr.stock(),
                    vr.attributes() != null ? vr.attributes() : Map.of(),
                    vr.sortOrder() != null ? vr.sortOrder() : 0
                );
                variants.add(variant);
                totalStock += vr.stock();
            }
            product.setVariants(variants);
            product.setStock(totalStock);
            product.setPrice(variantRequests.get(0).price());
        } else {
            product.setPrice(request.price());
            product.setStock(request.stock());
        }

        product.setUpdatedAt(Instant.now());
        productRepository.save(product);
        return toResponse(product, List.of());
    }

    @Transactional
    @CacheEvict(value = {"products", "productById"}, allEntries = true)
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
    @CacheEvict(value = {"products", "productById"}, allEntries = true)
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
    public List<ProductResponse> searchProducts(
        ProductSearchRequest request
    ) {
        return searchProductsCached(request).items();
    }

    @Cacheable(
        value = "products",
        key = "'search:' + #request.query() + ':' + #request.categoryId() + ':' + #request.sellerId() + ':' + #request.minPrice() + ':' + #request.maxPrice() + ':' + #request.page() + ':' + #request.size()"
    )
    public ProductCache searchProductsCached(
        ProductSearchRequest request
    ) {
        Sort sort = Sort.by(Sort.Direction.DESC, request.getSortBy());
        PageRequest pageRequest = PageRequest.of(
            request.getPage(),
            request.getSize(),
            sort
        );
        Page<Product> page = productRepository.search(
            request.query(),
            request.categoryId(),
            request.sellerId(),
            request.minPrice(),
            request.maxPrice(),
            pageRequest
        );
        return new ProductCache(toProductResponses(page.getContent()));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProductsPaged(
        ProductSearchRequest request
    ) {
        List<ProductResponse> content = searchProducts(request);
        long totalElements = productRepository.countBySearchFilters(
            request.query(),
            request.categoryId(),
            request.sellerId(),
            request.minPrice(),
            request.maxPrice()
        );
        int totalPages = (int) Math.ceil((double) totalElements / request.getSize());
        return new PageResponse<>(
            content,
            request.getPage(),
            request.getSize(),
            totalElements,
            totalPages
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

        List<UUID> sellerIds = products.stream()
                .map(Product::getSellerId)
                .distinct()
                .toList();
        Map<UUID, String> sellerNames = userRepository.findAllById(sellerIds)
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u.getDisplayName() != null ? u.getDisplayName() : ""));

return products
             .stream()
             .map(product -> {
                 String sellerName = sellerNames.get(product.getSellerId());
                 boolean active = discountService.isDiscountActive(product);
                 BigDecimal effective = discountService.computeEffectivePrice(product);
                 Long timeLeft = discountService.computeTimeLeft(product);
                 return ProductResponse.from(
                     product,
                     imagesByProduct.getOrDefault(product.getId(), List.of()),
                     effective,
                     active,
                     sellerName,
                     timeLeft
                 );
             })
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
