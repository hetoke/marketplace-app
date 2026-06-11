package com.marketplace.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import com.marketplace.user.dto.ChangePasswordRequest;
import com.marketplace.user.dto.UpdateProfileRequest;
import com.marketplace.user.dto.UserResponse;
import com.marketplace.user.model.User;
import com.marketplace.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private UserService userService;

	private User createTestUser() {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail("test@test.com");
		user.setPasswordHash("encoded_password");
		user.setDisplayName("Test User");
		user.setRole(User.Role.BUYER);
		user.setVerified(true);
		user.setCreatedAt(Instant.now());
		user.setUpdatedAt(Instant.now());
		return user;
	}

	// ==================== GET PROFILE ====================

	@Test
	void getProfile_success() {
		User user = createTestUser();
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

		UserResponse response = userService.getProfile(user.getId().toString());

		assertThat(response.id()).isEqualTo(user.getId().toString());
		assertThat(response.email()).isEqualTo("test@test.com");
		assertThat(response.displayName()).isEqualTo("Test User");
		assertThat(response.role()).isEqualTo(User.Role.BUYER);
		assertThat(response.isVerified()).isTrue();
	}

	@Test
	void getProfile_notFound_throwsResourceNotFoundException() {
		UUID randomId = UUID.randomUUID();
		when(userRepository.findById(randomId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getProfile(randomId.toString()))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("User")
				.hasMessageContaining(randomId.toString());
	}

	// ==================== UPDATE PROFILE ====================

	// --- DisplayName Range: positive on-bound ---

	@Test
	void updateProfile_displayNameLowerBound1char_success() {
		User user = createTestUser();
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		UpdateProfileRequest request = new UpdateProfileRequest("A", null);

		UserResponse response = userService.updateProfile(user.getId().toString(), request);

		assertThat(response.displayName()).isEqualTo("A");
		verify(userRepository).save(user);
	}

	@Test
	void updateProfile_displayNameUpperBound255chars_success() {
		User user = createTestUser();
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		String name255 = "A".repeat(255);
		UpdateProfileRequest request = new UpdateProfileRequest(name255, null);

		UserResponse response = userService.updateProfile(user.getId().toString(), request);

		assertThat(response.displayName()).isEqualTo(name255);
		verify(userRepository).save(user);
	}

	// --- ProfilePictureUrl: positive ---

	@Test
	void updateProfile_validProfilePictureUrl_success() {
		User user = createTestUser();
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		UpdateProfileRequest request = new UpdateProfileRequest(null, "https://example.com/pic.jpg");

		UserResponse response = userService.updateProfile(user.getId().toString(), request);

		verify(userRepository).save(user);
	}

	// --- Both Fields Null / No Change ---

	@Test
	void updateProfile_bothFieldsNull_success() {
		User user = createTestUser();
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		UpdateProfileRequest request = new UpdateProfileRequest(null, null);

		UserResponse response = userService.updateProfile(user.getId().toString(), request);

		assertThat(response.displayName()).isEqualTo("Test User");
		verify(userRepository).save(user);
	}

	@Test
	void updateProfile_notFound_throwsResourceNotFoundException() {
		UUID randomId = UUID.randomUUID();
		when(userRepository.findById(randomId)).thenReturn(Optional.empty());

		UpdateProfileRequest request = new UpdateProfileRequest("New Name", null);

		assertThatThrownBy(() -> userService.updateProfile(randomId.toString(), request))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	// ==================== CHANGE PASSWORD ====================

	// --- newPassword Range: positive on-bound ---

	@Test
	void changePassword_success() {
		User user = createTestUser();
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("oldpassword", "encoded_password")).thenReturn(true);
		when(passwordEncoder.encode("newpassword")).thenReturn("new_encoded_password");
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		ChangePasswordRequest request = new ChangePasswordRequest("oldpassword", "newpassword");

		userService.changePassword(user.getId().toString(), request);

		assertThat(user.getPasswordHash()).isEqualTo("new_encoded_password");
		verify(userRepository).save(user);
	}

	@Test
	void changePassword_newPasswordLowerBound8chars_success() {
		User user = createTestUser();
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("oldpassword", "encoded_password")).thenReturn(true);
		when(passwordEncoder.encode("abcdefgh")).thenReturn("new_encoded_password");
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		ChangePasswordRequest request = new ChangePasswordRequest("oldpassword", "abcdefgh");

		userService.changePassword(user.getId().toString(), request);

		assertThat(user.getPasswordHash()).isEqualTo("new_encoded_password");
		verify(userRepository).save(user);
	}

	@Test
	void changePassword_newPasswordUpperBound128chars_success() {
		User user = createTestUser();
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("oldpassword", "encoded_password")).thenReturn(true);
		String newPass128 = "a".repeat(128);
		when(passwordEncoder.encode(newPass128)).thenReturn("new_encoded_password");
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		ChangePasswordRequest request = new ChangePasswordRequest("oldpassword", newPass128);

		userService.changePassword(user.getId().toString(), request);

		assertThat(user.getPasswordHash()).isEqualTo("new_encoded_password");
		verify(userRepository).save(user);
	}

	// --- currentPassword Business Logic ---

	@Test
	void changePassword_wrongCurrentPassword_throwsBusinessException() {
		User user = createTestUser();
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrongpassword", "encoded_password")).thenReturn(false);

		ChangePasswordRequest request = new ChangePasswordRequest("wrongpassword", "newpassword");

		assertThatThrownBy(() -> userService.changePassword(user.getId().toString(), request))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Current password is incorrect");
	}

	@Test
	void changePassword_notFound_throwsResourceNotFoundException() {
		UUID randomId = UUID.randomUUID();
		when(userRepository.findById(randomId)).thenReturn(Optional.empty());

		ChangePasswordRequest request = new ChangePasswordRequest("oldpassword", "newpassword");

		assertThatThrownBy(() -> userService.changePassword(randomId.toString(), request))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	// ==================== USER MODEL - BINARY VARIABLES ====================

	@Test
	void user_verifiedTrue_returnsTrue() {
		User user = createTestUser();
		user.setVerified(true);

		assertThat(user.isVerified()).isTrue();
	}

	@Test
	void user_verifiedFalse_returnsFalse() {
		User user = createTestUser();
		user.setVerified(false);

		assertThat(user.isVerified()).isFalse();
	}

	@Test
	void user_mfaEnabledTrue_returnsTrue() {
		User user = createTestUser();
		user.setMfaEnabled(true);

		assertThat(user.isMfaEnabled()).isTrue();
	}

	@Test
	void user_mfaEnabledFalse_returnsFalse() {
		User user = createTestUser();
		user.setMfaEnabled(false);

		assertThat(user.isMfaEnabled()).isFalse();
	}

	// ==================== USER MODEL - SELECTION VARIABLES ====================

	@Test
	void user_roleBuyer() {
		User user = createTestUser();
		user.setRole(User.Role.BUYER);

		assertThat(user.getRole()).isEqualTo(User.Role.BUYER);
	}

	@Test
	void user_roleSeller() {
		User user = createTestUser();
		user.setRole(User.Role.SELLER);

		assertThat(user.getRole()).isEqualTo(User.Role.SELLER);
	}

	@Test
	void user_roleAdmin() {
		User user = createTestUser();
		user.setRole(User.Role.ADMIN);

		assertThat(user.getRole()).isEqualTo(User.Role.ADMIN);
	}

	@Test
	void user_authenticationTypeLocal() {
		User user = createTestUser();
		user.setAuthenticationType(User.AuthenticationType.LOCAL);

		assertThat(user.getAuthenticationType()).isEqualTo(User.AuthenticationType.LOCAL);
	}

	@Test
	void user_authenticationTypeOidc() {
		User user = createTestUser();
		user.setAuthenticationType(User.AuthenticationType.OIDC);

		assertThat(user.getAuthenticationType()).isEqualTo(User.AuthenticationType.OIDC);
	}

	@Test
	void user_authenticationTypeHybrid() {
		User user = createTestUser();
		user.setAuthenticationType(User.AuthenticationType.HYBRID);

		assertThat(user.getAuthenticationType()).isEqualTo(User.AuthenticationType.HYBRID);
	}
}
