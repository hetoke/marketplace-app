# 🧱 Modules

## 📦 Core Modules

```
marketplace-app/
├── user-management/          # Authentication, profiles, MFA
├── product-catalog/          # Products, categories, inventory
├── shopping-cart/           # Cart management, guest checkout
├── order-management/        # Orders, status tracking
├── payment-processing/      # Payments, refunds
├── notification/            # Notifications system
├── review-system/          # Product reviews, ratings
├── support-ticket/         # Customer support tickets
├── shared-kernel/          # Common types, exceptions, utilities
└── api-gateway/            # REST API controllers, routing
```

---

## 📚 Module Details

### 1. **User Management Module**
**Responsibilities:**
- User registration, login, logout
- MFA management
- Profile management (buyer/seller)
- Password reset, account recovery
- Identity Provider integration (OIDC/OAuth2)

**Key Entities:** User, SellerProfile, BuyerProfile, IdentityProvider, UserIdentity, OAuthClient

---

### 2. **Product Catalog Module**
**Responsibilities:**
- Product CRUD operations
- Category management
- Inventory tracking
- Product activation/deactivation

**Key Entities:** Product, Category, Inventory

---

### 3. **Shopping Cart Module**
**Responsibilities:**
- Cart creation and management
- Persistent cart across sessions
- Guest checkout support

**Key Entities:** Cart, CartItem

---

### 4. **Order Management Module**
**Responsibilities:**
- Order creation from cart
- Order status tracking (PENDING → CONFIRMED → DELIVERY → COMPLETE)
- Order cancellation
- Return/refund requests

**Key Entities:** Order

---

### 5. **Payment Processing Module**
**Responsibilities:**
- Payment method management
- Transaction processing
- Refund handling

**Key Entities:** Payment

---

### 6. **Notification Module**
**Responsibilities:**
- Notification system (order updates, security alerts, promotional)
- Notification delivery tracking

**Key Entities:** Notification

---

### 7. **Review System Module**
**Responsibilities:**
- Product reviews and ratings
- Review moderation
- Verified purchase validation

**Key Entities:** Review

---

### 8. **Support Ticket Module**
**Responsibilities:**
- Customer support ticketing
- Ticket assignment and escalation
- Resolution tracking

**Key Entities:** SupportTicket

---

### 9. **Shared Kernel Module**
**Responsibilities:**
- Common domain types and value objects
- Base entity classes
- Common exceptions and error handling
- Utility functions
- Configuration and constants

---

### 10. **API Gateway Module**
**Responsibilities:**
- REST API controllers
- Request validation
- Authentication middleware
- API versioning



## 📁Project Structure

```
marketplace-app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── marketplace/
│   │   │           ├── MarketplaceApplication.java
│   │   │           ├── api/
│   │   │           │   ├── controller/
│   │   │           │   │   ├── AuthController.java
│   │   │           │   │   ├── ProductController.java
│   │   │           │   │   ├── OrderController.java
│   │   │           │   │   ├── UserController.java
│   │   │           │   │   ├── CartController.java
│   │   │           │   │   ├── PaymentController.java
│   │   │           │   │   ├── ReviewController.java
│   │   │           │   │   ├── NotificationController.java
│   │   │           │   │   └── TicketController.java
│   │   │           │   └── dto/
│   │   │           ├── shared/
│   │   │           ├── user/
│   │   │           │   ├── model/
│   │   │           │   │   ├── User.java
│   │   │           │   │   ├── SellerProfile.java
│   │   │           │   │   ├── BuyerProfile.java
│   │   │           │   │   ├── IdentityProvider.java
│   │   │           │   │   ├── UserIdentity.java
│   │   │           │   │   └── OAuthClient.java
│   │   │           ├── product/
│   │   │           │   ├── model/
│   │   │           │   │   ├── Product.java
│   │   │           │   │   ├── Category.java
│   │   │           │   │   └── Inventory.java
│   │   │           ├── cart/
│   │   │           │   ├── model/
│   │   │           │   │   ├── Cart.java
│   │   │           │   │   └── CartItem.java
│   │   │           ├── order/
│   │   │           │   ├── model/
│   │   │           │   │   └── Order.java
│   │   │           ├── payment/
│   │   │           │   ├── model/
│   │   │           │   │   └── Payment.java
│   │   │           ├── notification/
│   │   │           │   ├── model/
│   │   │           │   │   └── Notification.java
│   │   │           ├── review/
│   │   │           │   ├── model/
│   │   │           │   │   └── Review.java
│   │   │           └── ticket/
│   │   │               ├── model/
│   │   │               └── SupportTicket.java
```
