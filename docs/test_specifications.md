# 🧪 Test Specifications

> Generated 2026-07-26, updated 2026-08-06 by reverse-engineering `src/test/java/com/marketplace/**`.
> This document is **descriptive, not aspirational** — every case listed below exists
> in the codebase today. Test method names are quoted verbatim so each row is
> independently verifiable.
>
> Companion documents: `api_specifications.md` §A (endpoint surface),
> `use_case_specifications.md` §C (use case status), `data_model.md` §B (schema).

---

## 1. Suite at a glance

| | |
|---|---|
| Test classes | **28** |
| Test methods (`@Test`) | **356** |
| Lines of test code | 6,181 |
| Framework | JUnit 5 (Jupiter) + Mockito + AssertJ + MockMvc (standalone) |
| Runner | `mvn test` (Surefire, via `spring-boot-starter-test`) |
| CI | `.github/workflows/ci.yml` — `mvn -B test` on push/PR to `main`, JDK 25 |

### Distribution by layer

| Layer | Classes | Cases | Share |
|---|---:|---:|---:|
| Service (unit) | 13 | 174 | 48.9% |
| Controller (web layer) | 11 | 174 | 48.9% |
| Repository | 1 | 8 | 2.2% |
| **Total** | **25** | **356** | **100%** |

> **Note:** `MarketplaceApplicationTests.contextLoads()` is now `@Disabled` and
> contributes 0 active cases. `SecurityIntegrationTest` (23 cases) exercises the full
> Spring Security filter chain and is counted separately in §9.

### Distribution by module

| Module | Classes | Cases |
|---|---:|---:|
| `user` (auth, profile, MFA) | 4 | 113 |
| `product` (catalog, categories) | 4 | 81 |
| `upload` (media) | 4 | 35 |
| `order` | 2 | 28 |
| `cart` | 3 | 29 |
| `payment` | 2 | 24 |
| `wishlist` | 2 | 15 |
| `notification` | 1 | 10 |
| `review` | 1 | 9 |
| `webhook` | 2 | 9 |
| `shared/security` (integration + unit) | 2 | 41 |

---

## 2. Test harness anatomy — read this before trusting a result

The suite uses **one** pattern almost everywhere: `@ExtendWith(MockitoExtension.class)`
with hand-injected mocks. There is **no** `@WebMvcTest`, no `@DataJpaTest`, and only a
single `@SpringBootTest`. This has concrete consequences for what the 356 cases do and
do not prove.

### 2.1 Service tests

```java
@ExtendWith(MockitoExtension.class)
class CartServiceTest {
    @Mock  private CartRepository cartRepository;
    @Mock  private ProductRepository productRepository;
    @InjectMocks private CartService cartService;
}
```

Pure unit tests. All collaborators mocked; assertions via AssertJ (`assertThat`,
`assertThatThrownBy`) plus Mockito `verify(...)`. **No database, no Spring context, no
transactions.** These are the highest-value cases in the suite — business rules,
branch coverage, exception paths.

### 2.2 Controller tests

```java
mockMvc = MockMvcBuilders.standaloneSetup(cartController)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(userId, null, List.of()));
```

**Standalone MockMvc** — the controller is instantiated directly with a mocked service,
`GlobalExceptionHandler` is registered manually, and the principal is stuffed into
`SecurityContextHolder` by hand.

What this **does** exercise:
- Request mapping, path variables, query parameter binding
- JSON deserialisation and **Bean Validation** (`@Valid` runs; the ~90 `returns400`
  cases are genuine)
- `GlobalExceptionHandler` mapping of domain exceptions to HTTP status
- Response body shape via `jsonPath` on the `ApiResponse` envelope
- HTTP method negotiation (`returns405`) and content-type negotiation (`returns415`)

What this does **not** exercise — because no filter chain and no Spring Security method
interceptor are present:

| Not covered | Why |
|---|---|
| `@PreAuthorize` role rules | Method security interceptor is absent — annotations are inert |
| `PermissionService` ownership checks (`isOwnerOfOrder`, `isOwnerOfProduct`, …) | Same — the SpEL is never evaluated |
| `JwtAuthenticationFilter` / `JwtSecurityContextRepository` | Not in the chain; principal is injected manually |
| `SecurityConfig` `permitAll` matchers | Not in the chain |
| `RateLimitFilter` | Not in the chain |
| CORS, CSRF configuration | Not in the chain |

> ⚠️ **The `returns403` cases do not test authorization.** `updateProduct_sellerMismatch_returns403`
> and `requestProductImages_notSeller_returns403` pass because the *mocked service* is
> told to throw `AccessDeniedException`, which `GlobalExceptionHandler` maps to 403.
> They verify the exception handler, not the access rule. **No test in this suite
> verifies that any endpoint is actually protected.** See §9.1.

### 2.3 Repository test

`ImageRepositoryTest` also uses `@ExtendWith(MockitoExtension.class)` and declares
`@Mock private ImageRepository imageRepository`, then stubs a method and calls that same
mock:

```java
when(imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(USER, USER_ID))
        .thenReturn(List.of(img1, img2, img3));
List<Image> results = imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(USER, USER_ID);
assertThat(results).hasSize(3);
```

> ⚠️ These 8 cases are **tautological** — they assert that Mockito returns what it was
> configured to return. No derived query, JPQL, ordering, pagination or cascade is
> validated, because no persistence provider is involved. Named `…RepositoryTest` but
> functionally a DTO-shape test. Converting to `@DataJpaTest` would make them real —
> see §12.2.

### 2.4 Application context test

`MarketplaceApplicationTests.contextLoads()` — previously the only `@SpringBootTest`,
running under `@ActiveProfiles("test")`. This class is now **`@Disabled`** with the
message "Requires full application context with database" and contributes **0 active
cases**. It was the sole consumer of the H2 configuration in `application.yml`.

### 2.5 Security integration test (new)

`SecurityIntegrationTest` — a full `@SpringBootTest` + `@ActiveProfiles("test")` that
boots the complete Spring Security filter chain. Uses `MockMvc` with
`webAppContextSetup(…).apply(springSecurity())` and real JWT tokens generated by
`JwtTokenProvider`. This is the **only** test class that proves:

- `JwtAuthenticationFilter` rejects missing / malformed / expired / tampered tokens
- `SecurityConfig` `permitAll` matchers work (public endpoints return 200 without auth)
- Role-based `@PreAuthorize` rules enforced (buyer ≠ seller ≠ admin)
- Refresh token rotation (valid / invalid / expired / deleted-user scenarios)
- Logout invalidates the refresh token

What this does **not** cover: `RateLimitFilter` (disabled in test profile), CORS, CSRF,
`PermissionService` ownership checks (tested only at the service layer).

### 2.6 Test doubles for external systems

| External system | How it is handled |
|---|---|
| PostgreSQL | Never contacted. Repositories mocked. H2 used only by §2.4 (now `@Disabled`). |
| Redis (cache + rate limit) | Never contacted. |
| SePay | `SePayService` mocked in `PaymentServiceTest`. |
| Supabase Storage | `SupabaseStorageClient` mocked in `UploadServiceTest` / `ImageServiceTest`. |
| SMTP / email | `EmailService` mocked wherever injected. |

> ⚠️ `com.github.tomakehurst:wiremock-jre8-standalone:2.35.2` is declared in `pom.xml`
> at test scope but is **never imported or used** anywhere in `src/test`. No HTTP-level
> stubbing exists; outbound calls are cut at the service-interface boundary. Either
> adopt WireMock for real SePay/Supabase contract tests or drop the dependency.

### 2.7 Case ID scheme used in this document

`TC-<MODULE>-<LAYER>-<nn>` — e.g. `TC-CART-S-05` is the 5th cart service case.
Layer codes: **S** = service, **C** = controller, **R** = repository, **I** = integration.
IDs are assigned in source order within each class and are stable for reference; they
do not exist in the code itself.

### 2.8 JwtTokenProvider unit test (new)

`JwtTokenProviderTest` — a plain JUnit 5 class with **no Spring context, no mocks**.
The `JwtTokenProvider` is instantiated directly with a test `@Value`-style secret via
`@BeforeEach`. Covers:

- Token generation (access, refresh, MFA) with correct claims
- Token validation (valid, expired, wrong key, malformed, tampered, empty, null)
- Subject extraction from access and refresh tokens
- Unique `jti` per refresh token

This is the **only** test for the class that signs and verifies every JWT in the
system. Previously listed in §12.1 as the highest-risk untested component.

---

## 3. User module — 113 cases

### 3.1 `AuthServiceTest` — 23 cases (`user/service/AuthServiceTest.java`)

Mocks: `UserRepository`, `RefreshTokenRepository`, `VerificationTokenRepository`,
`PasswordEncoder`, `JwtTokenProvider`, `EmailService`, `MFAService`.

