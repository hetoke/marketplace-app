package com.marketplace.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

	private String accessSecret;
	private String refreshSecret;
	private long accessTokenExpiration;
	private long refreshTokenExpiration;

	public String getAccessSecret() { return accessSecret; }
	public void setAccessSecret(String accessSecret) { this.accessSecret = accessSecret; }

	public String getRefreshSecret() { return refreshSecret; }
	public void setRefreshSecret(String refreshSecret) { this.refreshSecret = refreshSecret; }

	public long getAccessTokenExpiration() { return accessTokenExpiration; }
	public void setAccessTokenExpiration(long accessTokenExpiration) { this.accessTokenExpiration = accessTokenExpiration; }

	public long getRefreshTokenExpiration() { return refreshTokenExpiration; }
	public void setRefreshTokenExpiration(long refreshTokenExpiration) { this.refreshTokenExpiration = refreshTokenExpiration; }
}
