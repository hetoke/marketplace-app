# Authentication Security Flow

## Login Flow

```mermaid
flowchart TD
    A[POST /api/v1/auth/login] --> B{User exists?}
    B -- No --> C[❌ Invalid email or password]
    B -- Yes --> D{Authentication type?}
    D -- OIDC --> C
    D -- LOCAL / HYBRID --> E{Password matches?}
    E -- No --> C
    E -- Yes --> F{Email verified?}
    F -- No --> G[❌ Email verification required]
    F -- Yes --> H{MFA enabled?}
    H -- Yes --> I[Send OTP to email]
    I --> J[Return MFA token]
    J --> K{OTP verified?}
    K -- No --> L{Failed attempts >= 5?}
    L -- Yes --> M[🔒 Account locked 15 min]
    L -- No --> K
    K -- Yes --> N[✅ Issue access + refresh tokens]
    H -- No --> N
```

## Password Reset Flow

```mermaid
flowchart TD
    A[POST /api/v1/auth/forgot-password] --> B{User exists?}
    B -- No --> C[Generic response: If email exists...]
    B -- Yes --> D{Authentication type?}
    D -- OIDC --> C
    D -- LOCAL / HYBRID --> E[Generate reset token]
    E --> F[Send reset email]
    F --> C

    G[POST /api/v1/auth/reset-password] --> H{Token valid?}
    H -- No --> I[❌ Invalid reset token]
    H -- Yes --> J{Token expired?}
    J -- Yes --> K[❌ Token expired]
    J -- No --> L{User is OIDC?}
    L -- Yes --> M[❌ Not available for Google accounts]
    L -- No --> N[Encode new password]
    N --> O[Revoke all refresh tokens]
    O --> P[Send password change notification]
    P --> Q[✅ Password reset successfully]
```

## Change Password Flow (Authenticated)

```mermaid
flowchart TD
    A[PUT /api/v1/users/profile/password] --> B{Rate limit OK?}
    B -- No --> C[❌ 429 Too Many Requests]
    B -- Yes --> D{User is OIDC?}
    D -- Yes --> E[❌ Not available for Google accounts]
    D -- No --> F{Current password correct?}
    F -- No --> G[❌ Current password is incorrect]
    F -- Yes --> H{New password = current?}
    H -- Yes --> I[❌ Must be different from current]
    H -- No --> J{Complexity OK?}
    J -- No --> K[❌ Uppercase + lowercase + digit + special required]
    J -- Yes --> L[Encode new password]
    L --> M[Revoke all refresh tokens]
    M --> N[Send password change email]
    N --> O[✅ Password changed successfully]
```

## OIDC Login Flow

```mermaid
sequenceDiagram
    participant U as User
    participant FE as Frontend
    participant BE as Backend
    participant G as Google
    participant DB as Database

    U->>FE: Click "Sign in with Google"
    FE->>BE: GET /api/v1/auth/oidc/login
    BE->>BE: Generate state token (CSRF)
    BE-->>FE: Redirect to Google OAuth

    FE->>G: User authenticates with Google
    G-->>FE: Authorization code + state

    FE->>BE: GET /api/v1/auth/oidc/callback?code=...&state=...
    BE->>BE: Validate state token
    BE->>G: Exchange code for tokens
    G-->>BE: ID token + access token
    BE->>BE: Parse ID token (email, name, sub)

    BE->>DB: Find or create user
    alt User exists
        DB-->>BE: Return existing user
    else New user
        BE->>DB: Create user (OIDC type, random password hash)
        DB-->>BE: Return new user
    end

    BE->>DB: Store refresh token (7 day expiry)
    BE->>BE: Generate one-time code (60s expiry)
    BE-->>FE: Redirect with one-time code

    FE->>BE: POST /api/v1/auth/oidc/token {code}
    BE->>BE: Exchange one-time code
    BE-->>FE: Access token + Refresh token + User
```

## MFA Brute-Force Protection

```mermaid
flowchart TD
    A[OTP verification attempt] --> B{Account locked?}
    B -- Yes, locked until T --> C[❌ Try again in X seconds]
    B -- No --> D{OTP correct?}
    D -- Yes --> E[Reset failed attempts = 0]
    E --> F[✅ OTP verified]
    D -- No --> G[Increment failed attempts]
    G --> H{Failed >= 5?}
    H -- Yes --> I[Lock account for 15 min]
    I --> J[❌ Invalid or expired OTP]
    H -- No --> J
```

## Registration Flow (Enumeration Protected)

```mermaid
flowchart TD
    A[POST /api/v1/auth/register] --> B{Role valid?}
    B -- No --> C[❌ Invalid role]
    B -- Yes --> D{Email already exists?}
    D -- Yes --> E[Return null, same response]
    D -- No --> F[Create user, LOCAL type]
    F --> G[Send verification email]
    E --> H[201 Created: If email not registered...]
    G --> H
```