| ID | Test method | Scenario | Expected |
|---|---|---|---|
| TC-AUTH-S-01 | `register_roleBuyer_success` | Register with `role = BUYER` | User persisted, verification email sent |
| TC-AUTH-S-02 | `register_roleSeller_success` | Register with `role = SELLER` | User persisted as seller |
| TC-AUTH-S-03 | `register_roleAdmin_throwsBusinessException` | Attempt self-registration as `ADMIN` | `BusinessException` — privilege escalation blocked |
| TC-AUTH-S-04 | `register_passwordLowerBound8chars_success` | Password exactly 8 chars | Accepted (lower bound) |
| TC-AUTH-S-05 | `register_passwordUpperBound128chars_success` | Password exactly 128 chars | Accepted (upper bound) |
| TC-AUTH-S-06 | `register_displayNameLowerBound1char_success` | Display name 1 char | Accepted |
| TC-AUTH-S-07 | `register_displayNameUpperBound255chars_success` | Display name 255 chars | Accepted |
| TC-AUTH-S-08 | `register_duplicateEmail_throwsBusinessException` | Email already registered | `BusinessException` |
| TC-AUTH-S-09 | `login_success` | Valid credentials, verified account | Access + refresh tokens issued |
| TC-AUTH-S-10 | `login_wrongPassword_throwsBusinessException` | Bad password | `BusinessException` |
| TC-AUTH-S-11 | `login_userNotFound_throwsBusinessException` | Unknown email | `BusinessException` (same shape as TC-10 — no user enumeration) |
| TC-AUTH-S-12 | `login_unverifiedEmail_throwsBusinessException` | Account not email-verified | `BusinessException` |
| TC-AUTH-S-13 | `refreshToken_success` | Valid refresh token | New token pair issued |
| TC-AUTH-S-14 | `refreshToken_expired_throwsBusinessException` | `expires_at` in the past | `BusinessException` |
| TC-AUTH-S-15 | `refreshToken_invalidToken_throwsBusinessException` | Token not in store | `BusinessException` |
| TC-AUTH-S-16 | `verifyEmail_success` | Valid verification token | `is_verified = true` |
| TC-AUTH-S-17 | `verifyEmail_expiredToken_throwsBusinessException` | Expired token | `BusinessException` |
| TC-AUTH-S-18 | `verifyEmail_invalidToken_throwsBusinessException` | Unknown token | `BusinessException` |
| TC-AUTH-S-19 | `forgotPassword_existingEmail_success` | Known email | Reset token created, email sent |
| TC-AUTH-S-20 | `forgotPassword_nonExistingEmail_success` | Unknown email | **Succeeds silently** — deliberate anti-enumeration behaviour |
| TC-AUTH-S-21 | `resetPassword_success` | Valid reset token + new password | Password hash replaced |
| TC-AUTH-S-22 | `resetPassword_expiredToken_throwsBusinessException` | Expired token | `BusinessException` |
| TC-AUTH-S-23 | `resetPassword_invalidToken_throwsBusinessException` | Unknown token | `BusinessException` |

**Security-relevant coverage:** TC-03 (role escalation), TC-11 + TC-20 (user
enumeration resistance), TC-14/17/22 (token expiry).

### 3.2 `AuthControllerTest` — 45 cases (largest class in the suite)

Mocks: `AuthService`. Harness per §2.2. Heavy emphasis on input validation and HTTP
protocol conformance.

**`POST /api/v1/auth/register` — 23 cases**

| ID | Test method | Expected |
|---|---|---|
| TC-AUTH-C-01 | `register_roleBuyer_success` | 201 |
| TC-AUTH-C-02 | `register_roleSeller_success` | 201 |
| TC-AUTH-C-03 | `register_roleAdmin_returns400` | 400 |
| TC-AUTH-C-04 | `register_passwordLowerBound8chars_success` | 201 (boundary) |
| TC-AUTH-C-05 | `register_passwordUpperBound128chars_success` | 201 (boundary) |
| TC-AUTH-C-06 | `register_passwordBelowLowerBound7chars_returns400` | 400 (boundary −1) |
| TC-AUTH-C-07 | `register_passwordAboveUpperBound129chars_returns400` | 400 (boundary +1) |
| TC-AUTH-C-08 | `register_emptyPassword_returns400` | 400 |
| TC-AUTH-C-09 | `register_blankPassword_returns400` | 400 (whitespace only) |
| TC-AUTH-C-10 | `register_displayNameLowerBound1char_success` | 201 (boundary) |
| TC-AUTH-C-11 | `register_displayNameUpperBound255chars_success` | 201 (boundary) |
| TC-AUTH-C-12 | `register_displayNameBelowLowerBound0chars_returns400` | 400 (boundary −1) |
| TC-AUTH-C-13 | `register_displayNameAboveUpperBound256chars_returns400` | 400 (boundary +1) |
| TC-AUTH-C-14 | `register_blankDisplayName_returns400` | 400 |
| TC-AUTH-C-15 | `register_duplicateEmail_returns400` | 400 |
| TC-AUTH-C-16 | `register_invalidEmailNoAt_returns400` | 400 |
| TC-AUTH-C-17 | `register_invalidEmailNoDomain_returns400` | 400 |
| TC-AUTH-C-18 | `register_missingEmail_returns400` | 400 |
| TC-AUTH-C-19 | `register_emptyEmail_returns400` | 400 |
| TC-AUTH-C-20 | `register_malformedJson_returns400` | 400 |
| TC-AUTH-C-21 | `register_emptyBody_returns400` | 400 |
| TC-AUTH-C-22 | `register_wrongContentType_returns415` | **415** |
| TC-AUTH-C-23 | `register_getMethodNotAllowed_returns405` | **405** |

**`POST /api/v1/auth/login` — 8 cases**

| ID | Test method | Expected |
|---|---|---|
| TC-AUTH-C-24 | `login_success` | 200 + token payload |
| TC-AUTH-C-25 | `login_invalidCredentials_returns400` | 400 |
| TC-AUTH-C-26 | `login_missingPassword_returns400` | 400 |
| TC-AUTH-C-27 | `login_emptyEmail_returns400` | 400 |
| TC-AUTH-C-28 | `login_emptyPassword_returns400` | 400 |
| TC-AUTH-C-29 | `login_malformedJson_returns400` | 400 |
| TC-AUTH-C-30 | `login_wrongContentType_returns415` | 415 |
| TC-AUTH-C-31 | `login_getMethodNotAllowed_returns405` | 405 |

**Token, verification and password-reset endpoints — 14 cases**

| ID | Test method | Endpoint | Expected |
|---|---|---|---|
| TC-AUTH-C-32 | `refreshToken_success` | `POST /auth/refresh` | 200 |
| TC-AUTH-C-33 | `refreshToken_emptyToken_returns400` | `POST /auth/refresh` | 400 |
| TC-AUTH-C-34 | `verifyEmail_success` | `POST /auth/verify-email` | 200 |
| TC-AUTH-C-35 | `verifyEmail_emptyToken_returns400` | `POST /auth/verify-email` | 400 |
| TC-AUTH-C-36 | `verifyEmail_missingToken_returns400` | `POST /auth/verify-email` | 400 |
| TC-AUTH-C-37 | `forgotPassword_success` | `POST /auth/forgot-password` | 200 |
| TC-AUTH-C-38 | `forgotPassword_invalidEmail_returns400` | `POST /auth/forgot-password` | 400 |
| TC-AUTH-C-39 | `forgotPassword_emptyEmail_returns400` | `POST /auth/forgot-password` | 400 |
| TC-AUTH-C-40 | `forgotPassword_missingEmail_returns400` | `POST /auth/forgot-password` | 400 |
| TC-AUTH-C-41 | `resetPassword_success` | `POST /auth/reset-password` | 200 |
| TC-AUTH-C-42 | `resetPassword_shortPassword_returns400` | `POST /auth/reset-password` | 400 |
| TC-AUTH-C-43 | `resetPassword_longPassword_returns400` | `POST /auth/reset-password` | 400 |
| TC-AUTH-C-44 | `resetPassword_emptyToken_returns400` | `POST /auth/reset-password` | 400 |
| TC-AUTH-C-45 | `resetPassword_missingToken_returns400` | `POST /auth/reset-password` | 400 |

> **Not covered by this class:** `POST /auth/logout`, `POST /auth/resend-verification`,
> `GET /auth/oidc/login`, `GET /auth/oidc/callback`, `POST /auth/oidc/token`,
> `POST /auth/mfa/verify`, `POST /auth/mfa/recovery` — 7 of the 13 `AuthController`
> endpoints have zero controller tests.

### 3.3 `UserServiceTest` — 25 cases

| ID | Test method | Scenario | Expected |
|---|---|---|---|
| TC-USER-S-01 | `getProfile_success` | Existing user | Profile DTO returned |
| TC-USER-S-02 | `getProfile_notFound_throwsResourceNotFoundException` | Unknown id | `ResourceNotFoundException` |
| TC-USER-S-03 | `updateProfile_displayNameLowerBound1char_success` | 1 char | Accepted |
| TC-USER-S-04 | `updateProfile_displayNameUpperBound255chars_success` | 255 chars | Accepted |
| TC-USER-S-05 | `updateProfile_bothFieldsNull_success` | All fields null | No-op update succeeds (partial-update semantics) |
| TC-USER-S-06 | `updateProfile_notFound_throwsResourceNotFoundException` | Unknown id | `ResourceNotFoundException` |
| TC-USER-S-07 | `changePassword_success` | Correct current password | Hash replaced |
| TC-USER-S-08 | `changePassword_newPasswordLowerBound8chars_success` | 8 chars | Accepted |
| TC-USER-S-09 | `changePassword_newPasswordUpperBound128chars_success` | 128 chars | Accepted |
| TC-USER-S-10 | `changePassword_wrongCurrentPassword_throwsBusinessException` | Wrong current password | `BusinessException` |
| TC-USER-S-11 | `changePassword_notFound_throwsResourceNotFoundException` | Unknown id | `ResourceNotFoundException` |
| TC-USER-S-12 | `changePassword_oidcUser_throwsBusinessException` | OIDC-linked user | `BusinessException` — password change blocked for non-local accounts |
| TC-USER-S-13 | `changePassword_samePassword_throwsBusinessException` | New password equals current | `BusinessException` — no-op password change blocked |
| TC-USER-S-14 | `user_verifiedTrue_returnsTrue` | Entity accessor | `true` |
| TC-USER-S-15 | `user_verifiedFalse_returnsFalse` | Entity accessor | `false` |
| TC-USER-S-16 | `user_mfaEnabledTrue_returnsTrue` | Entity accessor | `true` |
| TC-USER-S-17 | `user_mfaEnabledFalse_returnsFalse` | Entity accessor | `false` |
| TC-USER-S-18 | `user_roleBuyer` | Enum round-trip | `Role.BUYER` |
| TC-USER-S-19 | `user_roleSeller` | Enum round-trip | `Role.SELLER` |
| TC-USER-S-20 | `user_roleAdmin` | Enum round-trip | `Role.ADMIN` |
| TC-USER-S-21 | `user_authenticationTypeLocal` | Enum round-trip | `LOCAL` |
| TC-USER-S-22 | `user_authenticationTypeOidc` | Enum round-trip | `OIDC` |
| TC-USER-S-23 | `user_authenticationTypeHybrid` | Enum round-trip | `HYBRID` |

