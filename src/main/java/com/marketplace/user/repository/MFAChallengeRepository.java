package com.marketplace.user.repository;

import com.marketplace.user.model.MFAChallenge;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MFAChallengeRepository extends JpaRepository<MFAChallenge, UUID> {

    List<MFAChallenge> findByUserIdAndTypeOrderByCreatedAtDesc(UUID userId, MFAChallenge.ChallengeType type);

    @Modifying
    @Query("DELETE FROM MFAChallenge m WHERE m.user.id = :userId AND m.type = :type")
    void deleteByUserIdAndType(UUID userId, MFAChallenge.ChallengeType type);
}
