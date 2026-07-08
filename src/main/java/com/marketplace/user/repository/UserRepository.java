package com.marketplace.user.repository;

import com.marketplace.user.model.User;
import com.marketplace.user.model.User.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	long countByRole(User.Role role);

	long countByStatus(UserStatus status);

	long countByCreatedAtBetween(Instant start, Instant end);

	long countByVerifiedTrue();

	@Query("SELECT FUNCTION('DATE', u.createdAt) as date, COUNT(u) as cnt FROM User u " +
	       "WHERE u.createdAt BETWEEN :start AND :end GROUP BY FUNCTION('DATE', u.createdAt) ORDER BY date")
	List<Object[]> countByCreatedAtBetweenGroupByDate(
	        @Param("start") Instant start, @Param("end") Instant end);

	@Query("SELECT u FROM User u WHERE " +
	       "(LOWER(u.displayName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR :query IS NULL) AND " +
	       "(u.role = :role OR :role IS NULL) AND " +
	       "(u.status = :status OR :status IS NULL)")
	Page<User> searchUsers(
	        @Param("query") String query,
	        @Param("role") User.Role role,
	        @Param("status") UserStatus status,
	        Pageable pageable);
}
