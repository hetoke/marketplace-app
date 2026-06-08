package com.marketplace.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role = Role.BUYER;

	@Column(name = "is_verified", nullable = false)
	private boolean verified = false;

	@Column(name = "mfa_enabled", nullable = false)
	private boolean mfaEnabled = false;

	@Column(name = "display_name")
	private String displayName;

	@Column(name = "profile_picture_url", length = 512)
	private String profilePictureUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "authentication_type", nullable = false, length = 20)
	private AuthenticationType authenticationType = AuthenticationType.LOCAL;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	public enum Role {
		BUYER, SELLER, ADMIN
	}

	public enum AuthenticationType {
		LOCAL, OIDC, HYBRID
	}

	public UUID getId() { return id; }
	public void setId(UUID id) { this.id = id; }

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public String getPasswordHash() { return passwordHash; }
	public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

	public Role getRole() { return role; }
	public void setRole(Role role) { this.role = role; }

	public boolean isVerified() { return verified; }
	public void setVerified(boolean verified) { this.verified = verified; }

	public boolean isMfaEnabled() { return mfaEnabled; }
	public void setMfaEnabled(boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }

	public String getDisplayName() { return displayName; }
	public void setDisplayName(String displayName) { this.displayName = displayName; }

	public String getProfilePictureUrl() { return profilePictureUrl; }
	public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }

	public AuthenticationType getAuthenticationType() { return authenticationType; }
	public void setAuthenticationType(AuthenticationType authenticationType) { this.authenticationType = authenticationType; }

	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

	public Instant getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