> ⚠️ TC-14 → TC-23 (10 of 25 cases, 40% of this class) are **entity getter/setter
> assertions**, not service behaviour. They inflate the case count without covering
> logic. Low-value but harmless.

### 3.4 `UserControllerTest` — 24 cases

| ID | Test method | Endpoint | Expected |
|---|---|---|---|
| TC-USER-C-01 | `getProfile_success` | `GET /users/profile` | 200 |
| TC-USER-C-02 | `getProfile_notFound_returns404` | `GET /users/profile` | 404 |
| TC-USER-C-03 | `getProfile_postMethodNotAllowed_returns405` | `POST /users/profile` | 405 |
| TC-USER-C-04 | `updateProfile_success` | `PUT /users/profile` | 200 |
| TC-USER-C-05 | `updateProfile_displayNameLowerBound1char_success` | `PUT /users/profile` | 200 (boundary) |
| TC-USER-C-06 | `updateProfile_displayNameUpperBound255chars_success` | `PUT /users/profile` | 200 (boundary) |
| TC-USER-C-07 | `updateProfile_displayNameBelowLowerBound0chars_returns400` | `PUT /users/profile` | 400 |
| TC-USER-C-08 | `updateProfile_displayNameAboveUpperBound256chars_returns400` | `PUT /users/profile` | 400 |
| TC-USER-C-09 | `updateProfile_bothFieldsNull_success` | `PUT /users/profile` | 200 |
| TC-USER-C-10 | `updateProfile_notFound_returns404` | `PUT /users/profile` | 404 |
| TC-USER-C-11 | `updateProfile_malformedJson_returns400` | `PUT /users/profile` | 400 |
| TC-USER-C-12 | `updateProfile_postMethodNotAllowed_returns405` | `POST /users/profile` | 405 |
| TC-USER-C-13 | `changePassword_success` | `PUT /users/profile/password` | 200 |
| TC-USER-C-14 | `changePassword_newPasswordLowerBound8chars_success` | | 200 (boundary) |
| TC-USER-C-15 | `changePassword_newPasswordUpperBound128chars_success` | | 200 (boundary) |
| TC-USER-C-16 | `changePassword_newPasswordBelowLowerBound7chars_returns400` | | 400 |
| TC-USER-C-17 | `changePassword_newPasswordAboveUpperBound129chars_returns400` | | 400 |
| TC-USER-C-18 | `changePassword_wrongCurrentPassword_returns400` | | 400 |
| TC-USER-C-19 | `changePassword_notFound_returns404` | | 404 |
| TC-USER-C-20 | `changePassword_emptyCurrentPassword_returns400` | | 400 |
| TC-USER-C-21 | `changePassword_emptyNewPassword_returns400` | | 400 |
| TC-USER-C-22 | `changePassword_missingBothPasswords_returns400` | | 400 |
| TC-USER-C-23 | `changePassword_malformedJson_returns400` | | 400 |
| TC-USER-C-24 | `changePassword_getMethodNotAllowed_returns405` | | 405 |

> **Not covered:** all 5 MFA endpoints on `UserController`
> (`/mfa/setup`, `/mfa/verify`, `/mfa`, `/mfa/disable/send-otp`, `/mfa/status`).

---

## 4. Product module — 82 cases

### 4.1 `ProductServiceTest` — 22 cases

Mocks: `ProductRepository`, `CategoryRepository`, `ProductImageRepository`,
`DiscountService` (behaviour of the discount calculation itself is **not** tested — see §12.4).

| ID | Test method | Scenario | Expected |
|---|---|---|---|
| TC-PROD-S-01 | `createProduct_success` | Valid request | Product persisted, slug generated |
| TC-PROD-S-02 | `createProduct_success_appendsUuidOnSlugCollision` | Slug already exists | UUID suffix appended — uniqueness preserved |
| TC-PROD-S-03 | `createProduct_categoryNotFound_throwsResourceNotFoundException` | Unknown category | `ResourceNotFoundException` |
| TC-PROD-S-04 | `updateProduct_success` | Owner updates own product | Fields updated |
| TC-PROD-S-05 | `updateProduct_productNotFound_throwsResourceNotFoundException` | Unknown product | `ResourceNotFoundException` |
| TC-PROD-S-06 | `updateProduct_sellerMismatch_throwsAccessDeniedException` | Different seller | `AccessDeniedException` — **service-level ownership check** |
| TC-PROD-S-07 | `updateProduct_categoryNotFound_throwsResourceNotFoundException` | Unknown new category | `ResourceNotFoundException` |
| TC-PROD-S-08 | `deleteProduct_success` | Owner deletes | Product removed |
| TC-PROD-S-09 | `deleteProduct_productNotFound_throwsResourceNotFoundException` | Unknown product | `ResourceNotFoundException` |
| TC-PROD-S-10 | `deleteProduct_sellerMismatch_throwsAccessDeniedException` | Different seller | `AccessDeniedException` |
| TC-PROD-S-11 | `getProductById_success` | Existing product | DTO returned |
| TC-PROD-S-12 | `getProductById_withImages` | Product with images | Images included in response |
| TC-PROD-S-13 | `getProductById_productNotFound_throwsResourceNotFoundException` | Unknown id | `ResourceNotFoundException` |
| TC-PROD-S-14 | `searchProducts_success` | Filters supplied | Paged results |
| TC-PROD-S-15 | `searchProducts_emptyResults` | No matches | Empty page, not an error |
| TC-PROD-S-16 | `searchProducts_defaultPagination` | No page/size given | Defaults applied (page 0, size 20) |
| TC-PROD-S-17 | `searchProducts_customPagination` | Explicit page/size | Honoured |
| TC-PROD-S-18 | `getProductsByCategory_success` | Category filter | Paged results |
| TC-PROD-S-19 | `getSellerProducts_success` | Seller's own listing | Only that seller's products |
| TC-PROD-S-20 | `createProduct_nameWithSpecialChars_generatesCleanSlug` | `"Tên & Sản/Phẩm!"` style input | Special chars stripped from slug |
| TC-PROD-S-21 | `createProduct_nameWithSpaces_generatesHyphens` | Spaces in name | Spaces → hyphens |
| TC-PROD-S-22 | `createProduct_nameWithConsecutiveHyphens_collapsesHyphens` | `"a  --  b"` | Consecutive hyphens collapsed |

**Slug generation** (TC-02, TC-20, TC-21, TC-22) is the best-covered single algorithm
in the codebase — 4 dedicated cases including collision handling.

### 4.2 `ProductControllerTest` — 27 cases

| ID | Test method | Expected |
|---|---|---|
| TC-PROD-C-01 | `createProduct_success_returns201` | 201 |
| TC-PROD-C-02 | `createProduct_blankName_returns400` | 400 |
| TC-PROD-C-03 | `createProduct_nameBelowLowerBound0chars_returns400` | 400 (boundary −1) |
| TC-PROD-C-04 | `createProduct_nameAboveUpperBound256chars_returns400` | 400 (boundary +1) |
| TC-PROD-C-05 | `createProduct_nullCategoryId_returns400` | 400 |
| TC-PROD-C-06 | `createProduct_nullPrice_returns201` | 201 — price is optional (nullable) |
| TC-PROD-C-07 | `createProduct_negativePrice_returns400` | 400 |
| TC-PROD-C-08 | `createProduct_zeroPrice_returns400` | 400 — price must be strictly positive |
| TC-PROD-C-09 | `createProduct_nullStock_returns201` | 201 — stock is optional (nullable) |
| TC-PROD-C-10 | `createProduct_negativeStock_returns400` | 400 |
| TC-PROD-C-11 | `createProduct_categoryNotFound_returns404` | 404 |
| TC-PROD-C-12 | `createProduct_malformedJson_returns400` | 400 |
| TC-PROD-C-13 | `createProduct_postMethodNotAllowed_returns405` | 405 |
| TC-PROD-C-14 | `searchProducts_success_returns200` | 200 |
| TC-PROD-C-15 | `searchProducts_withQueryParams_returns200` | 200, params bound |
| TC-PROD-C-16 | `searchProducts_putMethodNotAllowed_returns405` | 405 |
| TC-PROD-C-17 | `getProductById_success_returns200` | 200 |
| TC-PROD-C-18 | `getProductById_notFound_returns404` | 404 |
| TC-PROD-C-19 | `getProductById_postMethodNotAllowed_returns405` | 405 |
| TC-PROD-C-20 | `updateProduct_success_returns200` | 200 |
| TC-PROD-C-21 | `updateProduct_notFound_returns404` | 404 |
| TC-PROD-C-22 | `updateProduct_sellerMismatch_returns403` | 403 (via handler — see §2.2 warning) |
| TC-PROD-C-23 | `updateProduct_blankName_returns400` | 400 |
| TC-PROD-C-24 | `updateProduct_malformedJson_returns400` | 400 |
| TC-PROD-C-25 | `deleteProduct_success_returns200` | 200 |
| TC-PROD-C-26 | `deleteProduct_notFound_returns404` | 404 |
| TC-PROD-C-27 | `deleteProduct_sellerMismatch_returns403` | 403 |

> **Not covered:** `DELETE /products/{productId}/images/{imageId}`; no discount-field
> validation cases (percent 0–100, fixed > 0, end > start — see §12.4).

### 4.3 `CategoryServiceTest` — 16 cases

