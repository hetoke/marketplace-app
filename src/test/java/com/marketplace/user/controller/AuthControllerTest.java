package com.marketplace.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.GlobalExceptionHandler;
import com.marketplace.user.dto.AuthResponse;
import com.marketplace.user.dto.TokenResponse;
import com.marketplace.user.dto.UserResponse;
import com.marketplace.user.model.User;
import com.marketplace.user.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

	private MockMvc mockMvc;

	@Mock
	private AuthService authService;

	@InjectMocks
	private AuthController authController;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(authController)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	private UserResponse createTestUserResponse() {
		return new UserResponse("uuid-123", "test@test.com", "Test User", null,
				User.Role.BUYER, false, null);
	}

	// ==================== REGISTER ====================

	// --- Role Selection: BUYER, SELLER, ADMIN ---

	@Test
	void register_roleBuyer_success() throws Exception {
		UserResponse userResponse = createTestUserResponse();
		when(authService.register(any())).thenReturn(userResponse);

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new RegisterBody("test@test.com", "password123", "Test User", "BUYER"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("Registration successful"))
				.andExpect(jsonPath("$.data.email").value("test@test.com"))
				.andExpect(jsonPath("$.data.displayName").value("Test User"));
	}

	@Test
	void register_roleSeller_success() throws Exception {
		UserResponse sellerResponse = new UserResponse("uuid-456", "seller@test.com", "Seller User", null,
				User.Role.SELLER, false, null);
		when(authService.register(any())).thenReturn(sellerResponse);

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new RegisterBody("seller@test.com", "password123", "Seller User", "SELLER"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.role").value("SELLER"));
	}

	@Test
	void register_roleAdmin_returns400() throws Exception {
		when(authService.register(any())).thenThrow(new BusinessException("Invalid role"));

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"admin@test.com\",\"password\":\"password123\",\"displayName\":\"Admin\",\"role\":\"ADMIN\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid role"));
	}

	// --- Password Range: min=8, max=128 ---

	@Test
	void register_passwordLowerBound8chars_success() throws Exception {
		UserResponse userResponse = createTestUserResponse();
		when(authService.register(any())).thenReturn(userResponse);

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"test@test.com\",\"password\":\"abcdefgh\",\"displayName\":\"Test\",\"role\":\"BUYER\"}"))
				.andExpect(status().isCreated());
	}

	@Test
	void register_passwordUpperBound128chars_success() throws Exception {
		UserResponse userResponse = createTestUserResponse();
		when(authService.register(any())).thenReturn(userResponse);

		String password128 = "a".repeat(128);
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new RegisterBody("test@test.com", password128, "Test", "BUYER"))))
				.andExpect(status().isCreated());
	}

	@Test
	void register_passwordBelowLowerBound7chars_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"test@test.com\",\"password\":\"abcdefg\",\"displayName\":\"Test\",\"role\":\"BUYER\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("password"));
	}

	@Test
	void register_passwordAboveUpperBound129chars_returns400() throws Exception {
		String password129 = "a".repeat(129);
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new RegisterBody("test@test.com", password129, "Test", "BUYER"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"));
	}

	@Test
	void register_emptyPassword_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"test@test.com\",\"password\":\"\",\"displayName\":\"Test\",\"role\":\"BUYER\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void register_blankPassword_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"test@test.com\",\"password\":\"   \",\"displayName\":\"Test\",\"role\":\"BUYER\"}"))
				.andExpect(status().isBadRequest());
	}

	// --- DisplayName Range: min=1, max=255 ---

	@Test
	void register_displayNameLowerBound1char_success() throws Exception {
		UserResponse userResponse = createTestUserResponse();
		when(authService.register(any())).thenReturn(userResponse);

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"test@test.com\",\"password\":\"password123\",\"displayName\":\"A\",\"role\":\"BUYER\"}"))
				.andExpect(status().isCreated());
	}

	@Test
	void register_displayNameUpperBound255chars_success() throws Exception {
		UserResponse userResponse = createTestUserResponse();
		when(authService.register(any())).thenReturn(userResponse);

		String name255 = "A".repeat(255);
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new RegisterBody("test@test.com", "password123", name255, "BUYER"))))
				.andExpect(status().isCreated());
	}

	@Test
	void register_displayNameBelowLowerBound0chars_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"test@test.com\",\"password\":\"password123\",\"displayName\":\"\",\"role\":\"BUYER\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void register_displayNameAboveUpperBound256chars_returns400() throws Exception {
		String name256 = "A".repeat(256);
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new RegisterBody("test@test.com", "password123", name256, "BUYER"))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void register_blankDisplayName_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"test@test.com\",\"password\":\"password123\",\"displayName\":\"   \",\"role\":\"BUYER\"}"))
				.andExpect(status().isBadRequest());
	}

	// --- Email Format ---

	@Test
	void register_duplicateEmail_returns400() throws Exception {
		when(authService.register(any())).thenThrow(new BusinessException("Email already registered"));

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new RegisterBody("dup@test.com", "password123", "Test", "BUYER"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Email already registered"));
	}

	@Test
	void register_invalidEmailNoAt_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"test.com\",\"password\":\"password123\",\"displayName\":\"Test\",\"role\":\"BUYER\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
	}

	@Test
	void register_invalidEmailNoDomain_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"test@\",\"password\":\"password123\",\"displayName\":\"Test\",\"role\":\"BUYER\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void register_missingEmail_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"password\":\"password123\",\"displayName\":\"Test\",\"role\":\"BUYER\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void register_emptyEmail_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"\",\"password\":\"password123\",\"displayName\":\"Test\",\"role\":\"BUYER\"}"))
				.andExpect(status().isBadRequest());
	}

	// --- Combined / Edge Cases ---

	@Test
	void register_malformedJson_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{invalid json}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Malformed request body"));
	}

	@Test
	void register_emptyBody_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void register_wrongContentType_returns415() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.TEXT_PLAIN)
						.content("some text"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.message").value("Expected Content-Type: application/json"));
	}

	@Test
	void register_getMethodNotAllowed_returns405() throws Exception {
		mockMvc.perform(get("/api/v1/auth/register"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.message").value("Method GET not allowed for /api/v1/auth/register"));
	}

	// ==================== LOGIN ====================

	@Test
	void login_success() throws Exception {
		AuthResponse authResponse = new AuthResponse("access-token", "refresh-token", createTestUserResponse());
		when(authService.login(any())).thenReturn(authResponse);

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"test@test.com\",\"password\":\"password123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").value("access-token"))
				.andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
	}

	@Test
	void login_invalidCredentials_returns400() throws Exception {
		when(authService.login(any())).thenThrow(new BusinessException("Invalid email or password"));

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"test@test.com\",\"password\":\"wrong\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid email or password"));
	}

	@Test
	void login_missingPassword_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"test@test.com\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void login_emptyEmail_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"\",\"password\":\"password123\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void login_emptyPassword_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"test@test.com\",\"password\":\"\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void login_malformedJson_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{bad}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Malformed request body"));
	}

	@Test
	void login_wrongContentType_returns415() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.TEXT_PLAIN)
						.content("some text"))
				.andExpect(status().isUnsupportedMediaType());
	}

	@Test
	void login_getMethodNotAllowed_returns405() throws Exception {
		mockMvc.perform(get("/api/v1/auth/login"))
				.andExpect(status().isMethodNotAllowed());
	}

	// ==================== REFRESH TOKEN ====================

	@Test
	void refreshToken_success() throws Exception {
		TokenResponse tokenResponse = new TokenResponse("access-token", "refresh-token");
		when(authService.refreshToken(any())).thenReturn(tokenResponse);

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"valid-refresh-token\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").value("access-token"))
				.andExpect(jsonPath("$.data.user").doesNotExist());
	}

	@Test
	void refreshToken_emptyToken_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"\"}"))
				.andExpect(status().isBadRequest());
	}

	// ==================== VERIFY EMAIL ====================

	@Test
	void verifyEmail_success() throws Exception {
		mockMvc.perform(post("/api/v1/auth/verify-email")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"valid-token\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Email verified successfully"));
	}

	@Test
	void verifyEmail_emptyToken_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/verify-email")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void verifyEmail_missingToken_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/verify-email")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());
	}

	// ==================== FORGOT PASSWORD ====================

	@Test
	void forgotPassword_success() throws Exception {
		mockMvc.perform(post("/api/v1/auth/forgot-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"test@test.com\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("If the email exists, a reset link has been sent"));
	}

	@Test
	void forgotPassword_invalidEmail_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/forgot-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"not-an-email\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void forgotPassword_emptyEmail_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/forgot-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void forgotPassword_missingEmail_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/forgot-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());
	}

	// ==================== RESET PASSWORD ====================

	@Test
	void resetPassword_success() throws Exception {
		mockMvc.perform(post("/api/v1/auth/reset-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"valid-token\",\"newPassword\":\"newpassword123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Password reset successfully"));
	}

	@Test
	void resetPassword_shortPassword_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/reset-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"valid-token\",\"newPassword\":\"short\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("newPassword"));
	}

	@Test
	void resetPassword_longPassword_returns400() throws Exception {
		String password129 = "a".repeat(129);
		mockMvc.perform(post("/api/v1/auth/reset-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new ResetBody("valid-token", password129))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void resetPassword_emptyToken_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/reset-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"\",\"newPassword\":\"newpassword123\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void resetPassword_missingToken_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/reset-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"newPassword\":\"newpassword123\"}"))
				.andExpect(status().isBadRequest());
	}

	private record RegisterBody(String email, String password, String displayName, String role) {}
	private record ResetBody(String token, String newPassword) {}
}
