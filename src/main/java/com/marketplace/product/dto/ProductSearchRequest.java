package com.marketplace.product.dto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record ProductSearchRequest(
        String query,
        UUID categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer page,
        Integer size,
        String sortBy
) {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name", "slug", "price", "stock", "createdAt", "updatedAt"
    );

    public int getPage() { return page != null && page >= 0 && page <= 10000 ? page : 0; }
    public int getSize() { return size != null && size > 0 && size <= 100 ? size : 20; }
    public String getSortBy() {
        return sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
    }
}