| ID | Test method | Scenario | Expected |
|---|---|---|---|
| TC-CAT-S-01 | `createCategory_success` | Valid root category | Persisted |
| TC-CAT-S-02 | `createCategory_withParent` | Valid child category | Parent linked |
| TC-CAT-S-03 | `createCategory_slugAlreadyExists_throwsBusinessException` | Duplicate slug | `BusinessException` |
| TC-CAT-S-04 | `createCategory_parentNotFound_throwsResourceNotFoundException` | Unknown parent | `ResourceNotFoundException` |
| TC-CAT-S-05 | `updateCategory_success` | Valid change | Updated |
| TC-CAT-S-06 | `updateCategory_categoryNotFound_throwsResourceNotFoundException` | Unknown id | `ResourceNotFoundException` |
| TC-CAT-S-07 | `updateCategory_slugAlreadyExistsDifferentCategory_throwsBusinessException` | Slug owned by another row | `BusinessException` (own slug allowed) |
| TC-CAT-S-08 | `updateCategory_selfParenting_throwsBusinessException` | Category set as its own parent | `BusinessException` — **cycle prevention** |
| TC-CAT-S-09 | `updateCategory_parentNotFound_throwsResourceNotFoundException` | Unknown parent | `ResourceNotFoundException` |
| TC-CAT-S-10 | `updateCategory_clearParent_success` | Parent set to null | Promoted to root |
| TC-CAT-S-11 | `getCategoryById_success` | Existing | DTO |
| TC-CAT-S-12 | `getCategoryById_notFound_throwsResourceNotFoundException` | Unknown | `ResourceNotFoundException` |
| TC-CAT-S-13 | `getAllCategories_success` | Populated tree | List returned |
| TC-CAT-S-14 | `deleteCategory_success` | Leaf category | Deleted |
| TC-CAT-S-15 | `deleteCategory_notFound_throwsResourceNotFoundException` | Unknown | `ResourceNotFoundException` |
| TC-CAT-S-16 | `deleteCategory_hasSubcategories_throwsBusinessException` | Has children | `BusinessException` — **referential integrity guard** |

> ⚠️ TC-08 blocks only *direct* self-parenting (A → A). A **multi-level cycle**
> (A → B → A) is not covered by any test. See §12.4.

### 4.4 `CategoryControllerTest` — 17 cases

| ID | Test method | Expected |
|---|---|---|
| TC-CAT-C-01 | `createCategory_success_returns201` | 201 |
| TC-CAT-C-02 | `createCategory_blankName_returns400` | 400 |
| TC-CAT-C-03 | `createCategory_blankSlug_returns400` | 400 |
| TC-CAT-C-04 | `createCategory_nameAboveUpperBound256chars_returns400` | 400 |
| TC-CAT-C-05 | `createCategory_slugAlreadyExists_returns409` | **409 Conflict** |
| TC-CAT-C-06 | `createCategory_malformedJson_returns400` | 400 |
| TC-CAT-C-07 | `getAllCategories_success_returns200` | 200 |
| TC-CAT-C-08 | `getCategoryById_success_returns200` | 200 |
| TC-CAT-C-09 | `getCategoryById_notFound_returns404` | 404 |
| TC-CAT-C-10 | `updateCategory_success_returns200` | 200 |
| TC-CAT-C-11 | `updateCategory_notFound_returns404` | 404 |
| TC-CAT-C-12 | `updateCategory_slugAlreadyExists_returns409` | 409 |
| TC-CAT-C-13 | `updateCategory_blankName_returns400` | 400 |
| TC-CAT-C-14 | `updateCategory_malformedJson_returns400` | 400 |
| TC-CAT-C-15 | `deleteCategory_success_returns200` | 200 |
| TC-CAT-C-16 | `deleteCategory_notFound_returns404` | 404 |
| TC-CAT-C-17 | `deleteCategory_hasSubcategories_returns409` | 409 |

This class is the only one asserting **409 Conflict** mapping (TC-05, TC-12, TC-17).

---

## 5. Cart, Order, Payment — 82 cases

### 5.1 `CartServiceTest` — 14 cases

| ID | Test method | Scenario | Expected |
|---|---|---|---|
| TC-CART-S-01 | `getCart_createsNewCart_whenNoActiveCartExists` | First visit | Cart lazily created |
| TC-CART-S-02 | `getCart_returnsExistingCart_whenActiveCartExists` | Returning user | Existing `ACTIVE` cart reused |
| TC-CART-S-03 | `addItem_createsNewItem_whenProductNotInCart` | New product | Line created, `unit_price` snapshotted |
| TC-CART-S-04 | `addItem_increasesQuantity_whenProductAlreadyInCart` | Duplicate add | Quantity incremented, not duplicated |
| TC-CART-S-05 | `addItem_throwsException_whenProductNotFound` | Unknown product | Exception |
| TC-CART-S-06 | `addItem_throwsException_whenProductNotActive` | Deactivated product | Exception — inactive products unbuyable |
| TC-CART-S-07 | `addItem_throwsException_whenInsufficientStock` | Qty > stock | Exception |
| TC-CART-S-08 | `updateQuantity_updatesItemQuantity` | Valid change | Quantity updated |
| TC-CART-S-09 | `updateQuantity_restoresStock_whenQuantityDecreased` | Quantity lowered | Stock incremented back |
| TC-CART-S-10 | `updateQuantity_throwsException_whenCartItemNotFound` | Unknown item | Exception |
| TC-CART-S-11 | `updateQuantity_throwsException_whenItemBelongsToDifferentCart` | Cross-cart item id | Exception — **IDOR guard** |
| TC-CART-S-12 | `removeItem_removesItemFromCart` | Valid removal | Line deleted |
| TC-CART-S-13 | `removeItem_throwsException_whenItemBelongsToDifferentCart` | Cross-cart item id | Exception — **IDOR guard** |
| TC-CART-S-14 | `clearCart_removesAllItems` | Clear | All lines removed |

TC-11 and TC-13 are the suite's clearest ownership tests, and they run at the **service**
layer, so they hold regardless of the `@PreAuthorize` gap in §2.2.

> Note TC-09: cart operations do adjust stock, which means the codebase reserves stock
> at cart level for *decrements*. This partially contradicts
> `use_case_specifications.md` §C7 ("no inventory reservation") — the reservation is
> real but is not enforced as an availability guarantee across concurrent buyers. No
> concurrency test exists to characterise it (§12.4).

### 5.2 `CartControllerTest` — 11 cases

| ID | Test method | Endpoint | Expected |
|---|---|---|---|
| TC-CART-C-01 | `getCart_returnsCart` | `GET /cart` | 200, `$.data.itemCount` asserted |
| TC-CART-C-02 | `addItem_returnsCreated` | `POST /cart/items/{productId}` | 201, `"Item added to cart"` |
| TC-CART-C-03 | `addItem_returnsBadRequest_whenQuantityLessThanOne` | | 400 (`quantity: 0`) |
| TC-CART-C-04 | `addItem_returnsNotFound_whenProductNotFound` | | 404 |
| TC-CART-C-05 | `addItem_returnsBadRequest_whenInsufficientStock` | | 400 |
| TC-CART-C-06 | `updateQuantity_returnsOk` | `PUT /cart/items/{itemId}` | 200 |
| TC-CART-C-07 | `updateQuantity_returnsBadRequest_whenQuantityLessThanOne` | | 400 |
| TC-CART-C-08 | `updateQuantity_returnsNotFound_whenItemNotFound` | | 404 |
| TC-CART-C-09 | `removeItem_returnsOk` | `DELETE /cart/items/{itemId}` | 200 |
| TC-CART-C-10 | `removeItem_returnsNotFound_whenItemNotFound` | | 404 |
| TC-CART-C-11 | `clearCart_returnsOk` | `DELETE /cart` | 200 |

### 5.3 `CartExpirationServiceTest` — 4 cases

| ID | Test method | Scenario | Expected |
|---|---|---|---|
| TC-CEXP-S-01 | `expireAbandonedCarts_restoresStockForExpiredCarts` | One expired cart | Stock returned to products |
| TC-CEXP-S-02 | `expireAbandonedCarts_handlesMultipleItemsInCart` | Multi-line cart | All lines restored |
| TC-CEXP-S-03 | `expireAbandonedCarts_handlesNoExpiredCarts` | Nothing to do | No-op, no error |
| TC-CEXP-S-04 | `expireAbandonedCarts_handlesMultipleExpiredCarts` | Several carts | All processed |

### 5.4 `OrderServiceTest` — 17 cases

| ID | Test method | Scenario | Expected |
|---|---|---|---|
| TC-ORD-S-01 | `placeOrder_createsOrderSuccessfully` | Valid cart | Order `PENDING`, items snapshotted, stock decremented |
| TC-ORD-S-02 | `placeOrder_throwsException_whenCartNotFound` | No cart | Exception |
| TC-ORD-S-03 | `placeOrder_throwsException_whenCartIsEmpty` | Empty cart | Exception |
| TC-ORD-S-04 | `placeOrder_throwsException_whenInsufficientStock` | Stock dropped since add | Exception |
| TC-ORD-S-05 | `placeOrder_throwsException_whenProductNotActive` | Product deactivated since add | Exception |
| TC-ORD-S-06 | `getOrders_returnsOrdersForUser` | Order history | Only caller's orders |
| TC-ORD-S-07 | `getOrder_returnsOrderForUser` | Own order | Order returned |
| TC-ORD-S-08 | `getOrder_throwsException_whenOrderNotBelongToUser` | Another user's order id | Exception — **IDOR guard** |
| TC-ORD-S-09 | `cancelOrder_cancelsSuccessfully` | `PENDING` order | `CANCELLED`, reason stored |
| TC-ORD-S-10 | `cancelOrder_throwsException_whenNotCancellable` | Wrong status | Exception |
| TC-ORD-S-11 | `cancelOrder_throwsException_whenPaidOrder` | Already paid | Exception — "request a refund first" |
| TC-ORD-S-12 | `cancelOrder_throwsException_whenRefundRequested` | Refund pending | Exception |
| TC-ORD-S-13 | `requestReturn_requestsSuccessfully` | Delivered order | `RETURN_REQUESTED` |
| TC-ORD-S-14 | `requestReturn_throwsException_whenNotDelivered` | Not yet delivered | Exception |
| TC-ORD-S-15 | `updateStatus_transitionsPENDINGtoCONFIRMED` | Legal transition | Status advanced |
| TC-ORD-S-16 | `updateStatus_throwsException_whenInvalidTransition` | Illegal transition | Exception — **state machine enforced** |
| TC-ORD-S-17 | `updateStatus_throwsException_whenNotPaidAndShipping` | Ship an unpaid order | Exception |
| TC-ORD-S-18 | `updateStatus_throwsException_whenNotOrderOwner` | Wrong owner | Exception — **IDOR guard** |

