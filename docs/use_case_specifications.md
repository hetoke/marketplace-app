# 🎯 Use Case Specifications

## 👤 1. User Management Use Cases

### UC-001: User Registration
**Actor:** Guest User  
**Preconditions:** None  
**Main Flow:**
1. User visits registration page
2. User enters email, password, and user type (buyer/seller)
3. System validates input data
4. System checks if email already exists
5. System creates user account with pending verification
6. System sends verification email
7. System returns success message

**Alternative Flows:**
- Email already exists → Show error
- Invalid input → Show validation errors

---

### UC-002: User Login with OIDC
**Actor:** Registered User  
**Preconditions:** User account exists  
**Main Flow:**
1. User selects login method (email/password or SSO)
2. If SSO: User selects identity provider
3. System redirects to IdP for authentication
4. User authenticates with IdP
5. IdP returns user claims
6. System creates/syncs user profile from IdP data
7. System generates JWT token
8. System returns user info and token

**Alternative Flows:**
- Local login: User enters email/password → System validates credentials
- If MFA enabled, system prompts for MFA code
- Invalid credentials → Show error
- Account not verified → Redirect to verification
- MFA code invalid → Retry MFA

---

### UC-003: Multi-Factor Authentication Setup
**Actor:** Registered User  
**Preconditions:** User logged in  
**Main Flow:**
1. User navigates to security settings
2. User selects MFA method (Email OTP - TOTP for future)
3. System generates/setup MFA credentials
4. User confirms MFA setup with test code
5. System enables MFA for account
6. System generates backup recovery codes
7. System stores encrypted recovery codes

**Postconditions:** MFA enabled for user account

**Notes:** Mandatory for sellers, optional for buyers

---

### UC-004: Profile Management
**Actor:** Registered User  
**Preconditions:** User logged in  
**Main Flow:**
1. User navigates to profile page
2. User updates profile information
3. System validates and saves changes
4. If using IdP: System syncs with external provider
5. System returns updated profile

---

## 🛍️ 2. Product Management Use Cases

### UC-005: Create Product
**Actor:** Seller  
**Preconditions:** Seller authenticated  
**Main Flow:**
1. Seller navigates to product creation page
2. Seller fills product details (name, description, price, images)
3. Seller selects category and adds tags
4. Seller sets inventory count and low stock threshold
5. Seller configures SEO metadata
6. Seller publishes product
7. System saves product and makes it active
8. System creates associated inventory record

**Alternative Flows:**
- Invalid data → Show validation errors
- Insufficient permissions → Access denied

---

### UC-006: Search Products with Advanced Filtering
**Actor:** Buyer  
**Preconditions:** None  
**Main Flow:**
1. User enters search query or browses categories
2. User applies filters (price range, category, ratings, tags)
3. User uses faceted search with auto-complete
4. User sorts results (price, ratings, popularity, new)
5. System searches product catalog
6. System returns paginated results with filter options

---

### UC-007: Product Review Submission
**Actor:** Buyer  
**Preconditions:** Buyer purchased product  
**Main Flow:**
1. Buyer navigates to product page
2. Buyer selects rating (1-5 stars)
3. Buyer optionally adds review comment
4. Buyer submits review
5. System validates purchase verification
6. System saves review and updates product rating
7. System marks review as verified purchase

---

## 🛒 3. Shopping Cart Use Cases

### UC-008: Add Item to Cart with Quantity Selection
**Actor:** Buyer  
**Preconditions:** Product exists and in stock  
**Main Flow:**
1. User views product details
2. User selects quantity (multiple item selection)
3. User clicks "Add to Cart"
4. System adds item to cart with selected quantity
5. System updates cart total and item count
6. System shows success notification
7. System reserves inventory for cart items

**Alternative Flows:**
- Insufficient inventory → Show availability error

---

### UC-009: Manage Wishlist
**Actor:** Buyer  
**Preconditions:** User authenticated  
**Main Flow:**
1. User views product details
2. User clicks "Add to Wishlist"
3. System adds product to user's wishlist
4. User can view wishlist from account page
5. User can remove items from wishlist
6. User can move wishlist items to cart

---

## 📦 4. Order Management Use Cases

### UC-010: Place Order
**Actor:** Buyer  
**Preconditions:** Cart contains items, user authenticated  
**Main Flow:**
1. User proceeds to checkout
2. User selects/enters shipping address
3. User selects payment method
4. User reviews order details including total amount
5. User confirms order placement
6. System validates inventory availability
7. System creates order record with PENDING status
8. System processes payment
9. System sends order confirmation notification
10. System updates inventory counts

---

