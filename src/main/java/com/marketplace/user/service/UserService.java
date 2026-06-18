package com.marketplace.user.service;

import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import com.marketplace.user.dto.ChangePasswordRequest;
import com.marketplace.user.dto.UpdateProfileRequest;
import com.marketplace.user.dto.UserResponse;
import com.marketplace.user.model.User;
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

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public UserResponse getProfile(String userId) {
		User user = userRepository.findById(UUID.fromString(userId))
				.orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
		return UserResponse.from(user);
	}

	@Transactional
	public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
		User user = userRepository.findById(UUID.fromString(userId))
				.orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

		if (request.displayName() != null) {
			user.setDisplayName(request.displayName());
		}
		user.setUpdatedAt(Instant.now());
		userRepository.save(user);

		return UserResponse.from(user);
	}

	@Transactional
	public void changePassword(String userId, ChangePasswordRequest request) {
		User user = userRepository.findById(UUID.fromString(userId))
				.orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new BusinessException("Current password is incorrect");
		}

		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		user.setUpdatedAt(Instant.now());
		userRepository.save(user);
	}
}