Strongest business-rule coverage in the suite: the cancel guard is covered from three
angles (TC-10/11/12) and the status machine from three (TC-15/16/17).

> **Not covered:** transitions `CONFIRMED → SHIPPED`, `SHIPPED → DELIVERED`,
> `RETURN_REQUESTED → RETURNED`; terminality of `CANCELLED`/`RETURNED`. Only
> `PENDING → CONFIRMED` is positively asserted (TC-15).

### 5.5 `OrderControllerTest` — 11 cases

| ID | Test method | Endpoint | Expected |
|---|---|---|---|
| TC-ORD-C-01 | `placeOrder_returnsCreated` | `POST /orders` | 201 |
| TC-ORD-C-02 | `placeOrder_returnsBadRequest_whenAddressMissing` | | 400 |
| TC-ORD-C-03 | `placeOrder_returnsBadRequest_whenCartEmpty` | | 400 |
| TC-ORD-C-04 | `getOrders_returnsOrders` | `GET /orders` | 200 |
| TC-ORD-C-05 | `getOrder_returnsOrder` | `GET /orders/{orderId}` | 200 |
| TC-ORD-C-06 | `getOrder_returnsNotFound_whenOrderNotFound` | | 404 |
| TC-ORD-C-07 | `updateStatus_returnsOk` | `PUT /orders/{orderId}/status` | 200 |
| TC-ORD-C-08 | `cancelOrder_returnsOk` | `POST /orders/{orderId}/cancel` | 200 |
| TC-ORD-C-09 | `cancelOrder_returnsBadRequest_whenReasonMissing` | | 400 — reason mandatory |
| TC-ORD-C-10 | `requestReturn_returnsOk` | `POST /orders/{orderId}/return` | 200 |
| TC-ORD-C-11 | `requestReturn_returnsBadRequest_whenReasonMissing` | | 400 |

### 5.6 `PaymentServiceTest` — 17 cases

Mocks include `SePayService` — the gateway is stubbed at the service interface, not over HTTP.

| ID | Test method | Scenario | Expected |
|---|---|---|---|
| TC-PAY-S-01 | `initiatePayment_success` | Valid unpaid order | `PENDING` payment + checkout URL |
| TC-PAY-S-02 | `initiatePayment_alreadyPaid_throwsBusinessException` | Order already paid | `BusinessException` — **double-charge guard** |
| TC-PAY-S-03 | `initiatePayment_orderNotFound_throwsResourceNotFound` | Unknown order | `ResourceNotFoundException` |
| TC-PAY-S-04 | `initiatePayment_notOrderOwner_throwsAccessDenied` | Wrong buyer | `AccessDeniedException` — **IDOR guard** |
| TC-PAY-S-05 | `initiatePayment_cancelledOrder_throwsBusinessException` | Cancelled order | `BusinessException` |
| TC-PAY-S-06 | `initiatePayment_refundRequested_throwsBusinessException` | Refund pending | `BusinessException` |
| TC-PAY-S-07 | `handleIpnNotification_approvedPayment_marksOrderPaid` | Valid signed IPN | Payment `COMPLETED`, order `PAID` |
| TC-PAY-S-08 | `handleIpnNotification_invalidSecretKey_throwsBusinessException` | Bad HMAC/secret | `BusinessException` — **webhook forgery guard** |
| TC-PAY-S-09 | `handleIpnNotification_alreadyCompleted_idempotent` | Duplicate IPN | No double-processing — **idempotency** |
| TC-PAY-S-10 | `requestRefund_success` | Completed payment | `REFUND_REQUESTED` |
| TC-PAY-S-11 | `requestRefund_notCompletedPayment_throwsBusinessException` | Payment not completed | `BusinessException` |
| TC-PAY-S-12 | `requestRefund_wrongOrderStatus_throwsBusinessException` | Order in wrong state | `BusinessException` |
| TC-PAY-S-13 | `requestRefund_notOrderOwner_throwsAccessDenied` | Wrong buyer | `AccessDeniedException` |
| TC-PAY-S-14 | `approveRefund_success` | Admin approves | `REFUNDED`, `refunded_at` set |
| TC-PAY-S-15 | `approveRefund_notRefundRequested_throwsBusinessException` | No pending request | `BusinessException` |
| TC-PAY-S-16 | `approveRefund_sepayFails_throwsBusinessException` | Gateway refund fails | `BusinessException` — no local state drift |
| TC-PAY-S-17 | `approveRefund_paymentNotFound_throwsResourceNotFound` | Unknown payment | `ResourceNotFoundException` |

TC-08 and TC-09 are the two most valuable cases in the whole suite: signature
verification and IPN idempotency are the classic failure modes of gateway integrations.

### 5.7 `PaymentControllerTest` — 7 cases

| ID | Test method | Endpoint | Expected |
|---|---|---|---|
| TC-PAY-C-01 | `initiatePayment_success` | `POST /orders/{orderId}/pay` | 200/201 + checkout URL |
| TC-PAY-C-02 | `initiatePayment_alreadyPaid_returns400` | | 400 |
| TC-PAY-C-03 | `initiatePayment_orderNotFound_returns404` | | 404 |
| TC-PAY-C-04 | `getPayment_success` | `GET /payments/{paymentId}` | 200 |
| TC-PAY-C-05 | `requestRefund_success` | `POST /payments/{paymentId}/refund` | 200 |
| TC-PAY-C-06 | `requestRefund_notCompletedPayment_returns400` | | 400 |
| TC-PAY-C-07 | `getPaymentHistory_success` | `GET /payments/history` | 200 |

> **Not covered:** `POST /payments/ipn`, `POST /payments/callback`,
> `POST /payments/{paymentId}/refund/approve`, `GET /payments/seller/history` — 4 of 8
> payment endpoints. Note the IPN endpoint is `permitAll` and has **no controller test**
> despite being the only publicly reachable state-changing endpoint (service-layer
> TC-PAY-S-07/08/09 do cover the logic behind it).

---

## 6. Review, Notification, Wishlist — 27 cases

### 6.1 `ReviewServiceTest` — 9 cases

| ID | Test method | Scenario | Expected |
|---|---|---|---|
| TC-REV-S-01 | `createReview_createsReviewSuccessfully` | First review | Persisted, product rating rolled up |
| TC-REV-S-02 | `createReview_throwsException_whenProductNotFound` | Unknown product | Exception |
| TC-REV-S-03 | `createReview_upserts_whenAlreadyReviewed` | Second review, same buyer+product | **Upserts** the existing row rather than failing |
| TC-REV-S-04 | `updateReview_updatesReviewSuccessfully` | Owner edits | Updated, rating recalculated |
| TC-REV-S-05 | `updateReview_throwsException_whenNotOwner` | Different buyer | Exception — **IDOR guard** |
| TC-REV-S-06 | `deleteReview_deletesReviewSuccessfully` | Owner deletes | Removed, rating recalculated |
| TC-REV-S-07 | `deleteReview_throwsException_whenNotOwner` | Different buyer | Exception |
| TC-REV-S-08 | `getProductAverageRating_returnsAverage` | Several reviews | Correct mean |
| TC-REV-S-09 | `getProductReviewCount_returnsCount` | Several reviews | Correct count |

> ⚠️ **`verified_purchase` is never tested.** UC-007/UC-018 make purchase verification a
> core requirement ("System validates purchase verification"), and the column exists,
> but no case asserts that a non-purchaser is blocked or that the flag is set correctly.
> Rating bounds (1–5) are also untested at this layer. See §12.4.

### 6.2 `NotificationServiceTest` — 10 cases

| ID | Test method | Scenario | Expected |
|---|---|---|---|
| TC-NOT-S-01 | `createNotification_createsNotificationSuccessfully` | New notification | Persisted with type/title/message |
| TC-NOT-S-02 | `getUnreadCount_returnsCount` | Mixed read/unread | Correct unread count |
| TC-NOT-S-03 | `markAsRead_marksNotificationAsRead` | Own notification | `is_read = true` |
| TC-NOT-S-04 | `markAsRead_throwsException_whenNotFound` | Unknown id | Exception |
| TC-NOT-S-05 | `markAllAsRead_marksAllNotificationsAsRead` | Bulk | All flagged read |
| TC-NOT-S-06 | `deleteNotification_deletesNotification` | Own notification | Deleted |
| TC-NOT-S-07 | `deleteNotification_throwsException_whenNotOwner` | Another user's | Exception — **IDOR guard** |
| TC-NOT-S-08 | `deleteNotification_throwsException_whenNotFound` | Unknown id | Exception |
| TC-NOT-S-09 | `clearAllNotifications_clearsAllNotifications` | Clear all | All removed |
| TC-NOT-S-10 | `getUserNotifications_returnsNotifications` | List | Caller's notifications |

> **No `NotificationControllerTest` exists** — all 8 endpoints untested at the web layer.
> Email delivery is never asserted (only in-app rows).

### 6.3 `WishlistServiceTest` — 8 cases

| ID | Test method | Scenario | Expected |
|---|---|---|---|
| TC-WISH-S-01 | `getWishlist_returnsItems` | Populated | Items returned |
| TC-WISH-S-02 | `getWishlist_returnsEmptyList` | Empty | Empty list, not null/error |
| TC-WISH-S-03 | `addToWishlist_addsItem` | New product | Row created |
| TC-WISH-S-04 | `addToWishlist_throwsException_whenProductNotFound` | Unknown product | Exception |
| TC-WISH-S-05 | `addToWishlist_throwsException_whenAlreadyInWishlist` | Duplicate | Exception — unique constraint upheld in code |
| TC-WISH-S-06 | `removeFromWishlist_removesItem` | Existing item | Removed |
| TC-WISH-S-07 | `removeFromWishlist_throwsException_whenItemNotFound` | Not present | Exception |
| TC-WISH-S-08 | `clearWishlist_removesAllItems` | Clear all | Emptied |

