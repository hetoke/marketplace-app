package com.marketplace.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketplace.email.EmailService;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.EmailVerificationRequiredException;
import com.marketplace.shared.security.JwtTokenProvider;
import com.marketplace.user.dto.AuthResponse;
import com.marketplace.user.dto.LoginRequest;
import com.marketplace.user.dto.RefreshTokenRequest;
import com.marketplace.user.dto.TokenResponse;
import com.marketplace.user.dto.RegisterRequest;
import com.marketplace.user.dto.ResetPasswordRequest;
import com.marketplace.user.dto.UserResponse;
import com.marketplace.user.dto.VerifyEmailRequest;
import com.marketplace.user.model.RefreshToken;
import com.marketplace.user.model.User;
import com.marketplace.user.model.VerificationToken;
import com.marketplace.user.repository.RefreshTokenRepository;
import com.marketplace.user.repository.UserRepository;
import com.marketplace.user.repository.VerificationTokenRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private VerificationTokenRepository verificationTokenRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private EmailService emailService;

	@InjectMocks
	private AuthService authService;

	// ==================== REGISTER ====================

	// --- Role Selection: BUYER, SELLER, ADMIN ---

	@Test
	void register_roleBuyer_success() {
		RegisterRequest request = new RegisterRequest("buyer@test.com", "password123", "Buyer User", User.Role.BUYER);

		when(userRepository.existsByEmail("buyer@test.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
		when(userRepository.save(any(User.class))).thenAnswer(inv -> {
			User u = inv.getArgument(0);
			u.setId(UUID.randomUUID());
			return u;
		});

		UserResponse response = authService.register(request);

		assertThat(response.email()).isEqualTo("buyer@test.com");
		assertThat(response.displayName()).isEqualTo("Buyer User");
		assertThat(response.role()).isEqualTo(User.Role.BUYER);
		assertThat(response.isVerified()).isFalse();
		verify(userRepository).save(any(User.class));
		verify(verificationTokenRepository).save(any(VerificationToken.class));
	}

	@Test
	void register_roleSeller_success() {
		RegisterRequest request = new RegisterRequest("seller@test.com", "password123", "Seller User", User.Role.SELLER);

		when(userRepository.existsByEmail("seller@test.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
		when(userRepository.save(any(User.class))).thenAnswer(inv -> {
			User u = inv.getArgument(0);
			u.setId(UUID.randomUUID());
			return u;
		});

		UserResponse response = authService.register(request);

		assertThat(response.email()).isEqualTo("seller@test.com");
		assertThat(response.role()).isEqualTo(User.Role.SELLER);
		verify(userRepository).save(any(User.class));
	}

	@Test
	void register_roleAdmin_throwsBusinessException() {
		RegisterRequest request = new RegisterRequest("admin@test.com", "password123", "Admin User", User.Role.ADMIN);

		assertThatThrownBy(() -> authService.register(request))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Invalid role");

		verify(userRepository, never()).save(any());
	}

	// --- Password Range: positive on-bound ---

	@Test
	void register_passwordLowerBound8chars_success() {
		RegisterRequest request = new RegisterRequest("test@test.com", "abcdefgh", "Test User", User.Role.BUYER);

		when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
		when(passwordEncoder.encode("abcdefgh")).thenReturn("encoded_password");
		when(userRepository.save(any(User.class))).thenAnswer(inv -> {
			User u = inv.getArgument(0);
			u.setId(UUID.randomUUID());
			return u;
		});

		UserResponse response = authService.register(request);

		assertThat(response.email()).isEqualTo("test@test.com");
		verify(userRepository).save(any(User.class));
	}

	@Test
	void register_passwordUpperBound128chars_success() {
		String password128 = "a".repeat(128);
		RegisterRequest request = new RegisterRequest("test@test.com", password128, "Test User", User.Role.BUYER);

		when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
		when(passwordEncoder.encode(password128)).thenReturn("encoded_password");
		when(userRepository.save(any(User.class))).thenAnswer(inv -> {
			User u = inv.getArgument(0);
			u.setId(UUID.randomUUID());
			return u;
		});

		UserResponse response = authService.register(request);

		assertThat(response.email()).isEqualTo("test@test.com");
		verify(userRepository).save(any(User.class));
	}

	// --- DisplayName Range: positive on-bound ---

	@Test
	void register_displayNameLowerBound1char_success() {
		RegisterRequest request = new RegisterRequest("test@test.com", "password123", "A", User.Role.BUYER);

		when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
		when(userRepository.save(any(User.class))).thenAnswer(inv -> {
			User u = inv.getArgument(0);
			u.setId(UUID.randomUUID());
			return u;
		});

		UserResponse response = authService.register(request);

		assertThat(response.displayName()).isEqualTo("A");
		verify(userRepository).save(any(User.class));
	}

	@Test
	void register_displayNameUpperBound255chars_success() {
		String name255 = "A".repeat(255);
		RegisterRequest request = new RegisterRequest("test@test.com", "password123", name255, User.Role.BUYER);

		when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
		when(userRepository.save(any(User.class))).thenAnswer(inv -> {
			User u = inv.getArgument(0);
			u.setId(UUID.randomUUID());
			return u;
		});

		UserResponse response = authService.register(request);

		assertThat(response.displayName()).isEqualTo(name255);
		verify(userRepository).save(any(User.class));
	}

	// --- Email: duplicate check ---

	@Test
	void register_duplicateEmail_throwsBusinessException() {
		RegisterRequest request = new RegisterRequest("test@test.com", "password123", "Test User", User.Role.BUYER);
		when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(request))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Email already registered");

		verify(userRepository, never()).save(any());
	}

	// ==================== LOGIN ====================

	@Test
	void login_success() {
		LoginRequest request = new LoginRequest("test@test.com", "password123");

		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail("test@test.com");
		user.setPasswordHash("encoded_password");
		user.setRole(User.Role.BUYER);
		user.setVerified(true);

		when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
		when(jwtTokenProvider.generateAccessToken(anyString(), anyString(), anyString())).thenReturn("access-token");
		when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("refresh-token");

		AuthResponse response = authService.login(request);

		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(response.refreshToken()).isEqualTo("refresh-token");
		assertThat(response.user().email()).isEqualTo("test@test.com");

		verify(refreshTokenRepository).save(any(RefreshToken.class));
	}

	@Test
	void login_wrongPassword_throwsBusinessException() {
		LoginRequest request = new LoginRequest("test@test.com", "wrongpassword");

		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail("test@test.com");
		user.setPasswordHash("encoded_password");

		when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrongpassword", "encoded_password")).thenReturn(false);

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Invalid email or password");

		verify(refreshTokenRepository, never()).save(any());
	}

	@Test
	void login_userNotFound_throwsBusinessException() {
		LoginRequest request = new LoginRequest("nonexistent@test.com", "password123");
		when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Invalid email or password");
	}

	@Test
	void login_unverifiedEmail_throwsBusinessException() {
		LoginRequest request = new LoginRequest("test@test.com", "password123");

		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail("test@test.com");
		user.setPasswordHash("encoded_password");
		user.setVerified(false);

		when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(EmailVerificationRequiredException.class)
				.hasMessage("Please verify your email before logging in");

		verify(refreshTokenRepository, never()).save(any());
	}

	// ==================== REFRESH TOKEN ====================

	@Test
	void refreshToken_success() {
		RefreshTokenRequest request = new RefreshTokenRequest("old-refresh-token");

		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail("test@test.com");
		user.setRole(User.Role.BUYER);
		user.setVerified(true);

		RefreshToken refreshToken = new RefreshToken(user, "old-refresh-token",
				Instant.now().plus(1, ChronoUnit.DAYS));

		when(refreshTokenRepository.findByToken("old-refresh-token")).thenReturn(Optional.of(refreshToken));
		when(jwtTokenProvider.generateAccessToken(anyString(), anyString(), anyString())).thenReturn("new-access");
		when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("new-refresh");

		TokenResponse response = authService.refreshToken(request);

		assertThat(response.accessToken()).isEqualTo("new-access");
		assertThat(response.refreshToken()).isEqualTo("new-refresh");

		verify(refreshTokenRepository).delete(refreshToken);
		verify(refreshTokenRepository).save(any(RefreshToken.class));
	}

	@Test
	void refreshToken_expired_throwsBusinessException() {
		RefreshTokenRequest request = new RefreshTokenRequest("expired-token");

		User user = new User();
		user.setId(UUID.randomUUID());

		RefreshToken refreshToken = new RefreshToken(user, "expired-token",
				Instant.now().minus(1, ChronoUnit.DAYS));

		when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(refreshToken));

		assertThatThrownBy(() -> authService.refreshToken(request))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Refresh token expired");

		verify(refreshTokenRepository).delete(refreshToken);
		verify(refreshTokenRepository, never()).save(any());
	}

	@Test
	void refreshToken_invalidToken_throwsBusinessException() {
		RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");
		when(refreshTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.refreshToken(request))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Invalid refresh token");
	}

	// ==================== VERIFY EMAIL ====================

	@Test
	void verifyEmail_success() {
		VerifyEmailRequest request = new VerifyEmailRequest("valid-token");

		User user = new User();
		user.setId(UUID.randomUUID());
		user.setVerified(false);

		VerificationToken token = new VerificationToken(user, "valid-token",
				Instant.now().plus(1, ChronoUnit.HOURS));

		when(verificationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

		authService.verifyEmail(request);

		assertThat(user.isVerified()).isTrue();
		verify(userRepository).save(user);
		verify(verificationTokenRepository).delete(token);
	}

	@Test
	void verifyEmail_expiredToken_throwsBusinessException() {
		VerifyEmailRequest request = new VerifyEmailRequest("expired-token");

		User user = new User();
		user.setId(UUID.randomUUID());

		VerificationToken token = new VerificationToken(user, "expired-token",
				Instant.now().minus(1, ChronoUnit.HOURS));

		when(verificationTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

		assertThatThrownBy(() -> authService.verifyEmail(request))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Verification token expired");

		verify(verificationTokenRepository).delete(token);
	}

	@Test
	void verifyEmail_invalidToken_throwsBusinessException() {
		VerifyEmailRequest request = new VerifyEmailRequest("invalid-token");
		when(verificationTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.verifyEmail(request))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Invalid verification token");
	}

	// ==================== FORGOT PASSWORD ====================

	@Test
	void forgotPassword_existingEmail_success() {
		com.marketplace.user.dto.ForgotPasswordRequest request =
				new com.marketplace.user.dto.ForgotPasswordRequest("test@test.com");

		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail("test@test.com");

		when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

		authService.forgotPassword(request);

		verify(verificationTokenRepository).save(any(VerificationToken.class));
	}

	@Test
	void forgotPassword_nonExistingEmail_success() {
		com.marketplace.user.dto.ForgotPasswordRequest request =
				new com.marketplace.user.dto.ForgotPasswordRequest("nonexistent@test.com");

		when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

		authService.forgotPassword(request);

		verify(verificationTokenRepository, never()).save(any());
	}

	// ==================== RESET PASSWORD ====================

	@Test
	void resetPassword_success() {
		ResetPasswordRequest request = new ResetPasswordRequest("valid-reset-token", "newpassword");

		User user = new User();
		user.setId(UUID.randomUUID());
		user.setPasswordHash("old_encoded_password");

		VerificationToken token = new VerificationToken(user, "valid-reset-token",
				Instant.now().plus(1, ChronoUnit.HOURS));

		when(verificationTokenRepository.findByToken("valid-reset-token")).thenReturn(Optional.of(token));
		when(passwordEncoder.encode("newpassword")).thenReturn("new_encoded_password");

		authService.resetPassword(request);

		assertThat(user.getPasswordHash()).isEqualTo("new_encoded_password");
		verify(userRepository).save(user);
		verify(verificationTokenRepository).delete(token);
	}

	@Test
	void resetPassword_expiredToken_throwsBusinessException() {
		ResetPasswordRequest request = new ResetPasswordRequest("expired-token", "newpassword");

		User user = new User();
		user.setId(UUID.randomUUID());

		VerificationToken token = new VerificationToken(user, "expired-token",
				Instant.now().minus(1, ChronoUnit.HOURS));

		when(verificationTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

		assertThatThrownBy(() -> authService.resetPassword(request))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Reset token expired");

		verify(verificationTokenRepository).delete(token);
	}

	@Test
	void resetPassword_invalidToken_throwsBusinessException() {
		ResetPasswordRequest request = new ResetPasswordRequest("invalid-token", "newpassword");
		when(verificationTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.resetPassword(request))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Invalid reset token");
	}
}
