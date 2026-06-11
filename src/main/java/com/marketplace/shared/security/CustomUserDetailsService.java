package com.marketplace.shared.security;

import com.marketplace.user.model.User;
import com.marketplace.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
		User user = userRepository.findById(UUID.fromString(userId))
				.orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

		return new org.springframework.security.core.userdetails.User(
				user.getId().toString(),
				user.getPasswordHash(),
				user.isVerified(),
				true, true, true,
				java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
		);
	}
}