### 6.4 `WishlistControllerTest` — 8 cases

| ID | Test method | Endpoint | Expected |
|---|---|---|---|
| TC-WISH-C-01 | `getWishlist_returnsItems` | `GET /buyers/wishlist` | 200 |
| TC-WISH-C-02 | `getWishlist_returnsEmptyList` | | 200 + empty array |
| TC-WISH-C-03 | `addToWishlist_returnsCreated` | `POST /buyers/wishlist/{productId}` | 201 |
| TC-WISH-C-04 | `addToWishlist_returnsNotFound_whenProductNotFound` | | 404 |
| TC-WISH-C-05 | `addToWishlist_returnsBadRequest_whenAlreadyInWishlist` | | 400 |
| TC-WISH-C-06 | `removeFromWishlist_returnsOk` | `DELETE /buyers/wishlist/{productId}` | 200 |
| TC-WISH-C-07 | `removeFromWishlist_returnsNotFound_whenItemNotFound` | | 404 |
| TC-WISH-C-08 | `clearWishlist_returnsOk` | `DELETE /buyers/wishlist` | 200 |

Wishlist is the **only** module with 1:1 service-to-controller case parity.

---

## 7. Upload & Webhook — 44 cases

### 7.1 `UploadServiceTest` — 12 cases

Mocks: `UploadSessionRepository`, `ImageRepository`, `ProductRepository`,
`SupabaseStorageClient`, upload properties.

| ID | Test method | Scenario | Expected |
|---|---|---|---|
| TC-UPL-S-01 | `requestUserUpload_success` | Avatar upload requested | `PENDING` session + signed URL |
| TC-UPL-S-02 | `requestUserUpload_sessionStoresJwtUserId` | Session provenance | `uploaded_by` = JWT subject, not a client-supplied id — **spoofing guard** |
| TC-UPL-S-03 | `requestProductUpload_success` | Owner requests product image URL | Session created |
| TC-UPL-S-04 | `requestProductUpload_notSeller_throwsAccessDenied` | Non-owner | `AccessDeniedException` |
| TC-UPL-S-05 | `requestProductUpload_productNotFound_throwsResourceNotFound` | Unknown product | `ResourceNotFoundException` |
| TC-UPL-S-06 | `requestProductUpload_quotaExceeded_throwsBusinessException` | Already 10 images | `BusinessException` — **quota enforced** |
| TC-UPL-S-07 | `completeUploadFromWebhook_validSession_createsImage` | Valid webhook | Session `COMPLETED`, `images` row inserted |
| TC-UPL-S-08 | `completeUploadFromWebhook_noSession_logsAndReturns` | Unknown storage path | Logged, no throw — **unsolicited upload ignored** |
| TC-UPL-S-09 | `completeUploadFromWebhook_expiredSession_marksExpired` | Past `expires_at` | Session `EXPIRED`, no image row |
| TC-UPL-S-10 | `completeUploadFromWebhook_invalidFileType_marksFailed` | Disallowed content type | Session `FAILED` — **post-upload type validation** |
| TC-UPL-S-11 | `completeUploadFromWebhook_fileTooLarge_marksFailed` | > 5 MB | Session `FAILED` |
| TC-UPL-S-12 | `completeUploadFromWebhook_userUpsert_replacesExistingImage` | Second avatar | Old avatar replaced, not accumulated |

The best-designed group in the suite: it covers the full two-phase lifecycle including
all three failure terminal states (`EXPIRED`, `FAILED` ×2) and the spoofing guard.

### 7.2 `ImageServiceTest` — 11 cases

| ID | Test method | Scenario | Expected |
|---|---|---|---|
| TC-IMG-S-01 | `getImages_returnsList` | Entity has images | List returned |
| TC-IMG-S-02 | `getImages_noImages_returnsEmptyList` | None | Empty list |
| TC-IMG-S-03 | `deleteImage_ownImage_success` | Owner deletes | Removed from storage + DB |
| TC-IMG-S-04 | `deleteImage_notOwner_throwsAccessDenied` | Non-owner | `AccessDeniedException` |
| TC-IMG-S-05 | `deleteImage_notFound_throwsResourceNotFound` | Unknown id | `ResourceNotFoundException` |
| TC-IMG-S-06 | `deleteImage_storageDeleteFails_stillDeletesFromDb` | Supabase delete errors | **DB row still removed** — no orphan metadata |
| TC-IMG-S-07 | `deleteImagesByEntity_deletesFromStorageAndDb` | Cascade on entity delete | Both cleared |
| TC-IMG-S-08 | `deleteImagesByEntity_noImages_skipsStorage` | Nothing to delete | No pointless storage call |
| TC-IMG-S-09 | `extractStoragePath_validSupabaseUrl_extractsCorrectly` | Standard public URL | Path extracted |
| TC-IMG-S-10 | `extractStoragePath_noPublicMarker_returnsFullPath` | Non-standard URL | Falls back to full path |
| TC-IMG-S-11 | `extractStoragePath_invalidUrl_returnsNull` | Garbage input | `null`, no throw |

TC-06 documents a deliberate consistency trade-off (favour DB cleanup over storage
consistency); TC-09/10/11 give the URL parser proper equivalence-class coverage.

### 7.3 `ImageRepositoryTest` — 8 cases

> ⚠️ **See §2.3 — these cases mock the repository under test and are tautological.**
> Listed for completeness; they should not be counted as repository coverage.

| ID | Test method | Nominal intent |
|---|---|---|
| TC-IMG-R-01 | `findByEntityTypeAndEntityId_returnsCorrectOrder` | `created_at ASC` ordering |
| TC-IMG-R-02 | `findByEntityTypeAndEntityId_noMatch_returnsEmpty` | Empty result |
| TC-IMG-R-03 | `findByEntityTypeAndEntityId_differentEntityTypes_returnsOnlyMatching` | Polymorphic discrimination |
| TC-IMG-R-04 | `deleteByEntityTypeAndEntityId_removesOnlyMatching` | Scoped bulk delete |
| TC-IMG-R-05 | `findAllByOrderByCreatedAtAsc_pagination` | Paged query |
| TC-IMG-R-06 | `findAllByOrderByCreatedAtAsc_empty_returnsEmptyPage` | Empty page |
| TC-IMG-R-07 | `findByFileUrlContaining_findsMatch` | URL substring search |
| TC-IMG-R-08 | `findByFileUrlContaining_noMatch_returnsEmpty` | No match |

### 7.4 `UploadControllerTest` — 4 cases

| ID | Test method | Endpoint | Expected |
|---|---|---|---|
| TC-UPL-C-01 | `requestUserAvatar_success` | `GET /users/avatar/upload-url` | 200 + signed URL |
| TC-UPL-C-02 | `requestProductImages_success` | `GET /products/{productId}/images/upload-url` | 200 |
| TC-UPL-C-03 | `requestProductImages_productNotFound_returns404` | | 404 |
| TC-UPL-C-04 | `requestProductImages_notSeller_returns403` | | 403 (via handler) |

### 7.5 `WebhookServiceTest` — 4 cases

| ID | Test method | Scenario | Expected |
|---|---|---|---|
| TC-WHK-S-01 | `handleStorageInsert_delegatesToUploadService` | Valid insert event | Delegates with correct path |
| TC-WHK-S-02 | `handleStorageInsert_nullPath_doesNotCallUploadService` | Null path | No delegation, no throw |
| TC-WHK-S-03 | `handleStorageInsert_emptyPath_doesNotCallUploadService` | Empty path | No delegation |
| TC-WHK-S-04 | `handleStorageInsert_uploadServiceThrows_propagatesException` | Downstream failure | Exception propagates (Supabase retries) |

### 7.6 `WebhookControllerTest` — 5 cases

| ID | Test method | Scenario | Expected |
|---|---|---|---|
| TC-WHK-C-01 | `handleStorageWebhook_validInsert_processesAndReturns200` | Correct secret + INSERT | 200, processed |
| TC-WHK-C-02 | `handleStorageWebhook_validDelete_returns200WithIgnored` | DELETE event | 200 + `ignored` — only INSERT acted on |
| TC-WHK-C-03 | `handleStorageWebhook_wrongSecret_returns403` | Bad shared secret | **403** |
| TC-WHK-C-04 | `handleStorageWebhook_missingSecret_returns403` | No secret header | **403** |
| TC-WHK-C-05 | `handleStorageWebhook_serviceThrowsException_propagates` | Downstream failure | Propagates |

TC-03 and TC-04 are the **only** authentication assertions anywhere in the controller
tests that exercise real verification code — because the webhook secret is checked
inside the controller/service, not by `SecurityConfig`.

---

## 8. Application — 1 case (disabled)

| ID | Test method | Scenario | Expected |
|---|---|---|---|
| TC-APP-A-01 | `MarketplaceApplicationTests.contextLoads` | Boot the full Spring context under the `test` profile | **`@Disabled`** — "Requires full application context with database" |

> As of 2026-08-06 this class is disabled. It previously proved every bean wired up
> and no `@Value`/config binding was broken. The security-enforcement gap it partially
> filled is now covered by `SecurityIntegrationTest` (§9).

---

## 9. Security integration — 41 cases (new)

Two new test classes added since the original document was generated. Together they
close the two highest-risk gaps identified in §11 (original numbering): the
untested `JwtTokenProvider` and the absent security-enforcement tests.

### 9.1 `SecurityIntegrationTest` — 23 cases (`shared/security/SecurityIntegrationTest.java`)

`@SpringBootTest` + `@ActiveProfiles("test")` with full `MockMvc` using
`webAppContextSetup(…).apply(springSecurity())`. Real JWT tokens; real Spring Security
filter chain; `ProductService` is the only `@MockitoBean`.

