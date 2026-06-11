package com.marketplace.user.repository;

import com.marketplace.user.model.VerificationToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

	Optional<VerificationToken> findByToken(String token);

	void deleteByUserId(UUID userId);
}
