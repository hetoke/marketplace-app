# 📡 API Specifications


## 🌐 API Endpoints by Module

### 🔐 1. Authentication & User Management

#### Auth Endpoints
```
POST   /api/v1/auth/register              # User registration
POST   /api/v1/auth/login                 # User login (local/OIDC)
POST   /api/v1/auth/refresh               # Token refresh
POST   /api/v1/auth/logout                # User logout
POST   /api/v1/auth/forgot-password       # Password reset request
POST   /api/v1/auth/reset-password        # Password reset
POST   /api/v1/auth/verify-email          # Email verification
GET    /api/v1/auth/providers             # Get available IdPs
POST   /api/v1/auth/oidc/login            # OIDC login initiation
GET    /api/v1/auth/oidc/callback         # OIDC callback endpoint
```

#### User Profile Endpoints
```
GET    /api/v1/users/profile              # Get current user profile
PUT    /api/v1/users/profile              # Update user profile
PUT    /api/v1/users/profile/password     # Change password
GET    /api/v1/users/{userId}             # Get user public profile
```

#### MFA Endpoints
```
POST   /api/v1/users/mfa/setup            # Setup MFA
POST   /api/v1/users/mfa/verify           # Verify MFA code
DELETE /api/v1/users/mfa                 # Disable MFA
POST   /api/v1/users/mfa/recovery         # Use recovery code
GET    /api/v1/users/mfa/status           # Get MFA status
```

#### Seller Profile Endpoints
```
POST   /api/v1/sellers/profile            # Create seller profile
GET    /api/v1/sellers/profile            # Get seller profile
PUT    /api/v1/sellers/profile            # Update seller profile
GET    /api/v1/sellers/{sellerId}         # Get public seller info
```

#### Buyer Profile Endpoints
```
GET    /api/v1/buyers/profile             # Get buyer profile
PUT    /api/v1/buyers/profile             # Update buyer profile
POST   /api/v1/buyers/wishlist/{productId} # Add to wishlist
DELETE /api/v1/buyers/wishlist/{productId} # Remove from wishlist
GET    /api/v1/buyers/wishlist            # Get wishlist items
```

---

### 🛍️ 2. Product Catalog

#### Product Endpoints
```
POST   /api/v1/products                   # Create product (Seller)
GET    /api/v1/products                   # List products (with filters)
GET    /api/v1/products/{productId}       # Get product details
PUT    /api/v1/products/{productId}       # Update product (Seller)
DELETE /api/v1/products/{productId}       # Delete product (Seller)
PATCH  /api/v1/products/{productId}/status # Activate/Deactivate (Seller)
```

#### Category Endpoints
```
GET    /api/v1/categories                 # List all categories
POST   /api/v1/categories                 # Create category (Admin)
GET    /api/v1/categories/{categoryId}    # Get category
PUT    /api/v1/categories/{categoryId}    # Update category (Admin)
DELETE /api/v1/categories/{categoryId}    # Delete category (Admin)
```

---

### 🛒 3. Shopping Cart

#### Cart Endpoints
```
GET    /api/v1/cart                       # Get current user's cart
POST   /api/v1/cart/items                 # Add item to cart
PUT    /api/v1/cart/items/{itemId}        # Update cart item quantity
DELETE /api/v1/cart/items/{itemId}        # Remove item from cart
DELETE /api/v1/cart                       # Clear cart
POST   /api/v1/cart/checkout              # Proceed to checkout
GET    /api/v1/cart/guest/{cartToken}     # Get guest cart
```

---

### 📦 4. Order Management

#### Order Endpoints
```
POST   /api/v1/orders                     # Create order from cart
GET    /api/v1/orders                     # List user's orders
GET    /api/v1/orders/{orderId}           # Get order details
PUT    /api/v1/orders/{orderId}/status    # Update order status (Admin/Seller)
POST   /api/v1/orders/{orderId}/cancel    # Cancel order
POST   /api/v1/orders/{orderId}/return    # Request return
GET    /api/v1/orders/{orderId}/items     # Get order items
```

---

### 💰 5. Payment Processing

#### Payment Endpoints
```
POST   /api/v1/payments                   # Process payment
GET    /api/v1/payments/{paymentId}       # Get payment details
POST   /api/v1/payments/{paymentId}/refund # Initiate refund
GET    /api/v1/payments/methods           # Get available payment methods
GET    /api/v1/payments/history           # Get payment history
```

---

### 🔔 6. Notification System