| ID | Test method | Scenario | Expected |
|---|---|---|---|
| TC-SEC-I-01 | `protectedEndpoint_noToken_returns401` | No `Authorization` header | **401** |
| TC-SEC-I-02 | `protectedEndpoint_emptyAuthHeader_returns401` | `Authorization: ` (empty) | **401** |
| TC-SEC-I-03 | `protectedEndpoint_noBearerPrefix_returns401` | `Authorization: <token>` (no `Bearer `) | **401** |
| TC-SEC-I-04 | `protectedEndpoint_wrongScheme_returns401` | `Authorization: Basic <token>` | **401** |
| TC-SEC-I-05 | `protectedEndpoint_validBuyerToken_returns200` | Valid JWT with `BUYER` role | **200** |
| TC-SEC-I-06 | `protectedEndpoint_validSellerToken_returns200` | Valid JWT with `SELLER` role | **200** |
| TC-SEC-I-07 | `protectedEndpoint_expiredToken_returns401` | Expired JWT | **401** |
| TC-SEC-I-08 | `protectedEndpoint_malformedToken_returns401` | Garbage token string | **401** |
| TC-SEC-I-09 | `protectedEndpoint_tamperedToken_returns401` | Valid JWT with modified payload | **401** |
| TC-SEC-I-10 | `protectedEndpoint_refreshTokenAsAccessToken_returns401` | Refresh token used as access token | **401** |
| TC-SEC-I-11 | `protectedEndpoint_tokenForDeletedUser_returns401` | Valid JWT but user deleted from DB | **401** |
| TC-SEC-I-12 | `publicEndpoint_noToken_returns200` | Public endpoint, no auth | **200** |
| TC-SEC-I-13 | `publicEndpoint_withToken_alsoWorks` | Public endpoint with valid token | **200** |
| TC-SEC-I-14 | `roleAuthorization_buyerCannotAccessSellerEndpoint_returns403` | `BUYER` token hits seller-only route | **403** |
| TC-SEC-I-15 | `roleAuthorization_sellerCanAccessSellerEndpoint_returns200` | `SELLER` token hits seller-only route | **200** |
| TC-SEC-I-16 | `roleAuthorization_buyerCanAccessBuyerEndpoint_returns200` | `BUYER` token hits buyer-only route | **200** |
| TC-SEC-I-17 | `roleAuthorization_sellerCannotAccessBuyerEndpoint_returns403` | `SELLER` token hits buyer-only route | **403** |
| TC-SEC-I-18 | `refreshToken_validToken_returnsNewTokenPair` | Valid refresh token | **200** + new token pair |
| TC-SEC-I-19 | `refreshToken_invalidToken_returns400` | Unknown refresh token | **400** |
| TC-SEC-I-20 | `refreshToken_expiredDbRecord_returns400` | Expired refresh token in DB | **400** |
| TC-SEC-I-21 | `refreshToken_notInDb_returns400` | Token not in store | **400** |
| TC-SEC-I-22 | `logout_invalidatesRefreshToken` | Logout then attempt refresh | Refresh fails |
| TC-SEC-I-23 | `logout_thenRefreshFails` | Second refresh after logout | **400** |

This is the **only** class that exercises `JwtAuthenticationFilter`,
`JwtSecurityContextRepository`, `SecurityConfig` permitAll matchers, and
`@PreAuthorize` role rules with a real Spring context. It replaces the security-
enforcement gap that §12.3 (original) identified as the largest blind spot.

### 9.2 `JwtTokenProviderTest` — 18 cases (`shared/security/JwtTokenProviderTest.java`)

Plain JUnit 5 — no Spring context, no mocks. The provider is instantiated directly
with a test secret via `@BeforeEach`.

| ID | Test method | Scenario | Expected |
|---|---|---|---|
| TC-SEC-J-01 | `generateAccessToken_containsCorrectClaims` | Generate access token | `sub`, `role`, `type` claims present |
| TC-SEC-J-02 | `generateAccessToken_withSellerRole` | `SELLER` role | `role = SELLER` |
| TC-SEC-J-03 | `generateAccessToken_withAdminRole` | `ADMIN` role | `role = ADMIN` |
| TC-SEC-J-04 | `generateRefreshToken_hasSubjectAndJti` | Generate refresh token | `sub` and `jti` claims present |
| TC-SEC-J-05 | `generateRefreshToken_eachTokenHasUniqueJti` | Two refresh tokens | Different `jti` values |
| TC-SEC-J-06 | `generateMfaToken_hasMfaTypeClaim` | Generate MFA token | `type = MFA` claim |
| TC-SEC-J-07 | `validateToken_validAccess_returnsTrue` | Valid access token | `true` |
| TC-SEC-J-08 | `validateToken_validRefresh_returnsTrue` | Valid refresh token | `true` |
| TC-SEC-J-09 | `validateToken_validMfa_returnsTrue` | Valid MFA token | `true` |
| TC-SEC-J-10 | `validateToken_expiredAccess_returnsFalse` | Expired access token | `false` |
| TC-SEC-J-11 | `validateToken_expiredRefresh_returnsFalse` | Expired refresh token | `false` |
| TC-SEC-J-12 | `validateToken_wrongKey_returnsFalse` | Token signed with different key | `false` |
| TC-SEC-J-13 | `validateToken_malformedToken_returnsFalse` | Garbage string | `false` |
| TC-SEC-J-14 | `validateToken_emptyString_returnsFalse` | Empty string | `false` |
| TC-SEC-J-15 | `validateToken_tamperedPayload_returnsFalse` | Modified payload | `false` |
| TC-SEC-J-16 | `validateToken_nullToken_returnsFalse` | `null` input | `false` |
| TC-SEC-J-17 | `getUserIdFromToken_returnsSubject` | Extract subject from access token | Correct user id |
| TC-SEC-J-18 | `getUserIdFromToken_refreshToken_returnsSubject` | Extract subject from refresh token | Correct user id |

Previously listed in §12.1 (original) as the highest-risk untested class. Now fully
covered: signing, verification, expiry, claim extraction, and key isolation.

---

## 10. Test design techniques in use

The suite applies recognisable formal techniques — worth documenting because they are
consistent enough to be a house style.

### 10.1 Boundary Value Analysis

Explicit lower/upper bound pairs plus their ±1 rejections. This is the suite's
strongest characteristic (≈40 cases).

| Field | Valid range | Lower bound | Below | Upper bound | Above |
|---|---|---|---|---|---|
| Password (register / reset / change) | 8–128 | `…LowerBound8chars_success` | `…BelowLowerBound7chars_returns400` | `…UpperBound128chars_success` | `…AboveUpperBound129chars_returns400` |
| Display name | 1–255 | `…LowerBound1char_success` | `…BelowLowerBound0chars_returns400` | `…UpperBound255chars_success` | `…AboveUpperBound256chars_returns400` |
| Product name | 1–255 | — | `…nameBelowLowerBound0chars_returns400` | — | `…nameAboveUpperBound256chars_returns400` |
| Category name | ≤255 | — | — | — | `…nameAboveUpperBound256chars_returns400` |
| Price | > 0 | — | `createProduct_zeroPrice_returns400`, `…negativePrice_returns400` | — | — |
| Stock | ≥ 0 | — | `createProduct_negativeStock_returns400` | — | — |
| Cart quantity | ≥ 1 | — | `…QuantityLessThanOne` (0) | — | — |

### 10.2 Equivalence partitioning

`extractStoragePath` (TC-IMG-S-09/10/11): valid Supabase URL / URL without the public
marker / unparseable input. `register_invalidEmailNoAt` vs `register_invalidEmailNoDomain`
partition malformed-email space.

### 10.3 State transition testing

`OrderServiceTest` TC-15/16/17 and the cancel guards TC-10/11/12 test the
`OrderStatus` machine. `UploadServiceTest` TC-07/09/10/11 test the `UploadStatus`
machine including all three failure terminals.

### 10.4 Negative / exception-path testing

~200 of 356 cases (56%) assert a failure. Every service method with a lookup has a
`…NotFound…` case; every owner-scoped operation has a mismatch case.

### 10.5 Idempotency & duplicate-delivery testing

`handleIpnNotification_alreadyCompleted_idempotent` (TC-PAY-S-09),
`createReview_upserts_whenAlreadyReviewed` (TC-REV-S-03),
`completeUploadFromWebhook_userUpsert_replacesExistingImage` (TC-UPL-S-12),
`addItem_increasesQuantity_whenProductAlreadyInCart` (TC-CART-S-04).

### 10.6 HTTP protocol conformance

`…MethodNotAllowed_returns405` (6 cases), `…wrongContentType_returns415` (2 cases),
`…malformedJson_returns400` (6 cases), `…emptyBody_returns400`.

### 10.7 Authorization / IDOR testing at the service layer

Because the web layer cannot test `@PreAuthorize` (§2.2), ownership is asserted in
services instead — and it is done consistently:
TC-CART-S-11, TC-CART-S-13, TC-ORD-S-08, TC-ORD-S-18, TC-PAY-S-04, TC-PAY-S-13,
TC-PROD-S-06, TC-PROD-S-10, TC-REV-S-05, TC-REV-S-07, TC-NOT-S-07, TC-IMG-S-04,
TC-UPL-S-04, TC-UPL-S-02.

**14 IDOR-class cases** — the suite's most systematic security theme.

---

## 11. Coverage matrix vs. use cases

Traceability to `use_case_specifications.md`. "Service" / "Controller" = a test class
covers that use case at that layer.

