# 🏗️ Sprint Plan (Reformatted Based on Docs)

## Sprint 0 — Foundation (1 week)

### Goal
Project can run locally and deploy.

### Deliverables

#### Backend
- Spring Boot setup
- PostgreSQL setup
- Flyway/Liquibase migrations
- Global exception handling
- OpenAPI/Swagger

#### DevOps
- Docker
- Docker Compose
- GitHub Repository
- GitHub Actions CI

#### Architecture
Modular Monolith structure, based on modules.md:


### Definition of Done
```bash
docker compose up
```
starts application and database.

---

## Sprint 1 — Authentication & Users (2 weeks)

### Goal
Users can register and log in.

### Features

#### User Management
- Registration
- Login
- Logout
- JWT Authentication
- Refresh Tokens

#### Profile
- View Profile
- Update Profile

#### Security
- Password hashing
- Email verification
- Password reset

### Entities
- `User`
- `Role`
- `RefreshToken`
- `VerificationToken`

### API Endpoints
```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout
POST   /api/v1/auth/forgot-password
POST   /api/v1/auth/reset-password
POST   /api/v1/auth/verify-email
GET    /api/v1/users/profile
PUT    /api/v1/users/profile
PUT    /api/v1/users/profile/password
```

### Use Cases
- UC-001: User Registration

### Demo
```text
Register
Verify Email
Login
Access Protected Endpoint
```

---

## Sprint 2 — Product Catalog (2 weeks)

### Goal
Seller can manage products.

### Features

#### Categories
- Create Category
- Update Category
- View Categories

#### Products
- Create Product
- Update Product
- Delete Product
- Product Details

#### Search
- Search by name
- Filter by category
- Filter by price

### Entities
- `Category`
- `Product`
- `ProductImage`

### API Endpoints
```
POST   /api/v1/products
GET    /api/v1/products
GET    /api/v1/products/{productId}
PUT    /api/v1/products/{productId}
DELETE /api/v1/products/{productId}
GET    /api/v1/categories
POST   /api/v1/categories
GET    /api/v1/categories/{categoryId}
PUT    /api/v1/categories/{categoryId}
DELETE /api/v1/categories/{categoryId}
```

### Use Cases
- UC-005: Create Product
- UC-006: Search Products with Advanced Filtering

### Demo
```text
Seller creates product
Buyer browses products
```

---

## Sprint 3 — Cart & Wishlist (2 weeks)

### Goal
Buyer can prepare purchases.

### Features

#### Cart
- Add to cart
- Remove from cart
- Update quantity

#### Wishlist
- Add wishlist item
- Remove wishlist item

### Entities
- `Cart`
- `CartItem`
- `Wishlist`
- `WishlistItem`

### API Endpoints
```
GET    /api/v1/cart
POST   /api/v1/cart/items
PUT    /api/v1/cart/items/{itemId}
DELETE /api/v1/cart/items/{itemId}
DELETE /api/v1/cart
POST   /api/v1/buyers/wishlist/{productId}
DELETE /api/v1/buyers/wishlist/{productId}
GET    /api/v1/buyers/wishlist
```

### Use Cases
- UC-008: Add Item to Cart with Quantity Selection
- UC-009: Manage Wishlist

### Demo
```text
Browse Product
Add To Cart
Add To Wishlist
```

---

## Sprint 4 — Orders & Checkout (2 weeks)

### Goal
Complete purchasing flow.

### Features

#### Checkout
- Create order
- Address management

#### Orders
- Order history
- Order details
- Order status tracking

#### Inventory
- Automatic stock deduction

### Entities
- `Order`
- `OrderItem`
- `Address`
- `Inventory`

### API Endpoints
```
POST   /api/v1/orders
GET    /api/v1/orders
GET    /api/v1/orders/{orderId}
PUT    /api/v1/orders/{orderId}/status
POST   /api/v1/orders/{orderId}/cancel
POST   /api/v1/orders/{orderId}/return
GET    /api/v1/orders/{orderId}/items
```

### Use Cases
- UC-010: Place Order
- UC-011: Track Order Status
- UC-012: Cancel Order with Time Window
- UC-013: Request Return/Refund

### Demo
```text
Add To Cart
Checkout
View Order
```

