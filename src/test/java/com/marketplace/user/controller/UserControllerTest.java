package com.marketplace.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.GlobalExceptionHandler;
import com.marketplace.shared.exception.ResourceNotFoundException;
import com.marketplace.user.dto.UserResponse;
import com.marketplace.user.model.User;
import com.marketplace.user.service.UserService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

	private MockMvc mockMvc;

	@Mock
	private UserService userService;

	@InjectMocks
	private UserController userController;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(userController)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();

		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("test-user-id", null, java.util.List.of()));
	}

	private UserResponse createTestUserResponse() {
		return new UserResponse(UUID.randomUUID().toString(), "test@test.com", "Test User", null,
				User.Role.BUYER, true, User.AuthenticationType.LOCAL, null);
	}

	// ==================== GET PROFILE ====================

	@Test
	void getProfile_success() throws Exception {
		UserResponse userResponse = createTestUserResponse();
		when(userService.getProfile("test-user-id")).thenReturn(userResponse);

		mockMvc.perform(get("/api/v1/users/profile"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.email").value("test@test.com"))
				.andExpect(jsonPath("$.data.displayName").value("Test User"));
	}

	@Test
	void getProfile_notFound_returns404() throws Exception {
		when(userService.getProfile("test-user-id"))
				.thenThrow(new ResourceNotFoundException("User", "id", "test-user-id"));

		mockMvc.perform(get("/api/v1/users/profile"))
				.andExpect(status().isNotFound());
	}

	@Test
	void getProfile_postMethodNotAllowed_returns405() throws Exception {
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
						.post("/api/v1/users/profile"))
				.andExpect(status().isMethodNotAllowed());
	}

	// ==================== UPDATE PROFILE ====================

	// --- DisplayName Range: min=1, max=255 ---

	@Test
	void updateProfile_success() throws Exception {
		UserResponse userResponse = new UserResponse(
				UUID.randomUUID().toString(), "test@test.com", "Updated Name", null,
				User.Role.BUYER, true, User.AuthenticationType.LOCAL, null);
		when(userService.updateProfile(anyString(), any())).thenReturn(userResponse);

		mockMvc.perform(put("/api/v1/users/profile")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"displayName\":\"Updated Name\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Profile updated"))
				.andExpect(jsonPath("$.data.displayName").value("Updated Name"));
	}

	@Test
	void updateProfile_displayNameLowerBound1char_success() throws Exception {
		UserResponse userResponse = new UserResponse(
				UUID.randomUUID().toString(), "test@test.com", "A", null,
				User.Role.BUYER, true, User.AuthenticationType.LOCAL, null);
		when(userService.updateProfile(anyString(), any())).thenReturn(userResponse);

		mockMvc.perform(put("/api/v1/users/profile")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"displayName\":\"A\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.displayName").value("A"));
	}

	@Test
	void updateProfile_displayNameUpperBound255chars_success() throws Exception {
		String name255 = "A".repeat(255);
		UserResponse userResponse = new UserResponse(
				UUID.randomUUID().toString(), "test@test.com", name255, null,
				User.Role.BUYER, true, User.AuthenticationType.LOCAL, null);
		when(userService.updateProfile(anyString(), any())).thenReturn(userResponse);

		mockMvc.perform(put("/api/v1/users/profile")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new UpdateBody(name255, null))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.displayName").value(name255));
	}

	@Test
	void updateProfile_displayNameBelowLowerBound0chars_returns400() throws Exception {
		mockMvc.perform(put("/api/v1/users/profile")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"displayName\":\"\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void updateProfile_displayNameAboveUpperBound256chars_returns400() throws Exception {
		String name256 = "A".repeat(256);

		mockMvc.perform(put("/api/v1/users/profile")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new UpdateBody(name256, null))))
				.andExpect(status().isBadRequest());
	}

	// --- ProfilePictureUrl Range: max=512 ---

	@Test
	void updateProfile_validProfilePictureUrl_success() throws Exception {
		UserResponse userResponse = new UserResponse(
				UUID.randomUUID().toString(), "test@test.com", "Test User", null,
				User.Role.BUYER, true, User.AuthenticationType.LOCAL, null);
		when(userService.updateProfile(anyString(), any())).thenReturn(userResponse);

		mockMvc.perform(put("/api/v1/users/profile")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"profilePictureUrl\":\"https://example.com/pic.jpg\"}"))
				.andExpect(status().isOk());
	}

	@Test
	void updateProfile_profilePictureUrlAboveUpperBound513chars_returns400() throws Exception {
		String url513 = "a".repeat(513);

		mockMvc.perform(put("/api/v1/users/profile")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new UpdateBody(null, url513))))
				.andExpect(status().isBadRequest());
	}

	// --- Both Fields Null / No Change ---

	@Test
	void updateProfile_bothFieldsNull_success() throws Exception {
		UserResponse userResponse = createTestUserResponse();
		when(userService.updateProfile(anyString(), any())).thenReturn(userResponse);

		mockMvc.perform(put("/api/v1/users/profile")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isOk());
	}

	// --- Other Edge Cases ---

	@Test
	void updateProfile_notFound_returns404() throws Exception {
		when(userService.updateProfile(anyString(), any()))
				.thenThrow(new ResourceNotFoundException("User", "id", "test-user-id"));

		mockMvc.perform(put("/api/v1/users/profile")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"displayName\":\"New Name\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void updateProfile_malformedJson_returns400() throws Exception {
		mockMvc.perform(put("/api/v1/users/profile")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{bad}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Malformed request body"));
	}

	@Test
	void updateProfile_postMethodNotAllowed_returns405() throws Exception {
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
						.post("/api/v1/users/profile"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.message").value("Method POST not allowed for /api/v1/users/profile"));
	}

	// ==================== CHANGE PASSWORD ====================

	// --- newPassword Range: min=8, max=128 ---

	@Test
	void changePassword_success() throws Exception {
		mockMvc.perform(put("/api/v1/users/profile/password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"currentPassword\":\"oldpassword\",\"newPassword\":\"newpassword123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Password changed successfully"));
	}

	@Test
	void changePassword_newPasswordLowerBound8chars_success() throws Exception {
		mockMvc.perform(put("/api/v1/users/profile/password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"currentPassword\":\"oldpassword\",\"newPassword\":\"abcdefgh\"}"))
				.andExpect(status().isOk());
	}

	@Test
	void changePassword_newPasswordUpperBound128chars_success() throws Exception {
		String newPass128 = "a".repeat(128);
		mockMvc.perform(put("/api/v1/users/profile/password")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new ChangePassBody("oldpassword", newPass128))))
				.andExpect(status().isOk());
	}

	@Test
	void changePassword_newPasswordBelowLowerBound7chars_returns400() throws Exception {
		mockMvc.perform(put("/api/v1/users/profile/password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"currentPassword\":\"oldpassword\",\"newPassword\":\"abcdefg\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("newPassword"));
	}

	@Test
	void changePassword_newPasswordAboveUpperBound129chars_returns400() throws Exception {
		String newPass129 = "a".repeat(129);
		mockMvc.perform(put("/api/v1/users/profile/password")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new ChangePassBody("oldpassword", newPass129))))
				.andExpect(status().isBadRequest());
	}

	// --- currentPassword Business Logic ---

	@Test
	void changePassword_wrongCurrentPassword_returns400() throws Exception {
		doThrow(new BusinessException("Current password is incorrect"))
				.when(userService).changePassword(anyString(), any());

		mockMvc.perform(put("/api/v1/users/profile/password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"currentPassword\":\"wrongpassword\",\"newPassword\":\"newpassword123\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Current password is incorrect"));
	}

	@Test
	void changePassword_notFound_returns404() throws Exception {
		doThrow(new ResourceNotFoundException("User", "id", "test-user-id"))
				.when(userService).changePassword(anyString(), any());

		mockMvc.perform(put("/api/v1/users/profile/password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"currentPassword\":\"oldpassword\",\"newPassword\":\"newpassword123\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void changePassword_emptyCurrentPassword_returns400() throws Exception {
		mockMvc.perform(put("/api/v1/users/profile/password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"currentPassword\":\"\",\"newPassword\":\"newpassword123\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void changePassword_emptyNewPassword_returns400() throws Exception {
		mockMvc.perform(put("/api/v1/users/profile/password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"currentPassword\":\"oldpassword\",\"newPassword\":\"\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void changePassword_missingBothPasswords_returns400() throws Exception {
		mockMvc.perform(put("/api/v1/users/profile/password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void changePassword_malformedJson_returns400() throws Exception {
		mockMvc.perform(put("/api/v1/users/profile/password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{bad}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Malformed request body"));
	}

	@Test
	void changePassword_getMethodNotAllowed_returns405() throws Exception {
		mockMvc.perform(get("/api/v1/users/profile/password"))
				.andExpect(status().isMethodNotAllowed());
	}

	private record UpdateBody(String displayName, String profilePictureUrl) {}
	private record ChangePassBody(String currentPassword, String newPassword) {}
}
