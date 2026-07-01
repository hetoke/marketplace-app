package com.marketplace.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

	private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
	private final JwtProperties jwtProperties;
	private final SecretKey accessKey;
	private final SecretKey refreshKey;

	public JwtTokenProvider(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
		this.accessKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(
				java.util.Base64.getEncoder().encodeToString(jwtProperties.getAccessSecret().getBytes())));
		this.refreshKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(
				java.util.Base64.getEncoder().encodeToString(jwtProperties.getRefreshSecret().getBytes())));
	}

	public String generateAccessToken(String userId, String email, String role) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

		return Jwts.builder()
				.subject(userId)
				.claim("email", email)
				.claim("role", role)
				.issuedAt(now)
				.expiration(expiry)
				.signWith(accessKey)
				.compact();
	}

	public String generateRefreshToken(String userId) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

		return Jwts.builder()
				.subject(userId)
				.id(UUID.randomUUID().toString())
				.issuedAt(now)
				.expiration(expiry)
				.signWith(refreshKey)
				.compact();
	}

	public String getUserIdFromToken(String token, TokenType type) {
		return parseClaims(token, type).getSubject();
	}

	public boolean validateToken(String token, TokenType type) {
		SecretKey key = (type == TokenType.ACCESS) ? accessKey : refreshKey;
		try {
			Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
			return true;
		} catch (SecurityException e) {
			log.debug("Invalid JWT signature: {}", e.getMessage());
		} catch (MalformedJwtException e) {
			log.debug("Malformed JWT token: {}", e.getMessage());
		} catch (ExpiredJwtException e) {
			log.debug("Expired JWT token: {}", e.getMessage());
		} catch (UnsupportedJwtException e) {
			log.debug("Unsupported JWT token: {}", e.getMessage());
		} catch (IllegalArgumentException e) {
			log.debug("JWT claims string is empty: {}", e.getMessage());
		}
		return false;
	}

	private Claims parseClaims(String token, TokenType type) {
		SecretKey key = (type == TokenType.ACCESS) ? accessKey : refreshKey;
		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
	}
}
