package com.marketplace.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketplace.product.service.ProductService;
import com.marketplace.shared.dto.PageResponse;
import com.marketplace.user.model.RefreshToken;
import com.marketplace.user.model.User;
import com.marketplace.user.repository.RefreshTokenRepository;
import com.marketplace.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class SecurityIntegrationTest {

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@org.springframework.test.context.bean.override.mockito.MockitoBean
	private ProductService productService;

	private User buyerUser;
	private User sellerUser;

	@BeforeEach
	void setUp() {
		mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
				.webAppContextSetup(webApplicationContext)
				.apply(SecurityMockMvcConfigurers.springSecurity())
				.build();

		refreshTokenRepository.deleteAll();
		userRepository.deleteAll();

		when(productService.getSellerProducts(anyString(), anyInt(), anyInt()))
				.thenReturn(new PageResponse<>(java.util.List.of(), 0, 10, 0, 0));

		buyerUser = createUser("buyer@test.com", "Buyer User", User.Role.BUYER, true);
		sellerUser = createUser("seller@test.com", "Seller User", User.Role.SELLER, true);
	}

	// ==================== NO TOKEN ====================

	@Test
	void protectedEndpoint_noToken_returns401() throws Exception {
		mockMvc.perform(get("/api/v1/users/profile"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.message").value("Authentication required"));
	}

	@Test
	void protectedEndpoint_emptyAuthHeader_returns401() throws Exception {
		mockMvc.perform(get("/api/v1/users/profile")
						.header("Authorization", ""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpoint_noBearerPrefix_returns401() throws Exception {
		String token = jwtTokenProvider.generateAccessToken(
				buyerUser.getId().toString(), buyerUser.getEmail(), "BUYER");

		mockMvc.perform(get("/api/v1/users/profile")
						.header("Authorization", token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpoint_wrongScheme_returns401() throws Exception {
		String token = jwtTokenProvider.generateAccessToken(
				buyerUser.getId().toString(), buyerUser.getEmail(), "BUYER");

		mockMvc.perform(get("/api/v1/users/profile")
						.header("Authorization", "Basic " + token))
				.andExpect(status().isUnauthorized());
	}

	// ==================== VALID TOKEN ====================

	@Test
	void protectedEndpoint_validBuyerToken_returns200() throws Exception {
		String token = jwtTokenProvider.generateAccessToken(
				buyerUser.getId().toString(), buyerUser.getEmail(), "BUYER");

		mockMvc.perform(get("/api/v1/users/profile")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.email").value("buyer@test.com"))
				.andExpect(jsonPath("$.data.displayName").value("Buyer User"));
	}

	@Test
	void protectedEndpoint_validSellerToken_returns200() throws Exception {
		String token = jwtTokenProvider.generateAccessToken(
				sellerUser.getId().toString(), sellerUser.getEmail(), "SELLER");

		mockMvc.perform(get("/api/v1/users/profile")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.email").value("seller@test.com"));
	}

	// ==================== EXPIRED TOKEN ====================

	@Test
	void protectedEndpoint_expiredToken_returns401() throws Exception {
		JwtProperties expiredProps = new JwtProperties();
		expiredProps.setAccessSecret("test-access-secret-for-unit-tests-must-be-long-enough");
		expiredProps.setRefreshSecret("test-refresh-secret-for-unit-tests-must-be-long-enough");
		expiredProps.setAccessTokenExpiration(-1);
		expiredProps.setRefreshTokenExpiration(604_800_000);
		JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProps);

		String token = expiredProvider.generateAccessToken(
				buyerUser.getId().toString(), buyerUser.getEmail(), "BUYER");

		mockMvc.perform(get("/api/v1/users/profile")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	// ==================== MALFORMED TOKEN ====================

	@Test
	void protectedEndpoint_malformedToken_returns401() throws Exception {
		mockMvc.perform(get("/api/v1/users/profile")
						.header("Authorization", "Bearer not-a-valid-jwt-token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpoint_tamperedToken_returns401() throws Exception {
		String token = jwtTokenProvider.generateAccessToken(
				buyerUser.getId().toString(), buyerUser.getEmail(), "BUYER");
		String[] parts = token.split("\\.");
		parts[1] = parts[1].substring(0, parts[1].length() - 2) + "XX";
		String tampered = String.join(".", parts);

		mockMvc.perform(get("/api/v1/users/profile")
						.header("Authorization", "Bearer " + tampered))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpoint_refreshTokenAsAccessToken_returns401() throws Exception {
		String refreshToken = jwtTokenProvider.generateRefreshToken(buyerUser.getId().toString());

		mockMvc.perform(get("/api/v1/users/profile")
						.header("Authorization", "Bearer " + refreshToken))
				.andExpect(status().isUnauthorized());
	}

	// ==================== USER NOT IN DB ====================

	@Test
	void protectedEndpoint_tokenForDeletedUser_returns401() throws Exception {
		String fakeId = UUID.randomUUID().toString();
		String token = jwtTokenProvider.generateAccessToken(fakeId, "ghost@test.com", "BUYER");

		mockMvc.perform(get("/api/v1/users/profile")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	// ==================== PUBLIC ENDPOINTS ====================

	@Test
	void publicEndpoint_noToken_returns200() throws Exception {
		mockMvc.perform(get("/api/v1/products"))
				.andExpect(status().isOk());
	}

	@Test
	void publicEndpoint_withToken_alsoWorks() throws Exception {
		String token = jwtTokenProvider.generateAccessToken(
				buyerUser.getId().toString(), buyerUser.getEmail(), "BUYER");

		mockMvc.perform(get("/api/v1/products")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	// ==================== ROLE-BASED AUTHORIZATION ====================

	@Test
	void roleAuthorization_buyerCannotAccessSellerEndpoint_returns403() throws Exception {
		String token = jwtTokenProvider.generateAccessToken(
				buyerUser.getId().toString(), buyerUser.getEmail(), "BUYER");

		mockMvc.perform(get("/api/v1/seller/products")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.error").value("Forbidden"))
				.andExpect(jsonPath("$.message").value("Access denied"));
	}

	@Test
	void roleAuthorization_sellerCanAccessSellerEndpoint_returns200() throws Exception {
		String token = jwtTokenProvider.generateAccessToken(
				sellerUser.getId().toString(), sellerUser.getEmail(), "SELLER");

		mockMvc.perform(get("/api/v1/seller/products")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void roleAuthorization_buyerCanAccessBuyerEndpoint_returns200() throws Exception {
		String token = jwtTokenProvider.generateAccessToken(
				buyerUser.getId().toString(), buyerUser.getEmail(), "BUYER");

		mockMvc.perform(get("/api/v1/cart")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void roleAuthorization_sellerCannotAccessBuyerEndpoint_returns403() throws Exception {
		String token = jwtTokenProvider.generateAccessToken(
				sellerUser.getId().toString(), sellerUser.getEmail(), "SELLER");

		mockMvc.perform(get("/api/v1/cart")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());
	}

	// ==================== REFRESH TOKEN ====================

	@Test
	void refreshToken_validToken_returnsNewTokenPair() throws Exception {
		String refreshTokenValue = jwtTokenProvider.generateRefreshToken(buyerUser.getId().toString());
		RefreshToken refreshToken = new RefreshToken(
				buyerUser, refreshTokenValue, Instant.now().plus(7, ChronoUnit.DAYS));
		refreshTokenRepository.save(refreshToken);

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + refreshTokenValue + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
	}

	@Test
	void refreshToken_invalidToken_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"invalid-token\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void refreshToken_expiredDbRecord_returns400() throws Exception {
		String refreshTokenValue = jwtTokenProvider.generateRefreshToken(buyerUser.getId().toString());
		RefreshToken refreshToken = new RefreshToken(
				buyerUser, refreshTokenValue, Instant.now().minus(1, ChronoUnit.DAYS));
		refreshTokenRepository.save(refreshToken);

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + refreshTokenValue + "\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void refreshToken_notInDb_returns400() throws Exception {
		String refreshTokenValue = jwtTokenProvider.generateRefreshToken(buyerUser.getId().toString());

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + refreshTokenValue + "\"}"))
				.andExpect(status().isBadRequest());
	}

	// ==================== LOGOUT ====================

	@Test
	void logout_invalidatesRefreshToken() throws Exception {
		String refreshTokenValue = jwtTokenProvider.generateRefreshToken(buyerUser.getId().toString());
		RefreshToken refreshToken = new RefreshToken(
				buyerUser, refreshTokenValue, Instant.now().plus(7, ChronoUnit.DAYS));
		refreshTokenRepository.save(refreshToken);

		String accessToken = jwtTokenProvider.generateAccessToken(
				buyerUser.getId().toString(), buyerUser.getEmail(), "BUYER");

		mockMvc.perform(post("/api/v1/auth/logout")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk());

		Optional<RefreshToken> afterLogout = refreshTokenRepository.findByToken(refreshTokenValue);
		assertThat(afterLogout).isEmpty();
	}

	@Test
	void logout_thenRefreshFails() throws Exception {
		String refreshTokenValue = jwtTokenProvider.generateRefreshToken(buyerUser.getId().toString());
		RefreshToken refreshToken = new RefreshToken(
				buyerUser, refreshTokenValue, Instant.now().plus(7, ChronoUnit.DAYS));
		refreshTokenRepository.save(refreshToken);

		String accessToken = jwtTokenProvider.generateAccessToken(
				buyerUser.getId().toString(), buyerUser.getEmail(), "BUYER");

		mockMvc.perform(post("/api/v1/auth/logout")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + refreshTokenValue + "\"}"))
				.andExpect(status().isBadRequest());
	}

	// ==================== HELPER ====================

	private User createUser(String email, String displayName, User.Role role, boolean verified) {
		User user = new User();
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode("Password1!"));
		user.setDisplayName(displayName);
		user.setRole(role);
		user.setVerified(verified);
		user.setAuthenticationType(User.AuthenticationType.LOCAL);
		return userRepository.save(user);
	}
}
