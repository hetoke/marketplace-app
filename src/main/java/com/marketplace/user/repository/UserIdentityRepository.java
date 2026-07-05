package com.marketplace.user.repository;

import com.marketplace.user.model.UserIdentity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {

    Optional<UserIdentity> findByProviderAndProviderUserId(UserIdentity.Provider provider, String providerUserId);

    List<UserIdentity> findByUserId(UUID userId);
}
