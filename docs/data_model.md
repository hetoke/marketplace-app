## 🧱 Core Entities & Relationships (Data Model)


### 1. **User**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `email` | String (unique) | Email address |
| `password_hash` | String | Encrypted password |
| `role` | Enum (`BUYER`, `SELLER`, `ADMIN`) | User role |
| `is_verified` | Boolean | Email verification status |
| `mfa_enabled` | Boolean | MFA enabled |
| `mfa_methods` | JSON[] | List of MFA methods |
| `display_name` | String | Name from IdP |
| `profile_picture_url` | String | Profile image |
| `last_login_at` | Timestamp | Last login |
| `authentication_type` | Enum (`LOCAL`, `OIDC`, `HYBRID`) | Auth type |
| `default_idp_id` | UUID (FK) | Preferred IdP |
| `recovery_codes` | String[] | Encrypted backup codes |
| `trusted_devices` | JSON | Trusted device tokens |
| `created_at` | Timestamp | Created at |
| `updated_at` | Timestamp | Last updated |

---

### 2. **IdentityProvider**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `name` | String | Provider name (Google, Auth0, etc.) |
| `provider_type` | Enum | Type of provider |
| `issuer_url` | String | OIDC issuer |
| `client_id` | String | OIDC client ID |
| `client_secret` | Encrypted String | Encrypted secret |
| `enabled` | Boolean | Active status |
| `created_at` | Timestamp | Created at |

---

### 3. **UserIdentity**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `user_id` | UUID (FK) | Internal user |
| `provider_id` | UUID (FK) | Identity provider |
| `subject` | String | OIDC `sub` claim |
| `email` | String | Email from provider |
| `name` | String | Display name |
| `picture_url` | String | Profile picture |
| `claims` | JSON | Full OIDC claims |
| `linked_at` | Timestamp | Account linked |
| `last_sync_at` | Timestamp | Last synced |

---

### 4. **OAuthClient**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `client_id` | String | OAuth client ID |
| `client_secret` | Encrypted String | Encrypted secret |
| `name` | String | App name |
| `client_type` | Enum (`CONFIDENTIAL`, `PUBLIC`) | Client type |
| `grant_types` | JSON | Allowed grants |
| `redirect_uris` | JSON | Allowed URIs |
| `scopes` | JSON | Allowed scopes |
| `pkce_required` | Boolean | PKCE enforcement |
| `active` | Boolean | Enabled |
| `created_at` | Timestamp | Created at |

---

### 5. **SellerProfile**
| Field | Type | Description |
|-------|------|-------------|
| `user_id` | UUID (FK) | Reference to User |
| `store_name` | String | Store name |
| `bio` | Text | Description |
| `logo_url` | String | Logo image |
| `payout_method` | JSON | Payout preferences |
| `rating` | Float | Average rating |
| `total_sales` | Integer | Number of sales |
| `joined_at` | Timestamp | Joined date |

---

### 6. **BuyerProfile**
| Field | Type | Description |
|-------|------|-------------|
| `user_id` | UUID (FK) | Reference to User |
| `wishlist` | UUID[] | Product IDs |
| `saved_payment_methods` | JSON | Encrypted payment info |
| `preferred_currency` | String | Default currency |
| `notifications_enabled` | Boolean | Receive notifications |

---

### 7. **Product**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `seller_id` | UUID (FK) | Seller |
| `name` | String | Product name |
| `description` | Text | Description |
| `price` | Decimal | Price |
| `currency` | String | Currency code |
| `category_id` | UUID (FK) | Category |
| `tags` | String[] | Searchable tags |
| `images` | String[] | Image URLs |
| `inventory_count` | Integer | Available quantity |
| `low_stock_alert` | Boolean | Notify when low |
| `is_active` | Boolean | Published or draft |
| `created_at` | Timestamp | Created |
| `updated_at` | Timestamp | Updated |

---

### 8. **Category**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `name` | String | Category name |
| `slug` | String | URL-friendly name |
| `parent_id` | UUID (FK) | Parent category |
| `description` | Text | Optional description |