#### Notification Endpoints
```
GET    /api/v1/notifications              # Get user notifications
PUT    /api/v1/notifications/{notificationId}/read # Mark as read
DELETE /api/v1/notifications/{notificationId} # Delete notification
PUT    /api/v1/notifications/settings     # Update notification preferences
DELETE /api/v1/notifications              # Clear all notifications
```

---

### ⭐ 7. Review System

#### Review Endpoints
```
POST   /api/v1/reviews                    # Submit review
GET    /api/v1/reviews                    # List reviews (with filters)
GET    /api/v1/reviews/{reviewId}         # Get review details
PUT    /api/v1/reviews/{reviewId}         # Update review
DELETE /api/v1/reviews/{reviewId}         # Delete review
GET    /api/v1/products/{productId}/reviews # Get product reviews
```

---

### 🎫 8. Support Ticket

#### Ticket Endpoints
```
POST   /api/v1/tickets                    # Create support ticket
GET    /api/v1/tickets                    # List user tickets
GET    /api/v1/tickets/{ticketId}         # Get ticket details
PUT    /api/v1/tickets/{ticketId}         # Update ticket
POST   /api/v1/tickets/{ticketId}/messages # Add message to ticket
GET    /api/v1/tickets/categories         # Get ticket categories
GET    /api/v1/tickets/priorities         # Get ticket priorities
```

---

## 📋 Request/Response Examples

### User Registration with OIDC Support
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "displayName": "John Doe",
  "role": "BUYER",
  "authenticationType": "LOCAL"  # or "OIDC"
}

Response: 201 Created
{
  "id": "uuid-here",
  "email": "user@example.com",
  "displayName": "John Doe",
  "role": "BUYER",
  "isVerified": false,
  "authenticationType": "LOCAL",
  "createdAt": "2024-01-01T10:00:00Z"
}
```

### Product Creation with Inventory
```http
POST /api/v1/products
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Wireless Headphones",
  "description": "High-quality wireless headphones",
  "price": 99.99,
  "currency": "USD",
  "categoryId": "category-uuid",
  "tags": ["electronics", "audio", "wireless"],
  "images": ["https://example.com/image1.jpg"],
  "inventory": {
    "quantity": 50,
    "lowStockThreshold": 5
  }
}

Response: 201 Created
{
  "id": "product-uuid",
  "name": "Wireless Headphones",
  "price": 99.99,
  "isActive": true,
  "createdAt": "2024-01-01T10:00:00Z"
}
```

### Order Creation with Payment Processing
```http
POST /api/v1/orders
Authorization: Bearer <token>
Content-Type: application/json

{
  "cartId": "cart-uuid",
  "shippingAddress": {
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "USA"
  }
}

Response: 201 Created
{
  "id": "order-uuid",
  "totalAmount": 99.99,
  "currency": "USD",
  "status": "PENDING",
  "paymentStatus": "PENDING",
  "placedAt": "2024-01-01T10:00:00Z"
}
```

### MFA Setup
```http
POST /api/v1/users/mfa/setup
Authorization: Bearer <token>
Content-Type: application/json

{
  "method": "EMAIL"  # Future: "TOTP"
}

Response: 200 OK
{
  "mfaEnabled": true,
  "methods": ["EMAIL"],
  "recoveryCodesGenerated": true
}
```

---

## 🚨 Error Response Format

```json
{
  "timestamp": "2024-01-01T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/users",
  "errors": [
    {
      "field": "email",
      "message": "must be a valid email address"
    }
  ]
}
```

---

## 🔐 Authentication Headers

All protected endpoints require:
```
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

For guest cart operations:
```
X-Guest-Token: <guest-cart-token>
Content-Type: application/json
```

---

## 📈 Rate Limiting

```
Rate Limit: 1000 requests/hour per IP
Rate Limit: 100 requests/minute per authenticated user
Rate Limit: 10 requests/minute for auth endpoints
```

---

## 🔄 API Versioning

```
Current Version: v1
Base URL: /api/v1/
Future versions: /api/v2/, etc.
```

---

## 📊 HTTP Status Codes

| Code | Description |
|------|-------------|
| 200 | OK - Successful GET, PUT, PATCH |
| 201 | Created - Successful POST |
| 204 | No Content - Successful DELETE |
| 400 | Bad Request - Invalid request data |
| 401 | Unauthorized - Authentication required |
| 403 | Forbidden - Access denied |
| 404 | Not Found - Resource not found |
| 409 | Conflict - Resource already exists |
| 422 | Unprocessable Entity - Validation errors |
| 429 | Too Many Requests - Rate limit exceeded |
| 500 | Internal Server Error - Server error |

---