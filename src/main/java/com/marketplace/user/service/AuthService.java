package com.marketplace.user.service;

import com.marketplace.shared.dto.ApiResponse;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.EmailVerificationRequiredException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import com.marketplace.shared.security.JwtTokenProvider;
import com.marketplace.user.dto.AuthResponse;
import com.marketplace.user.dto.ForgotPasswordRequest;
import com.marketplace.user.dto.LoginRequest;
import com.marketplace.user.dto.RefreshTokenRequest;
import com.marketplace.user.dto.RegisterRequest;
import com.marketplace.user.dto.ResetPasswordRequest;
import com.marketplace.user.dto.TokenResponse;
import com.marketplace.user.dto.UserResponse;
import com.marketplace.user.dto.ResendVerificationTokenRequest;
import com.marketplace.user.dto.VerifyEmailRequest;
import com.marketplace.user.model.RefreshToken;
import com.marketplace.user.model.User;
import com.marketplace.user.model.VerificationToken;
import com.marketplace.user.repository.RefreshTokenRepository;
import com.marketplace.user.repository.UserRepository;
import com.marketplace.user.repository.VerificationTokenRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);
	private static final Set<User.Role> ALLOWED_REGISTRATION_ROLES = Set.of(User.Role.BUYER, User.Role.SELLER);

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final VerificationTokenRepository verificationTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final MFAService mfaService;

	public AuthService(UserRepository userRepository,
					   RefreshTokenRepository refreshTokenRepository,
					   VerificationTokenRepository verificationTokenRepository,
					   PasswordEncoder passwordEncoder,
					   JwtTokenProvider jwtTokenProvider,
					   MFAService mfaService) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.verificationTokenRepository = verificationTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
		this.mfaService = mfaService;
	}

	@Transactional
	public UserResponse register(RegisterRequest request) {
		if (!ALLOWED_REGISTRATION_ROLES.contains(request.role())) {
			throw new BusinessException("Invalid role");
		}

		if (userRepository.existsByEmail(request.email())) {
			throw new BusinessException("Email already registered");
		}

		User user = new User();
		user.setEmail(request.email());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setDisplayName(request.displayName());
		user.setRole(request.role());
		user.setVerified(false);
		user.setAuthenticationType(User.AuthenticationType.LOCAL);
		userRepository.save(user);

		String verificationToken = createVerificationToken(user);
		log.info("Email verification token for {}: {}", user.getEmail(), verificationToken);

		return UserResponse.from(user);
	}

	@Transactional
	public AuthResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.email())
				.orElseThrow(() -> new BusinessException("Invalid email or password"));

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new BusinessException("Invalid email or password");
		}

		if (!user.isVerified()) {
			throw new EmailVerificationRequiredException("Please verify your email before logging in");
		}

		user.setUpdatedAt(Instant.now());
		userRepository.save(user);

		if (user.isMfaEnabled()) {
			String mfaToken = jwtTokenProvider.generateMfaToken(user.getId().toString());
			mfaService.sendLoginOTP(user);
			return new AuthResponse(null, null, null, true, mfaToken);
		}

		return generateTokenPair(user);
	}

	@Transactional
	public TokenResponse refreshToken(RefreshTokenRequest request) {
		String tokenPrefix = request.refreshToken().length() > 8
				? request.refreshToken().substring(0, 8) + "..."
				: request.refreshToken();
		log.info("Refresh attempt for token prefix: {}", tokenPrefix);

		RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
				.orElseThrow(() -> {
					log.warn("Refresh token not found in DB (prefix: {})", tokenPrefix);
					return new BusinessException("Invalid refresh token");
				});

		if (refreshToken.isExpired()) {
			log.warn("Refresh token expired (prefix: {}, expiresAt: {})", tokenPrefix, refreshToken.getExpiresAt());
			refreshTokenRepository.delete(refreshToken);
			throw new BusinessException("Refresh token expired");
		}

		User user = refreshToken.getUser();
		refreshTokenRepository.delete(refreshToken);

		String accessToken = jwtTokenProvider.generateAccessToken(
				user.getId().toString(), user.getEmail(), user.getRole().name());
		String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId().toString());

		RefreshToken rotated = new RefreshToken(user, newRefreshToken,
				Instant.now().plus(7, ChronoUnit.DAYS));
		refreshTokenRepository.save(rotated);

		log.info("Refresh successful for user {} (prefix: {})", user.getId(), tokenPrefix);
		return new TokenResponse(accessToken, newRefreshToken);
	}

	@Transactional
	public void logout(String userId) {
		refreshTokenRepository.deleteByUserId(UUID.fromString(userId));
	}

	@Transactional
	public void verifyEmail(VerifyEmailRequest request) {
		VerificationToken token = verificationTokenRepository.findByToken(request.token())
				.orElseThrow(() -> new BusinessException("Invalid verification token"));

		if (token.isExpired()) {
			verificationTokenRepository.delete(token);
			throw new BusinessException("Verification token expired");
		}

		User user = token.getUser();
		user.setVerified(true);
		user.setUpdatedAt(Instant.now());
		userRepository.save(user);
		verificationTokenRepository.delete(token);
	}

	@Transactional
	public void resendVerificationToken(ResendVerificationTokenRequest request) {
		User user = userRepository.findByEmail(request.email()).orElse(null);
		if (user == null || user.isVerified()) {
			return;
		}

		verificationTokenRepository.deleteByUserId(user.getId());

		String verificationToken = createVerificationToken(user);
		log.info("Resent email verification token for {}: {}", user.getEmail(), verificationToken);
	}

	@Transactional
	public void forgotPassword(ForgotPasswordRequest request) {
		User user = userRepository.findByEmail(request.email()).orElse(null);
		if (user == null) {
			return;
		}

		String resetToken = UUID.randomUUID().toString();
		VerificationToken vToken = new VerificationToken(user, resetToken, Instant.now().plus(1, ChronoUnit.HOURS));
		verificationTokenRepository.save(vToken);

		log.info("Password reset token for {}: {}", user.getEmail(), resetToken);
	}

	@Transactional
	public void resetPassword(ResetPasswordRequest request) {
		VerificationToken token = verificationTokenRepository.findByToken(request.token())
				.orElseThrow(() -> new BusinessException("Invalid reset token"));

		if (token.isExpired()) {
			verificationTokenRepository.delete(token);
			throw new BusinessException("Reset token expired");
		}

		User user = token.getUser();
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		user.setUpdatedAt(Instant.now());
		userRepository.save(user);
		verificationTokenRepository.delete(token);
	}

	private AuthResponse generateTokenPair(User user) {
		String accessToken = jwtTokenProvider.generateAccessToken(
				user.getId().toString(), user.getEmail(), user.getRole().name());
		String refreshTokenValue = jwtTokenProvider.generateRefreshToken(user.getId().toString());

		RefreshToken refreshToken = new RefreshToken(user, refreshTokenValue,
				Instant.now().plus(7, ChronoUnit.DAYS));
		refreshTokenRepository.save(refreshToken);

		return new AuthResponse(accessToken, refreshTokenValue, UserResponse.from(user));
	}

	private String createVerificationToken(User user) {
		String token = UUID.randomUUID().toString();
		VerificationToken verificationToken = new VerificationToken(
				user, token, Instant.now().plus(24, ChronoUnit.HOURS));
		verificationTokenRepository.save(verificationToken);
		return token;
	}
}