---

## Sprint 5 — Payments & Refunds (2 weeks)

### Goal
Handle payment lifecycle.

### Features

#### Payments
- SEPay Sandbox
- VNPay Sandbox (optional)

#### Refunds
- Request refund
- Approve refund

#### Transaction History
- Payment records
- Seller revenue tracking

### Entities
- `Payment`
- `Transaction`
- `Refund`

### API Endpoints
```
POST   /api/v1/payments
GET    /api/v1/payments/{paymentId}
POST   /api/v1/payments/{paymentId}/refund
GET    /api/v1/payments/methods
GET    /api/v1/payments/history
```

### Use Cases
- UC-014: Process Payment with Multiple Methods
- UC-015: View Payment History

### Demo
```text
Checkout
Pay
Refund
```

---

## Sprint 6 — Reviews & Notifications (2 weeks)

### Goal
Marketplace interaction.

### Features

#### Reviews
- Product ratings
- Product reviews

#### Notifications
- In-app notifications
- Email notifications

Types:
```
Order Updates
Payment Updates
Low Stock Alerts
Security Alerts
```

### Entities
- `Review`
- `Notification`

### API Endpoints
```
POST   /api/v1/reviews
GET    /api/v1/reviews
GET    /api/v1/reviews/{reviewId}
PUT    /api/v1/reviews/{reviewId}
DELETE /api/v1/reviews/{reviewId}
GET    /api/v1/products/{productId}/reviews
GET    /api/v1/notifications
PUT    /api/v1/notifications/{notificationId}/read
DELETE /api/v1/notifications/{notificationId}
PUT    /api/v1/notifications/settings
DELETE /api/v1/notifications
```

### Use Cases
- UC-007: Product Review Submission
- UC-016: Receive Order Status Notifications
- UC-017: Receive Security Notifications
- UC-018: Submit Product Review with Verification

### Demo
```text
Purchase Product
Leave Review
Receive Notification
```

---

## Sprint 7 — Google Login & MFA (2 weeks)

### Goal
Advanced authentication.

### Features

#### Google SSO
- Google OAuth Login

#### MFA
- Email OTP
- Recovery Codes

#### Security
- Login alerts
- Device tracking

### Entities
- `IdentityProvider`
- `UserIdentity`
- `MFAChallenge`
- `RecoveryCode`

### API Endpoints
```
GET    /api/v1/auth/providers
POST   /api/v1/auth/oidc/login
GET    /api/v1/auth/oidc/callback
POST   /api/v1/users/mfa/setup
POST   /api/v1/users/mfa/verify
DELETE /api/v1/users/mfa
POST   /api/v1/users/mfa/recovery
GET    /api/v1/users/mfa/status
```

### Use Cases
- UC-002: User Login with OIDC
- UC-003: Multi-Factor Authentication Setup

### Demo
```text
Login With Google
Enable MFA
Verify OTP
```

---

## Sprint 8 — Admin Dashboard & Analytics (2 weeks)

### Goal
Administration features.

### Features

#### Admin
- User Management
- Product Moderation

#### Analytics
- Revenue Statistics
- Orders Statistics
- User Statistics
- Product Statistics

### Entities
- `AnalyticsReport`
- `AdminActionLog`

### API Endpoints
```
GET    /api/v1/admin/users
PUT    /api/v1/admin/users/{userId}/status
GET    /api/v1/admin/products
PUT    /api/v1/admin/products/{productId}/status
GET    /api/v1/admin/analytics/revenue
GET    /api/v1/admin/analytics/orders
GET    /api/v1/admin/analytics/users
GET    /api/v1/admin/analytics/products
```

### Demo
```text
Admin Dashboard
Reports
```

---

## Sprint 9 — Hardening & Deployment (2 weeks)

### Goal
Production-ready portfolio project.

### Testing
- Unit Tests
- Integration Tests

### Security
- Rate Limiting
- Input Validation
- Audit Logging

### Performance
- Redis Cache
- Query Optimization

### Deployment
- Docker Deployment
- Production Environment
- Monitoring Basics

### Documentation
- Architecture Diagram
- ERD
- API Documentation
- README

### Demo
Public URL + GitHub Repository.
