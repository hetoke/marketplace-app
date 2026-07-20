package com.marketplace.product.service;

import com.marketplace.product.model.DiscountType;
import com.marketplace.product.model.Product;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class DiscountService {

    public boolean isDiscountActive(Product product) {
        DiscountType type = product.getDiscountType();
        BigDecimal value = product.getDiscountValue();
        if (type == null || value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        Instant now = Instant.now();
        if (product.getDiscountStart() != null && now.isBefore(product.getDiscountStart())) {
            return false;
        }
        if (product.getDiscountEnd() != null && now.isAfter(product.getDiscountEnd())) {
            return false;
        }
        return true;
    }

    public BigDecimal computeEffectivePrice(Product product) {
        if (!isDiscountActive(product)) {
            return product.getPrice();
        }
        BigDecimal price = product.getPrice();
        return switch (product.getDiscountType()) {
            case PERCENT -> price.multiply(
                BigDecimal.ONE.subtract(
                    product.getDiscountValue().divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP)
                )
            );
            case FIXED -> price.subtract(product.getDiscountValue()).max(BigDecimal.ZERO);
        };
    }

    public BigDecimal computeDiscountAmount(Product product) {
        return product.getPrice().subtract(computeEffectivePrice(product));
    }
}
