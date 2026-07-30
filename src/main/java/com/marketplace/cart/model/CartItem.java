package com.marketplace.cart.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cart_items", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"cart_id", "product_id", "variant_id"})
})
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(name = "sku")
    private String sku;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt = Instant.now();

    public CartItem() {}

    public CartItem(Cart cart, UUID productId, Integer quantity, BigDecimal unitPrice) {
        this.cart = cart;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public CartItem(Cart cart, UUID productId, UUID variantId, String sku, Integer quantity, BigDecimal unitPrice) {
        this.cart = cart;
        this.productId = productId;
        this.variantId = variantId;
        this.sku = sku;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Cart getCart() { return cart; }
    public void setCart(Cart cart) { this.cart = cart; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public UUID getVariantId() { return variantId; }
    public void setVariantId(UUID variantId) { this.variantId = variantId; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public Instant getAddedAt() { return addedAt; }
    public void setAddedAt(Instant addedAt) { this.addedAt = addedAt; }

    public BigDecimal getTotalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
