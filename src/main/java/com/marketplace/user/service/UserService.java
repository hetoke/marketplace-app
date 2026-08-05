package com.marketplace.user.service;

import com.marketplace.email.EmailService;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import com.marketplace.user.dto.ChangePasswordRequest;
import com.marketplace.user.dto.SellerResponse;
import com.marketplace.user.dto.UpdateProfileRequest;
import com.marketplace.user.dto.UserResponse;
import com.marketplace.user.model.User;
import com.marketplace.user.repository.RefreshTokenRepository;
import com.marketplace.user.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenRepository refreshTokenRepository;
	private final EmailService emailService;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
			RefreshTokenRepository refreshTokenRepository, EmailService emailService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.refreshTokenRepository = refreshTokenRepository;
		this.emailService = emailService;
	}

	public UserResponse getProfile(String userId) {
		User user = userRepository.findById(UUID.fromString(userId))
				.orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
		return UserResponse.from(user);
	}

	public SellerResponse getSellerProfile(String sellerId) {
		User user = userRepository.findById(UUID.fromString(sellerId))
				.orElseThrow(() -> new ResourceNotFoundException("Seller", "id", sellerId));
		return SellerResponse.from(user);
	}

	@Transactional
	public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
		User user = userRepository.findById(UUID.fromString(userId))
				.orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

		if (request.displayName() != null) {
			user.setDisplayName(request.displayName());
		}
		if (request.defaultStreet() != null) {
			user.setDefaultStreet(request.defaultStreet());
		}
		if (request.defaultProvince() != null) {
			user.setDefaultProvince(request.defaultProvince());
		}
		if (request.defaultDistrict() != null) {
			user.setDefaultDistrict(request.defaultDistrict());
		}
		if (request.defaultWard() != null) {
			user.setDefaultWard(request.defaultWard());
		}
		user.setUpdatedAt(Instant.now());
		userRepository.save(user);

		return UserResponse.from(user);
	}

	@Transactional
	public void changePassword(String userId, ChangePasswordRequest request) {
		User user = userRepository.findById(UUID.fromString(userId))
				.orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

		if (user.getAuthenticationType() == User.AuthenticationType.OIDC) {
			throw new BusinessException("Password change is not available for Google sign-in accounts");
		}

		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new BusinessException("Current password is incorrect");
		}

		if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
			throw new BusinessException("New password must be different from current password");
		}

		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		user.setUpdatedAt(Instant.now());
		userRepository.save(user);

		refreshTokenRepository.deleteByUserId(user.getId());
		emailService.sendPasswordChangeNotification(user.getEmail());
	}
}