---

### 9. **Order**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `buyer_id` | UUID (FK) | Buyer |
| `items` | JSON | Product list |
| `total_amount` | Decimal | Total |
| `currency` | String | Currency |
| `status` | Enum (`PENDING`, `CONFIRMED`, `DELIVERY`, `COMPLETE`, `CANCELLED`) | Order status |
| `payment_status` | Enum (`PAID`, `PENDING`, `REFUNDED`) | Payment status |
| `payment_method` | String | Payment method |
| `shipping_address` | JSON | Delivery address |
| `placed_at` | Timestamp | Order placed |
| `delivered_at` | Timestamp | Delivered |
| `cancelled_at` | Timestamp | Cancelled |
| `return_requested` | Boolean | Return requested |
| `return_reason` | Text | Return reason |

---

### 10. **Payment**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `order_id` | UUID (FK) | Order |
| `amount` | Decimal | Amount |
| `currency` | String | Currency |
| `method` | String | Payment method |
| `status` | Enum (`SUCCESS`, `FAILED`, `PENDING`) | Status |
| `transaction_id` | String | Gateway ID |
| `fees` | Decimal | Fees |
| `processed_at` | Timestamp | Processed at |

---

### 11. **Review**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `product_id` | UUID (FK) | Product |
| `buyer_id` | UUID (FK) | Reviewer |
| `rating` | Integer (1-5) | Rating |
| `comment` | Text | Comment |
| `verified_purchase` | Boolean | Verified purchase |
| `created_at` | Timestamp | Posted at |

---

### 12. **Notification**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `user_id` | UUID (FK) | Recipient |
| `type` | Enum (`ORDER_UPDATE`, `SECURITY_ALERT`, `PROMOTIONAL`) | Type |
| `message` | Text | Content |
| `is_read` | Boolean | Read status |
| `created_at` | Timestamp | Sent at |

---

### 13. **Cart**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `user_id` | UUID (FK) | User (nullable for guest) |
| `status` | Enum (`ACTIVE`, `ABANDONED`, `CONVERTED`) | Cart status |
| `created_at` | Timestamp | Created |
| `updated_at` | Timestamp | Updated |
| `expires_at` | Timestamp | Guest cart expiry |

---

### 14. **CartItem**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `cart_id` | UUID (FK) | Cart |
| `product_id` | UUID (FK) | Product |
| `quantity` | Integer | Quantity |
| `unit_price` | Decimal | Price at add time |
| `total_price` | Decimal | Quantity × unit_price |
| `added_at` | Timestamp | Added at |

---

### 15. **Inventory**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `product_id` | UUID (FK) | Product |
| `quantity` | Integer | Total stock |
| `reserved_quantity` | Integer | Reserved stock |
| `available_quantity` | Integer | Available |
| `low_stock_threshold` | Integer | Alert threshold |
| `last_updated` | Timestamp | Last updated |

---

### 16. **SupportTicket**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `user_id` | UUID (FK) | Creator |
| `subject` | String | Subject |
| `description` | Text | Issue description |
| `status` | Enum (`OPEN`, `IN_PROGRESS`, `RESOLVED`) | Ticket status |
| `priority` | Enum (`LOW`, `MEDIUM`, `HIGH`) | Priority |
| `assigned_to` | UUID (FK) | Admin |
| `created_at` | Timestamp | Created |
| `resolved_at` | Timestamp | Resolved |

---

## 🔗 Core Relationships

```plaintext
[User] 1--1 [SellerProfile]
[User] 1--1 [BuyerProfile]
[User] 1--* [Cart]
[Cart] 1--* [CartItem]
[CartItem] *--1 [Product]
[User] 1--* [Order]
[Order] 1--* [OrderItem]
[Order] 1--1 [Payment]
[Product] *--1 [Category]
[Product] 1--1 [Inventory]
[Product] 1--* [Review]
[Order] 1--* [Notification]
[User] 1--* [SupportTicket]
```