### UC-011: Track Order Status
**Actor:** Buyer/Seller  
**Preconditions:** Order exists  
**Main Flow:**
1. User navigates to order history
2. User selects specific order
3. System displays current order status (PENDING → CONFIRMED → DELIVERY → COMPLETE)
4. System shows status timeline with timestamps
5. User can see shipping information and tracking (if available)

---

### UC-012: Cancel Order with Time Window
**Actor:** Buyer  
**Preconditions:** Order in cancellable status (PENDING/CONFIRMED)  
**Main Flow:**
1. User views order details
2. User clicks "Cancel Order" (within allowed time window)
3. System checks cancellation eligibility
4. System prompts for cancellation reason
5. User provides reason and confirms
6. System updates order status to CANCELLED
7. System initiates refund if payment processed
8. System sends cancellation notification to both parties

---

### UC-013: Request Return/Refund
**Actor:** Buyer  
**Preconditions:** Order completed, within return window  
**Main Flow:**
1. Buyer navigates to order details
2. Buyer clicks "Request Return/Refund"
3. Buyer selects reason and adds details
4. System validates return eligibility (time window, condition)
5. System creates return request with PENDING status
6. System notifies seller of return request
7. Seller reviews and approves/denies return
8. System processes refund if approved
9. System updates order status and inventory

---

## 💰 5. Payment Processing Use Cases

### UC-014: Process Payment with Multiple Methods
**Actor:** Payment System  
**Preconditions:** Order created, payment method selected  
**Main Flow:**
1. System validates payment method (Credit Card mock, PayPal Sandbox)
2. System charges selected payment method
3. System handles payment gateway response
4. System updates payment status (SUCCESS/FAILED/PENDING)
5. System creates transaction record
6. System notifies buyer and seller of payment status
7. System reserves funds in escrow until delivery

**Support Methods:** Credit Card (mock), PayPal Sandbox

---

### UC-015: View Payment History
**Actor:** Buyer/Seller  
**Preconditions:** Payment transactions exist  
**Main Flow:**
1. User navigates to payment history section
2. System retrieves user's payment transactions
3. System displays transaction details (amount, method, status, date)
4. User can filter by date range, status
5. User can view refund history
6. Seller can view revenue summary

---

## 🔔 6. Notification Use Cases

### UC-016: Receive Order Status Notifications
**Actor:** User  
**Preconditions:** Order status changes  
**Main Flow:**
1. System detects order status change (Confirmed, Shipped, Delivered, Cancelled)
2. System creates notification record
3. System sends notification via preferred channels:
   - In-app notification
   - Email notification
4. User receives notification
5. User can mark as read or dismiss
6. User can configure notification preferences

---

### UC-017: Receive Security Notifications
**Actor:** User  
**Preconditions:** Security event occurs  
**Main Flow:**
1. System detects security event (new device login, password change)
2. System creates security notification
3. System sends notification via email and in-app
4. User receives security alert with event details
5. User can review recent account activity

---

## ⭐ 7. Review & Rating Use Cases

### UC-018: Submit Product Review with Verification
**Actor:** Buyer  
**Preconditions:** Buyer completed purchase  
**Main Flow:**
1. Buyer navigates to purchased product
2. Buyer rates product (1-5 stars)
3. Buyer optionally writes review comment
4. Buyer submits review
5. System validates purchase history (verified purchase)
6. System saves review and updates product average rating
7. System displays review with verified purchase badge

---

## 🎫 8. Support Ticket Use Cases

### UC-019: Create Support Ticket
**Actor:** User  
**Preconditions:** User authenticated  
**Main Flow:**
1. User navigates to support page
2. User selects ticket category/priority
3. User enters subject and detailed description
4. User can attach files or screenshots
5. User submits ticket
6. System creates ticket record with OPEN status
7. System assigns ticket to support agent
8. System sends confirmation to user
9. System sends notification to assigned agent

---

## 📋 Use Case Matrix

| Use Case ID | Module | Actor | Priority |
|-------------|---------|--------|----------|
| UC-001 | User Management | Guest | High |
| UC-002 | User Management | User | High |
| UC-003 | User Management | User | High |
| UC-004 | User Management | User | Medium |
| UC-005 | Product Catalog | Seller | High |
| UC-006 | Product Catalog | User | High |
| UC-007 | Review System | Buyer | Medium |
| UC-008 | Shopping Cart | User | High |
| UC-009 | User Management | Buyer | Medium |
| UC-010 | Order Management | Buyer | High |
| UC-011 | Order Management | User | High |
| UC-012 | Order Management | Buyer | Medium |
| UC-013 | Order Management | Buyer | Medium |
| UC-014 | Payment Processing | System | High |
| UC-015 | Payment Processing | User | Medium |
| UC-016 | Notification | User | High |
| UC-017 | Notification | User | Medium |
| UC-018 | Review System | Buyer | Medium |
| UC-019 | Support Ticket | User | Medium |

---