| UC | Title | Service | Controller | Notes |
|---|---|:-:|:-:|---|
| UC-001 | User Registration | ✅ | ✅ | 2 + 23 cases; strongest-covered use case |
| UC-002 | Login with OIDC | ⚠️ | ⚠️ | Local login covered (TC-AUTH-S-09→12, TC-AUTH-C-24→31). Security filter chain covered (§9.1). **OIDC token exchange and MFA challenge: 0 cases** |
| UC-003 | MFA Setup | ❌ | ❌ | `MFAService` has no test class; 5 MFA endpoints untested |
| UC-004 | Profile Management | ✅ | ✅ | 11 + 24 cases |
| UC-005 | Create Product | ✅ | ✅ | 22 + 27 cases |
| UC-006 | Search & Filtering | ✅ | ✅ | TC-PROD-S-14→19, TC-PROD-C-14→16 |
| UC-007 | Review Submission | ⚠️ | ❌ | Service only; **`verified_purchase` untested**; no `ReviewControllerTest` |
| UC-008 | Add to Cart | ✅ | ✅ | 14 + 11 cases |
| UC-009 | Manage Wishlist | ✅ | ✅ | 8 + 8 cases |
| UC-010 | Place Order | ✅ | ✅ | TC-ORD-S-01→05, TC-ORD-C-01→03 |
| UC-011 | Track Order Status | ✅ | ✅ | TC-ORD-S-06/07/15→18 |
| UC-012 | Cancel Order | ✅ | ✅ | TC-ORD-S-09→12 (time window not applicable — never implemented) |
| UC-013 | Request Return/Refund | ✅ | ✅ | TC-ORD-S-13/14 + TC-PAY-S-10→17 |
| UC-014 | Process Payment | ✅ | ⚠️ | Service excellent (17 cases). IPN/callback endpoints untested at web layer |
| UC-015 | View Payment History | ⚠️ | ⚠️ | Buyer history only (TC-PAY-C-07); seller history untested |
| UC-016 | Order Notifications | ⚠️ | ❌ | `NotificationService` covered; **email delivery and all 8 endpoints untested** |
| UC-017 | Security Notifications | ❌ | ❌ | No case asserts a `SECURITY_ALERT` is raised |
| UC-018 | Review with Verification | ❌ | ❌ | The verification requirement itself is untested |
| UC-019 | Support Ticket | n/a | n/a | Feature not implemented |
| UC-A01 | Image / avatar upload | ✅ | ✅ | 12 + 11 + 4 cases; second-best covered |
| UC-A02 | Discount campaign | ❌ | ❌ | `DiscountService` has **no test class** |
| UC-A03 | Suspend user (Admin) | ❌ | ❌ | `AdminUserService` untested |
| UC-A04 | Moderate product (Admin) | ❌ | ❌ | `AdminProductService` untested |
| UC-A05 | Admin audit trail | ❌ | ❌ | No case asserts an `admin_action_log` write |
| UC-A06 | Refresh session | ✅ | ✅ | TC-AUTH-S-13→15, TC-AUTH-C-32/33 |
| UC-A07 | Resend verification | ❌ | ❌ | Endpoint untested |
| UC-A08 | Recovery-code login | ❌ | ❌ | Untested |
| UC-A09 | Seller revenue view | ❌ | ❌ | Untested |
| UC-A10 | Cart expiry sweep | ✅ | n/a | 4 cases |

**Summary: 13 of 29 use cases well covered, 6 partially, 10 with zero coverage.**

---

## 12. Coverage gaps

### 12.1 Components with no test class at all

**Services (7):**

| Component | Risk | Why it matters |
|---|---|---|
| `MFAService` | **High** | OTP generation, hashing, expiry, recovery-code consumption — all security-critical, all untested |
| `OidcService` | **High** | Token exchange, `sub` claim matching, identity linking, `HYBRID` promotion. Account-takeover surface if linking is wrong |
| `PermissionService` | **High** | Every `@PreAuthorize` ownership check delegates here — and §2.2 means nothing else covers it either |
| `SePayService` | Medium | HMAC construction, checkout URL building, refund calls |
| `DiscountService` | Medium | Money arithmetic + active-window logic; feeds cart and order totals |
| `AdminAnalyticsService` | Medium | Revenue/order aggregation correctness |
| `AdminUserService`, `AdminProductService`, `EmailService` | Low–Medium | Status changes + audit writes; mail assembly |

> **Note:** `JwtTokenProvider` was previously in this list but now has full coverage
> via `JwtTokenProviderTest` (§9.2 — 18 cases). `SecurityIntegrationTest` (§9.1 — 23
> cases) exercises `JwtAuthenticationFilter`, `SecurityConfig` matchers, and role
> authorization end-to-end.

**Controllers (8):** `ReviewController`, `NotificationController`, `AdminUserController`,
`AdminProductController`, `AdminAnalyticsController`, `AdminOrderController`,
`SellerController`, `SellerProductController`.

**Infrastructure (4):** `SecurityConfig` (partially covered by §9.1),
`JwtAuthenticationFilter` (covered by §9.1),
`JwtSecurityContextRepository` (covered by §9.1),
`RateLimitFilter`, `CacheConfig`.

### 12.2 No real persistence test

Every repository is mocked. Consequently **nothing verifies**: derived query
correctness, JPQL, `@Query` methods, unique constraints (cart+product, review
product+buyer, wishlist user+product), cascade deletes, `@Version` optimistic-lock
behaviour, or that the JPA mappings match the Flyway schema. Because the `test` profile
disables Flyway, **`mvn test` never runs a migration** — a broken migration ships green.

*Fix:* Testcontainers PostgreSQL + `@DataJpaTest` with Flyway enabled. Start by
converting `ImageRepositoryTest` (§2.3), which currently proves nothing.

### 12.3 Security-enforcement test — partially addressed

`SecurityIntegrationTest` (§9.1) now covers `JwtAuthenticationFilter` rejection
(missing/malformed/expired/tampered tokens), `SecurityConfig` `permitAll` matchers,
and role-based `@PreAuthorize` rules for buyer/seller/admin across ~15 routes.

**Still untested:** `PermissionService` ownership checks (`isOwnerOfOrder`,
`isOwnerOfProduct`, etc.) — these are SpEL-evaluated inside `@PreAuthorize` but are
only asserted at the service layer (§10.7), not via the filter chain. `RateLimitFilter`
is disabled in the test profile.

### 12.4 Untested feature areas

- **Discounts** — no case covers `PERCENT`/`FIXED` maths, window boundaries, or the
  effective price flowing into `cart_items.unit_price` / `order_items.unit_price`.
  Explicitly listed as unmet in `discount_campaign_plan.md` §10.2.
- **Multi-level category cycles** — TC-CAT-S-08 blocks A → A only, not A → B → A.
- **Concurrency** — no test for two buyers racing the last unit, nor for
  `ObjectOptimisticLockingFailureException` handling on any `@Version` entity.
- **Review purchase verification** and **rating bounds (1–5)**.
- **Order state machine** beyond `PENDING → CONFIRMED` (§5.4).
- **Notification email delivery** — only DB rows asserted.
- **Rate limiting** — including the dead-regex defect noted in
  `api_specifications.md` §A15, which a test would have caught.

### 12.5 Low-value cases inflating the count

| Cases | Issue |
|---|---|
| 8 (`ImageRepositoryTest`) | Tautological — mock the subject under test (§2.3) |
| 10 (`UserServiceTest` TC-14→23) | Entity getter/setter assertions, not service logic |
| **18 of 356 (5%)** | Effective real-case count ≈ **338** |

---

## 13. Running the suite

```bash
# All tests
./mvnw test

# One class
./mvnw test -Dtest=PaymentServiceTest

# One case
./mvnw test -Dtest=PaymentServiceTest#handleIpnNotification_alreadyCompleted_idempotent

# Pattern — every ownership/IDOR case
./mvnw test -Dtest='*Test#*NotOwner*+*notOrderOwner*+*sellerMismatch*'

# Full verification as CI runs it
./mvnw -B compile && ./mvnw -B test && ./mvnw -B package
```

**Runtime characteristics:** no external services required — Docker, PostgreSQL and
Redis do **not** need to be running. Only `MarketplaceApplicationTests` starts a Spring
context (H2, in-memory). The suite is fast and hermetic; that is its main virtue and
also the source of every gap in §12.

**No coverage tooling is configured.** JaCoCo is not in `pom.xml`, so no line/branch
coverage figure exists for this project — the analysis above is by inspection, not
instrumentation. Adding `jacoco-maven-plugin` with a `report` goal would make §12
measurable rather than argued.

---

## 14. Recommended additions, by value

Ordered by risk reduction per unit of effort. Items marked ✅ have been implemented.

| # | Addition | Closes | Status |
|---|---|---|---|
| 1 | `SecurityIntegrationTest` — `@SpringBootTest` + real JWT filter chain, assert 401/403 across roles | §12.3 — the largest blind spot | ✅ Done (§9.1, 23 cases) |
| 2 | `JwtTokenProviderTest` — signing, verification, expiry, claims | §12.1 — highest-risk untested class | ✅ Done (§9.2, 18 cases) |
| 3 | `PermissionServiceTest` | §12.1 — every `@PreAuthorize` delegates here | Open |
| 4 | `MFAServiceTest` + `OidcServiceTest` | UC-002, UC-003, UC-A08 | Open |
| 5 | `DiscountServiceTest` — percent/fixed, window edges, cart/order wiring | §12.4, `discount_campaign_plan.md` §10.2 item 2 | Open |
| 6 | Convert `ImageRepositoryTest` to `@DataJpaTest` + Testcontainers, Flyway enabled | §12.2 and §12.5 in one change | Open |
| 7 | Review verified-purchase + rating-bounds cases | UC-007 / UC-018 — a stated requirement with zero coverage | Open |
| 8 | `ReviewControllerTest`, `NotificationControllerTest`, admin controller tests | §12.1 controllers | Open |
| 9 | Remaining order-status transitions + terminal-state cases | §5.4 | Open |
| 10 | `RateLimitFilterTest` | Would have caught the §A15 regex defect | Open |
| 11 | Add JaCoCo, publish coverage in CI | Makes §12 measurable | Open |
| 12 | Either adopt WireMock for SePay/Supabase contract tests, or remove the unused dependency | §2.6 | Open |
| 13 | Concurrency test for optimistic locking on the last-unit race | §12.4 | Open |
