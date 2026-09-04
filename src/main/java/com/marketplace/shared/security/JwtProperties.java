package com.marketplace.shared.security;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

	private String accessSecret;
	private String refreshSecret;
	private long accessTokenExpiration;
	private long refreshTokenExpiration;

	@PostConstruct
	public void validate() {
		if (accessSecret == null || accessSecret.length() < 32) {
			throw new IllegalStateException(
					"app.jwt.access-secret must be at least 32 characters");
		}
		if (refreshSecret == null || refreshSecret.length() < 32) {
			throw new IllegalStateException(
					"app.jwt.refresh-secret must be at least 32 characters");
		}
	}

	public String getAccessSecret() { return accessSecret; }
	public void setAccessSecret(String accessSecret) { this.accessSecret = accessSecret; }

	public String getRefreshSecret() { return refreshSecret; }
	public void setRefreshSecret(String refreshSecret) { this.refreshSecret = refreshSecret; }

	public long getAccessTokenExpiration() { return accessTokenExpiration; }
	public void setAccessTokenExpiration(long accessTokenExpiration) { this.accessTokenExpiration = accessTokenExpiration; }

	public long getRefreshTokenExpiration() { return refreshTokenExpiration; }
	public void setRefreshTokenExpiration(long refreshTokenExpiration) { this.refreshTokenExpiration = refreshTokenExpiration; }
}
